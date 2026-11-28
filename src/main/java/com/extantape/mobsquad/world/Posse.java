package com.extantape.mobsquad.world;

import com.extantape.mobsquad.model.Critter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/**
 * The critters out in the world right now. Nothing here is saved: a critter out in the world is a mob that's never
 * saved with the world, and whenever it goes (called back, tuckered out, its chunk unloads, its owner leaves, the
 * server stops) it's simply back in the Bunkhouse list, so it can only ever come back once.
 */
public final class Posse implements Runnable {
    public static final long FOLLOW_TICKS = 10;
    /** Further than this from its owner, a critter catches up by stepping beside them. */
    static final double CATCH_UP = 24;
    /** Closer than this, it stops and waits. */
    static final double CLOSE = 3.5;
    private final NamespacedKey ownerKey, critterKey;
    private final Map<UUID, Out> out = new HashMap<>();
    private final Consumer<Out> onGone;
    private boolean catchingUp;
    private final Fights fights = new Fights(this);
    private Specials specials;
    private SpiritCoats coats;
    private java.util.function.Predicate<UUID> sidekick = id -> false;

    /** A critter out with its owner: which one, whose, and the mob standing in for it. */
    public record Out(UUID owner, Critter critter, Mob mob) {}

    public Posse(Plugin plugin, Consumer<Out> onGone) {
        this.ownerKey = new NamespacedKey(plugin, "owner");
        this.critterKey = new NamespacedKey(plugin, "critter");
        this.onGone = onGone;
    }

    /** The critter a mob stands in for, if it's one of ours. */
    public Out of(Entity e) {
        if (!(e instanceof Mob)) return null;
        String id = e.getPersistentDataContainer().get(critterKey, PersistentDataType.STRING);
        if (id == null) return null;
        Out o = out.get(UUID.fromString(id));
        return o != null && o.mob().equals(e) ? o : null;
    }

    /** Whether an entity is a critter, even a stray one this server run doesn't know (say, after a crash). */
    public boolean isCritter(Entity e) {
        return e.getPersistentDataContainer().has(critterKey, PersistentDataType.STRING);
    }

    /** True only while a critter is being moved beside its owner, so the safety rules let that teleport through. */
    public boolean catchingUp() {
        return catchingUp;
    }

    public Fights fights() {
        return fights;
    }

    /** Tells the posse which critters are sidekicks: they don't count toward the posse and don't answer a recall. */
    public void sidekicks(java.util.function.Predicate<UUID> isSidekick) {
        this.sidekick = isSidekick;
    }

    public boolean isSidekick(UUID critter) {
        return sidekick.test(critter);
    }

    /** The critters out with a player that count toward their posse: everyone but a sidekick. */
    public int counted(UUID owner) {
        int n = 0;
        for (Out o : of(owner)) if (!sidekick.test(o.critter().id())) n++;
        return n;
    }

    /** Calls a player's critters home, all but a sidekick. Returns how many went. */
    public int recall(UUID owner) {
        int n = 0;
        for (Out o : of(owner)) if (!sidekick.test(o.critter().id())) { back(o.critter().id()); n++; }
        return n;
    }

    public void specials(Specials specials) {
        this.specials = specials;
    }

    public void coats(SpiritCoats coats) {
        this.coats = coats;
    }

    public java.util.Collection<Out> all() {
        return out.values();
    }

    public Out byId(UUID critter) {
        return out.get(critter);
    }

    /** Swaps in a critter's new level and experience, and grows its stats to match. */
    public void update(Critter c) {
        Out o = out.get(c.id());
        if (o == null) return;
        Out n = new Out(o.owner(), c, o.mob());
        out.put(c.id(), n);
        if (c.level() != o.critter().level()) grow(n);
        if (coats != null) coats.apply(n); // a new level or a new pick can change the coat
    }

    /** Moves a critter somewhere on purpose (catching up, an enderman's fetch), letting the safety rules allow it. */
    public void moveCritter(Entity e, Location to) {
        catchingUp = true;
        try {
            e.teleport(to);
        } finally {
            catchingUp = false;
        }
    }

    private static final org.bukkit.attribute.Attribute[] GROWN = {
        org.bukkit.attribute.Attribute.MAX_HEALTH, org.bukkit.attribute.Attribute.MOVEMENT_SPEED, org.bukkit.attribute.Attribute.FLYING_SPEED};
    /** Each critter's stats as its mob was spawned, before growth. */
    private final Map<UUID, double[]> bases = new HashMap<>();

