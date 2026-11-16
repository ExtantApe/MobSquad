package com.extantape.mobsquad.api;

import java.util.UUID;

/**
 * One critter, read-only. {@code species} is MobSquad's id ("iron_golem"), {@code entityType} the Minecraft entity
 * ("IRON_GOLEM"). {@code resting} is true while it's tuckered out. {@code fights} is false for a critter that never
 * fights (the allay).
 */
public record CritterInfo(UUID id, String species, String entityType, String name, int level, boolean resting, boolean fights) {}
