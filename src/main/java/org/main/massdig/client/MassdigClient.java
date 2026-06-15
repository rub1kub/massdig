package org.main.massdig.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.lwjgl.glfw.GLFW;

import java.util.LinkedHashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class MassdigClient implements ClientModInitializer {
    private static final int MIN_RADIUS = 1;
    private static final int MAX_RADIUS = 6;
    private static final int MAX_QUEUE_SIZE = 4096;
    private static final int MAX_PREVIEW_BLOCKS = 768;
    private static final int MAX_RETRIES = 5;
    private static final int PREVIEW_COLOR = 0x55FFD36E;
    private static final int QUEUE_COLOR = 0x5539A9FF;
    private static final int ACTIVE_COLOR = 0x66FF5A5F;
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath("massdig", "category")
    );

    private static final Map<BlockPos, DigTask> pendingTasks = new LinkedHashMap<>();
    private static final Set<BlockPos> previewTargets = new LinkedHashSet<>();

    private static MassdigConfig config;
    private static KeyMapping openConfigKey;
    private static KeyMapping toggleKey;
    private static KeyMapping fastToggleKey;
    private static KeyMapping incRadiusKey;
    private static KeyMapping decRadiusKey;
    private static DigTask activeTask;
    private static double packetTokens;
    private static int slowdownTicks;
    private static int lastBrokenCount;

    public static MassdigConfig config() {
        return config;
    }

    public static boolean isActive() {
        return config.radiusEnabled;
    }

    public static boolean isFastMiningActive() {
        return config.fastMiningEnabled;
    }

    public static boolean shouldRemoveMiningCooldown() {
        return config != null && config.fastMiningEnabled;
    }

    public static boolean shouldSuppressVanillaMining() {
        return config != null && config.radiusEnabled && activeTask != null && !activeTask.waitingForConfirm;
    }

    public static void setActive(boolean active) {
        config.radiusEnabled = active;
        config.save();
        if (!active) {
            stopActiveDigging(Minecraft.getInstance());
            clearDigging();
        }
    }

    public static void toggle() {
        setActive(!config.radiusEnabled);
    }

    public static int getRadius() {
        return config.radius;
    }

    public static void setRadius(int radius) {
        config.radius = MassdigConfig.clamp(radius, MIN_RADIUS, MAX_RADIUS);
        config.save();
    }

    public static int queuedBlocks() {
        return pendingTasks.size() + (activeTask == null ? 0 : 1);
    }

    @Override
    public void onInitializeClient() {
        config = MassdigConfig.load();

        openConfigKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.massdig.open_config",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_SEMICOLON,
                CATEGORY
        ));
        toggleKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.massdig.toggle",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_BACKSLASH,
                CATEGORY
        ));
        fastToggleKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.massdig.fast_toggle",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                CATEGORY
        ));
        incRadiusKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.massdig.radius_inc",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_BRACKET,
                CATEGORY
        ));
        decRadiusKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.massdig.radius_dec",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_LEFT_BRACKET,
                CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(MassdigClient::onEndTick);
        LevelRenderEvents.BEFORE_GIZMOS.register(context -> renderPreview());
    }

    private static void onEndTick(Minecraft client) {
        if (client.player == null || client.level == null) {
            clearDigging();
            return;
        }

        refillPacketBudget();
        handleKeys(client);

        updatePreview(client);

        if (!config.radiusEnabled) {
            showFastMiningStatus(client);
            return;
        }

        if (!isRadiusMiningHeld(client)) {
            stopActiveDigging(client);
            showFastMiningStatus(client);
            return;
        }

        collectTargets(client);

        if (config.mode().legacyBurst) {
            tickLegacyBurst(client);
        } else {
            tickSequentialDigging(client);
        }

        showStatus(client);
    }

    private static void handleKeys(Minecraft client) {
        while (openConfigKey.consumeClick()) {
            client.setScreen(new MassdigScreen(null));
        }
        while (toggleKey.consumeClick()) {
            toggle();
        }
        while (fastToggleKey.consumeClick()) {
            config.fastMiningEnabled = !config.fastMiningEnabled;
            config.save();
        }
        while (incRadiusKey.consumeClick()) {
            setRadius(config.radius + 1);
        }
        while (decRadiusKey.consumeClick()) {
            setRadius(config.radius - 1);
        }
    }

    private static boolean isRadiusMiningHeld(Minecraft client) {
        return client.options.keyAttack.isDown()
                && client.player != null
                && isHoldingPickaxe(client.player);
    }

    private static boolean isHoldingPickaxe(Player player) {
        ItemStack stack = player.getMainHandItem();
        return stack.is(ItemTags.PICKAXES);
    }

    private static void refillPacketBudget() {
        MassdigMode mode = config.mode();
        int pps = config.packetLimitPerSecond;
        if (slowdownTicks > 0) {
            pps = Math.max(30, pps / 2);
            slowdownTicks--;
        }
        packetTokens = Math.min(mode.burstPackets, packetTokens + pps / 20.0D);
    }

    private static void collectTargets(Minecraft client) {
        collectTargets(client, true);
    }

    private static void updatePreview(Minecraft client) {
        previewTargets.clear();
        if (!config.radiusEnabled || !config.showPreview || client.player == null || client.level == null) {
            return;
        }
        collectTargets(client, false);
    }

    private static void collectTargets(Minecraft client, boolean queue) {
        HitResult hitResult = client.hitResult;
        if (!(hitResult instanceof BlockHitResult blockHitResult)) {
            return;
        }

        BlockPos origin = blockHitResult.getBlockPos();
        Direction side = blockHitResult.getDirection();
        Direction.Axis axis = side.getAxis();
        BlockState originState = client.level.getBlockState(origin);

        switch (config.shape()) {
            case PLANE -> {
                for (int a = -config.radius; a <= config.radius; a++) {
                    for (int b = -config.radius; b <= config.radius; b++) {
                        BlockPos pos = switch (axis) {
                            case X -> origin.offset(0, a, b);
                            case Y -> origin.offset(a, 0, b);
                            case Z -> origin.offset(a, b, 0);
                        };
                        addTarget(client, pos, side, originState, pos.equals(origin), queue);
                    }
                }
            }
            case TUNNEL -> {
                Direction into = side.getOpposite();
                for (int depth = 0; depth <= config.radius; depth++) {
                    for (int a = -config.radius; a <= config.radius; a++) {
                        for (int b = -config.radius; b <= config.radius; b++) {
                            BlockPos pos = switch (axis) {
                                case X -> origin.offset(into.getStepX() * depth, a, b);
                                case Y -> origin.offset(a, into.getStepY() * depth, b);
                                case Z -> origin.offset(a, b, into.getStepZ() * depth);
                            };
                            addTarget(client, pos, side, originState, pos.equals(origin), queue);
                        }
                    }
                }
            }
            case CUBE -> {
                for (int x = -config.radius; x <= config.radius; x++) {
                    for (int y = -config.radius; y <= config.radius; y++) {
                        for (int z = -config.radius; z <= config.radius; z++) {
                            BlockPos pos = origin.offset(x, y, z);
                            addTarget(client, pos, side, originState, pos.equals(origin), queue);
                        }
                    }
                }
            }
        }
    }

    private static void addTarget(Minecraft client, BlockPos pos, Direction side, BlockState originState, boolean focused, boolean queue) {
        if (queue) {
            addTask(client, pos, side, originState, focused);
        } else if (canTarget(client, pos, originState) && previewTargets.size() < MAX_PREVIEW_BLOCKS) {
            previewTargets.add(pos.immutable());
        }
    }

    private static void addTask(Minecraft client, BlockPos pos, Direction side, BlockState originState, boolean focused) {
        ClientLevel level = client.level;
        Player player = client.player;
        if (level == null || player == null || pendingTasks.size() >= MAX_QUEUE_SIZE) {
            return;
        }
        if (pendingTasks.containsKey(pos) || (activeTask != null && activeTask.pos.equals(pos))) {
            return;
        }

        if (!canTarget(client, pos, originState)) {
            return;
        }

        BlockState state = level.getBlockState(pos);
        float destroySpeed = state.getDestroySpeed(level, pos);
        float progressPerTick = state.getDestroyProgress(player, level, pos);
        MassdigMode mode = config.mode();
        boolean hardBlock = destroySpeed >= 2.5F || progressPerTick < 0.06F;
        int requiredTicks = (int) Math.ceil(mode.stopProgress / progressPerTick)
                + config.safetyExtraTicks
                + (hardBlock ? mode.hardBlockBonusTicks : 0);
        int priority = focused ? 0 : (config.hardBlocksFirst && hardBlock ? 1 : 2);
        pendingTasks.put(pos.immutable(), new DigTask(pos.immutable(), side, MassdigConfig.clamp(requiredTicks, 1, 240), priority));
    }

    private static boolean canTarget(Minecraft client, BlockPos pos, BlockState originState) {
        ClientLevel level = client.level;
        Player player = client.player;
        if (level == null || player == null) {
            return false;
        }

        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return false;
        }
        if (config.sameBlockOnly && !state.is(originState.getBlock())) {
            return false;
        }
        if (config.keepWithinReach && !player.isWithinBlockInteractionRange(pos, 1.5D)) {
            return false;
        }

        float destroySpeed = state.getDestroySpeed(level, pos);
        if (config.skipUnbreakable && destroySpeed < 0.0F) {
            return false;
        }

        float progressPerTick = state.getDestroyProgress(player, level, pos);
        return progressPerTick > 0.0F;
    }

    private static void tickSequentialDigging(Minecraft client) {
        if (client.level == null || client.getConnection() == null) {
            clearDigging();
            return;
        }

        if (activeTask == null) {
            activeTask = pollNextTask();
        }
        if (activeTask == null) {
            return;
        }

        BlockState state = client.level.getBlockState(activeTask.pos);
        if (state.isAir()) {
            lastBrokenCount++;
            activeTask = null;
            return;
        }

        if (!activeTask.started) {
            if (packetTokens < 1.0D) {
                return;
            }
            packetTokens -= 1.0D;
            activeTask.started = true;
            activeTask.age = 0;
        }

        if (activeTask.waitingForConfirm) {
            activeTask.confirmAge++;
            if (activeTask.confirmAge < config.mode().confirmTicks) {
                return;
            }

            if (client.level.getBlockState(activeTask.pos).isAir()) {
                lastBrokenCount++;
                activeTask = null;
                return;
            }

            if (activeTask.retries >= MAX_RETRIES) {
                activeTask = null;
                if (config.autoSlowdown) {
                    slowdownTicks = Math.max(slowdownTicks, 60);
                }
                return;
            }

            activeTask.started = false;
            activeTask.waitingForConfirm = false;
            activeTask.age = 0;
            activeTask.requiredTicks = MassdigConfig.clamp(activeTask.requiredTicks + config.safetyExtraTicks + 3, 1, 280);
            if (config.autoSlowdown) {
                slowdownTicks = Math.max(slowdownTicks, 40);
            }
            return;
        }

        if (client.gameMode == null || client.player == null) {
            clearDigging();
            return;
        }
        if (!client.player.isWithinBlockInteractionRange(activeTask.pos, 2.0D)) {
            stopActiveDigging(client);
            return;
        }

        boolean showEffect = client.gameMode.continueDestroyBlock(activeTask.pos, activeTask.side);
        if (showEffect) {
            client.level.addBreakingBlockEffect(activeTask.pos, activeTask.side);
            client.player.swing(InteractionHand.MAIN_HAND);
        }

        activeTask.age++;
        if (client.level.getBlockState(activeTask.pos).isAir()
                || activeTask.age >= activeTask.requiredTicks + config.mode().confirmTicks + 20) {
            client.gameMode.stopDestroyBlock();
            activeTask.waitingForConfirm = true;
            activeTask.confirmAge = 0;
            activeTask.retries++;
        }
    }

    private static void tickLegacyBurst(Minecraft client) {
        if (client.level == null || client.getConnection() == null) {
            clearDigging();
            return;
        }

        int processed = 0;
        Iterator<Map.Entry<BlockPos, DigTask>> iterator = pendingTasks.entrySet().iterator();
        while (iterator.hasNext() && processed < config.legacyBlocksPerTick && packetTokens >= 2.0D) {
            DigTask task = iterator.next().getValue();
            iterator.remove();

            if (client.level.getBlockState(task.pos).isAir()) {
                continue;
            }

            boolean started = sendAction(client, ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, task.pos, task.side);
            boolean stopped = started && sendAction(client, ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, task.pos, task.side);
            if (!stopped) {
                pendingTasks.put(task.pos, task);
                break;
            }
            processed++;
        }
    }

    private static DigTask pollNextTask() {
        DigTask best = null;
        BlockPos bestPos = null;
        for (Map.Entry<BlockPos, DigTask> entry : pendingTasks.entrySet()) {
            DigTask task = entry.getValue();
            if (best == null || task.priority < best.priority || (task.priority == best.priority && task.requiredTicks > best.requiredTicks)) {
                best = task;
                bestPos = entry.getKey();
            }
        }
        if (bestPos != null) {
            pendingTasks.remove(bestPos);
        }
        return best;
    }

    private static boolean sendAction(Minecraft client, ServerboundPlayerActionPacket.Action action, BlockPos pos, Direction side) {
        if (client.getConnection() == null || packetTokens < 1.0D) {
            return false;
        }
        packetTokens -= 1.0D;
        client.getConnection().send(new ServerboundPlayerActionPacket(action, pos, side));
        return true;
    }

    private static void showStatus(Minecraft client) {
        if (!config.radiusEnabled || client.gui == null) {
            return;
        }

        int side = config.radius * 2 + 1;
        Component text = Component.empty()
                .append(Component.translatable("massdig.overlay.prefix"))
                .append(" [")
                .append(Integer.toString(side))
                .append("x")
                .append(Integer.toString(side))
                .append("] ")
                .append(Component.translatable(config.mode().nameKey()))
                .append("/")
                .append(Component.translatable(config.shape().nameKey()))
                .append(" ")
                .append(Component.translatable("massdig.overlay.queue"))
                .append(" ")
                .append(Integer.toString(queuedBlocks()))
                .append(" ")
                .append(Component.translatable("massdig.overlay.done"))
                .append(" ")
                .append(Integer.toString(lastBrokenCount));
        client.gui.setOverlayMessage(text, false);
    }

    private static void showFastMiningStatus(Minecraft client) {
        if (!config.fastMiningEnabled || client.gui == null) {
            return;
        }
        client.gui.setOverlayMessage(Component.translatable("massdig.overlay.fast_mining"), false);
    }

    private static void clearDigging() {
        clearMining();
        previewTargets.clear();
        lastBrokenCount = 0;
    }

    private static void clearMining() {
        pendingTasks.clear();
        activeTask = null;
    }

    private static void stopActiveDigging(Minecraft client) {
        if (activeTask != null && activeTask.started && !activeTask.waitingForConfirm && client.gameMode != null) {
            client.gameMode.stopDestroyBlock();
        }
        clearMining();
    }

    private static void renderPreview() {
        if (previewTargets.isEmpty() && pendingTasks.isEmpty() && activeTask == null) {
            return;
        }

        GizmoStyle previewStyle = GizmoStyle.fill(PREVIEW_COLOR);
        for (BlockPos pos : previewTargets) {
            if (!pendingTasks.containsKey(pos) && (activeTask == null || !activeTask.pos.equals(pos))) {
                Gizmos.cuboid(pos, 0.015F, previewStyle);
            }
        }

        GizmoStyle queueStyle = GizmoStyle.fill(QUEUE_COLOR);
        for (BlockPos pos : pendingTasks.keySet()) {
            Gizmos.cuboid(pos, 0.02F, queueStyle);
        }

        if (activeTask != null) {
            Gizmos.cuboid(activeTask.pos, 0.03F, GizmoStyle.fill(ACTIVE_COLOR));
        }
    }

    private static final class DigTask {
        final BlockPos pos;
        final Direction side;
        final int priority;
        int requiredTicks;
        int age;
        int confirmAge;
        int retries;
        boolean started;
        boolean waitingForConfirm;

        DigTask(BlockPos pos, Direction side, int requiredTicks, int priority) {
            this.pos = pos;
            this.side = side;
            this.requiredTicks = requiredTicks;
            this.priority = priority;
        }
    }
}
