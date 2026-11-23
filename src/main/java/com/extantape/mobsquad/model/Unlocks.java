package com.extantape.mobsquad.model;

import java.util.Locale;
import java.util.Set;

/**
 * How a player's tiers open. The brief: quest chapters introduce new mobs and unlock tiers; without quests, tiers unlock
 * by Wranglin' level alone. A server picks one of three ways in config ({@code quests.tier-unlock}):
 * <ul>
 *   <li>{@code level} (the default): Wranglin' level only, as before quests existed. Quest unlocks are kept but unused.</li>
 *   <li>{@code either}: the level, or a quest that unlocked the tier early.</li>
 *   <li>{@code quest}: only a quest opens a tier above Yard. Yard is always open, so nobody is ever locked out of everything.</li>
 * </ul>
 */
public final class Unlocks {
    public enum Mode {
        LEVEL, EITHER, QUEST;

        /** Reads a config value; anything else is null, so the caller can warn and keep the default. */
        public static Mode parse(String s) {
            if (s == null) return null;
            try {
                return valueOf(s.strip().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    private Unlocks() {}

    public static boolean open(Mode mode, Tier tier, int level, Set<Tier> quested) {
        if (tier == Tier.YARD) return true;
        boolean byLevel = Levels.unlocked(tier, level), byQuest = quested.contains(tier);
        return switch (mode) {
            case LEVEL -> byLevel;
            case EITHER -> byLevel || byQuest;
            case QUEST -> byQuest;
        };
    }

    /** When a locked tier opens, to finish a sentence such as "You can coax one ...". */
    public static String when(Mode mode, Tier tier) {
        return switch (mode) {
            case LEVEL -> "from Wranglin' level " + tier.level;
            case EITHER -> "from Wranglin' level " + tier.level + ", or sooner through a quest";
            case QUEST -> "once a quest opens the " + tier.title + " tier";
        };
    }
}
