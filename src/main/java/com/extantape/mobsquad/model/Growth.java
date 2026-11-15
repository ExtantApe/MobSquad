package com.extantape.mobsquad.model;

/**
 * A critter's own level, 1 to 100 (brief, "Levels and spirit coats"). Level n needs 10 (n - 1)² experience in all:
 * level 2 at 10, level 10 at 810, level 50 at 24,010, level 100 at 98,010. A hostile mob a critter helped beat is worth
 * its max health, so a zombie gives 20, and every minute out with its owner gives {@link #WALK_XP}.
 *
 * <p>Stats grow a little each level and slow near the top: the growth fraction is 1 - (1 - (level - 1) / 99)², which
 * gets a level 25 critter about 43% of the way and level 50 about 75%. At level 100 a critter has four times its
 * health, three times its attack and a fifth more speed. Specials grow at the milestones, levels 25, 50, 75 and 100.
 */
public final class Growth {
    public static final int MAX = 100;
    public static final long WALK_XP = 2;
    public static final int[] MILESTONES = {25, 50, 75, 100};

    private Growth() {}

    public static long xpFor(int level) {
        long n = Math.max(1, Math.min(MAX, level)) - 1;
        return 10L * n * n;
    }

    public static int level(long xp) {
        int n = (int) Math.floor(Math.sqrt(Math.max(0, xp) / 10.0)) + 1;
        while (n > 1 && xpFor(n) > xp) n--;
        while (n < MAX && xpFor(n + 1) <= xp) n++;
        return Math.min(MAX, n);
    }

    /** 0 at level 1, 1 at level 100, quick at first and slower near the top. */
    public static double fraction(int level) {
        double t = (Math.max(1, Math.min(MAX, level)) - 1) / (double) (MAX - 1);
        return 1 - (1 - t) * (1 - t);
    }

    public static double health(int level) {
        return 1 + 3 * fraction(level);
    }

    public static double attack(int level) {
        return 1 + 2 * fraction(level);
    }

    public static double speed(int level) {
        return 1 + 0.2 * fraction(level);
    }

    /** How many milestones a critter has passed: 0 below level 25, 4 at level 100. */
    public static int stage(int level) {
        int s = 0;
        for (int m : MILESTONES) if (level >= m) s++;
        return s;
    }
}
