package com.extantape.mobsquad.model;

import java.util.UUID;

/**
 * A bonded mob. It lives in its owner's Bunkhouse list; out in the world it's a mob that's never saved with the world.
 * {@code tuckeredUntil} is when it's rested enough to go out again, in epoch milliseconds (0 when it's ready).
 * {@code minutes} is how long it has spent out with its owner, which counts toward becoming their sidekick.
 */
public record Critter(UUID id, Species species, String name, int level, long xp, long tuckeredUntil, long minutes) {
    public static Critter fresh(Species species) {
        return new Critter(UUID.randomUUID(), species, species.title(), 1, 0, 0, 0);
    }

    public boolean resting(long now) {
        return tuckeredUntil > now;
    }

    /** With new experience; the level follows from it. */
    public Critter withXp(long newXp) {
        return new Critter(id, species, name, Growth.level(newXp), newXp, tuckeredUntil, minutes);
    }

    public Critter tuckered(long until) {
        return new Critter(id, species, name, level, xp, until, minutes);
    }

    /** One more minute out, with the experience that comes with it. */
    public Critter minuteOut(long walkXp) {
        return new Critter(id, species, name, level >= Growth.MAX ? level : Growth.level(xp + walkXp), level >= Growth.MAX ? xp : xp + walkXp, tuckeredUntil, minutes + 1);
    }
}
