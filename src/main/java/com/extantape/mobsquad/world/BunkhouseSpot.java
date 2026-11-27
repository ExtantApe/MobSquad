package com.extantape.mobsquad.world;

import java.util.function.Function;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;

/**
 * Finding the block that {@code /mobsquad bunkhouse add|remove|check <x> <y> <z> [world]} means, for admins and the
 * console (0.13.0). Only a vault is ever accepted. A chunk that was never generated is refused without being
 * generated, because nobody can have put a vault there, and a mistyped number shouldn't grow the world.
 */
public final class BunkhouseSpot {
    private BunkhouseSpot() {}

    /** The vault, or the sentence that says why there isn't one. Exactly one of the two is set. */
    public record Found(Block vault, String problem) {}

    /** A world by its name ({@code survival}) or its key ({@code minecraft:the_nether}), or null. */
    public static World world(String text, Function<String, World> byName, Function<NamespacedKey, World> byKey) {
        World w = byName.apply(text);
        if (w != null || text.indexOf(':') < 0) return w;
        NamespacedKey key = NamespacedKey.fromString(text);
        return key == null ? null : byKey.apply(key);
    }

    /** "1 64 -20 in survival", for messages. */
    public static String where(World world, int x, int y, int z) {
        return x + " " + y + " " + z + " in " + world.getName();
    }

    /** Loads the chunk if it has to, then looks at the block. */
    public static Found find(World world, int x, int y, int z) {
        String none = "There's no vault at " + where(world, x, y, z) + ".";
        if (y < world.getMinHeight() || y >= world.getMaxHeight()) return new Found(null, none + " That height is outside the world.");
        int cx = x >> 4, cz = z >> 4;
        if (!world.isChunkGenerated(cx, cz)) return new Found(null, none + " That part of the world hasn't been made yet.");
        world.getChunkAt(cx, cz); // loads it, if it isn't loaded
        Block b = world.getBlockAt(x, y, z);
        return b.getType() == Material.VAULT ? new Found(b, null) : new Found(null, none);
    }
}
