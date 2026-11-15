package com.extantape.mobsquad.coax;

import com.extantape.mobsquad.Squads;
import com.extantape.mobsquad.model.Critter;
import com.extantape.mobsquad.model.Species;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * The chicken's puzzle. Hold seeds and right-click a chicken to start a trail, then walk it at least
 * {@value #DISTANCE} blocks from where it stood, within {@value #SECONDS} seconds, still holding seeds. Run and it
 * loses interest. A named or leashed chicken, or one on a claim where you can't interact, isn't yours to coax.
 */
public final class ChickenTrail implements Listener, Runnable {
    public static final double DISTANCE = 10, LOST = 8;
    public static final int SECONDS = 40;
    public static final long PERIOD = 5;
    /** Faster than this across the ground in one check, about 1.25 times walking pace, counts as running. */
    static final double RUNNING = 1.35;
    static final Set<Material> SEEDS = Set.of(Material.WHEAT_SEEDS, Material.BEETROOT_SEEDS, Material.MELON_SEEDS,
        Material.PUMPKIN_SEEDS, Material.TORCHFLOWER_SEEDS, Material.PITCHER_POD);
    private final Squads squads;
    private final Map<UUID, Trail> trails = new HashMap<>();

    static final class Trail {
        final UUID player;
        final Chicken chicken;
        final Location start;
        final long until;
        Location last;

        Trail(UUID player, Chicken chicken, Location start, long until, Location last) {
            this.player = player;
            this.chicken = chicken;
            this.start = start;
            this.until = until;
            this.last = last;
        }
    }

    public ChickenTrail(Squads squads) {
        this.squads = squads;
    }

    static boolean seeds(ItemStack it) {
        return it != null && SEEDS.contains(it.getType());
    }

    /** Runs late, so a claim plugin that refuses the click has already said no. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onClick(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || !(event.getRightClicked() instanceof Chicken chicken)) return;
        Player p = event.getPlayer();
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (!seeds(hand) || !p.hasPermission("mobsquad.use") || squads.posse().isCritter(chicken)) return;
        if (!squads.settings().enabledIn(p.getWorld().getName())) return;
        event.setCancelled(true); // no breeding while coaxing
        String refusal = chicken.customName() != null ? "That chicken has a name. It belongs to somebody already."
            : chicken.isLeashed() ? "That chicken's on a lead. It belongs to somebody already."
            : chicken.isInsideVehicle() || !chicken.getPassengers().isEmpty() ? "That chicken's busy."
            : trails.values().stream().anyMatch(t -> t.chicken.equals(chicken)) ? "Somebody's already leading that chicken."
            : squads.get(p) == null ? "Your Bunkhouse list can't be read right now. Ask an admin to check the server log."
            : null;
        if (refusal != null) {
            p.sendMessage(Component.text(refusal, NamedTextColor.YELLOW));
            return;
        }
        if (p.getGameMode() != GameMode.CREATIVE) hand.subtract();
        trails.put(p.getUniqueId(), new Trail(p.getUniqueId(), chicken, chicken.getLocation(), System.currentTimeMillis() + SECONDS * 1000L, p.getLocation()));
        p.sendMessage(Component.text("The chicken's interested. Walk it " + (int) DISTANCE + " blocks on a trail of seeds. Don't run.", NamedTextColor.GOLD));
        chicken.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, chicken.getLocation().add(0, 0.6, 0), 5, 0.3, 0.3, 0.3);
    }

    @Override
    public void run() {
        long now = System.currentTimeMillis();
        for (Iterator<Trail> it = trails.values().iterator(); it.hasNext();) {
            Trail t = it.next();
            Player p = Bukkit.getPlayer(t.player);
            String lost = lost(t, p, now);
            if (lost != null) {
                it.remove();
                if (!lost.isEmpty()) p.sendMessage(Component.text(lost, NamedTextColor.YELLOW));
                continue;
            }
            Chicken c = t.chicken;
            double toPlayer = c.getLocation().distance(p.getLocation());
            if (toPlayer > 2.5) c.getPathfinder().moveTo(p.getLocation(), 1.0);
            if (c.getLocation().distance(t.start) >= DISTANCE) {
                it.remove();
                coaxed(p, c);
            }
        }
    }

    private static String lost(Trail t, Player p, long now) {
        if (p == null || !p.isOnline()) return ""; // gone quietly
        Chicken c = t.chicken;
        if (!c.isValid()) return "The chicken wandered off.";
        Location here = p.getLocation();
        boolean running = p.isSprinting() || p.isInsideVehicle() || p.isGliding() || p.isFlying()
            || here.getWorld().equals(t.last.getWorld()) && moved(t.last, here) > RUNNING && moved(t.last, here) < LOST; // a longer jump is a teleport, not running
        t.last = here;
        if (running) return "Too fast. The chicken lost interest.";
        if (!c.getWorld().equals(p.getWorld()) || c.getLocation().distance(p.getLocation()) > LOST) return "The chicken lost the trail.";
        if (!seeds(p.getInventory().getItemInMainHand())) return "You put the seeds away, so the chicken lost interest.";
        if (now > t.until) return "The chicken got bored. Try again, a little quicker.";
        return null;
    }

    private static double moved(Location a, Location b) {
        return Math.hypot(b.getX() - a.getX(), b.getZ() - a.getZ());
    }

    private void coaxed(Player p, Chicken c) {
        Location at = c.getLocation();
        c.remove();
        Critter critter = Critter.fresh(Species.CHICKEN);
        squads.add(p, critter);
        at.getWorld().spawnParticle(Particle.HEART, at.clone().add(0, 0.6, 0), 6, 0.4, 0.3, 0.4);
        p.playSound(at, Sound.ENTITY_CHICKEN_AMBIENT, 1f, 1.2f);
        p.sendMessage(Component.text("You coaxed a chicken! It's waiting in your Bunkhouse.", NamedTextColor.GREEN));
        squads.addWranglin(p, squads.settings().coaxXp());
    }

    public void forget(UUID player) {
        trails.remove(player);
    }
}
