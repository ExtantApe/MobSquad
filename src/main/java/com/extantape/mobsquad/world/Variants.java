package com.extantape.mobsquad.world;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Cat;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Frog;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Wolf;

/**
 * The real, data-driven variants of the six critters that have them, reached through Paper's registries so a variant
 * a datapack adds works too. Keys are plain ("ashen") or namespaced ("minecraft:ashen").
 */
final class Variants {
    private Variants() {}

    private static NamespacedKey key(String k) {
        return NamespacedKey.fromString(k.contains(":") ? k : "minecraft:" + k);
    }

    private static <T extends Keyed> T find(RegistryKey<T> registry, String k) {
        NamespacedKey nk = key(k);
        return nk == null ? null : RegistryAccess.registryAccess().getRegistry(registry).get(nk);
    }

    private static <T extends Keyed> String name(T value) {
        return value == null ? null : value.key().asString();
    }

    /** Whether this server has that variant for that species. */
    static boolean exists(String species, String k) {
        try {
            return switch (species) {
                case "wolf" -> find(RegistryKey.WOLF_VARIANT, k) != null;
                case "cat" -> find(RegistryKey.CAT_VARIANT, k) != null;
                case "pig" -> find(RegistryKey.PIG_VARIANT, k) != null;
                case "cow" -> find(RegistryKey.COW_VARIANT, k) != null;
                case "chicken" -> find(RegistryKey.CHICKEN_VARIANT, k) != null;
                case "frog" -> find(RegistryKey.FROG_VARIANT, k) != null;
                default -> false;
            };
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** The variant an entity wears now, as a namespaced key, or null if it has none. */
    static String current(Entity e) {
        if (e instanceof Wolf w) return name(w.getVariant());
        if (e instanceof Cat c) return name(c.getCatType());
        if (e instanceof Pig p) return name(p.getVariant());
        if (e instanceof Cow c) return name(c.getVariant());
        if (e instanceof Chicken c) return name(c.getVariant());
        if (e instanceof Frog f) return name(f.getVariant());
        return null;
    }

    /** Puts a variant on an entity; false if the entity has no such variant to wear. */
    static boolean set(Entity e, String k) {
        if (k == null) return false;
        if (e instanceof Wolf w) { var v = find(RegistryKey.WOLF_VARIANT, k); if (v == null) return false; w.setVariant(v); return true; }
        if (e instanceof Cat c) { var v = find(RegistryKey.CAT_VARIANT, k); if (v == null) return false; c.setCatType(v); return true; }
        if (e instanceof Pig p) { var v = find(RegistryKey.PIG_VARIANT, k); if (v == null) return false; p.setVariant(v); return true; }
        if (e instanceof Cow c) { var v = find(RegistryKey.COW_VARIANT, k); if (v == null) return false; c.setVariant(v); return true; }
        if (e instanceof Chicken c) { var v = find(RegistryKey.CHICKEN_VARIANT, k); if (v == null) return false; c.setVariant(v); return true; }
        if (e instanceof Frog f) { var v = find(RegistryKey.FROG_VARIANT, k); if (v == null) return false; f.setVariant(v); return true; }
        return false;
    }

    static boolean particle(String k) {
        try {
            NamespacedKey nk = key(k);
            var p = nk == null ? null : Registry.PARTICLE_TYPE.get(nk);
            return p != null && p.getDataType() == Void.class;
        } catch (RuntimeException e) {
            return false;
        }
    }
}
