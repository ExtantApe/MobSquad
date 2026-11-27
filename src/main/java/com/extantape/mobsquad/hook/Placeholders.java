package com.extantape.mobsquad.hook;

import com.extantape.mobsquad.Squads;
import com.extantape.mobsquad.model.Critter;
import com.extantape.mobsquad.model.Species;
import com.extantape.mobsquad.model.Tier;
import com.extantape.mobsquad.model.Wrangler;
import com.extantape.mobsquad.quest.PlayStyle;
import com.extantape.mobsquad.quest.PlayerCounts;
import java.util.Locale;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * PlaceholderAPI values about the player they're asked for, so a quest plugin (BetonQuest's placeholder conditions, for
 * one) can check progress. Only online players have values; anyone else gets an empty string. Nothing here names
 * another player or says where anyone is. See {@link PlaceholderValues} for the list.
 */
public final class Placeholders extends PlaceholderExpansion {
    private final Plugin plugin;
    private final Squads squads;
    private final java.util.function.Predicate<org.bukkit.inventory.ItemStack> isBell;

    private Placeholders(Plugin plugin, Squads squads, java.util.function.Predicate<org.bukkit.inventory.ItemStack> isBell) {
        this.plugin = plugin;
        this.squads = squads;
        this.isBell = isBell;
    }

    /** Registers the placeholders. Call only when PlaceholderAPI is installed. {@code isBell} says whether an item is a dinnerbell. */
    public static boolean register(Plugin plugin, Squads squads, java.util.function.Predicate<org.bukkit.inventory.ItemStack> isBell) {
        return new Placeholders(plugin, squads, isBell).register();
    }

    @Override public String getIdentifier() { return "mobsquad"; }
    @Override public String getAuthor() { return "EXTANT APE"; }
    @Override public String getVersion() { return plugin.getPluginMeta().getVersion(); }
    @Override public boolean persist() { return true; }

    @Override
    public String onRequest(OfflinePlayer who, String params) {
        if (!(who instanceof Player p) || !p.isOnline()) return "";
        Wrangler w = squads.get(p);
        if (w == null) return "";
        String live = params.toLowerCase(Locale.ROOT);
        if (live.equals("roundup_ready")) return Boolean.toString(onMain(() -> w.roundup.ready(squads.posse()::isOut), false));
        if (live.equals("holding_bell")) return Boolean.toString(onMain(() -> isBell.test(p.getInventory().getItemInMainHand()), false));
        if (live.equals("style")) return onMain(() -> PlayerCounts.style(p), PlayStyle.ALL).id();
        return PlaceholderValues.value(params, squads.wranglinLevel(p), w, t -> squads.tierOpen(p, t));
    }

    /**
     * Player data is only safe to read on the main thread, and BetonQuest may ask from another one. From another thread
     * this waits up to two seconds for the main thread; if it can't, the answer is the safe one, {@code otherwise}.
     */
    private <T> T onMain(java.util.concurrent.Callable<T> read, T otherwise) {
        try {
            if (org.bukkit.Bukkit.isPrimaryThread()) return read.call();
            return org.bukkit.Bukkit.getScheduler().callSyncMethod(plugin, read).get(2, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception e) {
            return otherwise;
        }
    }

    /** The values, kept apart from PlaceholderAPI so they can be unit tested. */
    public static final class PlaceholderValues {
        private PlaceholderValues() {}

        /**
         * level: Wranglin' level. critters, pages: how many. tier_holler (and yonder, backcountry, yard): true or false,
         * open now by level and quests. page_creeper: true if that journal page is open. has_creeper: true if the player
         * has one. best_creeper: the highest level among their creepers, 0 if none. top: the highest level among all the
         * critters they own, 0 if none; it counts what they have, never what is out. holding_bell: true if the dinnerbell
         * is in their main hand. style: builder, fighter, explorer, caretaker or all, from their own play. For the Season 1
         * finale: roundup_coaxed (they've coaxed a critter since it started), roundup_sent (and sent it out from a
         * Bunkhouse) and roundup_ready (and it's out with them now). Anything else is null, which
         * PlaceholderAPI leaves as written.
         */
        public static String value(String params, int level, Wrangler w, java.util.function.Predicate<Tier> tierOpen) {
            String k = params.toLowerCase(Locale.ROOT);
            switch (k) {
                case "level": return Integer.toString(level);
                case "critters": return Integer.toString(w.critters.size());
                case "pages": return Integer.toString(w.pages.size());
                case "roundup_coaxed": return Boolean.toString(w.roundup.hasCoaxed());
                case "roundup_sent": return Boolean.toString(w.roundup.hasSent());
                case "top": return Integer.toString(w.critters.stream().mapToInt(Critter::level).max().orElse(0));
                default: break;
            }
            int u = k.indexOf('_');
            if (u < 0) return null;
            String kind = k.substring(0, u), arg = k.substring(u + 1);
            if (kind.equals("tier")) {
                for (Tier t : Tier.values()) if (t.name().equalsIgnoreCase(arg)) return Boolean.toString(tierOpen.test(t));
                return null;
            }
            Species s = Species.byId(arg);
            if (s == null) return null;
            return switch (kind) {
                case "page" -> Boolean.toString(w.pages.contains(s));
                case "has" -> Boolean.toString(w.critters.stream().anyMatch(c -> c.species() == s));
                case "best" -> Integer.toString(w.critters.stream().filter(c -> c.species() == s).mapToInt(Critter::level).max().orElse(0));
                default -> null;
            };
        }
    }
}
