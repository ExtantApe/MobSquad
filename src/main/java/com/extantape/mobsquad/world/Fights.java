package com.extantape.mobsquad.world;

import com.extantape.mobsquad.model.Species;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Piglin;
import org.bukkit.entity.PigZombie;
import org.bukkit.entity.Player;

/**
 * Critters fight hostile mobs, and only hostile mobs. A critter picks a fight with a hostile mob near its owner that's
 * after the owner or has hurt them or one of their critters, or with any hostile mob near the owner that isn't a neutral one (endermen
 * and piglins are left alone unless they start it) and hasn't been given a name. It never wanders more than
 * {@link #LEASH} blocks from its owner to do it. The skeleton shoots its own steady arrows; the rest close in and hit.
 */
public final class Fights {
    /** How far around the owner a critter looks for a fight. */
    public static final double SPOT = 10;
    /** Further than this from its owner, a critter drops the fight and comes back. */
    public static final double LEASH = 16;
    static final long COOLDOWN_MS = 1000, SHOT_MS = 1500;
    static final double RANGE = 15, ARROW_DAMAGE = 2.0;
    private final Posse posse;
    private final Map<UUID, LivingEntity> targets = new HashMap<>();
    private final Map<UUID, Long> lastHit = new HashMap<>();
    static final long PROVOKED_MS = 30_000;
    private final Map<UUID, Long> provoked = new HashMap<>();
    private final Map<UUID, UUID> provokedOwner = new HashMap<>();

    public Fights(Posse posse) {
        this.posse = posse;
    }

    /** Whether a critter may ever fight this entity. */
    public boolean fair(Entity e) {
        return e instanceof Enemy && e instanceof LivingEntity l && !l.isDead() && !posse.isCritter(e);
    }

    /**
     * Whether a critter may go after this target at all, whoever picked it (MobSquad or the mob's own instincts, such
     * as an iron golem's): only a fair one, and a neutral or named one only when it started something.
     */
    public boolean mayTarget(Entity critter, Entity target) {
        if (!fair(target)) return false;
        Posse.Out o = posse.of(critter);
        if (o == null) return false;
        Player owner = org.bukkit.Bukkit.getPlayer(o.owner());
        return owner != null && (startedIt(target, owner) || !neutral(target) && target.customName() == null);
    }

    static boolean neutral(Entity e) {
        return e instanceof Enderman || e instanceof PigZombie || e instanceof Piglin;
    }

    /** A mob started it when it's after the owner, or has really hurt the owner or one of their critters lately. */
    private boolean startedIt(Entity e, Player owner) {
        if (e instanceof Mob m && owner.equals(m.getTarget())) return true;
        Long when = provoked.get(e.getUniqueId());
        return when != null && System.currentTimeMillis() - when < PROVOKED_MS && owner.getUniqueId().equals(provokedOwner.get(e.getUniqueId()));
    }

    /** Called when a hostile mob's hit on a player or a critter goes through. */
    public void hurtBy(Entity attacker, UUID owner) {
        provoked.put(attacker.getUniqueId(), System.currentTimeMillis());
        provokedOwner.put(attacker.getUniqueId(), owner);
        if (provoked.size() > 512) provoked.values().removeIf(t -> System.currentTimeMillis() - t > PROVOKED_MS);
        provokedOwner.keySet().retainAll(provoked.keySet());
    }

    LivingEntity pick(Posse.Out o, Player owner) {
        LivingEntity best = null;
        double bestD = Double.MAX_VALUE;
        for (Entity e : owner.getNearbyEntities(SPOT, SPOT / 2, SPOT)) {
            if (!fair(e)) continue;
            boolean started = startedIt(e, owner);
            if (!started && (neutral(e) || e.customName() != null)) continue;
            double d = e.getLocation().distanceSquared(o.mob().getLocation()) - (started ? 1000 : 0);
            if (d < bestD) { bestD = d; best = (LivingEntity) e; }
        }
        return best;
    }

    /** One step of fighting for a critter. Returns true when it's busy fighting, so it doesn't walk back yet. */
    boolean step(Posse.Out o, Player owner) {
        Mob m = o.mob();
        Species s = o.critter().species();
        UUID id = o.critter().id();
        if (s == Species.ALLAY) return false; // the allay never fights
        if (m.getLocation().distance(owner.getLocation()) > LEASH) return drop(o);
        LivingEntity t = targets.get(id);
        if (t == null || !fair(t) || !t.isValid() || !t.getWorld().equals(m.getWorld()) || t.getLocation().distance(owner.getLocation()) > LEASH) {
            t = pick(o, owner);
            if (t == null) return drop(o);
            targets.put(id, t);
        }
        m.setTarget(null);
        if (s.attack <= 0) return shoot(o, t); // the skeleton
        double reach = 1.6 + (m.getWidth() + t.getWidth()) / 2;
        if (m.getLocation().distance(t.getLocation()) > reach) {
            m.getPathfinder().moveTo(t, 1.3);
        } else {
            long now = System.currentTimeMillis();
            if (now - lastHit.getOrDefault(id, 0L) >= COOLDOWN_MS) {
                lastHit.put(id, now);
                m.lookAt(t);
                m.swingMainHand();
                t.damage(s.attack, DamageSource.builder(DamageType.MOB_ATTACK).withCausingEntity(m).withDirectEntity(m).build());
            }
        }
        return true;
    }

    /** A steady shot every {@link #SHOT_MS} within {@link #RANGE} blocks with a clear line; closer in otherwise. */
    private boolean shoot(Posse.Out o, LivingEntity t) {
        Mob m = o.mob();
        double d = m.getLocation().distance(t.getLocation());
        if (d > RANGE || !m.hasLineOfSight(t)) {
            m.getPathfinder().moveTo(t, 1.2);
            return true;
        }
        m.getPathfinder().stopPathfinding();
        m.lookAt(t);
        long now = System.currentTimeMillis();
        if (now - lastHit.getOrDefault(o.critter().id(), 0L) < SHOT_MS) return true;
        lastHit.put(o.critter().id(), now);
        org.bukkit.util.Vector aim = t.getLocation().add(0, t.getHeight() * 0.6, 0).toVector().subtract(m.getEyeLocation().toVector());
        aim.setY(aim.getY() + aim.length() * 0.08); // a little arc
        org.bukkit.entity.Arrow a = m.launchProjectile(org.bukkit.entity.Arrow.class, aim.normalize().multiply(1.8));
        a.setPickupStatus(org.bukkit.entity.AbstractArrow.PickupStatus.DISALLOWED);
        a.setDamage(ARROW_DAMAGE);
        return true;
    }

    private boolean drop(Posse.Out o) {
        targets.remove(o.critter().id());
        if (o.mob().getTarget() != null) o.mob().setTarget(null);
        return false;
    }

    /** What a critter is fighting right now, if anything. */
    public LivingEntity target(UUID critter) {
        return targets.get(critter);
    }

    void forget(UUID critter) {
        targets.remove(critter);
        lastHit.remove(critter);
    }
}
