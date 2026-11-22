package com.extantape.mobsquad.world;

import com.destroystokyo.paper.ClientOption;
import com.extantape.mobsquad.Squads;
import com.extantape.mobsquad.model.CoatData;
import com.extantape.mobsquad.model.Coats;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

/**
 * Shows each out critter's spirit coat, only in the worlds listed under {@code spirit-coats.worlds}. Wolves, cats, pigs,
 * cows, chickens and frogs wear a real variant; every other critter gets a glow in the coat's color (a scoreboard team
 * color) and a light particle trail. Nothing here is saved: the look is put on when a critter goes out and whenever its
 * coat or world changes, and taken off at once when its owner leaves a magic world.
 *
 * The trail is a couple of particles every half second, sent only to players within their own view distance, and never
 * to a player who set particles to minimal in their game options.
 */
public final class SpiritCoats implements Runnable, Listener {
    public static final long PERIOD = 10;
    private static final String TEAM = "ms_coat_";
    private final Squads squads;
    private final Posse posse;
    private CoatData data = CoatData.EMPTY;
    /** The coat each critter shows right now (0 for none). */
    private final Map<UUID, Integer> shown = new HashMap<>();
    /** A variant critter's look before the coat, to put back. */
    private final Map<UUID, String> originals = new HashMap<>();
    private final Set<UUID> glowing = new HashSet<>();
    private final Map<String, Particle> particles = new HashMap<>();
    private int beat;

    public SpiritCoats(Squads squads, Posse posse) {
        this.squads = squads;
        this.posse = posse;
        removeTeams(); // a crash can leave teams behind
    }

    /** Checks coats.yml's entries against what this server has. */
    public static CoatData.Validator validator() {
        return new CoatData.Validator() {
            @Override public boolean particle(String key) { return Variants.particle(key); }
            @Override public boolean variant(String species, String key) { return Variants.exists(species, key); }
        };
    }

    public void data(CoatData data) {
        this.data = data;
        particles.clear();
        refreshAll();
    }

    public CoatData data() {
        return data;
    }

    /** What the critter should wear now: its worn coat in a magic world with its owner there, otherwise nothing. */
    private int wanted(Posse.Out o) {
        Mob m = o.mob();
        Player p = Bukkit.getPlayer(o.owner());
        if (p == null || !m.isValid()) return 0;
        if (!squads.settings().coatsIn(m.getWorld().getName()) || !squads.settings().coatsIn(p.getWorld().getName())) return 0;
        return Coats.worn(o.critter());
    }

    /** Puts on, changes or takes off a critter's look so it matches what it should wear. Cheap when nothing changed. */
    public void apply(Posse.Out o) {
        UUID id = o.critter().id();
        int want = wanted(o);
        if (want == shown.getOrDefault(id, 0)) return;
        take(o);
        if (want != 0) put(o, want);
        if (want == 0) shown.remove(id);
        else shown.put(id, want);
    }

    private void put(Posse.Out o, int coat) {
        Mob m = o.mob();
        String species = o.critter().species().id();
        if (data.hasVariants(species)) {
            String key = data.variant(species, coat);
            String before = Variants.current(m);
            if (key != null && Variants.set(m, key)) originals.putIfAbsent(o.critter().id(), before);
            return;
        }
        m.setGlowing(true);
        glowing.add(o.critter().id());
        Team t = team(coat);
        if (t != null) t.addEntity(m);
    }

    private void take(Posse.Out o) {
        UUID id = o.critter().id();
        Mob m = o.mob();
        if (originals.containsKey(id)) {
            String was = originals.remove(id);
            if (was != null && m.isValid()) Variants.set(m, was);
        }
        if (glowing.remove(id)) {
            Integer coat = shown.get(id);
            if (m.isValid()) m.setGlowing(false);
            if (coat != null) { Team t = scoreboard().getTeam(TEAM + coat); if (t != null) t.removeEntity(m); }
        }
    }

    /** A critter is leaving the world: forget its look. */
    public void forget(Posse.Out o) {
        take(o);
        shown.remove(o.critter().id());
    }

    public void refreshAll() {
        for (Posse.Out o : List.copyOf(posse.all())) apply(o);
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent e) {
        for (Posse.Out o : posse.of(e.getPlayer().getUniqueId())) apply(o);
    }

    private Scoreboard scoreboard() {
        return Bukkit.getScoreboardManager().getMainScoreboard();
    }

    private Team team(int coat) {
        try {
            Scoreboard sb = scoreboard();
            Team t = sb.getTeam(TEAM + coat);
            if (t == null) t = sb.registerNewTeam(TEAM + coat);
            t.color(data.coat(coat).color());
            return t;
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** Removes the scoreboard teams MobSquad made, on start and stop. */
    public void removeTeams() {
        if (Bukkit.getScoreboardManager() == null) return;
        Scoreboard sb = scoreboard();
        for (int i = 1; i <= Coats.COUNT; i++) {
            Team t = sb.getTeam(TEAM + i);
            if (t != null) t.unregister();
        }
    }

    private Particle particle(String key) {
        return particles.computeIfAbsent(key, k -> Registry.PARTICLE_TYPE.get(NamespacedKey.minecraft(k)));
    }

    /** Every half second: checks each critter's look, then leaves a light trail behind the ones that glow. */
    @Override
    public void run() {
        beat++;
        for (Posse.Out o : List.copyOf(posse.all())) {
            apply(o);
            UUID id = o.critter().id();
            if (!glowing.contains(id)) continue;
            String key = data.coat(shown.getOrDefault(id, 0)).particle();
            Particle particle = key == null ? null : particle(key);
            Mob m = o.mob();
            if (particle == null || !m.isValid()) continue;
            var at = m.getLocation().add(0, m.getHeight() / 2, 0);
            for (Player p : m.getWorld().getPlayers()) {
                ClientOption.ParticleVisibility vis = p.getClientOption(ClientOption.PARTICLE_VISIBILITY);
                if (vis == ClientOption.ParticleVisibility.MINIMAL) continue;
                if (vis == ClientOption.ParticleVisibility.DECREASED && beat % 2 == 0) continue;
                double range = Math.min(p.getViewDistance(), Bukkit.getViewDistance()) * 16.0;
                if (p.getLocation().distanceSquared(at) > range * range || !p.canSee(m)) continue;
                p.spawnParticle(particle, at, 2, 0.3, 0.3, 0.3, 0);
            }
        }
    }
}
