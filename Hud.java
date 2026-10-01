package uz.dexvisual;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class Hud {
    private static LivingEntity shown;
    private static float tAnim = 0f, hpMain = 1f, hpTrail = 1f;
    private static final float[] KEY_ANIM = new float[6];

    public static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        Draw.beginFrame();
        if (mc.player == null || mc.world == null) {
            Proj.awaitFov = true;
            return;
        }
        DexVisual.frame(mc);
        Draw.alpha = 1f;
        if (mc.options.hudHidden) {
            Proj.awaitFov = true;
            return;
        }
        int sw = ctx.getScaledWindowWidth();
        int sh = ctx.getScaledWindowHeight();
        Proj.update(mc, sw, sh);

        WorldVisuals.render(ctx, mc, sw, sh);

        int leftY = 6;
        if (Mods.WATERMARK.enabled) leftY += watermark(ctx, mc, 6, leftY) + 6;
        if (Mods.RADAR.enabled) leftY += radar(ctx, mc, 6, leftY) + 6;
        if (Mods.POTIONS.enabled) potions(ctx, mc, 6, leftY);

        arraylist(ctx, sw);
        targetHud(ctx, mc, sw, sh);

        int bottom = sh - 6;
        if (Mods.INFO.enabled) bottom -= info(ctx, mc, 6, bottom);
        if (Mods.KEYSTROKES.enabled) keystrokes(ctx, mc, 6, bottom - 76);
        if (Mods.ARMOR.enabled) armor(ctx, mc, sw, sh);

        Notifs.render(ctx, sw, sh);
        Draw.alpha = 1f;
        Proj.awaitFov = true;
    }

    // ---------- watermark ----------
    private static int watermark(DrawContext ctx, MinecraftClient mc, int x, int y) {
        String brand = "DEX VISUAL";
        String info = mc.getCurrentFps() + " fps";
        int bw = Draw.w(brand);
        int w = bw + Draw.w(info) + 22;
        int h = 17;
        Draw.rrect(ctx, x + 1, y + 2, w, h, 5, 0x55000000);
        Draw.rrect(ctx, x, y, w, h, 5, 0xE0121219);
        Draw.hGrad(ctx, x + 5, y + h - 2, w - 10, 1, 0);
        Draw.gradText(ctx, brand, x + 7, y + 4, 0, 0.012);
        Draw.rect(ctx, x + 11 + bw, y + 4, x + 12 + bw, y + h - 4, 0x44FFFFFF);
        Draw.text(ctx, info, x + 16 + bw, y + 4, 0xFFC8C8D4, false);
        return h;
    }

    // ---------- arraylist ----------
    private static void arraylist(DrawContext ctx, int sw) {
        if (!Mods.ARRAYLIST.enabled) return;
        List<Module> list = new ArrayList<>();
        for (Module m : Mods.ALL) {
            if (m.category == Module.Category.RENDER && m.anim > 0.01f) list.add(m);
        }
        list.sort((a, b) -> Integer.compare(Draw.w(b.name), Draw.w(a.name)));
        int y = 6;
        int i = 0;
        for (Module m : list) {
            float a = Draw.easeOut(m.anim);
            int w = Draw.w(m.name) + 10;
            int xRight = sw - 4 + (int) ((1f - a) * (w + 8));
            int x = xRight - w;
            Draw.alpha = a;
            Draw.rrect(ctx, x, y, w, 12, 3, 0xAA121219);
            int col = Theme.grad(i * 0.08);
            Draw.rect(ctx, xRight - 2, y + 2, xRight, y + 10, col);
            Draw.text(ctx, m.name, x + 4, y + 2, col, true);
            Draw.alpha = 1f;
            y += (int) (14 * a);
            i++;
        }
    }

    // ---------- target hud ----------
    private static void targetHud(DrawContext ctx, MinecraftClient mc, int sw, int sh) {
        LivingEntity t = Mods.TARGET_HUD.enabled ? DexVisual.target() : null;
        if (t != null && t != shown) {
            shown = t;
            hpMain = hpTrail = Draw.clamp01(t.getHealth() / Math.max(1f, t.getMaxHealth()));
        }
        tAnim = Draw.approach(tAnim, t != null ? 1f : 0f, 10f);
        if (tAnim < 0.02f || shown == null) {
            if (t == null && tAnim < 0.02f) shown = null;
            return;
        }
        float ratio = Draw.clamp01(shown.getHealth() / Math.max(1f, shown.getMaxHealth()));
        hpMain = Draw.approach(hpMain, ratio, 14f);
        hpTrail = Draw.approach(hpTrail, ratio, 3.2f);

        int w = 134, h = 42;
        int x = sw / 2 + 30, y = sh / 2 + 24;
        float e = Draw.easeOut(tAnim);
        MatrixStack ms = ctx.getMatrices();
        ms.push();
        ms.translate(x + w / 2.0, y + h / 2.0, 0.0);
        float sc = 0.85f + 0.15f * e;
        ms.scale(sc, sc, 1f);
        ms.translate(-(x + w / 2.0), -(y + h / 2.0), 0.0);
        Draw.alpha = e;

        Draw.rrect(ctx, x + 1, y + 2, w, h, 6, 0x55000000);
        Draw.rrect(ctx, x, y, w, h, 6, 0xE0121219);
        Draw.hGrad(ctx, x + 6, y + 1, w - 12, 1, 0);
        Draw.text(ctx, shown.getName().getString(), x + 8, y + 7, 0xFFFFFFFF, true);
        String hp = String.format(Locale.ROOT, "%.1f", shown.getHealth());
        Draw.text(ctx, hp, x + w - 8 - Draw.w(hp), y + 7, Draw.lerp(0xFFFF5A6A, 0xFF34D399, ratio), true);

        int bx = x + 8, by = y + 21, bw = w - 16, bh = 7;
        Draw.rrect(ctx, bx, by, bw, bh, 3, 0xFF2B2B38);
        Draw.bar(ctx, bx, by, (int) (bw * hpTrail), bh, 0xFFFFD166);
        Draw.bar(ctx, bx, by, (int) (bw * hpMain), bh, Theme.grad(0.1));
        String dist = String.format(Locale.ROOT, "%.1fm", mc.player.distanceTo(shown));
        Draw.text(ctx, dist, x + 8, y + 32, 0xFF9A9AAE, false);

        Draw.alpha = 1f;
        ms.pop();
    }

    // ---------- radar ----------
    private static int radar(DrawContext ctx, MinecraftClient mc, int x, int y) {
        int s = 84;
        double range = Mods.RADAR_RANGE.n;
        Draw.rrect(ctx, x + 1, y + 2, s, s, 6, 0x55000000);
        Draw.rrect(ctx, x, y, s, s, 6, 0xC0121219);
        Draw.hGrad(ctx, x + 6, y + 1, s - 12, 1, 0.3);
        int cx = x + s / 2, cy = y + s / 2;
        Draw.rect(ctx, x + 4, cy, x + s - 4, cy + 1, 0x22FFFFFF);
        Draw.rect(ctx, cx, y + 4, cx + 1, y + s - 4, 0x22FFFFFF);
        double yaw = Math.toRadians(mc.player.getYaw());
        double fxv = -Math.sin(yaw), fzv = Math.cos(yaw), rxv = -Math.cos(yaw), rzv = -Math.sin(yaw);
        double scale = (s / 2.0 - 5.0) / range;
        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player) continue;
            double dx = p.getX() - mc.player.getX();
            double dz = p.getZ() - mc.player.getZ();
            double sx = (dx * rxv + dz * rzv) * scale;
            double sz = (dx * fxv + dz * fzv) * scale;
            double lim = s / 2.0 - 5.0;
            double len = Math.hypot(sx, sz);
            if (len > lim) {
                sx = sx / len * lim;
                sz = sz / len * lim;
            }
            int px = (int) (cx + sx), py = (int) (cy - sz);
            Draw.rect(ctx, px - 2, py - 2, px + 3, py + 3, 0xFF000000);
            Draw.rect(ctx, px - 1, py - 1, px + 2, py + 2, Theme.grad(0));
        }
        Draw.rect(ctx, cx - 1, cy - 1, cx + 2, cy + 2, 0xFFFFFFFF);
        return s;
    }

    // ---------- potions ----------
    private static void potions(DrawContext ctx, MinecraftClient mc, int x, int y) {
        int i = 0;
        for (StatusEffectInstance ef : mc.player.getStatusEffects()) {
            String name = ef.getEffectType().value().getName().getString();
            if (ef.getAmplifier() > 0) name += " " + (ef.getAmplifier() + 1);
            String time;
            if (ef.isInfinite()) {
                time = "inf";
            } else {
                int sec = ef.getDuration() / 20;
                time = (sec / 60) + ":" + String.format(Locale.ROOT, "%02d", sec % 60);
            }
            int w = Draw.w(name) + Draw.w(time) + 24;
            int yy = y + i * 15;
            Draw.rrect(ctx, x, yy, w, 13, 3, 0xB0121219);
            Draw.rect(ctx, x + 3, yy + 3, x + 5, yy + 10, 0xFF000000 | ef.getEffectType().value().getColor());
            Draw.text(ctx, name, x + 9, yy + 3, 0xFFFFFFFF, true);
            Draw.text(ctx, time, x + w - 5 - Draw.w(time), yy + 3, 0xFFB8B8C8, true);
            i++;
        }
    }

    // ---------- info ----------
    private static int info(DrawContext ctx, MinecraftClient mc, int x, int bottom) {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"XYZ", mc.player.getBlockX() + " " + mc.player.getBlockY() + " " + mc.player.getBlockZ()});
        rows.add(new String[]{"BPS", String.format(Locale.ROOT, "%.1f", DexVisual.bps)});
        if (mc.getNetworkHandler() != null) {
            PlayerListEntry en = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            if (en != null) rows.add(new String[]{"PING", en.getLatency() + "ms"});
        }
        int h = rows.size() * 11 + 2;
        int top = bottom - h;
        int i = 0;
        for (String[] r : rows) {
            int yy = top + i * 11;
            Draw.text(ctx, r[0], x, yy, Theme.grad(i * 0.15), true);
            Draw.text(ctx, r[1], x + 28, yy, 0xFFFFFFFF, true);
            i++;
        }
        return h + 6;
    }

    // ---------- keystrokes ----------
    private static void key(DrawContext ctx, int x, int y, int w, int h, String label, int idx, boolean down) {
        KEY_ANIM[idx] = Draw.approach(KEY_ANIM[idx], down ? 1f : 0f, 22f);
        float a = KEY_ANIM[idx];
        Draw.rrect(ctx, x, y, w, h, 4, Draw.lerp(0xB0121219, Draw.a(Theme.grad(idx * 0.15), 0.9f), a));
        Draw.text(ctx, label, x + (w - Draw.w(label)) / 2, y + (h - 8) / 2, Draw.lerp(0xFFB8B8C8, 0xFFFFFFFF, a), false);
    }

    private static void keystrokes(DrawContext ctx, MinecraftClient mc, int x, int y) {
        var o = mc.options;
        key(ctx, x + 25, y, 22, 22, "W", 0, o.forwardKey.isPressed());
        key(ctx, x, y + 25, 22, 22, "A", 1, o.leftKey.isPressed());
        key(ctx, x + 25, y + 25, 22, 22, "S", 2, o.backKey.isPressed());
        key(ctx, x + 50, y + 25, 22, 22, "D", 3, o.rightKey.isPressed());
        key(ctx, x, y + 50, 35, 20, "LMB", 4, o.attackKey.isPressed());
        key(ctx, x + 37, y + 50, 35, 20, "RMB", 5, o.useKey.isPressed());
    }

    // ---------- armor ----------
    private static void armor(DrawContext ctx, MinecraftClient mc, int sw, int sh) {
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        int x = sw - 26, y = sh / 2 - 38;
        Draw.rrect(ctx, x - 3, y - 3, 24, 78, 5, 0x80121219);
        for (int i = 0; i < 4; i++) {
            ItemStack st = mc.player.getEquippedStack(slots[i]);
            if (!st.isEmpty()) {
                ctx.drawItem(st, x, y + i * 19);
                ctx.drawStackOverlay(Draw.tr(), st, x, y + i * 19);
            }
        }
    }

    private Hud() {}
}
