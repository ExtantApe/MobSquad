package com.extantape.mobsquad.hook;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataType;

/**
 * Keeps mob-levelling plugins off a critter. LevelledMobs 4 levels every mob it sees by default, tamed or named, which
 * would give a critter extra health, damage and speed on top of its own growth, and a second level in its nametag.
 * LevelledMobs leaves alone any mob that carries the string key {@code levelledmobs:nolevel} (it makes the key with
 * {@code new NamespacedKey(plugin, "noLevel")}, which Bukkit lowercases). Setting it costs nothing when LevelledMobs
 * isn't installed, and needs no LevelledMobs code or setting.
 */
public final class LevelledMobsOptOut {
    /** The key LevelledMobs checks before it levels a mob. */
    public static final NamespacedKey KEY = new NamespacedKey("levelledmobs", "nolevel");

    private LevelledMobsOptOut() {}

    /** Marks a mob so LevelledMobs won't level it. Call it before the mob is added to the world. */
    public static void mark(Entity e) {
        e.getPersistentDataContainer().set(KEY, PersistentDataType.STRING, "mobsquad");
    }
}
