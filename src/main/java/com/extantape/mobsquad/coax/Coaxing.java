package com.extantape.mobsquad.coax;

import com.extantape.mobsquad.Squads;
import com.extantape.mobsquad.model.Critter;
import com.extantape.mobsquad.model.Levels;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.NotePlayEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Runs the coaxing puzzles. Right-click a mob with its starting item to begin. The rules every puzzle shares: you need
 * its tier; a named, leashed or busy mob, or one somebody else is already coaxing, isn't yours to coax; a claim
 * plugin that refuses the click wins; stray too far or run out of time and it loses interest. While a mob is being
 * coaxed it never targets the player and a creeper can't go off.
 */
public final class Coaxing implements Listener, Runnable {
    public static final long PERIOD = 5;
    private final Squads squads;
    private final Map<EntityType, Puzzle> puzzles = new HashMap<>();
    private final Map<UUID, Attempt> attempts = new HashMap<>();
    /** After an attempt ends, the mob leaves that player alone for {@link #GRACE_MS}: a failed standoff never blows up in your face. */
    static final long GRACE_MS = 10_000;
    private final Map<UUID, Grace> grace = new HashMap<>();

    record Grace(UUID player, long until) {}

    public Coaxing(Squads squads) {
        this.squads = squads;
        for (Puzzle p : Puzzles.all()) puzzles.put(EntityType.valueOf(p.species().entity), p);
    }

    private boolean spared(Entity mob, Entity target) {
        Grace g = grace.get(mob.getUniqueId());
        return g != null && target != null && g.player().equals(target.getUniqueId()) && g.until() >= System.currentTimeMillis();
    }

    private Attempt on(Entity mob) {
        for (Attempt a : attempts.values()) if (a.mob.equals(mob)) return a;
        return null;
    }

    private static void say(Player p, String text, NamedTextColor color) {
        p.sendMessage(Component.text(text, color));
    }

