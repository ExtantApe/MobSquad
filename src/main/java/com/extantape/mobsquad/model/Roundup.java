package com.extantape.mobsquad.model;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * What the Season 1 finale (the Round-up at the Bunkhouse) needs to know about one player (0.13.0). A quest turns it on
 * with {@code /mobsquad roundup <player> start}. While it's on, MobSquad notes each critter the player coaxes, and each
 * of those the player sends out from a Bunkhouse. Critters that were given by a command, or coaxed before it was on,
 * don't count. Saved in the player's file, so a restart loses nothing.
 */
public final class Roundup {
    public boolean on;
    /** Critters coaxed while on. */
    public final Set<UUID> coaxed = new LinkedHashSet<>();
    /** Those of them that have been sent out from a Bunkhouse. */
    public final Set<UUID> sent = new LinkedHashSet<>();

    /** Turns it on. Starting it again keeps what has been done, so a quest can safely ask twice. */
    public boolean start() {
        if (on) return false;
        coaxed.clear();
        sent.clear();
        on = true;
        return true;
    }

    public boolean stop() {
        boolean was = on;
        on = false;
        coaxed.clear();
        sent.clear();
        return was;
    }

    /** A critter was coaxed. Returns true if that changed anything. */
    public boolean coaxed(UUID critter) {
        return on && coaxed.add(critter);
    }

    /** A critter was sent out from a Bunkhouse. Only one coaxed while on counts. Returns true if that changed anything. */
    public boolean sentOut(UUID critter) {
        return on && coaxed.contains(critter) && sent.add(critter);
    }

    public boolean hasCoaxed() {
        return on && !coaxed.isEmpty();
    }

    public boolean hasSent() {
        return on && !sent.isEmpty();
    }

    /** True while one of the sent critters is out with its owner, ready to be brought to the Ranger. */
    public boolean ready(Predicate<UUID> out) {
        return on && sent.stream().anyMatch(out);
    }
}
