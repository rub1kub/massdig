package org.main.massdig.client;

public enum AutoDigJobType {
    CLEAR_AREA("clear_area"),
    QUARRY("quarry"),
    TUNNEL("tunnel"),
    ORE_VEIN("ore_vein");

    public final String id;

    AutoDigJobType(String id) {
        this.id = id;
    }

    public AutoDigJobType next() {
        AutoDigJobType[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public static AutoDigJobType byId(String id) {
        for (AutoDigJobType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        return CLEAR_AREA;
    }

    public String nameKey() {
        return "massdig.auto.type." + id + ".name";
    }

    public String hintKey() {
        return "massdig.auto.type." + id + ".hint";
    }
}
