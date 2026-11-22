package com.extantape.mobsquad;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import org.bukkit.configuration.file.FileConfiguration;

/** The settings in config.yml, read once and checked. */
public record Settings(Set<String> disabledWorlds, int recoveryMinutes, int coaxXp, Set<String> coatWorlds) {
    public static Settings read(FileConfiguration c, Logger log) {
        return new Settings(new HashSet<>(c.getStringList("disabled-worlds")),
            clamp(c.getInt("recovery-minutes", 5), 0, 1440, "recovery-minutes", log),
            clamp(c.getInt("coax-xp", 100), 0, 100000, "coax-xp", log),
            new HashSet<>(c.getStringList("spirit-coats.worlds")));
    }

    public boolean enabledIn(String world) {
        return !disabledWorlds.contains(world);
    }

    /** Whether spirit coats are shown in this world: only the ones listed under spirit-coats.worlds, none by default. */
    public boolean coatsIn(String world) {
        return coatWorlds.contains(world);
    }

    static int clamp(int v, int min, int max, String key, Logger log) {
        if (v >= min && v <= max) return v;
        int to = Math.max(min, Math.min(max, v));
        log.warning(key + " must be between " + min + " and " + max + ". Using " + to + ".");
        return to;
    }
}
