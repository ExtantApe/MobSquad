package com.extantape.mobsquad.coax;

import com.extantape.mobsquad.model.Species;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** How one kind of mob is coaxed. */
public interface Puzzle {
    Species species();

    /** Whether right-clicking with this item starts the puzzle. */
    boolean starts(ItemStack hand);

    /** A reason this particular mob can't be coaxed, beyond the shared ones, or null. */
    default String refuse(Player p, Mob mob) {
        return null;
    }

    /** Whether the starting item is used up. */
    default boolean uses() {
        return true;
    }

    Attempt begin(Player p, Mob mob);

    /** The line the player sees when it starts. */
    String intro();
}
