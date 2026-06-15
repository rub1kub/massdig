package org.main.massdig.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.AABB;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public final class AutoDigController {
    private static final int MAX_PLAN_BLOCKS = 12000;
    private static final int MAX_RENDER_BLOCKS = 1800;
    private static final int PLAN_COLOR = 0x4439F56B;
    private static final int BOX_COLOR = 0x66FFFFFF;
    private static final int ACTIVE_COLOR = 0x77FF2E63;
    private static final int BLOCKED_COLOR = 0x66A855F7;
    private static final int PROTECTED_COLOR = 0x66FF9F1C;

    private static KeyMapping openAutoKey;
    private static KeyMapping pointAKey;
    private static KeyMapping pointBKey;
    private static KeyMapping startPauseKey;
    private static KeyMapping stopKey;

    private static BlockPos pointA;
    private static BlockPos pointB;
    private static final List<BlockPos> plan = new ArrayList<>();
    private static final Set<BlockPos> plannedSet = new HashSet<>();
    private static final Set<BlockPos> protectedSet = new HashSet<>();
    private static AutoDigState state = AutoDigState.IDLE;
    private static AutoTask activeTask;
    private static Component status = Component.translatable("massdig.auto.status.idle");
    private static int plannedCount;
    private static int protectedCount;
    private static int minedCount;
    private static int failedCount;
    private static int planMinY;
    private static int planMaxY;
    private static int estimatedSeconds;
    private static int waitTicks;
    private static boolean autopilotPressingForward;

    private AutoDigController() {
    }

    public static void register(KeyMapping.Category category) {
        openAutoKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.massdig.auto_open",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                category
        ));
        pointAKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.massdig.auto_point_a",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                category
        ));
        pointBKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.massdig.auto_point_b",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                category
        ));
        startPauseKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.massdig.auto_start_pause",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                category
        ));
        stopKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.massdig.auto_stop",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                category
        ));
    }

    public static void handleKeys(Minecraft client) {
        while (openAutoKey.consumeClick()) {
            client.setScreen(new AutoDigScreen(null));
        }
        while (pointAKey.consumeClick()) {
            setPointAFromLook(client);
        }
        while (pointBKey.consumeClick()) {
            setPointBFromLook(client);
        }
        while (startPauseKey.consumeClick()) {
            toggleRunning(client);
        }
        while (stopKey.consumeClick()) {
            stop(client);
        }
    }

    public static void tick(Minecraft client) {
        if (client.player == null || client.level == null) {
            stop(client);
            return;
        }
        if (state != AutoDigState.RUNNING) {
            releaseAutopilot(client);
            return;
        }
        if (client.screen != null) {
            pause(client, Component.translatable("massdig.auto.status.paused_screen"));
            return;
        }
        if (MassdigClient.config().pauseOnLowHealth && client.player.getHealth() <= 6.0F) {
            pause(client, Component.translatable("massdig.auto.status.low_hp"));
            return;
        }
        if (MassdigClient.config().autoStopWhenInventoryFull && client.player.getInventory().getFreeSlot() == Inventory.NOT_FOUND_INDEX) {
            pause(client, Component.translatable("massdig.auto.status.inventory_full"));
            return;
        }
        tickExecution(client);
        showHud(client);
    }

    public static boolean ownsMining() {
        return state == AutoDigState.RUNNING && activeTask != null && !activeTask.waitingForConfirm;
    }

    public static AutoDigState state() {
        return state;
    }

    public static BlockPos pointA() {
        return pointA;
    }

    public static BlockPos pointB() {
        return pointB;
    }

    public static int plannedCount() {
        return plannedCount;
    }

    public static int remainingCount() {
        return plan.size() + (activeTask == null ? 0 : 1);
    }

    public static int protectedCount() {
        return protectedCount;
    }

    public static int minedCount() {
        return minedCount;
    }

    public static int failedCount() {
        return failedCount;
    }

    public static int estimatedSeconds() {
        return estimatedSeconds;
    }

    public static int planMinY() {
        return planMinY;
    }

    public static int planMaxY() {
        return planMaxY;
    }

    public static Component status() {
        return status;
    }

    public static void setPointAFromLook(Minecraft client) {
        BlockPos pos = lookedBlock(client);
        if (pos != null) {
            pointA = pos.immutable();
            state = AutoDigState.PREVIEW;
            status = Component.translatable("massdig.auto.status.point_a");
            rebuildPlan(client);
        }
    }

    public static void setPointBFromLook(Minecraft client) {
        BlockPos pos = lookedBlock(client);
        if (pos != null) {
            pointB = pos.immutable();
            state = AutoDigState.PREVIEW;
            status = Component.translatable("massdig.auto.status.point_b");
            rebuildPlan(client);
        }
    }

    public static void rebuildPlan(Minecraft client) {
        plan.clear();
        plannedSet.clear();
        protectedSet.clear();
        plannedCount = 0;
        protectedCount = 0;
        minedCount = 0;
        failedCount = 0;
        estimatedSeconds = 0;
        planMinY = 0;
        planMaxY = 0;
        activeTask = null;

        if (client.player == null || client.level == null) {
            return;
        }

        switch (MassdigClient.config().autoJobType()) {
            case CLEAR_AREA -> buildBoxPlan(client, false);
            case QUARRY -> buildBoxPlan(client, true);
            case TUNNEL -> buildTunnelPlan(client);
            case ORE_VEIN -> buildOrePlan(client);
            case BRANCH_MINE -> buildBranchMinePlan(client);
            case FLATTEN -> buildFlattenPlan(client);
        }
        updatePlanStats();
        sortPlan(client.player);
        plannedCount = plan.size();
        state = plannedCount > 0 ? AutoDigState.PREVIEW : AutoDigState.BLOCKED;
        status = plannedCount > 0
                ? Component.translatable("massdig.auto.status.preview")
                : Component.translatable("massdig.auto.status.empty_plan");
    }

    public static void toggleRunning(Minecraft client) {
        if (state == AutoDigState.RUNNING) {
            pause(client, Component.translatable("massdig.auto.status.paused"));
            return;
        }
        start(client);
    }

    public static void start(Minecraft client) {
        if (plan.isEmpty()) {
            rebuildPlan(client);
        }
        if (plan.isEmpty()) {
            state = AutoDigState.BLOCKED;
            status = Component.translatable("massdig.auto.status.empty_plan");
            return;
        }
        state = AutoDigState.RUNNING;
        status = Component.translatable("massdig.auto.status.running");
        client.setScreen(null);
    }

    public static void pause(Minecraft client, Component reason) {
        if (client.gameMode != null && activeTask != null && activeTask.started && !activeTask.waitingForConfirm) {
            client.gameMode.stopDestroyBlock();
        }
        releaseAutopilot(client);
        activeTask = null;
        state = AutoDigState.PAUSED;
        status = reason;
    }

    public static void expandSelection(Minecraft client, int amount) {
        if (pointA == null || pointB == null) {
            return;
        }
        BlockPos min = boxMin(pointA, pointB);
        BlockPos max = boxMax(pointA, pointB);
        if (amount < 0 && (max.getX() - min.getX() < 2 || max.getY() - min.getY() < 2 || max.getZ() - min.getZ() < 2)) {
            status = Component.translatable("massdig.auto.status.selection_too_small");
            return;
        }
        pointA = min.offset(-amount, -amount, -amount);
        pointB = max.offset(amount, amount, amount);
        rebuildPlan(client);
    }

    public static void moveSelection(Minecraft client, Direction direction, int amount) {
        if (pointA == null || pointB == null) {
            return;
        }
        pointA = pointA.relative(direction, amount);
        pointB = pointB.relative(direction, amount);
        rebuildPlan(client);
    }

    public static void layerUp(Minecraft client) {
        MassdigClient.config().autoLayerY = Math.min(planMaxY, MassdigClient.config().autoLayerY + 1);
        MassdigClient.config().save();
    }

    public static void layerDown(Minecraft client) {
        MassdigClient.config().autoLayerY = Math.max(planMinY, MassdigClient.config().autoLayerY - 1);
        MassdigClient.config().save();
    }

    public static void stop(Minecraft client) {
        if (client != null && client.gameMode != null && activeTask != null && activeTask.started && !activeTask.waitingForConfirm) {
            client.gameMode.stopDestroyBlock();
        }
        releaseAutopilot(client);
        activeTask = null;
        plan.clear();
        plannedSet.clear();
        protectedSet.clear();
        plannedCount = 0;
        protectedCount = 0;
        minedCount = 0;
        failedCount = 0;
        state = AutoDigState.IDLE;
        status = Component.translatable("massdig.auto.status.idle");
    }

    public static void render() {
        if (pointA != null && pointB != null) {
            BlockPos min = boxMin(pointA, pointB);
            BlockPos max = boxMax(pointA, pointB);
            Gizmos.cuboid(new AABB(min.getX(), min.getY(), min.getZ(), max.getX() + 1.0D, max.getY() + 1.0D, max.getZ() + 1.0D),
                    GizmoStyle.stroke(BOX_COLOR, 0.04F));
        }

        GizmoStyle planStyle = GizmoStyle.fill(PLAN_COLOR);
        GizmoStyle layerStyle = GizmoStyle.fill(0x6639F56B);
        int rendered = 0;
        for (BlockPos pos : plan) {
            if (rendered++ >= MAX_RENDER_BLOCKS) {
                break;
            }
            Gizmos.cuboid(pos, pos.getY() == MassdigClient.config().autoLayerY ? 0.02F : 0.012F,
                    pos.getY() == MassdigClient.config().autoLayerY ? layerStyle : planStyle);
        }

        GizmoStyle protectedStyle = GizmoStyle.fill(PROTECTED_COLOR);
        rendered = 0;
        for (BlockPos pos : protectedSet) {
            if (rendered++ >= MAX_RENDER_BLOCKS / 3) {
                break;
            }
            Gizmos.cuboid(pos, 0.018F, protectedStyle);
        }

        if (activeTask != null) {
            Gizmos.cuboid(activeTask.pos, 0.032F, GizmoStyle.fill(ACTIVE_COLOR));
        } else if (state == AutoDigState.BLOCKED && !plan.isEmpty()) {
            Gizmos.cuboid(plan.get(0), 0.025F, GizmoStyle.fill(BLOCKED_COLOR));
        }
    }

    public static void drawMiniMap(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, 0x66000000);
        graphics.outline(x, y, width, height, 0x9939F56B);
        if (pointA == null || pointB == null) {
            return;
        }
        BlockPos min = boxMin(pointA, pointB);
        BlockPos max = boxMax(pointA, pointB);
        int spanX = Math.max(1, max.getX() - min.getX() + 1);
        int spanZ = Math.max(1, max.getZ() - min.getZ() + 1);
        int layer = MassdigClient.config().autoLayerY;
        int cellW = Math.max(1, width / Math.min(spanX, 48));
        int cellH = Math.max(1, height / Math.min(spanZ, 48));
        for (BlockPos pos : plan) {
            if (pos.getY() != layer) {
                continue;
            }
            int px = x + (pos.getX() - min.getX()) * width / spanX;
            int pz = y + (pos.getZ() - min.getZ()) * height / spanZ;
            graphics.fill(px, pz, Math.min(x + width, px + cellW), Math.min(y + height, pz + cellH), 0xAA39F56B);
        }
        for (BlockPos pos : protectedSet) {
            if (pos.getY() != layer) {
                continue;
            }
            int px = x + (pos.getX() - min.getX()) * width / spanX;
            int pz = y + (pos.getZ() - min.getZ()) * height / spanZ;
            graphics.fill(px, pz, Math.min(x + width, px + cellW), Math.min(y + height, pz + cellH), 0xAAFF9F1C);
        }
        if (activeTask != null && activeTask.pos.getY() == layer) {
            int px = x + (activeTask.pos.getX() - min.getX()) * width / spanX;
            int pz = y + (activeTask.pos.getZ() - min.getZ()) * height / spanZ;
            graphics.fill(px, pz, Math.min(x + width, px + cellW + 1), Math.min(y + height, pz + cellH + 1), 0xCCFF2E63);
        }
    }

    private static void tickExecution(Minecraft client) {
        if (client.level == null || client.player == null || client.gameMode == null) {
            return;
        }
        if (waitTicks > 0) {
            waitTicks--;
            return;
        }

        if (activeTask == null) {
            activeTask = pollReachableTask(client);
        }
        if (activeTask == null) {
            BlockPos nearest = nearestRemaining(client.player);
            if (nearest == null) {
                state = AutoDigState.DONE;
                status = Component.translatable("massdig.auto.status.done");
                releaseAutopilot(client);
                return;
            }
            if (MassdigClient.config().autoAutopilot) {
                if (manualOverride(client)) {
                    pause(client, Component.translatable("massdig.auto.status.manual_override"));
                    return;
                }
                autopilotToward(client, nearest);
                status = Component.translatable("massdig.auto.status.autopilot");
            } else {
                state = AutoDigState.BLOCKED;
                status = Component.translatable("massdig.auto.status.too_far");
                releaseAutopilot(client);
            }
            return;
        }

        releaseAutopilot(client);
        BlockState stateAtPos = client.level.getBlockState(activeTask.pos);
        if (stateAtPos.isAir()) {
            minedCount++;
            activeTask = null;
            return;
        }
        if (isDangerous(client.level, activeTask.pos)) {
            failOrPause(client, Component.translatable("massdig.auto.status.lava"));
            return;
        }
        if (!selectTool(client, stateAtPos)) {
            failOrPause(client, Component.translatable("massdig.auto.status.no_tool"));
            return;
        }

        if (!activeTask.started) {
            activeTask.started = true;
            activeTask.age = 0;
        }

        if (activeTask.waitingForConfirm) {
            activeTask.confirmAge++;
            if (activeTask.confirmAge < MassdigClient.config().mode().confirmTicks) {
                return;
            }
            if (client.level.getBlockState(activeTask.pos).isAir()) {
                minedCount++;
                activeTask = null;
                return;
            }
            if (activeTask.retries >= 4) {
                failedCount++;
                activeTask = null;
                waitTicks = 8;
                return;
            }
            activeTask.waitingForConfirm = false;
            activeTask.started = false;
            activeTask.requiredTicks = MassdigConfig.clamp(activeTask.requiredTicks + 5, 1, 280);
            activeTask.retries++;
            return;
        }

        boolean showEffect = client.gameMode.continueDestroyBlock(activeTask.pos, activeTask.side);
        if (showEffect) {
            client.level.addBreakingBlockEffect(activeTask.pos, activeTask.side);
            client.player.swing(InteractionHand.MAIN_HAND);
        }

        activeTask.age++;
        if (client.level.getBlockState(activeTask.pos).isAir()
                || activeTask.age >= activeTask.requiredTicks + MassdigClient.config().mode().confirmTicks + 20) {
            client.gameMode.stopDestroyBlock();
            activeTask.waitingForConfirm = true;
            activeTask.confirmAge = 0;
        }
    }

    private static AutoTask pollReachableTask(Minecraft client) {
        Player player = client.player;
        ClientLevel level = client.level;
        if (player == null || level == null) {
            return null;
        }
        for (int i = 0; i < plan.size(); i++) {
            BlockPos pos = plan.get(i);
            if (level.getBlockState(pos).isAir()) {
                plan.remove(i--);
                plannedSet.remove(pos);
                minedCount++;
                continue;
            }
            if (!player.isWithinBlockInteractionRange(pos, 1.5D)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (!canMine(client, pos, state)) {
                plan.remove(i--);
                plannedSet.remove(pos);
                protectedSet.add(pos);
                protectedCount++;
                continue;
            }
            plan.remove(i);
            plannedSet.remove(pos);
            return new AutoTask(pos, sideFor(player, pos), requiredTicks(player, level, pos, state));
        }
        return null;
    }

    private static BlockPos nearestRemaining(Player player) {
        return plan.stream()
                .min(Comparator.comparingDouble(pos -> pos.distSqr(player.blockPosition())))
                .orElse(null);
    }

    private static void buildBoxPlan(Minecraft client, boolean quarry) {
        if (pointA == null || pointB == null) {
            return;
        }
        BlockPos min = boxMin(pointA, pointB);
        BlockPos max = boxMax(pointA, pointB);
        int minY = quarry ? Math.max(client.level.getMinY(), max.getY() - MassdigClient.config().autoDepth + 1) : min.getY();
        for (int y = max.getY(); y >= minY; y--) {
            for (int z = min.getZ(); z <= max.getZ(); z++) {
                boolean reverse = (z - min.getZ()) % 2 != 0;
                if (reverse) {
                    for (int x = max.getX(); x >= min.getX(); x--) {
                        addPlanned(client, new BlockPos(x, y, z));
                    }
                } else {
                    for (int x = min.getX(); x <= max.getX(); x++) {
                        addPlanned(client, new BlockPos(x, y, z));
                    }
                }
            }
        }
    }

    private static void buildTunnelPlan(Minecraft client) {
        BlockPos start = lookedBlock(client);
        if (start == null || !(client.hitResult instanceof BlockHitResult hit)) {
            start = pointA;
        }
        if (start == null) {
            return;
        }
        Direction direction = client.hitResult instanceof BlockHitResult hit ? hit.getDirection().getOpposite() : horizontalFacing(client.player);
        Direction right = direction.getClockWise();
        int width = MassdigClient.config().autoWidth;
        int height = MassdigClient.config().autoHeight;
        int length = MassdigClient.config().autoLength;
        int left = width / 2;
        int rightCount = width - left - 1;

        for (int depth = 0; depth < length; depth++) {
            BlockPos center = start.relative(direction, depth);
            for (int y = 0; y < height; y++) {
                for (int x = -left; x <= rightCount; x++) {
                    addPlanned(client, center.relative(right, x).above(y));
                }
            }
        }
        pointA = start;
        pointB = start.relative(direction, length - 1).relative(right, rightCount).above(height - 1);
    }

    private static void buildBranchMinePlan(Minecraft client) {
        BlockPos start = lookedBlock(client);
        if (start == null) {
            start = pointA;
        }
        if (start == null) {
            return;
        }
        Direction forward = client.hitResult instanceof BlockHitResult hit ? hit.getDirection().getOpposite() : horizontalFacing(client.player);
        Direction right = forward.getClockWise();
        int mainLength = MassdigClient.config().autoLength;
        int height = MassdigClient.config().autoHeight;
        int spacing = MassdigClient.config().autoBranchSpacing;
        int branchLength = MassdigClient.config().autoBranchLength;

        for (int depth = 0; depth < mainLength; depth++) {
            addTunnelSlice(client, start.relative(forward, depth), right, 1, height);
            if (depth > 0 && depth % spacing == 0) {
                for (int branch = 1; branch <= branchLength; branch++) {
                    addTunnelSlice(client, start.relative(forward, depth).relative(right, branch), right, 1, height);
                    if (MassdigClient.config().autoBothBranches) {
                        addTunnelSlice(client, start.relative(forward, depth).relative(right.getOpposite(), branch), right, 1, height);
                    }
                }
            }
        }
        pointA = start;
        pointB = start.relative(forward, mainLength - 1).relative(right, branchLength).above(height - 1);
    }

    private static void buildFlattenPlan(Minecraft client) {
        if (pointA == null || pointB == null || client.level == null) {
            return;
        }
        BlockPos min = boxMin(pointA, pointB);
        BlockPos max = boxMax(pointA, pointB);
        int targetY = Math.min(pointA.getY(), pointB.getY());
        for (int y = max.getY(); y > targetY; y--) {
            for (int z = min.getZ(); z <= max.getZ(); z++) {
                for (int x = min.getX(); x <= max.getX(); x++) {
                    addPlanned(client, new BlockPos(x, y, z));
                }
            }
        }
    }

    private static void addTunnelSlice(Minecraft client, BlockPos center, Direction right, int width, int height) {
        int left = width / 2;
        int rightCount = width - left - 1;
        for (int y = 0; y < height; y++) {
            for (int x = -left; x <= rightCount; x++) {
                addPlanned(client, center.relative(right, x).above(y));
            }
        }
    }

    private static void buildOrePlan(Minecraft client) {
        BlockPos start = lookedBlock(client);
        if (start == null) {
            start = pointA;
        }
        if (start == null || client.level == null) {
            return;
        }
        BlockState origin = client.level.getBlockState(start);
        if (!isOre(origin)) {
            return;
        }
        String ore = oreKind(origin);
        Queue<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> seen = new HashSet<>();
        queue.add(start);
        seen.add(start);
        while (!queue.isEmpty() && plan.size() < Math.min(MAX_PLAN_BLOCKS, 512)) {
            BlockPos pos = queue.remove();
            BlockState state = client.level.getBlockState(pos);
            if (isOre(state) && oreKind(state).equals(ore)) {
                addPlanned(client, pos);
                for (Direction direction : Direction.values()) {
                    BlockPos next = pos.relative(direction);
                    if (seen.add(next)) {
                        queue.add(next);
                    }
                }
            }
        }
        pointA = start;
        pointB = start;
    }

    private static void addPlanned(Minecraft client, BlockPos pos) {
        if (plan.size() >= MAX_PLAN_BLOCKS || !plannedSet.add(pos.immutable())) {
            return;
        }
        if (client.level == null) {
            return;
        }
        BlockState state = client.level.getBlockState(pos);
        if (state.isAir()) {
            return;
        }
        if (!canMine(client, pos, state)) {
            protectedSet.add(pos.immutable());
            protectedCount++;
            return;
        }
        if (MassdigClient.config().autoKeepOres && isOre(state)) {
            protectedSet.add(pos.immutable());
            protectedCount++;
            return;
        }
        plan.add(pos.immutable());
    }

    private static boolean canMine(Minecraft client, BlockPos pos, BlockState state) {
        Player player = client.player;
        ClientLevel level = client.level;
        if (player == null || level == null || state.isAir()) {
            return false;
        }
        if (MassdigClient.config().skipUnbreakable && state.getDestroySpeed(level, pos) < 0.0F) {
            return false;
        }
        if (state.getDestroyProgress(player, level, pos) <= 0.0F) {
            return false;
        }
        if (MassdigClient.config().protectPlayerSpace && isPlayerSpace(player, pos)) {
            return false;
        }
        String path = blockPath(state);
        if (MassdigClient.config().protectUsefulBlocks && containsAny(path, "chest", "barrel", "shulker", "furnace",
                "smoker", "crafting_table", "anvil", "beacon", "spawner", "bed", "hopper", "dispenser", "dropper",
                "enchanting_table", "brewing_stand", "jukebox", "note_block", "respawn_anchor", "lectern", "sign")) {
            return false;
        }
        if (MassdigClient.config().protectFragileBlocks && containsAny(path, "glass", "pane", "torch", "lantern",
                "candle", "flower_pot", "banner", "carpet", "redstone", "repeater", "comparator", "rail", "ladder", "vine")) {
            return false;
        }
        return !isDangerous(level, pos);
    }

    private static boolean selectTool(Minecraft client, BlockState state) {
        if (!MassdigClient.config().autoToolSwitch || client.player == null) {
            return true;
        }
        Inventory inventory = client.player.getInventory();
        int bestSlot = -1;
        float bestSpeed = 0.0F;
        for (int slot = 0; slot < Inventory.getSelectionSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty() || isTooDamaged(stack)) {
                continue;
            }
            float speed = stack.getDestroySpeed(state);
            if (stack.isCorrectToolForDrops(state)) {
                speed += 1000.0F;
            }
            if (speed > bestSpeed) {
                bestSpeed = speed;
                bestSlot = slot;
            }
        }
        if (bestSlot >= 0 && bestSpeed > 1.0F) {
            inventory.setSelectedSlot(bestSlot);
            return true;
        }
        return !MassdigClient.config().autoStopWhenNoTool;
    }

    private static boolean isTooDamaged(ItemStack stack) {
        if (!stack.isDamageableItem()) {
            return false;
        }
        int remaining = stack.getMaxDamage() - stack.getDamageValue();
        int min = Math.max(1, stack.getMaxDamage() * MassdigClient.config().autoMinToolDurability / 100);
        return remaining <= min;
    }

    private static int requiredTicks(Player player, ClientLevel level, BlockPos pos, BlockState state) {
        float progress = state.getDestroyProgress(player, level, pos);
        if (progress <= 0.0F) {
            return 240;
        }
        int base = (int) Math.ceil(MassdigClient.config().mode().stopProgress / progress);
        int bonus = state.getDestroySpeed(level, pos) >= 2.5F ? MassdigClient.config().mode().hardBlockBonusTicks : 0;
        return MassdigConfig.clamp(base + bonus + MassdigClient.config().safetyExtraTicks, 1, 260);
    }

    private static void sortPlan(Player player) {
        switch (MassdigClient.config().autoPlanOrder()) {
            case NEAREST -> plan.sort(Comparator.comparingDouble(pos -> pos.distSqr(player.blockPosition())));
            case SNAKE -> plan.sort(Comparator
                    .comparingInt((BlockPos pos) -> pos.getY()).reversed()
                    .thenComparingInt(pos -> pos.getZ())
                    .thenComparingInt(pos -> (pos.getZ() & 1) == 0 ? pos.getX() : -pos.getX()));
            case TOP_DOWN -> plan.sort(Comparator
                    .comparingInt((BlockPos pos) -> pos.getY()).reversed()
                    .thenComparingDouble(pos -> pos.distSqr(player.blockPosition())));
        }
    }

    private static void updatePlanStats() {
        if (plan.isEmpty() && protectedSet.isEmpty()) {
            planMinY = 0;
            planMaxY = 0;
            estimatedSeconds = 0;
            return;
        }
        planMinY = Integer.MAX_VALUE;
        planMaxY = Integer.MIN_VALUE;
        for (BlockPos pos : plan) {
            planMinY = Math.min(planMinY, pos.getY());
            planMaxY = Math.max(planMaxY, pos.getY());
        }
        for (BlockPos pos : protectedSet) {
            planMinY = Math.min(planMinY, pos.getY());
            planMaxY = Math.max(planMaxY, pos.getY());
        }
        if (planMinY == Integer.MAX_VALUE) {
            planMinY = 0;
            planMaxY = 0;
        }
        MassdigClient.config().autoLayerY = MassdigConfig.clamp(MassdigClient.config().autoLayerY == 0 ? planMaxY : MassdigClient.config().autoLayerY, planMinY, planMaxY);
        estimatedSeconds = Math.max(1, plan.size() * 2 / 3);
    }

    private static Direction sideFor(Player player, BlockPos pos) {
        double dx = player.getX() - (pos.getX() + 0.5D);
        double dy = player.getEyeY() - (pos.getY() + 0.5D);
        double dz = player.getZ() - (pos.getZ() + 0.5D);
        double ax = Math.abs(dx);
        double ay = Math.abs(dy);
        double az = Math.abs(dz);
        if (ay >= ax && ay >= az) {
            return dy > 0.0D ? Direction.UP : Direction.DOWN;
        }
        if (ax >= az) {
            return dx > 0.0D ? Direction.EAST : Direction.WEST;
        }
        return dz > 0.0D ? Direction.SOUTH : Direction.NORTH;
    }

    private static void autopilotToward(Minecraft client, BlockPos pos) {
        if (client.player == null) {
            return;
        }
        double dx = pos.getX() + 0.5D - client.player.getX();
        double dz = pos.getZ() + 0.5D - client.player.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist < 1.2D) {
            releaseAutopilot(client);
            return;
        }
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0D);
        client.player.setYRot(yaw);
        client.options.keyUp.setDown(true);
        autopilotPressingForward = true;
    }

    private static boolean manualOverride(Minecraft client) {
        if (!autopilotPressingForward) {
            return false;
        }
        return client.options.keyLeft.isDown()
                || client.options.keyRight.isDown()
                || client.options.keyDown.isDown()
                || client.options.keyJump.isDown()
                || client.options.keyShift.isDown()
                || client.options.keyAttack.isDown()
                || client.options.keyUse.isDown();
    }

    private static void releaseAutopilot(Minecraft client) {
        if (autopilotPressingForward && client != null) {
            client.options.keyUp.setDown(false);
        }
        autopilotPressingForward = false;
    }

    private static void failOrPause(Minecraft client, Component reason) {
        if (client.gameMode != null && activeTask != null && activeTask.started && !activeTask.waitingForConfirm) {
            client.gameMode.stopDestroyBlock();
        }
        activeTask = null;
        state = AutoDigState.BLOCKED;
        status = reason;
        releaseAutopilot(client);
    }

    private static void showHud(Minecraft client) {
        if (!MassdigClient.config().hudEnabled || client.gui == null) {
            return;
        }
        Component text = Component.empty()
                .append(Component.translatable("massdig.auto.hud.prefix"))
                .append(": ")
                .append(Component.translatable(MassdigClient.config().autoJobType().nameKey()))
                .append("  ")
                .append(Component.translatable("massdig.auto.hud.left"))
                .append(" ")
                .append(Integer.toString(remainingCount()))
                .append("  ")
                .append(Component.translatable("massdig.auto.hud.done"))
                .append(" ")
                .append(Integer.toString(minedCount))
                .append("  ")
                .append(status);
        client.gui.setOverlayMessage(text, false);
    }

    private static BlockPos lookedBlock(Minecraft client) {
        HitResult hitResult = client.hitResult;
        if (hitResult instanceof BlockHitResult blockHitResult) {
            return blockHitResult.getBlockPos().immutable();
        }
        return null;
    }

    private static Direction horizontalFacing(Player player) {
        return player == null ? Direction.NORTH : player.getDirection();
    }

    private static BlockPos boxMin(BlockPos a, BlockPos b) {
        return new BlockPos(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()));
    }

    private static BlockPos boxMax(BlockPos a, BlockPos b) {
        return new BlockPos(Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()), Math.max(a.getZ(), b.getZ()));
    }

    private static boolean isPlayerSpace(Player player, BlockPos pos) {
        BlockPos feet = player.blockPosition();
        return pos.equals(feet) || pos.equals(feet.above()) || pos.equals(feet.below());
    }

    private static boolean isDangerous(ClientLevel level, BlockPos pos) {
        if (!MassdigClient.config().avoidLava) {
            return false;
        }
        for (Direction direction : Direction.values()) {
            if (level.getFluidState(pos.relative(direction)).is(FluidTags.LAVA)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isOre(BlockState state) {
        return blockPath(state).contains("ore");
    }

    private static String oreKind(BlockState state) {
        String path = blockPath(state);
        if (path.startsWith("deepslate_")) {
            path = path.substring("deepslate_".length());
        }
        return path.replace("_ore", "");
    }

    private static String blockPath(BlockState state) {
        Identifier id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return id.getPath();
    }

    private static boolean containsAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static final class AutoTask {
        final BlockPos pos;
        final Direction side;
        int requiredTicks;
        int age;
        int confirmAge;
        int retries;
        boolean started;
        boolean waitingForConfirm;

        AutoTask(BlockPos pos, Direction side, int requiredTicks) {
            this.pos = pos;
            this.side = side;
            this.requiredTicks = requiredTicks;
        }
    }
}
