package org.main.massdig.client;

public enum MassdigShape {
    PLANE("plane"),
    TUNNEL("tunnel"),
    CUBE("cube");

    public final String id;

    MassdigShape(String id) {
        this.id = id;
    }

    public MassdigShape next() {
        MassdigShape[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public static MassdigShape byId(String id) {
        for (MassdigShape shape : values()) {
            if (shape.id.equals(id)) {
                return shape;
            }
        }
        return PLANE;
    }

    public String nameKey() {
        return "massdig.shape." + id + ".name";
    }

    public String hintKey() {
        return "massdig.shape." + id + ".hint";
    }
}
