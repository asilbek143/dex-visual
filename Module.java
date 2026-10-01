package uz.dexvisual;

import java.util.ArrayList;
import java.util.List;

public final class Module {
    public enum Category { RENDER, HUD, CLIENT }

    public final String name;
    public final Category category;
    public boolean enabled;
    public final List<Setting> settings = new ArrayList<>();

    // UI / animation state
    public float anim;
    public float openAnim;
    public float hover;
    public boolean open;

    public Module(String name, Category category, boolean def) {
        this.name = name;
        this.category = category;
        this.enabled = def;
        this.anim = def ? 1f : 0f;
    }

    public Module add(Setting s) {
        settings.add(s);
        return this;
    }

    public void toggle() {
        enabled = !enabled;
        Notifs.push(name, enabled);
    }
}
