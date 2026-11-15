package com.extantape.mobsquad.model;

/** Mob tiers. Your Wranglin' level unlocks them; you need a mob's tier before you can coax it. */
public enum Tier {
    YARD("Yard", 1), HOLLER("Holler", 10), YONDER("Yonder", 25), BACKCOUNTRY("Backcountry", 40);

    public final String title;
    /** The Wranglin' level that unlocks this tier. */
    public final int level;

    Tier(String title, int level) {
        this.title = title;
        this.level = level;
    }
}
