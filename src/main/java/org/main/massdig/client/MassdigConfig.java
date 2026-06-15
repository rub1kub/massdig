package org.main.massdig.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class MassdigConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("massdig.json");

    public boolean enabled = false;
    public boolean radiusEnabled = false;
    public boolean fastMiningEnabled = false;
    public boolean showPreview = true;
    public int radius = 1;
    public String mode = MassdigMode.BALANCED.id;
    public String shape = MassdigShape.PLANE.id;
    public String preset = MassdigPreset.NORMAL.id;
    public String matchMode = MassdigMatchMode.ANY.id;
    public String protection = MassdigProtection.NORMAL.id;
    public int packetLimitPerSecond = MassdigMode.BALANCED.defaultPacketsPerSecond;
    public int safetyExtraTicks = 6;
    public int legacyBlocksPerTick = 24;
    public boolean hardBlocksFirst = true;
    public boolean autoSlowdown = true;
    public boolean smartAutoTune = true;
    public boolean hudEnabled = true;
    public boolean showSkippedPreview = true;
    public boolean skipUnbreakable = true;
    public boolean keepWithinReach = true;
    public boolean sameBlockOnly = false;
    public boolean protectUsefulBlocks = true;
    public boolean protectFragileBlocks = false;
    public boolean protectPlayerSpace = true;
    public boolean avoidLava = true;
    public boolean pauseWhenScreenOpen = true;
    public boolean pauseOnLowHealth = false;
    public String autoJobType = AutoDigJobType.CLEAR_AREA.id;
    public int autoWidth = 3;
    public int autoHeight = 3;
    public int autoLength = 32;
    public int autoDepth = 16;
    public boolean autoToolSwitch = true;
    public int autoMinToolDurability = 8;
    public boolean autoStopWhenInventoryFull = true;
    public boolean autoStopWhenNoTool = true;
    public boolean autoKeepOres = false;
    public boolean autoAutopilot = false;
    public String autoPlanOrder = AutoDigPlanOrder.TOP_DOWN.id;
    public int autoLayerY = 0;
    public int autoBranchSpacing = 3;
    public int autoBranchLength = 32;
    public boolean autoBothBranches = true;

    public static MassdigConfig load() {
        if (!Files.exists(PATH)) {
            MassdigConfig config = new MassdigConfig();
            config.save();
            return config;
        }

        try (Reader reader = Files.newBufferedReader(PATH)) {
            MassdigConfig config = GSON.fromJson(reader, MassdigConfig.class);
            return config == null ? new MassdigConfig() : config.sanitize();
        } catch (IOException ignored) {
            return new MassdigConfig();
        }
    }

    public void save() {
        sanitize();
        try {
            Files.createDirectories(PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(PATH)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException ignored) {
        }
    }

    public MassdigConfig sanitize() {
        if (enabled) {
            radiusEnabled = true;
            enabled = false;
        }
        radius = clamp(radius, 1, 6);
        mode = MassdigMode.byId(mode).id;
        shape = MassdigShape.byId(shape).id;
        preset = MassdigPreset.byId(preset).id;
        if (sameBlockOnly && MassdigMatchMode.byId(matchMode) == MassdigMatchMode.ANY) {
            matchMode = MassdigMatchMode.SAME.id;
            sameBlockOnly = false;
        }
        matchMode = MassdigMatchMode.byId(matchMode).id;
        protection = MassdigProtection.byId(protection).id;
        autoJobType = AutoDigJobType.byId(autoJobType).id;
        autoPlanOrder = AutoDigPlanOrder.byId(autoPlanOrder).id;
        packetLimitPerSecond = clamp(packetLimitPerSecond, 40, 260);
        safetyExtraTicks = clamp(safetyExtraTicks, 0, 20);
        legacyBlocksPerTick = clamp(legacyBlocksPerTick, 4, 64);
        autoWidth = clamp(autoWidth, 1, 9);
        autoHeight = clamp(autoHeight, 1, 6);
        autoLength = clamp(autoLength, 1, 256);
        autoDepth = clamp(autoDepth, 1, 128);
        autoMinToolDurability = clamp(autoMinToolDurability, 1, 90);
        autoBranchSpacing = clamp(autoBranchSpacing, 2, 16);
        autoBranchLength = clamp(autoBranchLength, 4, 128);
        return this;
    }

    public MassdigMode mode() {
        return MassdigMode.byId(mode);
    }

    public MassdigShape shape() {
        return MassdigShape.byId(shape);
    }

    public MassdigPreset preset() {
        return MassdigPreset.byId(preset);
    }

    public MassdigMatchMode matchMode() {
        return MassdigMatchMode.byId(matchMode);
    }

    public MassdigProtection protection() {
        return MassdigProtection.byId(protection);
    }

    public AutoDigJobType autoJobType() {
        return AutoDigJobType.byId(autoJobType);
    }

    public AutoDigPlanOrder autoPlanOrder() {
        return AutoDigPlanOrder.byId(autoPlanOrder);
    }

    public void setMode(MassdigMode mode) {
        this.mode = mode.id;
        this.packetLimitPerSecond = mode.defaultPacketsPerSecond;
    }

    public void setShape(MassdigShape shape) {
        this.shape = shape.id;
    }

    public void setMatchMode(MassdigMatchMode matchMode) {
        this.matchMode = matchMode.id;
    }

    public void setProtection(MassdigProtection protection) {
        this.protection = protection.id;
        this.packetLimitPerSecond = Math.min(packetLimitPerSecond, protection.packetCap);
    }

    public void setAutoJobType(AutoDigJobType type) {
        this.autoJobType = type.id;
    }

    public void setAutoPlanOrder(AutoDigPlanOrder order) {
        this.autoPlanOrder = order.id;
    }

    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
