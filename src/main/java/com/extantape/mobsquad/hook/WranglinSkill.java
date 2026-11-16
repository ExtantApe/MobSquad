package com.extantape.mobsquad.hook;

import org.bukkit.entity.Player;

/**
 * A skill plugin that shows Wranglin'. MobSquad keeps the level itself, in the player's file, and copies it here, so the
 * two never disagree and nothing is lost if that plugin goes away.
 */
public interface WranglinSkill {
    /** Shows this level for the player. Does nothing when their data isn't loaded yet. */
    void show(Player p, int level);
}
