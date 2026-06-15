package org.main.massdig.client;

public enum MassdigProtection {
    LIGHT("light", 220, 0),
    NORMAL("normal", 140, 10),
    STRONG("strong", 80, 24);

    public final String id;
    public final int packetCap;
    public final int slowdownBoostTicks;

    MassdigProtection(String id, int packetCap, int slowdownBoostTicks) {
        this.id = id;
        this.packetCap = packetCap;
        this.slowdownBoostTicks = slowdownBoostTicks;
    }

    public MassdigProtection next() {
        MassdigProtection[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public static MassdigProtection byId(String id) {
        for (MassdigProtection protection : values()) {
            if (protection.id.equals(id)) {
                return protection;
            }
        }
        return NORMAL;
    }

    public String nameKey() {
        return "massdig.protection." + id + ".name";
    }

    public String hintKey() {
        return "massdig.protection." + id + ".hint";
    }
}
