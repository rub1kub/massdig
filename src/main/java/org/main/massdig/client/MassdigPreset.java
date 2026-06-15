package org.main.massdig.client;

public enum MassdigPreset {
    SERVER("server"),
    NORMAL("normal"),
    DEEPSLATE("deepslate"),
    SOFT_BLOCKS("soft_blocks");

    public final String id;

    MassdigPreset(String id) {
        this.id = id;
    }

    public MassdigPreset next() {
        MassdigPreset[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public static MassdigPreset byId(String id) {
        for (MassdigPreset preset : values()) {
            if (preset.id.equals(id)) {
                return preset;
            }
        }
        return NORMAL;
    }

    public void apply(MassdigConfig config) {
        config.preset = id;
        switch (this) {
            case SERVER -> {
                config.radiusEnabled = true;
                config.fastMiningEnabled = false;
                config.showPreview = true;
                config.mode = MassdigMode.SAFE.id;
                config.shape = MassdigShape.PLANE.id;
                config.radius = 1;
                config.packetLimitPerSecond = 60;
                config.safetyExtraTicks = 10;
                config.legacyBlocksPerTick = 8;
                config.hardBlocksFirst = true;
                config.autoSlowdown = true;
                config.skipUnbreakable = true;
                config.keepWithinReach = true;
                config.sameBlockOnly = false;
            }
            case NORMAL -> {
                config.radiusEnabled = true;
                config.fastMiningEnabled = false;
                config.showPreview = true;
                config.mode = MassdigMode.BALANCED.id;
                config.shape = MassdigShape.PLANE.id;
                config.radius = 2;
                config.packetLimitPerSecond = 120;
                config.safetyExtraTicks = 6;
                config.legacyBlocksPerTick = 24;
                config.hardBlocksFirst = true;
                config.autoSlowdown = true;
                config.skipUnbreakable = true;
                config.keepWithinReach = true;
                config.sameBlockOnly = false;
            }
            case DEEPSLATE -> {
                config.radiusEnabled = true;
                config.fastMiningEnabled = false;
                config.showPreview = true;
                config.mode = MassdigMode.BALANCED.id;
                config.shape = MassdigShape.PLANE.id;
                config.radius = 2;
                config.packetLimitPerSecond = 90;
                config.safetyExtraTicks = 14;
                config.legacyBlocksPerTick = 12;
                config.hardBlocksFirst = true;
                config.autoSlowdown = true;
                config.skipUnbreakable = true;
                config.keepWithinReach = true;
                config.sameBlockOnly = false;
            }
            case SOFT_BLOCKS -> {
                config.radiusEnabled = true;
                config.fastMiningEnabled = true;
                config.showPreview = true;
                config.mode = MassdigMode.FAST.id;
                config.shape = MassdigShape.PLANE.id;
                config.radius = 3;
                config.packetLimitPerSecond = 180;
                config.safetyExtraTicks = 2;
                config.legacyBlocksPerTick = 32;
                config.hardBlocksFirst = false;
                config.autoSlowdown = true;
                config.skipUnbreakable = true;
                config.keepWithinReach = true;
                config.sameBlockOnly = false;
            }
        }
        config.save();
    }

    public String nameKey() {
        return "massdig.preset." + id + ".name";
    }
}
