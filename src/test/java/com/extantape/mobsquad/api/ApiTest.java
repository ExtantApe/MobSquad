package com.extantape.mobsquad.api;

import com.extantape.mobsquad.model.Critter;
import com.extantape.mobsquad.model.Growth;
import com.extantape.mobsquad.model.Species;
import com.extantape.mobsquad.model.CoatData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ApiTest {
    @Test
    void theAllayNeverFightsAndEveryOtherCritterDoes() {
        for (Species s : Species.values()) {
            CritterStats st = Api.statsFor(s, 50);
            if (s == Species.ALLAY) assertEquals(0, st.damage(), s.id());
            else assertTrue(st.damage() > 0, s.id());
            assertEquals(s != Species.ALLAY, Api.fights(s), s.id());
        }
    }

    @Test
    void statsGrowWithLevelTheSameWayAsInTheWorld() {
        CritterStats one = Api.statsFor(Species.FOX, 1), top = Api.statsFor(Species.FOX, 100);
        assertEquals(1, one.healthScale(), 1e-9);
        assertEquals(Species.FOX.attack, one.damage(), 1e-9);
        assertEquals(Growth.health(100), top.healthScale(), 1e-9);
        assertEquals(Species.FOX.attack * Growth.attack(100), top.damage(), 1e-9);
        assertEquals(Growth.speed(100), top.speedScale(), 1e-9);
    }

    @Test
    void theSkeletonsDamageIsItsArrow() {
        assertEquals(Api.ARROW, Api.statsFor(Species.SKELETON, 1).damage(), 1e-9);
    }

    @Test
    void anUnknownSpeciesIsHarmless() {
        CritterStats st = Api.statsFor(null, 40);
        assertEquals(0, st.damage());
        assertEquals(1, st.healthScale());
    }

    @Test
    void infoCopiesTheCritterAndSaysWhenItsResting() {
        Critter c = Critter.fresh(Species.IRON_GOLEM).withXp(Growth.xpFor(30)).tuckered(5_000);
        CritterInfo i = Api.info(c, 1_000, CoatData.EMPTY);
        assertEquals(c.id(), i.id());
        assertEquals("iron_golem", i.species());
        assertEquals("IRON_GOLEM", i.entityType());
        assertEquals(30, i.level());
        assertTrue(i.resting());
        assertFalse(Api.info(c, 6_000, CoatData.EMPTY).resting());
    }

    @Test
    void theApiReportsTheWornCoatByNumberAndName() {
        CoatData d = CoatData.parse("coats:\n  2: { name: Marigold, color: gold }\n  7: { name: Frost, color: blue }\n",
            new CoatData.Validator() {
                @Override public boolean particle(String key) { return true; }
                @Override public boolean variant(String species, String key) { return true; }
            }, java.util.logging.Logger.getAnonymousLogger(), CoatData.EMPTY);
        assertEquals(0, Api.info(at(9, 0), 0, d).coat());
        assertEquals("", Api.info(at(9, 0), 0, d).coatName());
        CritterInfo newest = Api.info(at(75, 0), 0, d);
        assertEquals(7, newest.coat());
        assertEquals("Frost", newest.coatName());
        CritterInfo picked = Api.info(at(75, 2), 0, d);
        assertEquals(2, picked.coat());
        assertEquals("Marigold", picked.coatName());
        assertEquals(0, new CritterInfo(java.util.UUID.randomUUID(), "bee", "BEE", "Bee", 1, false, true).coat(), "version 1 callers still build one");
        assertEquals(2, MobSquadApi.VERSION);
    }

    private static Critter at(int level, int coat) {
        return new Critter(java.util.UUID.randomUUID(), Species.CHICKEN, "Clucky", level, Growth.xpFor(level), 0, 0, coat);
    }
}
