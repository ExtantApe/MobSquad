package com.extantape.mobsquad.model;

import java.util.Locale;

/** The launch critters, the home county (brief, "Launch critters"). */
public enum Species {
    CHICKEN(Tier.YARD, "CHICKEN", 1, "Lead it on a trail of seeds, at a walk.",
        "You fall slowly while it's out."),
    AXOLOTL(Tier.YARD, "AXOLOTL", 2, "Show it a bucket of tropical fish in the water, then swim with it.",
        "Water breathing and regeneration while you're near it. Goes after drowned and guardians first, and plays dead when it's badly hurt."),
    BEE(Tier.YARD, "BEE", 2, "Show it a flower, then stand still while it looks you over.",
        "Crops near it grow faster."),
    FOX(Tier.HOLLER, "FOX", 2, "Crouch, offer berries, and stay crouched.",
        "Fetches items you drop or leave lying around."),
    ALLAY(Tier.HOLLER, "ALLAY", 0, "Give it an amethyst shard, then play it a tune on a note block.",
        "Right-click it holding an ore and it flies to the nearest one close by. Never fights."),
    CREEPER(Tier.YONDER, "CREEPER", 4, "Gunpowder starts a standoff. Crouch while it swells; gunpowder when it calms.",
        "A blast that hurts only hostile mobs and breaks nothing. It never goes off near a player."),
    ENDERMAN(Tier.YONDER, "ENDERMAN", 5, "Offer it a block, then stay near it without looking it in the eye.",
        "Right-click it, then swing at a block: it fetches it, if you could dig there. Teleports to you when it's in trouble."),
    SKELETON(Tier.YONDER, "SKELETON", 0, "Hand it an arrow, then hit a target block three times.",
        "Covering fire from range."),
    IRON_GOLEM(Tier.BACKCOUNTRY, "IRON_GOLEM", 8, "Only one a player built. Four iron ingots, then a poppy.",
        "Takes hits meant for you and throws hostile mobs back.");

    public final Tier tier;
    /** The entity type's name, so the model doesn't need the server. */
    public final String entity;
    /** Damage per hit when it fights a hostile mob; 0 for one that never fights up close (the skeleton shoots). */
    public final double attack;
    public final String hint;
    /** What its special does, for the Bunkhouse menu and the list. */
    public final String special;

    Species(Tier tier, String entity, double attack, String hint, String special) {
        this.tier = tier;
        this.entity = entity;
        this.attack = attack;
        this.hint = hint;
        this.special = special;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** "Iron golem". */
    public String title() {
        String s = id().replace('_', ' ');
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    public static Species byId(String id) {
        for (Species s : values()) if (s.id().equals(id)) return s;
        return null;
    }
}
