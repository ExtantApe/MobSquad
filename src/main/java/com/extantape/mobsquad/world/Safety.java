package com.extantape.mobsquad.world;

import com.extantape.mobsquad.Squads;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.entity.PlayerLeashEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * The safety rules (brief, "Safety and cost"). A critter never hurts or targets a player and is never hurt by one,
 * never changes a block, never explodes, never burns in the sun and never wanders off by teleporting. It can't be
 * leashed, bred, named, ridden, traded with or given items. Out of health, it's tuckered out instead of dying, so it
 * never drops anything.
 */
public final class Safety implements Listener {
    private final Squads squads;

    public Safety(Squads squads) {
        this.squads = squads;
    }

    private boolean critter(Entity e) {
        return e != null && squads.posse().isCritter(e);
    }

    private static Entity source(Entity damager) {
        return damager instanceof Projectile p && p.getShooter() instanceof Entity s ? s : damager;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent event) {
        Entity from = source(event.getDamager());
        if (critter(event.getEntity()) && from instanceof Player) event.setCancelled(true);
        else if (critter(from) && event.getEntity() instanceof Player) event.setCancelled(true);
        else if (critter(from) && critter(event.getEntity())) event.setCancelled(true);
    }

    /** Out of health: tuckered out, never dead. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!critter(event.getEntity()) || !(event.getEntity() instanceof org.bukkit.entity.LivingEntity l)) return;
        if (event.getFinalDamage() < l.getHealth()) return;
        event.setCancelled(true);
        Posse.Out o = squads.posse().of(event.getEntity());
        if (o != null) squads.tucker(o);
        else event.getEntity().remove(); // a stray from before a crash: it was never out as far as the Bunkhouse knows
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onTarget(EntityTargetEvent event) {
        if (critter(event.getEntity()) && (event.getTarget() instanceof Player || critter(event.getTarget()))) event.setCancelled(true);
        if (critter(event.getTarget()) && event.getEntity() instanceof org.bukkit.entity.Tameable) event.setCancelled(true); // pets leave them be
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlock(EntityChangeBlockEvent event) {
        if (critter(event.getEntity())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPrime(ExplosionPrimeEvent event) {
        if (critter(event.getEntity())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent event) {
        if (critter(event.getEntity())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBurn(EntityCombustEvent event) {
        if (critter(event.getEntity())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onTeleport(EntityTeleportEvent event) {
        if (critter(event.getEntity()) && !squads.posse().catchingUp()) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onTransform(EntityTransformEvent event) {
        if (critter(event.getEntity())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBreed(EntityBreedEvent event) {
        if (critter(event.getMother()) || critter(event.getFather())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onLeash(PlayerLeashEntityEvent event) {
        if (critter(event.getEntity())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent event) {
        if (critter(event.getRightClicked())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRemove(EntityRemoveEvent event) {
        squads.posse().gone(event.getEntity());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        squads.leave(event.getPlayer());
    }
}
