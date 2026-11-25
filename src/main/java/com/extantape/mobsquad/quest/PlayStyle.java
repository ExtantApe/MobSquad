package com.extantape.mobsquad.quest;

import java.util.Locale;

/**
 * Which kind of play a wrangler does most, so the Ten Winds story can pick the words and the first goal that suit them.
 * It reads only the player's own counts and never changes a reward. Everything here is pure, so it can be unit tested;
 * {@link com.extantape.mobsquad.hook.Placeholders} reads the counts from the game's own statistics.
 */
public enum PlayStyle {
    BUILDER, FIGHTER, EXPLORER, CARETAKER, ALL;

    /** The count that makes one full point for each kind of play. Chosen so one evening of any of them scores about 1. */
    static final double BLOCKS_PLACED = 200, HOSTILE_KILLS = 25, BLOCKS_WALKED = 2500, ANIMALS_BRED = 6;

    /**
     * The kind with the biggest share of its own goal. Nothing counted, or a tie at the top, is {@link #ALL}: the story's
     * "all of the above" path, which fits everyone.
     */
    public static PlayStyle pick(long blocksPlaced, long hostileKills, long blocksWalked, long animalsBred) {
        double[] score = {
            Math.max(0, blocksPlaced) / BLOCKS_PLACED, Math.max(0, hostileKills) / HOSTILE_KILLS,
            Math.max(0, blocksWalked) / BLOCKS_WALKED, Math.max(0, animalsBred) / ANIMALS_BRED };
        double top = 0;
        for (double s : score) top = Math.max(top, s);
        if (top <= 0) return ALL;
        int best = -1;
        for (int i = 0; i < score.length; i++) {
            if (Math.abs(score[i] - top) < 1e-9) {
                if (best >= 0) return ALL;
                best = i;
            }
        }
        return values()[best];
    }

    /** The placeholder value: builder, fighter, explorer, caretaker or all. */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }
}
