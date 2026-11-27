package com.extantape.mobsquad;

import com.extantape.mobsquad.model.Critter;
import com.extantape.mobsquad.model.Levels;
import com.extantape.mobsquad.model.Wrangler;
import com.extantape.mobsquad.store.Store;
import com.extantape.mobsquad.world.Posse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/** Players' squads while they're online: loaded on join, saved on every change and on leaving. */
public final class Squads {
    private final Store store;
    private final Logger log;
    private final Map<UUID, Wrangler> online = new HashMap<>();
    private Posse posse;
    private com.extantape.mobsquad.model.CoatData coatData = com.extantape.mobsquad.model.CoatData.EMPTY;
    private Settings settings;
    private com.extantape.mobsquad.hook.WranglinSkill skill;
    private java.util.function.BiConsumer<Player, com.extantape.mobsquad.model.Species> pageOpener;

    public Squads(Store store, Logger log, Settings settings) {
        this.store = store;
        this.log = log;
        this.settings = settings;
    }

    public void posse(Posse posse) {
        this.posse = posse;
        posse.sidekicks(id -> online.values().stream().anyMatch(w -> id.equals(w.sidekick)));
    }

    public void settings(Settings settings) {
        settings.disabledWorlds().addAll(keptOff);
        this.settings = settings;
    }

    private final java.util.Set<String> keptOff = new java.util.HashSet<>();

    /** Keeps MobSquad off in a world until the server stops, for another plugin's private world (API). */
    public void keepOff(String world) {
        keptOff.add(world);
        settings.disabledWorlds().add(world);
    }

    public void coatData(com.extantape.mobsquad.model.CoatData data) {
        this.coatData = data;
    }

    public com.extantape.mobsquad.model.CoatData coatData() {
        return coatData;
    }

    /** Picks the coat a critter wears (1 to 10, only one it has earned), or 0 for its newest. Returns false if it can't be worn. */
    public boolean pickCoat(Player p, UUID critter, int coat) {
        Wrangler w = get(p);
        if (w == null) return false;
        Critter c = w.find(critter);
        if (c == null || coat < 0 || coat > com.extantape.mobsquad.model.Coats.earned(c.level())) return false;
        Critter n = c.withCoat(coat);
        w.replace(n);
        save(w);
        posse.update(n);
        return true;
    }

    public Settings settings() {
        return settings;
    }

    public Posse posse() {
        return posse;
    }

    public Wrangler get(Player p) {
        return online.computeIfAbsent(p.getUniqueId(), id -> {
            try {
                Store.Loaded l = store.load(id);
                if (l.skipped() > 0) log.warning(l.skipped() + " lines in " + id + "'s MobSquad file couldn't be read and were skipped. They're dropped on the next save.");
                return l.wrangler();
            } catch (IOException e) {
                log.log(Level.SEVERE, "Couldn't read " + id + "'s MobSquad file. Nothing is saved for them until it can be read.", e);
                return null;
            }
        });
    }

    public void save(Wrangler w) {
        if (w == null) return;
        try {
            store.save(w);
        } catch (IOException e) {
            log.log(Level.SEVERE, "Couldn't save " + w.player + "'s MobSquad file", e);
        }
    }

    public void leave(Player p) {
        posse.backAll(p.getUniqueId());
        save(online.remove(p.getUniqueId()));
    }

    /** A skill plugin (AuraSkills) that shows Wranglin', or null. */
    public void skill(com.extantape.mobsquad.hook.WranglinSkill skill) {
        this.skill = skill;
    }

    public com.extantape.mobsquad.hook.WranglinSkill skill() {
        return skill;
    }

    /** Whether this player may coax and send out critters of a tier, by level and quests as config says. */
    public boolean tierOpen(Player p, com.extantape.mobsquad.model.Tier tier) {
        Wrangler w = get(p);
        return com.extantape.mobsquad.model.Unlocks.open(settings.tierUnlock(), tier, wranglinLevel(p),
            w == null ? java.util.Set.of() : w.questTiers);
    }

    /** When a locked tier opens, to finish a sentence ("from Wranglin' level 25"). */
    public String tierWhen(com.extantape.mobsquad.model.Tier tier) {
        return com.extantape.mobsquad.model.Unlocks.when(settings.tierUnlock(), tier);
    }

    public int wranglinLevel(Player p) {
        Wrangler w = get(p);
        return w == null ? 1 : Levels.level(w.wranglinXp);
    }

    /** Copies the player's Wranglin' level to the skill plugin, if there is one. */
    public void showWranglin(Player p) {
        if (skill != null) skill.show(p, wranglinLevel(p));
    }

    public void addWranglin(Player p, long xp) {
        Wrangler w = get(p);
        if (w == null) return;
        int before = Levels.level(w.wranglinXp);
        w.wranglinXp += xp;
        int after = Levels.level(w.wranglinXp);
        save(w);
        if (skill != null && after != before) skill.show(p, after);
        if (after > before) p.sendMessage(Component.text("Your Wranglin' is now level " + after + ".", NamedTextColor.GOLD));
    }

    /** Called when a player gets a critter whose La Ménagerie page they haven't opened. */
    public void pageOpener(java.util.function.BiConsumer<Player, com.extantape.mobsquad.model.Species> opener) {
        this.pageOpener = opener;
    }

    public void add(Player p, Critter c) {
        Wrangler w = get(p);
        if (w == null) return;
        w.critters.add(c);
        save(w);
        if (!w.pages.contains(c.species()) && pageOpener != null) pageOpener.accept(p, c.species());
    }

