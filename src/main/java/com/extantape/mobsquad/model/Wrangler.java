package com.extantape.mobsquad.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** One player's MobSquad: their own Wranglin' experience and the critters in their Bunkhouse. */
public final class Wrangler {
    public final UUID player;
    public long wranglinXp;
    public final List<Critter> critters = new ArrayList<>();
    /** The La Ménagerie pages this player has opened. */
    /** This player's sidekick, if they have one, and the last day (epoch day) they rolled for one. */
    public UUID sidekick;
    public long sidekickRoll;
    public final java.util.Set<Species> pages = java.util.EnumSet.noneOf(Species.class);
    /** Tiers a quest has unlocked for this player (0.9.0). Used when quests.tier-unlock is either or quest. */
    public final java.util.Set<Tier> questTiers = java.util.EnumSet.noneOf(Tier.class);

    public Wrangler(UUID player) {
        this.player = player;
    }

    public Critter find(UUID id) {
        for (Critter c : critters) if (c.id().equals(id)) return c;
        return null;
    }

    public void replace(Critter c) {
        for (int i = 0; i < critters.size(); i++) if (critters.get(i).id().equals(c.id())) critters.set(i, c);
    }
}
