package org.main.massdig.client;

public enum MassdigMatchMode {
    ANY("any"),
    SAME("same"),
    SIMILAR("similar"),
    ORES("ores");

    public final String id;

    MassdigMatchMode(String id) {
        this.id = id;
    }

    public MassdigMatchMode next() {
        MassdigMatchMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public static MassdigMatchMode byId(String id) {
        for (MassdigMatchMode mode : values()) {
            if (mode.id.equals(id)) {
                return mode;
            }
        }
        return ANY;
    }

    public String nameKey() {
        return "massdig.match." + id + ".name";
    }

    public String hintKey() {
        return "massdig.match." + id + ".hint";
    }
}
