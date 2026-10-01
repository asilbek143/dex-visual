package uz.dexvisual;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("dexvisual.json");
    }

    public static void load() {
        try {
            Path p = path();
            if (!Files.exists(p)) return;
            Map<String, Object> map = GSON.fromJson(Files.readString(p), new TypeToken<Map<String, Object>>() {}.getType());
            if (map == null) return;
            for (Module m : Mods.ALL) {
                Object en = map.get(m.name);
                if (en instanceof Boolean bv) m.enabled = bv;
                m.anim = m.enabled ? 1f : 0f;
                for (Setting s : m.settings) s.load(map.get(m.name + "." + s.name));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void save() {
        try {
            Map<String, Object> map = new LinkedHashMap<>();
            for (Module m : Mods.ALL) {
                map.put(m.name, m.enabled);
                for (Setting s : m.settings) map.put(m.name + "." + s.name, s.save());
            }
            Files.writeString(path(), GSON.toJson(map));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Config() {}
}
