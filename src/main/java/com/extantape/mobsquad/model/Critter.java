package com.extantape.mobsquad.model;

import java.util.UUID;

/**
 * A bonded mob. It lives in its owner's Bunkhouse list; out in the world it's a mob that's never saved with the world.
 * {@code tuckeredUntil} is when it's rested enough to go out again, in epoch milliseconds (0 when it's ready).
 * {@code minutes} is how long it has spent out with its owner, which counts toward becoming their sidekick.
 * {@code coat} is the spirit coat its owner picked, 1 to 10, or 0 to wear the newest one it has earned (see {@link Coats}).
 */
public record Critter(UUID id, Species species, String name, int level, long xp, long tuckeredUntil, long minutes, int coat) {
    /** A critter with no coat picked, which is how every save from before 0.8.0 reads. */
    public Critter(UUID id, Species species, String name, int level, long xp, long tuckeredUntil, long minutes) {
        this(id, species, name, level, xp, tuckeredUntil, minutes, 0);
    }

    public static Critter fresh(Species species) {
        return new Critter(UUID.randomUUID(), species, species.title(), 1, 0, 0, 0, 0);
    }

    public boolean resting(long now) {
        return tuckeredUntil > now;
    }

    /** With new experience; the level follows from it. */
    public Critter withXp(long newXp) {
        return new Critter(id, species, name, Growth.level(newXp), newXp, tuckeredUntil, minutes, coat);
    }

    public Critter tuckered(long until) {
        return new Critter(id, species, name, level, xp, until, minutes, coat);
    }

    /** With a coat picked (1 to 10), or 0 for the newest earned. */
    public Critter withCoat(int newCoat) {
        return new Critter(id, species, name, level, xp, tuckeredUntil, minutes, Math.max(0, Math.min(Coats.COUNT, newCoat)));
    }

    /** One more minute out, with the experience that comes with it. */
    public Critter minuteOut(long walkXp) {
        return new Critter(id, species, name, level >= Growth.MAX ? level : Growth.level(xp + walkXp), level >= Growth.MAX ? xp : xp + walkXp, tuckeredUntil, minutes + 1, coat);
    }
}
