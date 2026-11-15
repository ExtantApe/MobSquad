package com.extantape.mobsquad;

import static org.junit.jupiter.api.Assertions.*;

import com.extantape.mobsquad.model.Critter;
import com.extantape.mobsquad.model.Levels;
import com.extantape.mobsquad.model.Species;
import com.extantape.mobsquad.model.Tier;
import com.extantape.mobsquad.model.Wrangler;
import com.extantape.mobsquad.store.Store;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ModelTest {
    @Test
    void levelsClimbFiftyMoreEachTime() {
        assertEquals(1, Levels.level(0));
        assertEquals(1, Levels.level(49));
        assertEquals(2, Levels.level(50));
        assertEquals(10, Levels.level(2250));
        assertEquals(9, Levels.level(2249));
    }

    @Test
    void posseGrowsAtTenAndTwentyFive() {
        assertEquals(1, Levels.posse(1));
        assertEquals(1, Levels.posse(9));
        assertEquals(2, Levels.posse(10));
        assertEquals(3, Levels.posse(25));
    }

    @Test
    void tiersUnlockInOrder() {
        assertTrue(Levels.unlocked(Tier.YARD, 1));
        assertFalse(Levels.unlocked(Tier.HOLLER, 9));
        assertTrue(Levels.unlocked(Tier.HOLLER, 10));
        assertFalse(Levels.unlocked(Tier.BACKCOUNTRY, 39));
        assertEquals(9, Species.values().length, "the nine launch critters");
    }

    @Test
    void aPlayersFileRoundTrips(@TempDir Path dir) throws Exception {
        Store store = new Store(dir);
        UUID p = UUID.randomUUID();
        Wrangler w = new Wrangler(p);
        w.wranglinXp = 150;
        w.critters.add(new Critter(UUID.randomUUID(), Species.CHICKEN, "Clucky McCluckface", 3, 42, 0));
        w.critters.add(Critter.fresh(Species.IRON_GOLEM).tuckered(123456789L));
        store.save(w);
        Store.Loaded back = store.load(p);
        assertEquals(0, back.skipped());
        assertEquals(150, back.wrangler().wranglinXp);
        assertEquals(w.critters, back.wrangler().critters);
    }

    @Test
    void aBadLineIsSkippedNotGuessed(@TempDir Path dir) throws Exception {
        UUID p = UUID.randomUUID();
        Files.writeString(dir.resolve(p + ".txt"), "wranglin 10\ncritter not-a-uuid chicken 1 0 0 X\ncritter " + UUID.randomUUID() + " dragon 1 0 0 X\nnonsense\n");
        Store.Loaded back = new Store(dir).load(p);
        assertEquals(3, back.skipped());
        assertEquals(10, back.wrangler().wranglinXp);
        assertTrue(back.wrangler().critters.isEmpty());
    }

    @Test
    void aNewPlayerHasNothing(@TempDir Path dir) throws Exception {
        Store.Loaded back = new Store(dir).load(UUID.randomUUID());
        assertTrue(back.wrangler().critters.isEmpty());
        assertEquals(0, back.wrangler().wranglinXp);
    }
}
