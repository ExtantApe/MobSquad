package com.extantape.mobsquad.world;

import java.util.Arrays;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/**
 * Which vault blocks are Bunkhouses, kept in each chunk's own data so it's saved with the world. An admin marks one;
 * a vault out in a trial chamber is never a Bunkhouse unless marked.
 */
public final class Bunkhouses {
    private final NamespacedKey key;

    public Bunkhouses(Plugin plugin) {
        this.key = new NamespacedKey(plugin, "bunkhouse");
    }

    private static int pack(Block b) {
        return ((b.getY() + 4096) << 8) | ((b.getX() & 15) << 4) | (b.getZ() & 15);
    }

    private int[] read(Block b) {
        int[] a = b.getChunk().getPersistentDataContainer().get(key, PersistentDataType.INTEGER_ARRAY);
        return a == null ? new int[0] : a;
    }

    public boolean is(Block b) {
        int p = pack(b);
        for (int v : read(b)) if (v == p) return true;
        return false;
    }

    public void mark(Block b) {
        if (is(b)) return;
        int[] a = read(b);
        int[] n = Arrays.copyOf(a, a.length + 1);
        n[a.length] = pack(b);
        b.getChunk().getPersistentDataContainer().set(key, PersistentDataType.INTEGER_ARRAY, n);
    }

    public void unmark(Block b) {
        int p = pack(b);
        int[] n = Arrays.stream(read(b)).filter(v -> v != p).toArray();
        var pdc = b.getChunk().getPersistentDataContainer();
        if (n.length == 0) pdc.remove(key);
        else pdc.set(key, PersistentDataType.INTEGER_ARRAY, n);
    }
}
