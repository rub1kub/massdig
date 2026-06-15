package org.main.massdig.client;

public enum AutoDigPlanOrder {
    TOP_DOWN("top_down"),
    NEAREST("nearest"),
    SNAKE("snake");

    public final String id;

    AutoDigPlanOrder(String id) {
        this.id = id;
    }

    public AutoDigPlanOrder next() {
        AutoDigPlanOrder[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public static AutoDigPlanOrder byId(String id) {
        for (AutoDigPlanOrder order : values()) {
            if (order.id.equals(id)) {
                return order;
            }
        }
        return TOP_DOWN;
    }

    public String nameKey() {
        return "massdig.auto.order." + id + ".name";
    }

    public String hintKey() {
        return "massdig.auto.order." + id + ".hint";
    }
}
