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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.FluidTags;
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
    private static final int SKIPPED_COLOR = 0x557A7A7A;
    private static final int PROTECTED_COLOR = 0x66FF9F1C;
    private static final int DANGER_COLOR = 0x66D7263D;
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath("massdig", "category")
    );

    private static final Map<BlockPos, DigTask> pendingTasks = new LinkedHashMap<>();
    private static final Set<BlockPos> previewTargets = new LinkedHashSet<>();
    private static final Set<BlockPos> skippedTargets = new LinkedHashSet<>();
    private static final Set<BlockPos> protectedTargets = new LinkedHashSet<>();
    private static final Set<BlockPos> dangerTargets = new LinkedHashSet<>();

    private static MassdigConfig config;
    private static KeyMapping openConfigKey;
    private static KeyMapping toggleKey;
    private static KeyMapping fastToggleKey;
    private static KeyMapping cyclePresetKey;
    private static KeyMapping cycleShapeKey;
    private static KeyMapping cycleMatchKey;
    private static KeyMapping incRadiusKey;
    private static KeyMapping decRadiusKey;
    private static DigTask activeTask;
    private static double packetTokens;
    private static int slowdownTicks;
    private static int lastBrokenCount;
    private static int lastSkippedCount;

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
        cyclePresetKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.massdig.cycle_preset",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_B,
                CATEGORY
        ));
        cycleShapeKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.massdig.cycle_shape",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                CATEGORY
        ));
        cycleMatchKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.massdig.cycle_match",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_N,
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

        if (shouldPauseRadiusMining(client)) {
            stopActiveDigging(client);
            showPassiveStatus(client);
            return;
        }

        if (!config.radiusEnabled) {
            showPassiveStatus(client);
            return;
        }

        if (!isRadiusMiningHeld(client)) {
            stopActiveDigging(client);
            showPassiveStatus(client);
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
        while (cyclePresetKey.consumeClick()) {
            config.preset().next().apply(config);
        }
        while (cycleShapeKey.consumeClick()) {
            config.setShape(config.shape().next());
            config.save();
        }
        while (cycleMatchKey.consumeClick()) {
            config.setMatchMode(config.matchMode().next());
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

    private static boolean shouldPauseRadiusMining(Minecraft client) {
        if (!config.radiusEnabled) {
            return false;
        }
        if (config.pauseWhenScreenOpen && client.screen != null) {
            return true;
        }
        return config.pauseOnLowHealth && client.player != null && client.player.getHealth() <= 6.0F;
    }

    private static void refillPacketBudget() {
        MassdigMode mode = config.mode();
        MassdigProtection protection = config.protection();
        int pps = Math.min(config.packetLimitPerSecond, protection.packetCap);
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
        skippedTargets.clear();
        protectedTargets.clear();
        dangerTargets.clear();
        lastSkippedCount = 0;
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
            return;
        }

        TargetDecision decision = targetDecision(client, pos, originState);
        if (decision == TargetDecision.OK && previewTargets.size() < MAX_PREVIEW_BLOCKS) {
            previewTargets.add(pos.immutable());
        } else if (config.showSkippedPreview) {
            addSkippedPreview(pos, decision);
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

        if (targetDecision(client, pos, originState) != TargetDecision.OK) {
            return;
        }

        BlockState state = level.getBlockState(pos);
        float destroySpeed = state.getDestroySpeed(level, pos);
        float progressPerTick = state.getDestroyProgress(player, level, pos);
        MassdigMode mode = config.mode();
        boolean hardBlock = destroySpeed >= 2.5F || progressPerTick < 0.06F;
        int smartBonus = config.smartAutoTune ? smartBonusTicks(state) : 0;
        int requiredTicks = (int) Math.ceil(mode.stopProgress / progressPerTick)
                + config.safetyExtraTicks
                + (hardBlock ? mode.hardBlockBonusTicks : 0)
                + smartBonus;
        int priority = focused ? 0 : (config.hardBlocksFirst && hardBlock ? 1 : 2);
        pendingTasks.put(pos.immutable(), new DigTask(pos.immutable(), side, MassdigConfig.clamp(requiredTicks, 1, 240), priority));
    }

    private static boolean canTarget(Minecraft client, BlockPos pos, BlockState originState) {
        return targetDecision(client, pos, originState) == TargetDecision.OK;
    }

    private static TargetDecision targetDecision(Minecraft client, BlockPos pos, BlockState originState) {
        ClientLevel level = client.level;
        Player player = client.player;
        if (level == null || player == null) {
            return TargetDecision.SKIPPED;
        }

        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return TargetDecision.SKIPPED;
        }
        if (!matchesBlockMode(state, originState)) {
            return TargetDecision.SKIPPED;
        }
        if (config.keepWithinReach && !player.isWithinBlockInteractionRange(pos, 1.5D)) {
            return TargetDecision.SKIPPED;
        }

        float destroySpeed = state.getDestroySpeed(level, pos);
        if (config.skipUnbreakable && destroySpeed < 0.0F) {
            return TargetDecision.PROTECTED;
        }

        float progressPerTick = state.getDestroyProgress(player, level, pos);
        if (progressPerTick <= 0.0F) {
            return TargetDecision.PROTECTED;
        }
        if (config.protectPlayerSpace && isPlayerSpace(player, pos)) {
            return TargetDecision.PROTECTED;
        }
        if (config.protectUsefulBlocks && isUsefulBlock(state)) {
            return TargetDecision.PROTECTED;
        }
        if (config.protectFragileBlocks && isFragileBlock(state)) {
            return TargetDecision.PROTECTED;
        }
        if (config.avoidLava && isNearLava(level, pos)) {
            return TargetDecision.DANGER;
        }
        return TargetDecision.OK;
    }

    private static void addSkippedPreview(BlockPos pos, TargetDecision decision) {
        if (decision == TargetDecision.OK || lastSkippedCount >= MAX_PREVIEW_BLOCKS) {
            return;
        }
        lastSkippedCount++;
        BlockPos immutable = pos.immutable();
        switch (decision) {
            case PROTECTED -> protectedTargets.add(immutable);
            case DANGER -> dangerTargets.add(immutable);
            default -> skippedTargets.add(immutable);
        }
    }

    private static boolean matchesBlockMode(BlockState state, BlockState originState) {
        return switch (config.matchMode()) {
            case ANY -> true;
            case SAME -> state.is(originState.getBlock());
            case SIMILAR -> family(state) == family(originState);
            case ORES -> family(originState) == BlockFamily.ORE && family(state) == BlockFamily.ORE
                    && oreKind(state).equals(oreKind(originState));
        };
    }

    private static int smartBonusTicks(BlockState state) {
        return switch (family(state)) {
            case DEEPSLATE -> 8;
            case ORE -> blockPath(state).contains("deepslate") ? 8 : 3;
            case STONE -> 2;
            default -> 0;
        };
    }

    private static boolean isPlayerSpace(Player player, BlockPos pos) {
        BlockPos feet = player.blockPosition();
        return pos.equals(feet) || pos.equals(feet.above()) || pos.equals(feet.below());
    }

    private static boolean isNearLava(ClientLevel level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockPos nearby = pos.offset(direction.getStepX(), direction.getStepY(), direction.getStepZ());
            if (level.getFluidState(nearby).is(FluidTags.LAVA)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isUsefulBlock(BlockState state) {
        String path = blockPath(state);
        return containsAny(path, "chest", "barrel", "shulker", "furnace", "smoker", "crafting_table",
                "anvil", "beacon", "spawner", "bed", "hopper", "dispenser", "dropper", "enchanting_table",
                "brewing_stand", "jukebox", "note_block", "respawn_anchor", "lectern", "sign");
    }

    private static boolean isFragileBlock(BlockState state) {
        String path = blockPath(state);
        return containsAny(path, "glass", "pane", "torch", "lantern", "candle", "flower_pot", "banner",
                "carpet", "redstone", "repeater", "comparator", "rail", "ladder", "vine");
    }

    private static boolean containsAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static BlockFamily family(BlockState state) {
        String path = blockPath(state);
        if (path.contains("ore")) {
            return BlockFamily.ORE;
        }
        if (path.contains("deepslate") || path.contains("tuff")) {
            return BlockFamily.DEEPSLATE;
        }
        if (containsAny(path, "stone", "andesite", "diorite", "granite", "calcite")) {
            return BlockFamily.STONE;
        }
        if (containsAny(path, "dirt", "grass_block", "mud", "clay", "podzol", "mycelium")) {
            return BlockFamily.DIRT;
        }
        if (path.contains("sand")) {
            return BlockFamily.SAND;
        }
        if (path.contains("gravel")) {
            return BlockFamily.GRAVEL;
        }
        if (containsAny(path, "netherrack", "blackstone", "basalt")) {
            return BlockFamily.NETHER_STONE;
        }
        if (containsAny(path, "log", "stem", "wood", "hyphae", "planks")) {
            return BlockFamily.WOOD;
        }
        return BlockFamily.OTHER;
    }

    private static String oreKind(BlockState state) {
        String path = blockPath(state);
        if (path.startsWith("deepslate_")) {
            path = path.substring("deepslate_".length());
        }
        return path.replace("_ore", "");
    }

    private static String blockPath(BlockState state) {
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
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
                    slowdownTicks = Math.max(slowdownTicks, 60 + config.protection().slowdownBoostTicks);
                }
                return;
            }

            activeTask.started = false;
            activeTask.waitingForConfirm = false;
            activeTask.age = 0;
            activeTask.requiredTicks = MassdigConfig.clamp(activeTask.requiredTicks + config.safetyExtraTicks + 3, 1, 280);
            if (config.autoSlowdown) {
                slowdownTicks = Math.max(slowdownTicks, 40 + config.protection().slowdownBoostTicks);
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
        if (!config.hudEnabled || !config.radiusEnabled || client.gui == null) {
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
                .append(Component.translatable(config.matchMode().nameKey()))
                .append(" ")
                .append(Component.translatable("massdig.overlay.queue"))
                .append(" ")
                .append(Integer.toString(queuedBlocks()))
                .append(" ")
                .append(Component.translatable("massdig.overlay.skipped"))
                .append(" ")
                .append(Integer.toString(lastSkippedCount))
                .append(" ")
                .append(Component.translatable("massdig.overlay.done"))
                .append(" ")
                .append(Integer.toString(lastBrokenCount));
        client.gui.setOverlayMessage(text, false);
    }

    private static void showPassiveStatus(Minecraft client) {
        if (!config.hudEnabled || client.gui == null) {
            return;
        }
        if (!config.fastMiningEnabled && !config.radiusEnabled) {
            return;
        }
        Component text = Component.empty()
                .append(Component.translatable("massdig.overlay.fast"))
                .append(": ")
                .append(Component.translatable(config.fastMiningEnabled ? "massdig.value.on" : "massdig.value.off"))
                .append("  ")
                .append(Component.translatable("massdig.overlay.radius"))
                .append(": ")
                .append(Component.translatable(config.radiusEnabled ? "massdig.value.on" : "massdig.value.off"));
        if (config.radiusEnabled && !isHoldingPickaxe(client.player)) {
            text = text.copy().append("  ").append(Component.translatable("massdig.overlay.need_pickaxe"));
        }
        client.gui.setOverlayMessage(text, false);
    }

    private static void clearDigging() {
        clearMining();
        previewTargets.clear();
        skippedTargets.clear();
        protectedTargets.clear();
        dangerTargets.clear();
        lastBrokenCount = 0;
        lastSkippedCount = 0;
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
        if (previewTargets.isEmpty() && skippedTargets.isEmpty() && protectedTargets.isEmpty()
                && dangerTargets.isEmpty() && pendingTasks.isEmpty() && activeTask == null) {
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

        GizmoStyle skippedStyle = GizmoStyle.fill(SKIPPED_COLOR);
        for (BlockPos pos : skippedTargets) {
            Gizmos.cuboid(pos, 0.012F, skippedStyle);
        }

        GizmoStyle protectedStyle = GizmoStyle.fill(PROTECTED_COLOR);
        for (BlockPos pos : protectedTargets) {
            Gizmos.cuboid(pos, 0.018F, protectedStyle);
        }

        GizmoStyle dangerStyle = GizmoStyle.fill(DANGER_COLOR);
        for (BlockPos pos : dangerTargets) {
            Gizmos.cuboid(pos, 0.022F, dangerStyle);
        }

        if (activeTask != null) {
            Gizmos.cuboid(activeTask.pos, 0.03F, GizmoStyle.fill(ACTIVE_COLOR));
        }
    }

    private enum TargetDecision {
        OK,
        SKIPPED,
        PROTECTED,
        DANGER
    }

    private enum BlockFamily {
        STONE,
        DEEPSLATE,
        DIRT,
        SAND,
        GRAVEL,
        NETHER_STONE,
        WOOD,
        ORE,
        OTHER
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
