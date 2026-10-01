package uz.dexvisual;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ActionResult;
import org.lwjgl.glfw.GLFW;
import uz.dexvisual.mixin.SimpleOptionAccessor;

public class DexVisual implements ClientModInitializer {
    public static KeyBinding menuKey;
    public static KeyBinding zoomKey;
    public static long lastTickNs = System.nanoTime();
    public static double bps = 0;

    private static boolean gammaApplied = false;
    private static double savedGamma = 1.0;
    private static boolean zoomActive = false;
    private static int savedFov = 70;
    private static float zoomCur = 70f;
    private static boolean lastHitboxes = false;
    private static Entity lastTarget;
    private static long lastTargetTime;

    @Override
    public void onInitializeClient() {
        Config.load();

        menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.dexvisual.menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "category.dexvisual"));
        zoomKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.dexvisual.zoom", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_C, "category.dexvisual"));

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClient && entity instanceof LivingEntity) {
                lastTarget = entity;
                lastTargetTime = System.currentTimeMillis();
            }
            return ActionResult.PASS;
        });

        ClientTickEvents.END_CLIENT_TICK.register(DexVisual::tick);
        HudRenderCallback.EVENT.register(Hud::render);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            restoreOptions(client);
            Config.save();
        });
    }

    @SuppressWarnings("unchecked")
    private static <T> void force(SimpleOption<T> option, T value) {
        ((SimpleOptionAccessor<T>) (Object) option).dex$setValue(value);
    }

    private static void tick(MinecraftClient mc) {
        lastTickNs = System.nanoTime();
        while (menuKey.wasPressed()) {
            if (mc.currentScreen == null) mc.setScreen(new ClickGui());
        }
        if (mc.player == null || mc.world == null) return;

        double dx = mc.player.getX() - mc.player.prevX;
        double dz = mc.player.getZ() - mc.player.prevZ;
        bps += (Math.sqrt(dx * dx + dz * dz) * 20.0 - bps) * 0.3;

        if (Mods.HITBOXES.enabled != lastHitboxes) {
            mc.getEntityRenderDispatcher().setRenderHitboxes(Mods.HITBOXES.enabled);
            lastHitboxes = Mods.HITBOXES.enabled;
        }
        WorldVisuals.tick(mc);
    }

    /** Per-frame logic: animations, fullbright, smooth zoom. */
    public static void frame(MinecraftClient mc) {
        for (Module m : Mods.ALL) m.anim = Draw.approach(m.anim, m.enabled ? 1f : 0f, 12f);

        if (Mods.FULLBRIGHT.enabled) {
            if (!gammaApplied) {
                savedGamma = mc.options.getGamma().getValue();
                gammaApplied = true;
            }
            force(mc.options.getGamma(), 16.0);
        } else if (gammaApplied) {
            force(mc.options.getGamma(), savedGamma);
            gammaApplied = false;
        }

        boolean held = Mods.ZOOM.enabled && mc.currentScreen == null && zoomKey.isPressed();
        if (held && !zoomActive) {
            savedFov = mc.options.getFov().getValue();
            zoomCur = savedFov;
            zoomActive = true;
        }
        if (zoomActive) {
            float target = held ? (float) Mods.ZOOM_FOV.n : savedFov;
            zoomCur = Draw.approach(zoomCur, target, 14f);
            force(mc.options.getFov(), Math.round(zoomCur));
            if (!held && Math.abs(zoomCur - savedFov) < 0.6f) {
                force(mc.options.getFov(), savedFov);
                zoomActive = false;
            }
        }
    }

    private static void restoreOptions(MinecraftClient mc) {
        if (mc.options == null) return;
        if (gammaApplied) {
            force(mc.options.getGamma(), savedGamma);
            gammaApplied = false;
        }
        if (zoomActive) {
            force(mc.options.getFov(), savedFov);
            zoomActive = false;
        }
    }

    // ---------- targeting ----------
    /** Entity you last hit (4s) or are looking at. */
    public static LivingEntity target() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (lastTarget instanceof LivingEntity le && le != mc.player && le.isAlive() && !le.isRemoved()
                && System.currentTimeMillis() - lastTargetTime < 4000) {
            return le;
        }
        if (mc.targetedEntity instanceof LivingEntity le2 && le2 != mc.player) return le2;
        return null;
    }

    // ---------- glow outline (used by EntityMixin) ----------
    public static boolean glow(Entity e) {
        if (e == null || !e.getWorld().isClient) return false;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || e == mc.player) return false;
        if (Mods.TARGET_ESP.enabled && Mods.TARGET_GLOW.b && e == target()) return true;
        return Mods.ESP.enabled && Mods.ESP_GLOW.b && WorldVisuals.glowKind(e);
    }

    /** RGB color for the outline, or -1 for default. */
    public static int glowColor(Entity e) {
        if (!glow(e)) return -1;
        if (Mods.TARGET_ESP.enabled && Mods.TARGET_GLOW.b && e == target() && e instanceof LivingEntity le) {
            float r = Math.max(0f, Math.min(1f, le.getHealth() / Math.max(1f, le.getMaxHealth())));
            return ((int) (255 * (1 - r)) << 16) | ((int) (255 * r) << 8);
        }
        int k = WorldVisuals.kind(e);
        return k < 0 ? -1 : (WorldVisuals.kindColor(k) & 0xFFFFFF);
    }
}
