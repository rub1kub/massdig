package org.main.massdig.client;

public enum MassdigMode {
    SAFE("safe", 60, 6, 1.15F, 8, 8, false),
    BALANCED("balanced", 120, 10, 1.05F, 6, 6, false),
    FAST("fast", 200, 18, 0.92F, 4, 4, false),
    LEGACY_BURST("legacy_burst", 240, 36, 0.70F, 2, 2, true);

    public final String id;
    public final int defaultPacketsPerSecond;
    public final int burstPackets;
    public final float stopProgress;
    public final int hardBlockBonusTicks;
    public final int confirmTicks;
    public final boolean legacyBurst;

    MassdigMode(String id, int defaultPacketsPerSecond, int burstPackets, float stopProgress,
                int hardBlockBonusTicks, int confirmTicks, boolean legacyBurst) {
        this.id = id;
        this.defaultPacketsPerSecond = defaultPacketsPerSecond;
        this.burstPackets = burstPackets;
        this.stopProgress = stopProgress;
        this.hardBlockBonusTicks = hardBlockBonusTicks;
        this.confirmTicks = confirmTicks;
        this.legacyBurst = legacyBurst;
    }

    public MassdigMode next() {
        MassdigMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public static MassdigMode byId(String id) {
        for (MassdigMode mode : values()) {
            if (mode.id.equals(id)) {
                return mode;
            }
        }
        return BALANCED;
    }

    public String nameKey() {
        return "massdig.mode." + id + ".name";
    }

    public String hintKey() {
        return "massdig.mode." + id + ".hint";
    }
}
