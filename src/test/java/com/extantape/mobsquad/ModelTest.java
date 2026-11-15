package com.extantape.mobsquad;

import static org.junit.jupiter.api.Assertions.*;

import com.extantape.mobsquad.model.Critter;
import com.extantape.mobsquad.model.Critterbook;
import com.extantape.mobsquad.model.Sidekicks;
import com.extantape.mobsquad.model.Growth;
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
        w.critters.add(new Critter(UUID.randomUUID(), Species.CHICKEN, "Clucky McCluckface", 3, 42, 0, 77));
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

    @Test
    void critterLevelsRunOneToAHundred() {
        assertEquals(1, Growth.level(0));
        assertEquals(1, Growth.level(9));
        assertEquals(2, Growth.level(10));
        assertEquals(10, Growth.level(810));
        assertEquals(9, Growth.level(809));
        assertEquals(100, Growth.level(98_010));
        assertEquals(100, Growth.level(10_000_000));
        for (int n = 1; n <= 100; n++) assertEquals(n, Growth.level(Growth.xpFor(n)), "level " + n);
    }

    @Test
    void statsGrowAndSlowNearTheTop() {
        assertEquals(1.0, Growth.health(1), 1e-9);
        assertEquals(4.0, Growth.health(100), 1e-9);
        assertEquals(3.0, Growth.attack(100), 1e-9);
        assertEquals(1.2, Growth.speed(100), 1e-9);
        double early = Growth.fraction(11) - Growth.fraction(1), late = Growth.fraction(100) - Growth.fraction(90);
        assertTrue(early > 5 * late, "the first ten levels grow much more than the last ten");
        for (int n = 2; n <= 100; n++) assertTrue(Growth.fraction(n) > Growth.fraction(n - 1));
    }

    @Test
    void specialsGrowAtTheMilestones() {
        assertEquals(0, Growth.stage(24));
        assertEquals(1, Growth.stage(25));
        assertEquals(2, Growth.stage(50));
        assertEquals(3, Growth.stage(75));
        assertEquals(4, Growth.stage(100));
    }

    @Test
    void experienceSetsTheLevel() {
        Critter c = Critter.fresh(Species.FOX).withXp(Growth.xpFor(30) + 5);
        assertEquals(30, c.level());
        assertEquals(Growth.xpFor(30) + 5, c.xp());
    }

    @Test
    void everyCritterHasASpecial() {
        for (Species s : Species.values()) assertFalse(s.special.isBlank(), s.title());
    }

    @Test
    void critterbookNumbersRunOneToTheEndWithNoGaps() {
        var all = Critterbook.all();
        assertEquals(88, all.size());
        int expect = 1;
        for (int n : all.values()) assertEquals(expect++, n);
    }

    @Test
    void launchCritterNumbersNeverChange() {
        assertEquals(1, Critterbook.number(Species.CREEPER));
        assertEquals(3, Critterbook.number(Species.SKELETON));
        assertEquals(9, Critterbook.number(Species.CHICKEN));
        assertEquals(15, Critterbook.number(Species.ENDERMAN));
        assertEquals(25, Critterbook.number(Species.IRON_GOLEM));
        assertEquals(62, Critterbook.number(Species.FOX));
        assertEquals(63, Critterbook.number(Species.BEE));
        assertEquals(69, Critterbook.number(Species.AXOLOTL));
        assertEquals(75, Critterbook.number(Species.ALLAY));
        assertEquals(Species.CREEPER, Critterbook.inOrder().getFirst());
    }

    @Test
    void journalPagesRoundTrip(@TempDir Path dir) throws Exception {
        Store store = new Store(dir);
        UUID p = UUID.randomUUID();
        Wrangler w = new Wrangler(p);
        w.pages.add(Species.FOX);
        w.pages.add(Species.CREEPER);
        store.save(w);
        assertEquals(java.util.Set.of(Species.FOX, Species.CREEPER), store.load(p).wrangler().pages);
    }

    @Test
    void anOlderFileOpensPagesForCrittersYouHave(@TempDir Path dir) throws Exception {
        UUID p = UUID.randomUUID();
        Files.writeString(dir.resolve(p + ".txt"), "wranglin 0\ncritter " + UUID.randomUUID() + " bee 1 0 0 Buzz\n");
        Store.Loaded l = new Store(dir).load(p);
        assertEquals(0, l.skipped());
        assertEquals(java.util.Set.of(Species.BEE), l.wrangler().pages);
    }

    private static Critter withMinutes(Species s, long minutes) {
        Critter c = Critter.fresh(s);
        return new Critter(c.id(), s, c.name(), 1, 0, 0, minutes);
    }

    @Test
    void aSidekickNeedsTenHoursOutTogether() {
        Wrangler w = new Wrangler(UUID.randomUUID());
        Critter young = withMinutes(Species.FOX, Sidekicks.MINUTES - 1);
        w.critters.add(young);
        assertNull(Sidekicks.roll(w, java.util.List.of(young.id()), 100, () -> 0.0));
        assertEquals(0, w.sidekickRoll, "a day with nobody eligible doesn't use the roll up");
    }

    @Test
    void theRollIsSmallAndOnceADay() {
        Wrangler w = new Wrangler(UUID.randomUUID());
        Critter old = withMinutes(Species.FOX, Sidekicks.MINUTES);
        w.critters.add(old);
        assertNull(Sidekicks.roll(w, java.util.List.of(old.id()), 100, () -> Sidekicks.CHANCE));
        assertEquals(100, w.sidekickRoll);
        assertNull(Sidekicks.roll(w, java.util.List.of(old.id()), 100, () -> 0.0), "already rolled today");
        assertEquals(old, Sidekicks.roll(w, java.util.List.of(old.id()), 101, () -> 0.0));
        assertEquals(old.id(), w.sidekick);
    }

    @Test
    void oneSidekickAndOnlyFromCrittersOut() {
        Wrangler w = new Wrangler(UUID.randomUUID());
        Critter home = withMinutes(Species.BEE, 5000), out = withMinutes(Species.FOX, 700);
        w.critters.add(home);
        w.critters.add(out);
        assertEquals(out, Sidekicks.roll(w, java.util.List.of(out.id()), 5, () -> 0.0));
        assertNull(Sidekicks.roll(w, java.util.List.of(home.id(), out.id()), 6, () -> 0.0), "one per player");
    }

    @Test
    void sidekickAndMinutesRoundTrip(@TempDir Path dir) throws Exception {
        Store store = new Store(dir);
        UUID p = UUID.randomUUID();
        Wrangler w = new Wrangler(p);
        Critter c = withMinutes(Species.SKELETON, 612);
        w.critters.add(c);
        w.sidekick = c.id();
        w.sidekickRoll = 20407;
        store.save(w);
        Wrangler back = store.load(p).wrangler();
        assertEquals(612, back.critters.getFirst().minutes());
        assertEquals(c.id(), back.sidekick);
        assertEquals(20407, back.sidekickRoll);
    }

    @Test
    void aSidekickNotInTheListIsDropped(@TempDir Path dir) throws Exception {
        UUID p = UUID.randomUUID();
        Files.writeString(dir.resolve(p + ".txt"), "wranglin 0\nsidekick " + UUID.randomUUID() + "\n");
        assertNull(new Store(dir).load(p).wrangler().sidekick);
    }

    @Test
    void minutesOutAddUpAndEarnExperience() {
        Critter c = Critter.fresh(Species.FOX).minuteOut(2).minuteOut(2);
        assertEquals(2, c.minutes());
        assertEquals(4, c.xp());
    }
}
