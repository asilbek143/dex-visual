package uz.dexvisual;

import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** All 3D-anchored visuals, drawn in 2D through screen projection. */
public final class WorldVisuals {
    private record Circle(double x, double y, double z, long start) {}

    private static final List<Circle> CIRCLES = new ArrayList<>();
    private static final ArrayDeque<double[]> TRAIL = new ArrayDeque<>();
    private static final List<double[]> STORAGE = new ArrayList<>();
    private static final Map<Integer, Long> FIRST_SEEN = new HashMap<>();
    private static final Set<Integer> TOUCHED = new HashSet<>();
    private static boolean wasGround = true;
    private static int storageTimer = 0;

    // ---------- helpers ----------
    /** 0 player, 1 hostile, 2 animal, 3 item, -1 other */
    public static int kind(Entity e) {
        if (e instanceof PlayerEntity) return 0;
        if (e instanceof Monster) return 1;
        if (e instanceof AnimalEntity) return 2;
        if (e instanceof ItemEntity) return 3;
        return -1;
    }

    private static boolean espKind(int k) {
        return switch (k) {
            case 0 -> Mods.ESP_PLAYERS.b;
            case 1 -> Mods.ESP_HOSTILE.b;
            case 2 -> Mods.ESP_ANIMALS.b;
            case 3 -> Mods.ESP_ITEMS.b;
            default -> false;
        };
    }

    public static boolean glowKind(Entity e) {
        int k = kind(e);
        return k >= 0 && espKind(k);
    }

    public static int kindColor(int k) {
        return switch (k) {
            case 0 -> Theme.grad(0);
            case 1 -> 0xFFFF6B4A;
            case 2 -> 0xFF5BE38A;
            default -> 0xFF5BD6FF;
        };
    }

    private static double lerp(double a, double b, float t) {
        return a + (b - a) * t;
    }

    private static boolean bounds(double x1, double y1, double z1, double x2, double y2, double z2, double[] out) {
        double minX = 1e9, minY = 1e9, maxX = -1e9, maxY = -1e9;
        double[] p = new double[2];
        for (int i = 0; i < 8; i++) {
            double x = (i & 1) == 0 ? x1 : x2;
            double y = (i & 2) == 0 ? y1 : y2;
            double z = (i & 4) == 0 ? z1 : z2;
            if (!Proj.project(x, y, z, p)) return false;
            minX = Math.min(minX, p[0]);
            maxX = Math.max(maxX, p[0]);
            minY = Math.min(minY, p[1]);
            maxY = Math.max(maxY, p[1]);
        }
        out[0] = minX;
        out[1] = minY;
        out[2] = maxX;
        out[3] = maxY;
        return true;
    }

    // ---------- tick ----------
    public static void tick(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.world == null) {
            CIRCLES.clear();
            TRAIL.clear();
            return;
        }
        boolean ground = p.isOnGround();
        if (Mods.JUMP.enabled && wasGround && !ground && p.getVelocity().y > 0.1) {
            CIRCLES.add(new Circle(p.getX(), p.getY(), p.getZ(), System.nanoTime()));
        }
        wasGround = ground;

        if (Mods.TRAILS.enabled) {
            TRAIL.addLast(new double[]{p.getX(), p.getY() + 0.05, p.getZ()});
            int max = (int) Mods.TRAIL_LEN.n;
            while (TRAIL.size() > max) TRAIL.removeFirst();
        } else {
            TRAIL.clear();
        }

