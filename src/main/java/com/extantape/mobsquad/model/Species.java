package com.extantape.mobsquad.model;

import java.util.Locale;

/** The launch critters, the home county (brief, "Launch critters"). */
public enum Species {
    CHICKEN(Tier.YARD, "CHICKEN", "Lead it on a trail of seeds, at a walk."),
    AXOLOTL(Tier.YARD, "AXOLOTL", "Not coaxable yet."),
    BEE(Tier.YARD, "BEE", "Not coaxable yet."),
    FOX(Tier.HOLLER, "FOX", "Not coaxable yet."),
    ALLAY(Tier.HOLLER, "ALLAY", "Not coaxable yet."),
    CREEPER(Tier.YONDER, "CREEPER", "Not coaxable yet."),
    ENDERMAN(Tier.YONDER, "ENDERMAN", "Not coaxable yet."),
    SKELETON(Tier.YONDER, "SKELETON", "Not coaxable yet."),
    IRON_GOLEM(Tier.BACKCOUNTRY, "IRON_GOLEM", "Not coaxable yet.");

    public final Tier tier;
    /** The entity type's name, so the model doesn't need the server. */
    public final String entity;
    public final String hint;

    Species(Tier tier, String entity, String hint) {
        this.tier = tier;
        this.entity = entity;
        this.hint = hint;
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
