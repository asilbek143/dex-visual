package uz.dexvisual;

import java.util.Locale;

public final class Setting {
    public enum Type { BOOL, NUM, MODE }

    public final String name;
    public final Type type;
    public boolean b;
    public double n, min, max, step;
    public String[] modes;
    public int idx;
    public float anim;

    private Setting(String name, Type type) {
        this.name = name;
        this.type = type;
    }

    public static Setting bool(String name, boolean def) {
        Setting s = new Setting(name, Type.BOOL);
        s.b = def;
        s.anim = def ? 1f : 0f;
        return s;
    }

    public static Setting num(String name, double def, double min, double max, double step) {
        Setting s = new Setting(name, Type.NUM);
        s.min = min;
        s.max = max;
        s.step = step;
        s.n = def;
        return s;
    }

    public static Setting mode(String name, int def, String... modes) {
        Setting s = new Setting(name, Type.MODE);
        s.modes = modes;
        s.idx = def;
        return s;
    }

    public void setNum(double v) {
        v = Math.max(min, Math.min(max, v));
        if (step > 0) v = Math.round((v - min) / step) * step + min;
        n = Math.max(min, Math.min(max, v));
    }

    public void cycle(int d) {
        idx = ((idx + d) % modes.length + modes.length) % modes.length;
    }

    public String valueText() {
        return switch (type) {
            case NUM -> step >= 1 ? String.valueOf((int) Math.round(n)) : String.format(Locale.ROOT, "%.1f", n);
            case MODE -> modes[idx];
            case BOOL -> b ? "ON" : "OFF";
        };
    }

    public Object save() {
        return switch (type) {
            case BOOL -> (Object) b;
            case NUM -> (Object) n;
            case MODE -> (Object) idx;
        };
    }

    public void load(Object o) {
        if (o == null) return;
        switch (type) {
            case BOOL -> {
                if (o instanceof Boolean bv) {
                    b = bv;
                    anim = b ? 1f : 0f;
                }
            }
            case NUM -> {
                if (o instanceof Number nv) setNum(nv.doubleValue());
            }
            case MODE -> {
                if (o instanceof Number mv) idx = Math.max(0, Math.min(modes.length - 1, mv.intValue()));
            }
        }
    }
}
