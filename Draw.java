package uz.dexvisual;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;

public final class Draw {
    /** Global alpha multiplier applied to everything drawn through this class. */
    public static float alpha = 1f;
    public static float dt = 0.016f;
    private static long last = System.nanoTime();

    public static void beginFrame() {
        long now = System.nanoTime();
        if (now - last < 1_000_000L) return;
        dt = Math.min(0.1f, (now - last) / 1.0e9f);
        last = now;
    }

    public static TextRenderer tr() {
        return MinecraftClient.getInstance().textRenderer;
    }

    // ---- math / color ----
    public static float clamp01(float v) {
        return v < 0f ? 0f : (v > 1f ? 1f : v);
    }

    public static float approach(float cur, float target, float speed) {
        return cur + (target - cur) * (1f - (float) Math.exp(-speed * dt));
    }

    public static float easeOut(float t) {
        float u = 1f - clamp01(t);
        return 1f - u * u * u;
    }

    public static int a(int rgb, float a) {
        int v = Math.max(0, Math.min(255, (int) (a * 255f)));
        return (v << 24) | (rgb & 0xFFFFFF);
    }

    private static int ch(int a, int b, int shift, float t) {
        int x = (a >>> shift) & 255;
        int y = (b >>> shift) & 255;
        return ((int) (x + (y - x) * t)) & 255;
    }

    public static int lerp(int a, int b, float t) {
        t = clamp01(t);
        return (ch(a, b, 24, t) << 24) | (ch(a, b, 16, t) << 16) | (ch(a, b, 8, t) << 8) | ch(a, b, 0, t);
    }

    private static int mul(int c) {
        int al = (int) (((c >>> 24) & 255) * alpha);
        return (al << 24) | (c & 0xFFFFFF);
    }

    // ---- primitives ----
    public static void rect(DrawContext c, int x1, int y1, int x2, int y2, int col) {
        if (x2 <= x1 || y2 <= y1) return;
        int m = mul(col);
        if ((m >>> 24) == 0) return;
        c.fill(x1, y1, x2, y2, m);
    }

    public static void rrect(DrawContext c, int x, int y, int w, int h, int r, int col) {
        if (w <= 0 || h <= 0) return;
        r = Math.min(r, Math.min(w, h) / 2);
        if (r <= 0) {
            rect(c, x, y, x + w, y + h, col);
            return;
        }
        rect(c, x, y + r, x + w, y + h - r, col);
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int inset = (int) Math.round(r - Math.sqrt(r * (double) r - dy * dy));
            rect(c, x + inset, y + i, x + w - inset, y + i + 1, col);
            rect(c, x + inset, y + h - 1 - i, x + w - inset, y + h - i, col);
        }
    }

    public static void bar(DrawContext c, int x, int y, int w, int h, int col) {
        if (w <= 0) return;
        rrect(c, x, y, w, h, Math.min(2, w / 2), col);
    }

    public static void gradV(DrawContext c, int x1, int y1, int x2, int y2, int top, int bot) {
        if (x2 <= x1 || y2 <= y1) return;
        c.fillGradient(x1, y1, x2, y2, mul(top), mul(bot));
    }

    /** Horizontal animated theme gradient strip. */
    public static void hGrad(DrawContext c, int x, int y, int w, int h, double off) {
        for (int i = 0; i < w; i += 2) {
            rect(c, x + i, y, x + Math.min(i + 2, w), y + h, Theme.grad(off + i * 0.006));
        }
    }

    public static int w(String s) {
        return tr().getWidth(s);
    }

    public static void text(DrawContext c, String s, int x, int y, int col, boolean shadow) {
        int m = mul(col);
        if ((m >>> 24) < 8) return;
        c.drawText(tr(), s, x, y, m, shadow);
    }

    public static void gradText(DrawContext c, String s, int x, int y, double off, double spread) {
        int cx = x;
        for (int i = 0; i < s.length(); i++) {
            String t = String.valueOf(s.charAt(i));
            text(c, t, cx, y, Theme.grad(off + (cx - x) * spread), true);
            cx += w(t);
        }
    }

    public static void line(DrawContext c, double x1, double y1, double x2, double y2, float th, int col) {
        double dx = x2 - x1, dy = y2 - y1;
        double len = Math.sqrt(dx * dx + dy * dy);
        if (len < 0.5) return;
        int m = mul(col);
        if ((m >>> 24) == 0) return;
        MatrixStack ms = c.getMatrices();
        ms.push();
        ms.translate(x1, y1, 0.0);
        ms.multiply(RotationAxis.POSITIVE_Z.rotation((float) Math.atan2(dy, dx)));
        ms.translate(0.0, -th / 2.0, 0.0);
        c.fill(0, 0, (int) Math.ceil(len), Math.max(1, Math.round(th)), m);
        ms.pop();
    }

    private Draw() {}
}
