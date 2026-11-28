package com.extantape.mobsquad.hook;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LevelledMobsOptOutTest {
    @Test
    void usesTheKeyLevelledMobsChecks() {
        // LevelledMobs 4.5.3.2 builds it as new NamespacedKey(plugin, "noLevel"); Bukkit lowercases both parts.
        assertEquals("levelledmobs:nolevel", LevelledMobsOptOut.KEY.toString());
    }
}
