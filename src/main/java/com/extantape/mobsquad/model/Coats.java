package com.extantape.mobsquad.model;

import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Spirit coats (brief, "Levels and spirit coats"): a new one every 10 levels, ten in all, so coat 1 at level 10 and
 * coat 10 at level 100. A critter's level never drops in play, so a coat once earned stays earned. The look is only
 * ever shown in the worlds an admin lists under {@code spirit-coats.worlds}; the earning and the choice are not tied
 * to a world.
 */
public final class Coats {
    public static final int COUNT = 10;
    public static final int EVERY = 10;

    private Coats() {}

    /** How many coats a critter of this level has earned: 0 below level 10, 10 at level 100. */
    public static int earned(int level) {
        return Math.max(0, Math.min(COUNT, level / EVERY));
    }

    /** The level that earns a coat. */
    public static int levelFor(int coat) {
        return coat * EVERY;
    }

    /**
     * The coat a critter wears, 1 to 10, or 0 when it has none yet. A chosen coat counts only if it's earned;
     * otherwise (no choice, or a level an admin lowered since) it's the newest earned.
     */
    public static int worn(Critter c) {
        int earned = earned(c.level());
        return c.coat() >= 1 && c.coat() <= earned ? c.coat() : earned;
    }

    /**
     * The color to write a coat's name in on a dark menu or in chat. The dark team colors are fine for a glow but too
     * dim to read as text, so each maps to a lighter one. The name always goes with it: color never carries the
     * meaning.
     */
    public static NamedTextColor readable(NamedTextColor c) {
        if (c == NamedTextColor.DARK_GREEN) return NamedTextColor.GREEN;
        if (c == NamedTextColor.DARK_AQUA || c == NamedTextColor.BLUE || c == NamedTextColor.DARK_BLUE) return NamedTextColor.AQUA;
        if (c == NamedTextColor.DARK_PURPLE) return NamedTextColor.LIGHT_PURPLE;
        if (c == NamedTextColor.DARK_RED) return NamedTextColor.RED;
        if (c == NamedTextColor.BLACK || c == NamedTextColor.DARK_GRAY) return NamedTextColor.GRAY;
        return c;
    }
}
