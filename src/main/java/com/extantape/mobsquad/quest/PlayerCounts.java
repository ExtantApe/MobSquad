package com.extantape.mobsquad.quest;

import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;

/** Reads the game's own statistics for one player and turns them into a {@link PlayStyle}. Main thread only. */
public final class PlayerCounts {
    private PlayerCounts() {}

    public static PlayStyle style(Player p) {
        return PlayStyle.pick(placed(p), hostileKills(p), walked(p), p.getStatistic(Statistic.ANIMALS_BRED));
    }

    /** Blocks put down: the game counts using a block item each time it is placed. */
    static long placed(Player p) {
        long n = 0;
        for (Material m : Material.values()) {
            if (m.isLegacy() || !m.isBlock() || !m.isItem()) continue;
            try {
                n += p.getStatistic(Statistic.USE_ITEM, m);
            } catch (IllegalArgumentException ignored) {
                // not a countable item
            }
        }
        return n;
    }

    /** Hostile mobs beaten: only the kinds that are Monsters, so a cow doesn't count. */
    static long hostileKills(Player p) {
        long n = 0;
        for (EntityType t : EntityType.values()) {
            Class<?> c = t.getEntityClass();
            if (c == null || !Monster.class.isAssignableFrom(c)) continue;
            try {
                n += p.getStatistic(Statistic.KILL_ENTITY, t);
            } catch (IllegalArgumentException ignored) {
                // not a countable kind
            }
        }
        return n;
    }

    /** Blocks walked or sprinted on foot. */
    static long walked(Player p) {
        return (p.getStatistic(Statistic.WALK_ONE_CM) + (long) p.getStatistic(Statistic.SPRINT_ONE_CM)) / 100;
    }
}
