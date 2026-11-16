package com.extantape.mobsquad.api;

import com.extantape.mobsquad.model.Critter;
import com.extantape.mobsquad.model.Growth;
import com.extantape.mobsquad.model.Species;
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
        CritterInfo i = Api.info(c, 1_000);
        assertEquals(c.id(), i.id());
        assertEquals("iron_golem", i.species());
        assertEquals("IRON_GOLEM", i.entityType());
        assertEquals(30, i.level());
        assertTrue(i.resting());
        assertFalse(Api.info(c, 6_000).resting());
    }
}
