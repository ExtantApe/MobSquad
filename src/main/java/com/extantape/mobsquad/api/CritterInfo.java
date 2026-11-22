package com.extantape.mobsquad.api;

import java.util.UUID;

/**
 * One critter, read-only. {@code species} is MobSquad's id ("iron_golem"), {@code entityType} the Minecraft entity
 * ("IRON_GOLEM"). {@code resting} is true while it's tuckered out. {@code fights} is false for a critter that never
 * fights (the allay). {@code coat} is the spirit coat it wears, 1 to 10, or 0 if it hasn't earned one yet (before level
 * 10), and {@code coatName} its plain name ("Frost"), empty for 0. Added in API version 2. The coat is what the owner
 * picked or the newest earned, whether or not the world shows it.
 */
public record CritterInfo(UUID id, String species, String entityType, String name, int level, boolean resting, boolean fights, int coat, String coatName) {
    /** A critter with no coat, for code written against API version 1. */
    public CritterInfo(UUID id, String species, String entityType, String name, int level, boolean resting, boolean fights) {
        this(id, species, entityType, name, level, resting, fights, 0, "");
    }
}
