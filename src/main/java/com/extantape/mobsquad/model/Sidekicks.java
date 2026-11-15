package com.extantape.mobsquad.model;

import java.util.Collection;
import java.util.UUID;
import java.util.function.DoubleSupplier;

/**
 * The sidekick (brief, "The posse"): a critter you've spent a long time with has a small daily chance of becoming your
 * sidekick, one per player. "A long time" is {@link #MINUTES} minutes out together, ten hours; the chance is
 * {@link #CHANCE}, rolled at most once a day per player, while an eligible critter is out with them.
 */
public final class Sidekicks {
    public static final long MINUTES = 600;
    public static final double CHANCE = 0.10;

    private Sidekicks() {}

    /**
     * Today's roll for a player, given which critters are out with them. Returns the new sidekick, or null. Marks the
     * day as rolled only when there was a critter to roll for, so a day without one doesn't use the chance up.
     */
    public static Critter roll(Wrangler w, Collection<UUID> out, long day, DoubleSupplier random) {
        if (w.sidekick != null || w.sidekickRoll >= day) return null;
        Critter best = null;
        for (Critter c : w.critters)
            if (out.contains(c.id()) && c.minutes() >= MINUTES && (best == null || c.minutes() > best.minutes())) best = c;
        if (best == null) return null;
        w.sidekickRoll = day;
        if (random.getAsDouble() >= CHANCE) return null;
        w.sidekick = best.id();
        return best;
    }
}
