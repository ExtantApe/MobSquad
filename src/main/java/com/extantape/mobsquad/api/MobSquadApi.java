package com.extantape.mobsquad.api;

import java.util.List;
import java.util.UUID;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * MobSquad for other plugins, such as MobShowdown. Get it from Bukkit's services manager:
 * {@code Bukkit.getServicesManager().load(MobSquadApi.class)}. Call it on the main thread.
 * Nothing here lets another plugin change a player's critters: it can read them and send them home.
 */
public interface MobSquadApi {
    /** Bumped when a method is added or changes; check it before relying on anything newer. */
    int VERSION = 2; // 2: CritterInfo has the spirit coat

    default int version() {
        return VERSION;
    }

    /** A player's Bunkhouse, in their own order. Empty for a player who isn't online. */
    List<CritterInfo> critters(Player player);

    /** Whether the player has a critter out with them right now, sidekick included. */
    boolean hasCritterOut(Player player);

    /** Sends every critter the player has out back to their Bunkhouse, sidekick included. */
    void sendHome(Player player);

    /** Whether MobSquad is on in this world. */
    boolean enabledIn(World world);

    /**
     * Keeps MobSquad off in a world until the server stops, as if it were listed under disabled-worlds, without
     * touching config.yml. For a plugin's own private world, such as MobShowdown's arena.
     */
    void keepOff(String worldName);

    /** How strong a critter of this species is at this level, for a plugin that stages its own fights. */
    CritterStats stats(String species, int level);
}
