package com.extantape.mobsquad.model;

/**
 * Wranglin' levels when AuraSkills isn't installed. Level n needs 25 n (n - 1) experience in all, so each level takes
 * 50 more than the last: level 2 at 50, level 10 at 2,250. Coaxing a critter is worth {@link #COAX}.
 */
public final class Levels {
    public static final int COAX = 100;

    private Levels() {}

    public static long xpFor(int level) {
        return 25L * level * (level - 1);
    }

    public static int level(long xp) {
        int n = 1;
        while (xpFor(n + 1) <= xp) n++;
        return n;
    }

    /** How many critters may be out at once: one, two from level 10, three from level 25. */
    public static int posse(int level) {
        return level >= 25 ? 3 : level >= 10 ? 2 : 1;
    }

    public static boolean unlocked(Tier tier, int level) {
        return level >= tier.level;
    }
}
