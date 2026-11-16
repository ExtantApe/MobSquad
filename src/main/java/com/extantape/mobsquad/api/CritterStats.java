package com.extantape.mobsquad.api;

/**
 * A critter's strength at a level. {@code healthScale} multiplies the mob's normal max health and {@code speedScale}
 * its speed. {@code damage} is what one hit or shot does: its close-up attack, or the skeleton's arrow, both grown
 * with level; 0 for a critter that never fights.
 */
public record CritterStats(double healthScale, double damage, double speedScale) {}
