package com.extantape.mobsquad;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import org.bukkit.configuration.file.FileConfiguration;

/** The settings in config.yml, read once and checked. */
public record Settings(Set<String> disabledWorlds, int recoveryMinutes, int coaxXp, Set<String> coatWorlds, Set<String> noGlowWorlds,
                       com.extantape.mobsquad.model.Unlocks.Mode tierUnlock) {
    public static Settings read(FileConfiguration c, Logger log) {
        Settings s = new Settings(new HashSet<>(c.getStringList("disabled-worlds")),
            clamp(c.getInt("recovery-minutes", 5), 0, 1440, "recovery-minutes", log),
            clamp(c.getInt("coax-xp", 100), 0, 100000, "coax-xp", log),
            new HashSet<>(c.getStringList("spirit-coats.worlds")),
            new HashSet<>(c.getStringList("spirit-coats.no-glow-worlds")),
            tierUnlock(c.getString("quests.tier-unlock", "level"), log));
        for (String w : s.noGlowWorlds)
            if (!s.coatWorlds.contains(w))
                log.warning("spirit-coats.no-glow-worlds lists \"" + w + "\", which isn't under spirit-coats.worlds. Check the spelling: a world that's misspelled there gets the glow.");
        return s;
    }

    public boolean enabledIn(String world) {
        return !disabledWorlds.contains(world);
    }

    /** Whether spirit coats are shown in this world: only the ones listed under spirit-coats.worlds, none by default. */
    public boolean coatsIn(String world) {
        return coatWorlds.contains(world);
    }

    /**
     * Whether coats may glow in this world. A glow outline shows through walls, so it can lead others to where a
     * critter's owner is; worlds under spirit-coats.no-glow-worlds (such as a kids' world) show the coat's look and
     * trail without it.
     */
    public boolean glowIn(String world) {
        return !noGlowWorlds.contains(world);
    }

    static com.extantape.mobsquad.model.Unlocks.Mode tierUnlock(String v, Logger log) {
        var m = com.extantape.mobsquad.model.Unlocks.Mode.parse(v);
        if (m != null) return m;
        log.warning("quests.tier-unlock must be level, either or quest, not \"" + v + "\". Using level.");
        return com.extantape.mobsquad.model.Unlocks.Mode.LEVEL;
    }

    static int clamp(int v, int min, int max, String key, Logger log) {
        if (v >= min && v <= max) return v;
        int to = Math.max(min, Math.min(max, v));
        log.warning(key + " must be between " + min + " and " + max + ". Using " + to + ".");
        return to;
    }
}
