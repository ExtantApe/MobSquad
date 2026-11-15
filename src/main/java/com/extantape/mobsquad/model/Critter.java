package com.extantape.mobsquad.model;

import java.util.UUID;

/**
 * A bonded mob. It lives in its owner's Bunkhouse list; out in the world it's a mob that's never saved with the world.
 * {@code tuckeredUntil} is when it's rested enough to go out again, in epoch milliseconds (0 when it's ready).
 */
public record Critter(UUID id, Species species, String name, int level, long xp, long tuckeredUntil) {
    public static Critter fresh(Species species) {
        return new Critter(UUID.randomUUID(), species, species.title(), 1, 0, 0);
    }

    public boolean resting(long now) {
        return tuckeredUntil > now;
    }

    public Critter tuckered(long until) {
        return new Critter(id, species, name, level, xp, until);
    }
}