    /** Runs late, so a claim plugin that refuses the click has already said no. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onClick(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || !(event.getRightClicked() instanceof Mob mob)) return;
        Player p = event.getPlayer();
        ItemStack hand = p.getInventory().getItemInMainHand();
        Attempt mine = attempts.get(p.getUniqueId());
        if (mine != null && mine.mob.equals(mob)) {
            event.setCancelled(true);
            finish(mine, mine.click(hand));
            return;
        }
        Puzzle puzzle = puzzles.get(mob.getType());
        if (puzzle == null || !puzzle.starts(hand) || !p.hasPermission("mobsquad.use") || squads.posse().isCritter(mob)) return;
        if (!squads.settings().enabledIn(p.getWorld().getName())) return;
        event.setCancelled(true); // no breeding, taming, trading or item swaps while coaxing
        String what = puzzle.species().title().toLowerCase(java.util.Locale.ROOT);
        int level = squads.wranglinLevel(p);
        String refusal = mob.customName() != null ? "That " + what + " has a name. It belongs to somebody already."
            : mob.isLeashed() ? "That " + what + "'s on a lead. It belongs to somebody already."
            : mob.isInsideVehicle() || !mob.getPassengers().isEmpty() ? "That " + what + "'s busy."
            : on(mob) != null ? "Somebody's already coaxing that " + what + "."
            : !Levels.unlocked(puzzle.species().tier, level) ? "A " + what + " is a " + puzzle.species().tier.title
                + " critter. You can coax one from Wranglin' level " + puzzle.species().tier.level + "."
            : squads.get(p) == null ? "Your Bunkhouse list can't be read right now. Ask an admin to check the server log."
            : puzzle.refuse(p, mob);
        if (refusal != null) {
            say(p, refusal, NamedTextColor.YELLOW);
            grace.put(mob.getUniqueId(), new Grace(p.getUniqueId(), System.currentTimeMillis() + GRACE_MS)); // no going off right after a no
            return;
        }
        if (mine != null) drop(mine, "You turned to something else.");
        Attempt a = puzzle.begin(p, mob);
        if (puzzle.uses()) Puzzles.use(p, hand);
        attempts.put(p.getUniqueId(), a);
        say(p, puzzle.intro(), NamedTextColor.GOLD);
        mob.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, mob.getLocation().add(0, mob.getHeight() / 2, 0), 5, 0.3, 0.3, 0.3);
    }

    @Override
    public void run() {
        long now = System.currentTimeMillis();
        grace.values().removeIf(g -> g.until() < now);
        for (Attempt a : new ArrayList<>(attempts.values())) {
            a.ticks++;
            Player p = a.player;
            String shared = !p.isOnline() ? ""
                : !a.mob.isValid() ? "It wandered off."
                : !a.mob.getWorld().equals(p.getWorld()) || a.mob.getLocation().distance(p.getLocation()) > a.reach() ? "It lost interest when you wandered off."
                : now > a.until ? a.timeUp()
                : null;
            if (shared != null) {
                drop(a, shared);
                continue;
            }
            String r = a.tick();
            a.last = p.getLocation();
            finish(a, r);
        }
    }

    private void finish(Attempt a, String result) {
        if (result == null || attempts.get(a.player.getUniqueId()) != a) return;
        if (result.equals(Attempt.DONE)) coaxed(a);
        else drop(a, result);
    }

    private void drop(Attempt a, String why) {
        attempts.remove(a.player.getUniqueId(), a);
        if (a.mob.isValid()) {
            a.end();
            grace.put(a.mob.getUniqueId(), new Grace(a.player.getUniqueId(), System.currentTimeMillis() + GRACE_MS));
            if (a.mob instanceof org.bukkit.entity.Creeper c) {
                var away = c.getLocation().toVector().subtract(a.player.getLocation().toVector()).setY(0);
                if (away.lengthSquared() > 0.01) c.getPathfinder().moveTo(c.getLocation().add(away.normalize().multiply(6)), 1.0);
            }
        }
        if (!why.isEmpty() && a.player.isOnline()) say(a.player, why, NamedTextColor.YELLOW);
    }

    private void coaxed(Attempt a) {
        attempts.remove(a.player.getUniqueId());
        a.end();
        Player p = a.player;
        Puzzle puzzle = puzzles.get(a.mob.getType());
        Location at = a.mob.getLocation();
        a.mob.remove();
        squads.add(p, Critter.fresh(puzzle.species()));
        at.getWorld().spawnParticle(Particle.HEART, at.clone().add(0, 0.6, 0), 6, 0.4, 0.3, 0.4);
        p.playSound(at, Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.4f);
        String what = puzzle.species().title().toLowerCase(java.util.Locale.ROOT);
        say(p, "You coaxed " + (what.matches("^[aeiou].*") ? "an " : "a ") + what + "! It's waiting in your Bunkhouse.", NamedTextColor.GREEN);
        squads.addWranglin(p, squads.settings().coaxXp());
    }

    private void tell(Event e) {
        for (Attempt a : new ArrayList<>(attempts.values())) finish(a, a.event(e));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onNote(NotePlayEvent e) {
        tell(e);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onArrow(ProjectileHitEvent e) {
        tell(e);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent e) {
        tell(e);
    }

    /** A mob being coaxed never turns on the player coaxing it. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onTarget(EntityTargetEvent e) {
        Attempt a = on(e.getEntity());
        if (a != null && a.player.equals(e.getTarget()) || spared(e.getEntity(), e.getTarget())) e.setCancelled(true);
    }

    /** A creeper in a standoff, or just out of one, can't go off. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPrime(ExplosionPrimeEvent e) {
        if (on(e.getEntity()) != null || grace.containsKey(e.getEntity().getUniqueId())) e.setCancelled(true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Attempt a = attempts.get(e.getPlayer().getUniqueId());
        if (a != null) drop(a, "");
    }

    /** Puts every mob being coaxed back how it was, when the plugin stops. */
    public void stop() {
        for (Attempt a : List.copyOf(attempts.values())) drop(a, "");
    }
}
