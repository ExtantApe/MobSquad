package com.extantape.mobsquad.coax;

import org.bukkit.Location;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;

/**
 * One player's go at one mob's puzzle. {@link Coaxing} checks the rules every puzzle shares (the mob is still there,
 * the player is close enough, the time isn't up) and hands the rest to the puzzle. Each method returns null to carry
 * on, {@link #DONE} when the mob is coaxed, or a message saying why it ended. Breaking a rule ends the attempt, and
 * the player starts over with a fresh click.
 */
public abstract class Attempt {
    public static final String DONE = "\u0000done";
    public final Player player;
    public final Mob mob;
    final long until;
    Location last;
    int ticks;

    protected Attempt(Player player, Mob mob, int seconds) {
        this.player = player;
        this.mob = mob;
        this.until = System.currentTimeMillis() + seconds * 1000L;
        this.last = player.getLocation();
    }

    /** Every {@link Coaxing#PERIOD} ticks. */
    protected abstract String tick();

    /** The player right-clicks the same mob again, holding {@code hand}. */
    protected String click(ItemStack hand) {
        return null;
    }

    /** Something happened nearby that the puzzle might care about (a note block, an arrow, a hit). */
    protected String event(Event e) {
        return null;
    }

    /** What the player is told when time runs out. */
    protected String timeUp() {
        return "It got bored. Try again, a little quicker.";
    }

    /** How far the player may stray from the mob before it loses interest. */
    protected double reach() {
        return 8;
    }

    /** Called once when the attempt ends either way, to put the mob back how it was. */
    protected void end() {}

    /** Seconds since the attempt began. */
    protected double seconds() {
        return ticks * Coaxing.PERIOD / 20.0;
    }

    /** Horizontal blocks the player moved since the last check; a jump of {@code reach()} or more is a teleport. */
    protected double moved() {
        Location here = player.getLocation();
        double d = here.getWorld().equals(last.getWorld()) ? Math.hypot(here.getX() - last.getX(), here.getZ() - last.getZ()) : 0;
        return d >= reach() ? 0 : d;
    }
}