    /** A critter the player coaxed: added to their Bunkhouse, and noted for the Season 1 finale if it's on. */
    public void coaxed(Player p, Critter c) {
        Wrangler w = get(p);
        if (w == null) return;
        w.roundup.coaxed(c.id());
        add(p, c);
    }

    /** A player sent a critter out from a Bunkhouse, which the Season 1 finale may be waiting for. */
    public void sentFromBunkhouse(Player p, Critter c) {
        Wrangler w = get(p);
        if (w != null && w.roundup.sentOut(c.id())) save(w);
    }

    /** Experience for a critter that's out; tells its owner when it levels up or passes a milestone. */
    public void critterXp(Posse.Out o, long xp) {
        Player p = Bukkit.getPlayer(o.owner());
        if (p == null) return;
        Wrangler w = get(p);
        if (w == null) return;
        Critter c = w.find(o.critter().id());
        if (c == null || c.level() >= com.extantape.mobsquad.model.Growth.MAX) return;
        Critter n = c.withXp(c.xp() + xp);
        w.replace(n);
        posse.update(n);
        if (n.level() == c.level()) return;
        save(w);
        leveled(p, c, n);
    }

    private void leveled(Player p, Critter c, Critter n) {
        p.sendMessage(Component.text(n.name() + " is now level " + n.level() + ".", NamedTextColor.GOLD));
        if (com.extantape.mobsquad.model.Growth.stage(n.level()) > com.extantape.mobsquad.model.Growth.stage(c.level()))
            p.sendMessage(Component.text("Its special grows stronger: " + n.species().special, NamedTextColor.LIGHT_PURPLE));
        int coat = com.extantape.mobsquad.model.Coats.earned(n.level());
        if (coat > com.extantape.mobsquad.model.Coats.earned(c.level())) {
            var look = coatData.coat(coat);
            p.sendMessage(Component.text(n.name() + " earned a spirit coat: ", NamedTextColor.GOLD)
                .append(Component.text(look.name(), com.extantape.mobsquad.model.Coats.readable(look.color())))
                .append(Component.text(settings.coatWorlds().isEmpty() ? ". Pick it in the Bunkhouse." : ". It shows in magic worlds. Pick it in the Bunkhouse.", NamedTextColor.GOLD)));
        }
        p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.4f);
    }

    public boolean isSidekick(UUID owner, UUID critter) {
        Wrangler w = online.get(owner);
        return w != null && critter.equals(w.sidekick);
    }

    /** A minute out: time together and walking experience, then today's sidekick roll. */
    public void minuteOut(Player p, java.util.List<Posse.Out> out) {
        Wrangler w = get(p);
        if (w == null || out.isEmpty()) return;
        java.util.List<UUID> ids = new java.util.ArrayList<>();
        for (Posse.Out o : out) {
            Critter c = w.find(o.critter().id());
            if (c == null) continue;
            ids.add(c.id());
            Critter n = c.minuteOut(com.extantape.mobsquad.model.Growth.WALK_XP);
            w.replace(n);
            posse.update(n);
            if (n.level() > c.level()) leveled(p, c, n);
        }
        long day = java.time.LocalDate.now().toEpochDay();
        Critter chosen = com.extantape.mobsquad.model.Sidekicks.roll(w, ids, day, java.util.concurrent.ThreadLocalRandom.current()::nextDouble);
        if (chosen != null) {
            p.sendMessage(Component.text(chosen.name() + " has decided you're its favorite person. It's your sidekick now: it goes everywhere with you and doesn't count toward your posse.", NamedTextColor.GOLD));
            p.playSound(p.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.7f, 1.2f);
        }
        save(w);
    }

    /** Makes someone a player's sidekick, or nobody (admins). */
    public void setSidekick(Player p, Critter c) {
        Wrangler w = get(p);
        if (w == null) return;
        w.sidekick = c == null ? null : c.id();
        save(w);
    }

    /** Puts each online player's sidekick beside them whenever it can be: after joining, a world change, a rest. */
    public void keepSidekicks() {
        long now = System.currentTimeMillis();
        for (Player p : Bukkit.getOnlinePlayers()) {
            Wrangler w = online.get(p.getUniqueId());
            if (w == null || w.sidekick == null || posse.isOut(w.sidekick)) continue;
            Critter c = w.find(w.sidekick);
            if (c == null || c.resting(now) || p.isDead() || p.getGameMode() == org.bukkit.GameMode.SPECTATOR) continue;
            if (!settings.enabledIn(p.getWorld().getName()) || !p.hasPermission("mobsquad.use") || !tierOpen(p, c.species().tier)) continue;
            posse.send(p, c);
            p.sendActionBar(Component.text(c.name() + " is at your side.", NamedTextColor.GOLD));
        }
    }

    /** Out of health: back to the Bunkhouse to rest a few real minutes. */
    public void tucker(Posse.Out o) {
        posse.back(o.critter().id());
        Player p = Bukkit.getPlayer(o.owner());
        if (p == null) return;
        Wrangler w = get(p);
        if (w == null) return;
        Critter c = w.find(o.critter().id());
        if (c == null) return;
        w.replace(c.tuckered(System.currentTimeMillis() + settings.recoveryMinutes() * 60_000L));
        save(w);
        p.sendMessage(Component.text(c.name() + " is tuckered out and headed back to the Bunkhouse to rest.", NamedTextColor.YELLOW));
    }

    public void saveAll() {
        for (Wrangler w : online.values()) save(w);
    }
}
