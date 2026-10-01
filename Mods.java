package uz.dexvisual;

import java.util.ArrayList;
import java.util.List;

import static uz.dexvisual.Module.Category.CLIENT;
import static uz.dexvisual.Module.Category.HUD;
import static uz.dexvisual.Module.Category.RENDER;

public final class Mods {
    public static final List<Module> ALL = new ArrayList<>();

    // ---- ESP ----
    public static final Setting ESP_PLAYERS = Setting.bool("Players", true);
    public static final Setting ESP_HOSTILE = Setting.bool("Hostile", true);
    public static final Setting ESP_ANIMALS = Setting.bool("Animals", false);
    public static final Setting ESP_ITEMS = Setting.bool("Items", false);
    public static final Setting ESP_STYLE = Setting.mode("Style", 0, "Corners", "Box");
    public static final Setting ESP_NAMES = Setting.bool("Names", true);
    public static final Setting ESP_HEALTH = Setting.bool("Health", true);
    public static final Setting ESP_DIST = Setting.bool("Distance", true);
    public static final Setting ESP_GLOW = Setting.bool("Glow", false);
    public static final Setting ESP_RANGE = Setting.num("Range", 64, 16, 128, 8);
    public static final Module ESP = reg(new Module("ESP", RENDER, true)
            .add(ESP_PLAYERS).add(ESP_HOSTILE).add(ESP_ANIMALS).add(ESP_ITEMS).add(ESP_STYLE)
            .add(ESP_NAMES).add(ESP_HEALTH).add(ESP_DIST).add(ESP_GLOW).add(ESP_RANGE));

    // ---- Target ESP ----
    public static final Setting TARGET_RING = Setting.bool("Ring", true);
    public static final Setting TARGET_GLOW = Setting.bool("Glow", true);
    public static final Module TARGET_ESP = reg(new Module("Target ESP", RENDER, true)
            .add(TARGET_RING).add(TARGET_GLOW));

    // ---- Tracers ----
    public static final Setting TR_PLAYERS = Setting.bool("Players", true);
    public static final Setting TR_HOSTILE = Setting.bool("Hostile", false);
    public static final Setting TR_ANIMALS = Setting.bool("Animals", false);
    public static final Setting TR_FROM = Setting.mode("From", 0, "Bottom", "Crosshair");
    public static final Setting TR_THICK = Setting.num("Thickness", 1.5, 1, 3, 0.5);
    public static final Setting TR_RANGE = Setting.num("Range", 96, 16, 128, 8);
    public static final Module TRACERS = reg(new Module("Tracers", RENDER, false)
            .add(TR_PLAYERS).add(TR_HOSTILE).add(TR_ANIMALS).add(TR_FROM).add(TR_THICK).add(TR_RANGE));

    // ---- Storage ESP ----
    public static final Setting STORAGE_RANGE = Setting.num("Range", 48, 16, 96, 8);
    public static final Setting STORAGE_FILL = Setting.bool("Fill", true);
    public static final Module STORAGE = reg(new Module("Storage ESP", RENDER, false)
            .add(STORAGE_RANGE).add(STORAGE_FILL));

    // ---- Misc visuals ----
    public static final Setting JUMP_SIZE = Setting.num("Size", 1.6, 0.8, 3.0, 0.2);
    public static final Module JUMP = reg(new Module("Jump Circles", RENDER, false).add(JUMP_SIZE));
    public static final Setting TRAIL_LEN = Setting.num("Length", 40, 10, 80, 5);
    public static final Module TRAILS = reg(new Module("Trails", RENDER, false).add(TRAIL_LEN));
    public static final Module HAT = reg(new Module("China Hat", RENDER, false));
    public static final Module FULLBRIGHT = reg(new Module("Fullbright", RENDER, false));
    public static final Setting ZOOM_FOV = Setting.num("Zoom FOV", 25, 10, 60, 5);
    public static final Module ZOOM = reg(new Module("Zoom", RENDER, true).add(ZOOM_FOV));
    public static final Module HITBOXES = reg(new Module("Hitboxes", RENDER, false));
    public static final Module NO_HURT = reg(new Module("No Hurt Cam", RENDER, false));

    // ---- HUD ----
    public static final Module WATERMARK = reg(new Module("Watermark", HUD, true));
    public static final Module ARRAYLIST = reg(new Module("Arraylist", HUD, true));
    public static final Module TARGET_HUD = reg(new Module("Target HUD", HUD, true));
    public static final Setting RADAR_RANGE = Setting.num("Range", 32, 16, 64, 4);
    public static final Module RADAR = reg(new Module("Radar", HUD, true).add(RADAR_RANGE));
    public static final Module POTIONS = reg(new Module("Potions", HUD, true));
    public static final Module KEYSTROKES = reg(new Module("Keystrokes", HUD, false));
    public static final Module ARMOR = reg(new Module("Armor HUD", HUD, true));
    public static final Module INFO = reg(new Module("Info", HUD, true));

    // ---- Client ----
    public static final Setting THEME = Setting.mode("Theme", 0, Theme.NAMES);
    public static final Module INTERFACE = reg(new Module("Interface", CLIENT, true).add(THEME));
    public static final Module NOTIFS = reg(new Module("Notifications", CLIENT, true));

    private static Module reg(Module m) {
        ALL.add(m);
        return m;
    }

    private Mods() {}
}