    /** Health and speed for a critter's level, from its mob's stats as spawned, keeping how hurt it is. */
    void grow(Out o) {
        Mob m = o.mob();
        double[] base = bases.computeIfAbsent(o.critter().id(), k -> {
            double[] b = new double[GROWN.length];
            for (int i = 0; i < GROWN.length; i++) {
                var inst = m.getAttribute(GROWN[i]);
                b[i] = inst == null ? -1 : inst.getBaseValue();
            }
            return b;
        });
        int level = o.critter().level();
        double was = m.getAttribute(GROWN[0]).getValue(), share = was > 0 ? m.getHealth() / was : 1;
        for (int i = 0; i < GROWN.length; i++) {
            var inst = m.getAttribute(GROWN[i]);
            if (inst == null || base[i] < 0) continue;
            inst.setBaseValue(base[i] * (i == 0 ? com.extantape.mobsquad.model.Growth.health(level) : com.extantape.mobsquad.model.Growth.speed(level)));
        }
        double max = m.getAttribute(GROWN[0]).getValue();
        m.setHealth(Math.max(0.5, Math.min(max, share * max)));
    }

    public boolean isOut(UUID critter) {
        return out.containsKey(critter);
    }

    public List<Out> of(UUID owner) {
        List<Out> list = new ArrayList<>();
        for (Out o : out.values()) if (o.owner().equals(owner)) list.add(o);
        return list;
    }

    /** Sends a critter out beside its owner. */
    public Out send(Player owner, Critter c) {
        EntityType type = EntityType.valueOf(c.species().entity);
        Location at = owner.getLocation().clone().add(owner.getLocation().getDirection().setY(0).normalize().multiply(-1.5));
        Entity spawned = owner.getWorld().spawnEntity(at, type, org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.CUSTOM, e -> {
            e.setPersistent(false);
            e.getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING, owner.getUniqueId().toString());
            e.getPersistentDataContainer().set(critterKey, PersistentDataType.STRING, c.id().toString());
            com.extantape.mobsquad.hook.LevelledMobsOptOut.mark(e); // before the spawn event, so LevelledMobs never levels it
            e.customName(Component.text(c.name()));
            e.setCustomNameVisible(true);
            if (e instanceof Mob m) {
                m.setRemoveWhenFarAway(false);
                m.setCanPickupItems(false);
            }
        });
        Out o = new Out(owner.getUniqueId(), c, (Mob) spawned);
        out.put(c.id(), o);
        grow(o);
        o.mob().setHealth(o.mob().getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue());
        if (coats != null) coats.apply(o);
        return o;
    }

    /** Takes a critter out of the world, back to the Bunkhouse list. */
    public void back(UUID critter) {
        Out o = out.remove(critter);
        if (o == null) return;
        fights.forget(critter);
        bases.remove(critter);
        if (specials != null) specials.forget(critter);
        if (coats != null) coats.forget(o);
        if (o.mob().isValid()) o.mob().remove();
        onGone.accept(o);
    }

    public void backAll(UUID owner) {
        for (Out o : of(owner)) back(o.critter().id());
    }

    public void backEveryone() {
        for (UUID id : List.copyOf(out.keySet())) back(id);
    }

    /** The mob went some other way (unloaded, removed by another plugin, despawned on peaceful). */
    public void gone(Entity e) {
        Out o = of(e);
        if (o != null) back(o.critter().id());
    }

    /** Keeps every critter beside its owner. */
    @Override
    public void run() {
        for (Out o : List.copyOf(out.values())) {
            Player p = Bukkit.getPlayer(o.owner());
            Mob m = o.mob();
            if (p == null || !p.isOnline() || !m.isValid() || !m.getWorld().equals(p.getWorld())) {
                back(o.critter().id());
                continue;
            }
            double d = m.getLocation().distance(p.getLocation());
            if (specials != null && d <= 2 * CATCH_UP && specials.step(o, p)) continue;
            if (d <= Fights.LEASH && fights.step(o, p)) continue;
            if (d > CATCH_UP) moveCritter(m, p.getLocation());
            else if (d > CLOSE) m.getPathfinder().moveTo(p.getLocation(), 1.2);
            else m.getPathfinder().stopPathfinding();
            if (m.getTarget() != null && !fights.mayTarget(m, m.getTarget())) m.setTarget(null);
        }
    }
}
