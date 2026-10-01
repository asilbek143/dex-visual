package uz.dexvisual;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class ClickGui extends Screen {
    private static final int PW = 120, HH = 22, RH = 17, SR = 15;
    private static final Map<Module.Category, int[]> POS = new EnumMap<>(Module.Category.class);

    private record Hit(int x1, int y1, int x2, int y2, Module m, Setting s) {}

    private final List<Hit> hits = new ArrayList<>();
    private float open = 0f;
    private Setting dragSlider;
    private Hit dragHit;
    private Module.Category dragPanel;
    private int dragDX, dragDY;

    public ClickGui() {
        super(Text.literal("DEX VISUAL"));
    }

    @Override
    protected void init() {
        if (POS.isEmpty()) {
            Module.Category[] cs = Module.Category.values();
            int total = cs.length * PW + (cs.length - 1) * 12;
            int sx = (width - total) / 2;
            for (int i = 0; i < cs.length; i++) POS.put(cs[i], new int[]{sx + i * (PW + 12), 36});
        }
        open = 0f;
    }

    private static List<Module> modulesOf(Module.Category cat) {
        List<Module> l = new ArrayList<>();
        for (Module m : Mods.ALL) if (m.category == cat) l.add(m);
        return l;
    }

    private static int contentHeight(List<Module> list) {
        int h = 0;
        for (Module m : list) {
            h += RH;
            h += (int) (m.settings.size() * SR * Draw.easeOut(m.openAnim));
        }
        return h;
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // custom background drawn in render()
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        Draw.beginFrame();
        open = Draw.approach(open, 1f, 12f);
        hits.clear();
        Draw.alpha = 1f;
        Draw.gradV(ctx, 0, 0, width, height, Draw.a(0x07070D, 0.55f * open), Draw.a(0x1A0F2B, 0.78f * open));
        Draw.alpha = open;
        for (Module.Category cat : Module.Category.values()) panel(ctx, cat, mx, my);
        String hint = "LMB: toggle   |   RMB: settings   |   drag header: move   |   RSHIFT: close";
        Draw.alpha = open * 0.7f;
        Draw.text(ctx, hint, (width - Draw.w(hint)) / 2, height - 14, 0xFFB8B8C8, true);
        Draw.alpha = 1f;
    }

    private void panel(DrawContext ctx, Module.Category cat, int mx, int my) {
        int[] p = POS.get(cat);
        int x = p[0];
        int y = p[1] + (int) ((1f - open) * -18f);
        List<Module> list = modulesOf(cat);
        int total = HH + contentHeight(list) + 4;

        Draw.rrect(ctx, x + 2, y + 3, PW, total, 6, 0x50000000);
        Draw.rrect(ctx, x, y, PW, total, 6, 0xF0121219);
        String title = cat.name();
        Draw.gradText(ctx, title, x + (PW - Draw.w(title)) / 2, y + 7, 0, 0.012);
        Draw.hGrad(ctx, x + 6, y + HH - 3, PW - 12, 2, 0);

        int ry = y + HH;
        int idx = 0;
        for (Module m : list) {
            m.openAnim = Draw.approach(m.openAnim, m.open ? 1f : 0f, 14f);
            boolean hov = mx >= x + 3 && mx <= x + PW - 3 && my >= ry && my < ry + RH - 1;
            m.hover = Draw.approach(m.hover, hov ? 1f : 0f, 16f);
            Draw.rrect(ctx, x + 3, ry, PW - 6, RH - 2, 4, Draw.a(0xFFFFFF, 0.06f * m.hover));
            if (m.anim > 0.01f) {
                Draw.rrect(ctx, x + 3, ry, PW - 6, RH - 2, 4, Draw.a(Theme.grad(idx * 0.09), 0.78f * m.anim));
            }
            Draw.text(ctx, m.name, x + 9, ry + 4, Draw.lerp(0xFFA9A9B8, 0xFFFFFFFF, m.anim), false);
            if (!m.settings.isEmpty()) {
                Draw.text(ctx, m.open ? "-" : "+", x + PW - 13, ry + 4, 0xFFE0E0EA, false);
            }
            hits.add(new Hit(x + 3, ry, x + PW - 3, ry + RH - 1, m, null));
            ry += RH;

            int sh = (int) (m.settings.size() * SR * Draw.easeOut(m.openAnim));
            if (sh > 0) {
                ctx.enableScissor(x, ry, x + PW, ry + sh);
                for (int i = 0; i < m.settings.size(); i++) {
                    Setting s = m.settings.get(i);
                    int sy = ry + i * SR;
                    drawSetting(ctx, s, x, sy, i);
                    if (m.openAnim > 0.95f) hits.add(new Hit(x + 3, sy, x + PW - 3, sy + SR, m, s));
                }
                ctx.disableScissor();
                ry += sh;
            }
            idx++;
        }
    }

    private void drawSetting(DrawContext ctx, Setting s, int x, int sy, int i) {
        Draw.text(ctx, s.name, x + 10, sy + 2, 0xFFC8C8D4, false);
        switch (s.type) {
            case BOOL -> {
                s.anim = Draw.approach(s.anim, s.b ? 1f : 0f, 16f);
                int px = x + PW - 30, py = sy + 2;
                Draw.rrect(ctx, px, py, 20, 9, 4, Draw.lerp(0xFF2B2B38, Theme.grad(i * 0.12), s.anim));
                Draw.rrect(ctx, px + 1 + (int) (s.anim * 11), py + 1, 7, 7, 3, 0xFFFFFFFF);
            }
            case NUM -> {
                String v = s.valueText();
                Draw.text(ctx, v, x + PW - 10 - Draw.w(v), sy + 2, 0xFFFFFFFF, false);
                int tx1 = x + 10, tx2 = x + PW - 10;
                Draw.rect(ctx, tx1, sy + SR - 4, tx2, sy + SR - 2, 0xFF2B2B38);
                float fr = (float) ((s.n - s.min) / (s.max - s.min));
                int fx = tx1 + (int) ((tx2 - tx1) * fr);
                Draw.rect(ctx, tx1, sy + SR - 4, fx, sy + SR - 2, Theme.grad(i * 0.12));
                Draw.rrect(ctx, fx - 2, sy + SR - 5, 4, 4, 2, 0xFFFFFFFF);
            }
            case MODE -> {
                String v = s.valueText();
                Draw.text(ctx, v, x + PW - 10 - Draw.w(v), sy + 2, Theme.grad(i * 0.12), false);
            }
        }
    }

    private void slide(double mx) {
        if (dragSlider == null || dragHit == null) return;
        int x1 = dragHit.x1() + 7;
        int x2 = dragHit.x2() - 7;
        double fr = Math.max(0.0, Math.min(1.0, (mx - x1) / (double) (x2 - x1)));
        dragSlider.setNum(dragSlider.min + fr * (dragSlider.max - dragSlider.min));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        for (Module.Category cat : Module.Category.values()) {
            int[] p = POS.get(cat);
            if (mx >= p[0] && mx <= p[0] + PW && my >= p[1] && my <= p[1] + HH) {
                if (btn == 0) {
                    dragPanel = cat;
                    dragDX = (int) mx - p[0];
                    dragDY = (int) my - p[1];
                }
                return true;
            }
        }
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (mx >= h.x1() && mx <= h.x2() && my >= h.y1() && my < h.y2()) {
                if (h.s() == null) {
                    if (btn == 0) h.m().toggle();
                    else if (btn == 1 && !h.m().settings.isEmpty()) h.m().open = !h.m().open;
                } else {
                    Setting s = h.s();
                    if (s.type == Setting.Type.BOOL && btn == 0) {
                        s.b = !s.b;
                    } else if (s.type == Setting.Type.MODE) {
                        s.cycle(btn == 1 ? -1 : 1);
                    } else if (s.type == Setting.Type.NUM && btn == 0) {
                        dragSlider = s;
                        dragHit = h;
                        slide(mx);
                    }
                }
                return true;
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (dragPanel != null) {
            int[] p = POS.get(dragPanel);
            p[0] = Math.max(0, Math.min(width - PW, (int) mx - dragDX));
            p[1] = Math.max(0, Math.min(height - HH, (int) my - dragDY));
            return true;
        }
        if (dragSlider != null) {
            slide(mx);
            return true;
        }
        return super.mouseDragged(mx, my, btn, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        dragPanel = null;
        dragSlider = null;
        dragHit = null;
        return super.mouseReleased(mx, my, btn);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void removed() {
        Config.save();
    }
}