        if (Mods.STORAGE.enabled) {
            if (++storageTimer >= 10) {
                storageTimer = 0;
                scanStorage(mc);
            }
        } else {
            STORAGE.clear();
        }
    }

    private static void scanStorage(MinecraftClient mc) {
        STORAGE.clear();
        try {
            double range = Mods.STORAGE_RANGE.n;
            int r = (int) Math.ceil(range / 16.0);
            int pcx = mc.player.getBlockX() >> 4;
            int pcz = mc.player.getBlockZ() >> 4;
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    WorldChunk ch = mc.world.getChunkManager().getWorldChunk(pcx + dx, pcz + dz);
                    if (ch == null) continue;
                    for (BlockEntity be : ch.getBlockEntities().values()) {
                        int col;
                        if (be instanceof EnderChestBlockEntity) col = 0xFFB04CFF;
                        else if (be instanceof ChestBlockEntity) col = 0xFFFFA726;
                        else if (be instanceof BarrelBlockEntity) col = 0xFFC9A66B;
                        else if (be instanceof ShulkerBoxBlockEntity) col = 0xFFFF6FB5;
                        else continue;
                        BlockPos bp = be.getPos();
                        double ddx = bp.getX() + 0.5 - mc.player.getX();
                        double ddy = bp.getY() + 0.5 - mc.player.getY();
                        double ddz = bp.getZ() + 0.5 - mc.player.getZ();
                        if (ddx * ddx + ddy * ddy + ddz * ddz > range * range) continue;
                        STORAGE.add(new double[]{bp.getX(), bp.getY(), bp.getZ(), col});
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    // ---------- render ----------
    public static void render(DrawContext ctx, MinecraftClient mc, int sw, int sh) {
        float pt = Proj.partial;
        if (Mods.STORAGE.enabled) storage(ctx);
        if (Mods.JUMP.enabled) circles(ctx);
        if (Mods.TRAILS.enabled) trails(ctx);
        if (Mods.HAT.enabled) hat(ctx, mc, pt);
        if (Mods.TRACERS.enabled) tracers(ctx, mc, pt, sw, sh);
        if (Mods.ESP.enabled) esp(ctx, mc, pt);
        if (Mods.TARGET_ESP.enabled && Mods.TARGET_RING.b) targetRing(ctx, pt);
        Draw.alpha = 1f;
    }

    private static void esp(DrawContext ctx, MinecraftClient mc, float pt) {
        final long now = System.nanoTime();
        TOUCHED.clear();
        double range = Mods.ESP_RANGE.n;
        boolean corners = Mods.ESP_STYLE.idx == 0;
        double[] box = new double[4];
        for (Entity e : mc.world.getEntities()) {
            if (e == mc.player || e.isSpectator()) continue;
            int k = kind(e);
            if (k < 0 || !espKind(k)) continue;
            float dist = mc.player.distanceTo(e);
            if (dist > range) continue;
            double x = lerp(e.prevX, e.getX(), pt);
            double y = lerp(e.prevY, e.getY(), pt);
            double z = lerp(e.prevZ, e.getZ(), pt);
            double hw = e.getWidth() / 2.0;
            double h = e.getHeight();
            if (!bounds(x - hw, y, z - hw, x + hw, y + h, z + hw, box)) continue;
            int x1 = (int) box[0], y1 = (int) box[1], x2 = (int) box[2], y2 = (int) box[3];
            if (x2 - x1 < 2 || y2 - y1 < 2) continue;

            TOUCHED.add(e.getId());
            long first = FIRST_SEEN.computeIfAbsent(e.getId(), id -> now);
            Draw.alpha = Draw.easeOut((now - first) / 3.0e8f);

            int col = kindColor(k);
            if (corners) cornerBox(ctx, x1, y1, x2, y2, col);
            else outlineBox(ctx, x1, y1, x2, y2, col);

            if (Mods.ESP_HEALTH.b && e instanceof LivingEntity le) {
                float r = Draw.clamp01(le.getHealth() / Math.max(1f, le.getMaxHealth()));
                int bx = x1 - 5;
                int bh = y2 - y1;
                Draw.rect(ctx, bx - 1, y1 - 1, bx + 3, y2 + 1, 0xAA000000);
                int fh = (int) (bh * r);
                Draw.rect(ctx, bx, y2 - fh, bx + 2, y2, Draw.lerp(0xFFFF3B30, 0xFF34D399, r));
            }

            if (Mods.ESP_NAMES.b || Mods.ESP_DIST.b) {
                String label = "";
                if (Mods.ESP_NAMES.b) {
                    if (e instanceof ItemEntity ie) label = ie.getStack().getName().getString() + " x" + ie.getStack().getCount();
                    else label = e.getName().getString();
                }
                if (Mods.ESP_DIST.b) label += (label.isEmpty() ? "" : " ") + "[" + (int) dist + "m]";
                int tw = Draw.w(label);
                int tx = (x1 + x2) / 2 - tw / 2;
                int ty = y1 - 14;
                Draw.rrect(ctx, tx - 3, ty - 2, tw + 6, 12, 3, 0x99000000);
                Draw.text(ctx, label, tx, ty, 0xFFFFFFFF, true);
            }
        }
        FIRST_SEEN.keySet().retainAll(TOUCHED);
        Draw.alpha = 1f;
    }

    private static void hl(DrawContext c, int x, int y, int len, int col) {
        Draw.rect(c, x - 1, y - 1, x + len + 1, y + 2, 0xAA000000);
        Draw.rect(c, x, y, x + len, y + 1, col);
    }

    private static void vl(DrawContext c, int x, int y, int len, int col) {
        Draw.rect(c, x - 1, y - 1, x + 2, y + len + 1, 0xAA000000);
        Draw.rect(c, x, y, x + 1, y + len, col);
    }

    private static void cornerBox(DrawContext c, int x1, int y1, int x2, int y2, int col) {
        int len = Math.max(3, Math.min(x2 - x1, y2 - y1) / 4);
        hl(c, x1, y1, len, col);
        vl(c, x1, y1, len, col);
        hl(c, x2 - len, y1, len, col);
        vl(c, x2 - 1, y1, len, col);
        hl(c, x1, y2 - 1, len, col);
        vl(c, x1, y2 - len, len, col);
        hl(c, x2 - len, y2 - 1, len, col);
        vl(c, x2 - 1, y2 - len, len, col);
    }

    private static void outlineBox(DrawContext c, int x1, int y1, int x2, int y2, int col) {
        Draw.rect(c, x1 - 1, y1 - 1, x2 + 1, y2 + 1, 0xAA000000);
        hl(c, x1, y1, x2 - x1, col);
        hl(c, x1, y2 - 1, x2 - x1, col);
        vl(c, x1, y1, y2 - y1, col);
        vl(c, x2 - 1, y1, y2 - y1, col);
        Draw.rect(c, x1 + 1, y1 + 1, x2 - 1, y2 - 1, Draw.a(col, 0.07f));
    }

    private static void tracers(DrawContext ctx, MinecraftClient mc, float pt, int sw, int sh) {
        double range = Mods.TR_RANGE.n;
        double ox = sw / 2.0;
        double oy = Mods.TR_FROM.idx == 0 ? sh : sh / 2.0;
        float th = (float) Mods.TR_THICK.n;
        double[] o = new double[2];
        for (Entity e : mc.world.getEntities()) {
            if (e == mc.player || e.isSpectator()) continue;
            int k = kind(e);
            if (k < 0 || k == 3) continue;
            boolean on = k == 0 ? Mods.TR_PLAYERS.b : (k == 1 ? Mods.TR_HOSTILE.b : Mods.TR_ANIMALS.b);
            if (!on || mc.player.distanceTo(e) > range) continue;
            double x = lerp(e.prevX, e.getX(), pt);
            double y = lerp(e.prevY, e.getY(), pt);
            double z = lerp(e.prevZ, e.getZ(), pt);
            Proj.projectAny(x, y, z, o);
            int col = Draw.a(kindColor(k), 0.9f);
            Draw.line(ctx, ox, oy, o[0], o[1], th, col);
        }
    }

    private static void storage(DrawContext ctx) {
        double[] box = new double[4];
        for (double[] s : STORAGE) {
            if (!bounds(s[0] + 0.06, s[1], s[2] + 0.06, s[0] + 0.94, s[1] + 0.88, s[2] + 0.94, box)) continue;
            int x1 = (int) box[0], y1 = (int) box[1], x2 = (int) box[2], y2 = (int) box[3];
            if (x2 - x1 < 2 || y2 - y1 < 2) continue;
            int col = (int) s[3];
            if (Mods.STORAGE_FILL.b) Draw.rect(ctx, x1, y1, x2, y2, Draw.a(col, 0.18f));
            int line = Draw.a(col, 0.95f);
            Draw.rect(ctx, x1, y1, x2, y1 + 1, line);
            Draw.rect(ctx, x1, y2 - 1, x2, y2, line);
            Draw.rect(ctx, x1, y1, x1 + 1, y2, line);
            Draw.rect(ctx, x2 - 1, y1, x2, y2, line);
        }
    }

    private static void ring(DrawContext ctx, double x, double y, double z, double r, int seg,
                             float th, float alpha, double hueOff, double rot) {
        double[] p = new double[2];
        double px = 0, py = 0;
        boolean prevOk = false;
        for (int i = 0; i <= seg; i++) {
            double ang = rot + i * Math.PI * 2.0 / seg;
            boolean ok = Proj.project(x + Math.cos(ang) * r, y, z + Math.sin(ang) * r, p);
            if (ok && prevOk) {
                Draw.line(ctx, px, py, p[0], p[1], th, Draw.a(Theme.grad(hueOff + i / (double) seg), alpha));
            }
            px = p[0];
            py = p[1];
            prevOk = ok;
        }
    }

    private static void circles(DrawContext ctx) {
        long now = System.nanoTime();
        CIRCLES.removeIf(c -> (now - c.start) / 1.0e9 > 1.0);
        for (Circle c : CIRCLES) {
            float t = (now - c.start) / 1.0e9f;
            float e = Draw.easeOut(t);
            float a = 1f - t;
            double r = Mods.JUMP_SIZE.n * e;
            ring(ctx, c.x, c.y + 0.02, c.z, r, 36, 2f, a, 0, 0);
            ring(ctx, c.x, c.y + 0.02, c.z, r * 0.7, 28, 1f, a * 0.6f, 0.3, 0);
        }
    }

    private static void trails(DrawContext ctx) {
        double[] p = new double[2];
        double px = 0, py = 0;
        boolean prevOk = false;
        int i = 0;
        int n = TRAIL.size();
        for (double[] t : TRAIL) {
            boolean ok = Proj.project(t[0], t[1], t[2], p);
            if (ok && prevOk) {
                float al = (float) i / Math.max(1, n);
                Draw.line(ctx, px, py, p[0], p[1], 2f, Draw.a(Theme.grad(i * 0.03), al * 0.9f));
            }
            px = p[0];
            py = p[1];
            prevOk = ok;
            i++;
        }
    }

    private static void hat(DrawContext ctx, MinecraftClient mc, float pt) {
        if (mc.options.getPerspective().isFirstPerson()) return;
        Entity e = mc.player;
        double x = lerp(e.prevX, e.getX(), pt);
        double y = lerp(e.prevY, e.getY(), pt);
        double z = lerp(e.prevZ, e.getZ(), pt);
        double top = y + e.getHeight() + 0.02;
        double apex = top + 0.42;
        double rad = 0.5;
        double rot = System.nanoTime() / 1.0e9 * 0.6;
        ring(ctx, x, top, z, rad, 32, 1.5f, 0.95f, 0, rot);
        double[] a = new double[2];
        double[] b = new double[2];
        if (!Proj.project(x, apex, z, a)) return;
        for (int i = 0; i < 8; i++) {
            double ang = rot + i * Math.PI / 4.0;
            if (Proj.project(x + Math.cos(ang) * rad, top, z + Math.sin(ang) * rad, b)) {
                Draw.line(ctx, a[0], a[1], b[0], b[1], 1f, Draw.a(Theme.grad(i * 0.12), 0.7f));
            }
        }
    }

    private static void targetRing(DrawContext ctx, float pt) {
        LivingEntity t = DexVisual.target();
        if (t == null) return;
        double x = lerp(t.prevX, t.getX(), pt);
        double y = lerp(t.prevY, t.getY(), pt);
        double z = lerp(t.prevZ, t.getZ(), pt);
        double rad = t.getWidth() * 0.75 + 0.15;
        double h = t.getHeight();
        double time = System.nanoTime() / 1.0e9;
        for (int k = 0; k < 5; k++) {
            double ph = time * 1.8 - k * 0.10;
            double hh = (0.5 + 0.5 * Math.sin(ph * 3.0)) * h;
            float a = (1f - k / 5f) * 0.95f;
            ring(ctx, x, y + hh, z, rad, 28, 2f - k * 0.2f, a, time * 0.3, time * 0.8);
        }
    }

    private WorldVisuals() {}
}
