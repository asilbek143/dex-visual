package uz.dexvisual;

import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

public final class Notifs {
    private record N(String text, boolean on, long start) {}

    private static final List<N> LIST = new ArrayList<>();
    private static final float LIFE = 2.4f;
    private static final float FADE = 0.28f;

    public static void push(String name, boolean on) {
        if (!Mods.NOTIFS.enabled) return;
        LIST.add(new N(name, on, System.nanoTime()));
        if (LIST.size() > 6) LIST.remove(0);
    }

    public static void render(DrawContext ctx, int sw, int sh) {
        long now = System.nanoTime();
        LIST.removeIf(n -> (now - n.start) / 1.0e9f > LIFE);
        int idx = 0;
        for (int i = LIST.size() - 1; i >= 0; i--) {
            N n = LIST.get(i);
            float age = (now - n.start) / 1.0e9f;
            float a = Draw.easeOut(age / FADE);
            if (age > LIFE - FADE) a = Draw.easeOut((LIFE - age) / FADE);
            String state = n.on ? "enabled" : "disabled";
            int tw = Draw.w(n.text + " " + state);
            int w = tw + 30, h = 22;
            int xRight = sw - 8 + (int) ((1f - a) * (w + 12));
            int x = xRight - w;
            int y = sh - 46 - idx * 26;
            Draw.alpha = a;
            Draw.rrect(ctx, x + 1, y + 2, w, h, 5, 0x55000000);
            Draw.rrect(ctx, x, y, w, h, 5, 0xE0121219);
            int accent = n.on ? 0xFF34D399 : 0xFFFF5A6A;
            Draw.rrect(ctx, x + 6, y + 6, 3, h - 12, 1, accent);
            Draw.text(ctx, n.text, x + 15, y + 7, 0xFFFFFFFF, true);
            Draw.text(ctx, state, x + 15 + Draw.w(n.text + " "), y + 7, accent, true);
            Draw.alpha = 1f;
            idx++;
        }
    }

    private Notifs() {}
}
