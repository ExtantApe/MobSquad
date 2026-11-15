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
    private Settings settings;
    private java.util.function.BiConsumer<Player, com.extantape.mobsquad.model.Species> pageOpener;

    public Squads(Store store, Logger log, Settings settings) {
        this.store = store;
        this.log = log;
        this.settings = settings;
    }

    public void posse(Posse posse) {
        this.posse = posse;
    }

    public void settings(Settings settings) {
        this.settings = settings;
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

    public int wranglinLevel(Player p) {
        Wrangler w = get(p);
        return w == null ? 1 : Levels.level(w.wranglinXp);
    }

    public void addWranglin(Player p, long xp) {
        Wrangler w = get(p);
        if (w == null) return;
        int before = Levels.level(w.wranglinXp);
        w.wranglinXp += xp;
        int after = Levels.level(w.wranglinXp);
        save(w);
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
        p.sendMessage(Component.text(n.name() + " is now level " + n.level() + ".", NamedTextColor.GOLD));
        if (com.extantape.mobsquad.model.Growth.stage(n.level()) > com.extantape.mobsquad.model.Growth.stage(c.level()))
            p.sendMessage(Component.text("Its special grows stronger: " + n.species().special, NamedTextColor.LIGHT_PURPLE));
        p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.4f);
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
