package com.extantape.mobsquad.world;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.junit.jupiter.api.Test;

/** The coordinate form of /mobsquad bunkhouse: which world is meant, and the vault check. */
class BunkhouseSpotTest {
    private final List<String> calls = new ArrayList<>();

    private static <T> T fake(Class<T> type, Map<String, java.util.function.Function<Object[], Object>> answers) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, (p, m, args) -> {
            var f = answers.get(m.getName());
            if (f == null) throw new AssertionError("unexpected call: " + m.getName());
            return f.apply(args);
        }));
    }

    private World world(boolean generated, Material at) {
        Block block = fake(Block.class, Map.of("getType", a -> at));
        Chunk chunk = fake(Chunk.class, Map.of());
        return fake(World.class, Map.of(
            "getName", a -> "survival",
            "getMinHeight", a -> -64,
            "getMaxHeight", a -> 320,
            "isChunkGenerated", a -> { calls.add("generated? " + a[0] + " " + a[1]); return generated; },
            "getChunkAt", a -> { calls.add("load " + a[0] + " " + a[1]); return chunk; },
            "getBlockAt", a -> { calls.add("block " + a[0] + " " + a[1] + " " + a[2]); return block; }));
    }

    @Test
    void aVaultIsFoundAfterItsChunkIsLoaded() {
        var found = BunkhouseSpot.find(world(true, Material.VAULT), -17, 64, 40);
        assertNotNull(found.vault());
        assertNull(found.problem());
        assertEquals(List.of("generated? -2 2", "load -2 2", "block -17 64 40"), calls, "checked, loaded, then looked, in the right chunk");
    }

    @Test
    void anythingButAVaultIsRefused() {
        for (Material m : List.of(Material.AIR, Material.CHEST, Material.STONE, Material.TRIAL_SPAWNER, Material.ENDER_CHEST)) {
            var found = BunkhouseSpot.find(world(true, m), 1, 2, 3);
            assertNull(found.vault(), m.name());
            assertEquals("There's no vault at 1 2 3 in survival.", found.problem());
        }
    }

    @Test
    void anUnmadeChunkIsRefusedWithoutBeingMade() {
        var found = BunkhouseSpot.find(world(false, Material.VAULT), 100_000, 64, 0);
        assertNull(found.vault());
        assertTrue(found.problem().contains("hasn't been made yet"), found.problem());
        assertEquals(1, calls.size(), "nothing was loaded or generated: " + calls);
    }

    @Test
    void aHeightOutsideTheWorldIsRefusedBeforeAnythingLoads() {
        for (int y : new int[] {-65, 320, 5000}) {
            var found = BunkhouseSpot.find(world(true, Material.VAULT), 0, y, 0);
            assertNull(found.vault(), "y " + y);
            assertTrue(found.problem().contains("outside the world"), found.problem());
        }
        assertTrue(calls.isEmpty(), calls.toString());
        assertNotNull(BunkhouseSpot.find(world(true, Material.VAULT), 0, -64, 0).vault(), "the floor is in");
        assertNotNull(BunkhouseSpot.find(world(true, Material.VAULT), 0, 319, 0).vault(), "the top block is in");
    }

    @Test
    void aWorldIsNamedByItsNameOrItsKeyAndNothingElse() {
        World survival = world(true, Material.VAULT), nether = world(true, Material.VAULT);
        java.util.function.Function<String, World> byName = n -> n.equals("survival") ? survival : null;
        java.util.function.Function<NamespacedKey, World> byKey = k -> k.toString().equals("minecraft:the_nether") ? nether : null;
        assertSame(survival, BunkhouseSpot.world("survival", byName, byKey));
        assertSame(nether, BunkhouseSpot.world("minecraft:the_nether", byName, byKey));
        for (String bad : List.of("surviv", "survival extra", "the_nether", "minecraft:nope", "Bad Key:x", ":", ""))
            assertNull(BunkhouseSpot.world(bad, byName, byKey), "'" + bad + "'");
    }
}
