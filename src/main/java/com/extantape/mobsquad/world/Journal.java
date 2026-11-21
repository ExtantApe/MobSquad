package com.extantape.mobsquad.world;

import com.extantape.mobsquad.Squads;
import com.extantape.mobsquad.model.Critter;
import com.extantape.mobsquad.model.Critterbook;
import com.extantape.mobsquad.model.Species;
import com.extantape.mobsquad.model.Wrangler;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

/**
 * La Ménagerie, the journal (brief, "La Ménagerie (the journal)"). Its own menu lists the launch critters in Critterbook
 * order; a critter's page opens once you've met one (seen it up close) or have one, and clicking it opens the page as a
 * book. Locked pages show only their number. Each player's open pages are saved in their own file.
 */
public final class Journal implements Runnable, Listener {
    public static final long PERIOD = 40;
    /** How close a mob has to be, with a clear line, to count as met. */
    static final double MEET = 10;
    static final int SIZE = 27, INFO_SLOT = 22;
    static final Component TITLE = Component.text("La Ménagerie");

    /** Each page's voice: half ridiculous (brief). The creeper's line is the owner's own. */
    static final Map<Species, String> VOICE = new EnumMap<>(Species.class);
    static {
        VOICE.put(Species.CHICKEN, "Thinks it can fly. It can't. But it'll teach you to fall like a feather, which is nearly as good.");
        VOICE.put(Species.AXOLOTL, "Smiles all the time, even when it's pretending to be dead. Especially then.");
        VOICE.put(Species.BEE, "Busy, and a little bit judgy. Stand still and let it decide you're a flower.");
        VOICE.put(Species.FOX, "Finders keepers? Not this one. It brings your stuff right back, smug as anything.");
        VOICE.put(Species.ALLAY, "Hums when it's happy. Hums louder when it finds diamonds. Wouldn't hurt a fly, or a zombie.");
        VOICE.put(Species.CREEPER, "Momma said never look a creeper in the eye. But I like to laugh in the face of danger!");
        VOICE.put(Species.ENDERMAN, "Tall and deeply offended by eye contact. Look at its shoes instead. Compliment the shoes.");
        VOICE.put(Species.SKELETON, "All bones and no manners. Hand it an arrow, though, and it'll watch your back all day.");
        VOICE.put(Species.IRON_GOLEM, "Made of iron with a big soft heart. Gives the best hugs. Also throws zombies.");
    }

    private final Plugin plugin;
    private final Squads squads;

    public Journal(Plugin plugin, Squads squads) {
        this.plugin = plugin;
        this.squads = squads;
    }

    static final class Holder implements InventoryHolder {
        final UUID viewer;
        final List<Species> slots = new ArrayList<>();
        Inventory inventory;

        Holder(UUID viewer) {
            this.viewer = viewer;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    // ----- Meeting critters.

    /** Every two seconds, opens a page for each launch critter a player is close to and can see. */
    @Override
    public void run() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.hasPermission("mobsquad.use") || !squads.settings().enabledIn(p.getWorld().getName())) continue;
            Wrangler w = squads.get(p);
            if (w == null || w.pages.size() == Species.values().length) continue;
            for (Entity e : p.getNearbyEntities(MEET, MEET / 2, MEET)) {
                if (!(e instanceof LivingEntity l) || l.isDead()) continue;
                Species s = Species.byId(e.getType().getKey().getKey());
                if (s == null || w.pages.contains(s) || !p.hasLineOfSight(e)) continue;
                open(p, s, "met");
            }
        }
    }

    /** Opens a critter's page for a player, and tells them, with a link to the journal. */
    public void open(Player p, Species s, String how) {
        Wrangler w = squads.get(p);
        if (w == null || !w.pages.add(s)) return;
        squads.save(w);
        Component link = Component.text("[Open La Ménagerie]", NamedTextColor.AQUA)
            .clickEvent(ClickEvent.runCommand("/mobsquad journal"))
            .hoverEvent(HoverEvent.showText(Component.text("/mobsquad journal")));
        p.sendMessage(Component.text("New page in La Ménagerie: No. " + Critterbook.number(s) + ", the " + s.title().toLowerCase(Locale.ROOT) + "! ", NamedTextColor.GOLD).append(link));
    }

    // ----- The menu.

    public void show(Player p) {
        Wrangler w = squads.get(p);
        if (w == null) {
            p.sendMessage(Component.text("Your journal can't be read right now. Ask an admin to check the server log.", NamedTextColor.RED));
            return;
        }
        Holder h = new Holder(p.getUniqueId());
        h.inventory = Bukkit.createInventory(h, SIZE, TITLE);
        List<Species> order = Critterbook.inOrder();
        for (int i = 0; i < order.size(); i++) {
            Species s = order.get(i);
            h.slots.add(s);
            h.inventory.setItem(i, icon(s, w));
        }
        h.inventory.setItem(INFO_SLOT, BunkhouseMenu.item(Material.WRITTEN_BOOK, Component.text("La Ménagerie", NamedTextColor.GOLD), List.of(
            BunkhouseMenu.line("You've met " + w.pages.size() + " of " + order.size() + " critters.", NamedTextColor.GRAY),
            BunkhouseMenu.line("Get close to a critter, or coax one, to open its page.", NamedTextColor.GRAY),
            BunkhouseMenu.line("Numbers follow the order each mob first came to Minecraft.", NamedTextColor.GRAY))));
        p.openInventory(h.inventory);
    }

    private static ItemStack icon(Species s, Wrangler w) {
        int n = Critterbook.number(s);
        if (!w.pages.contains(s))
            return BunkhouseMenu.item(Material.GRAY_DYE, Component.text("No. " + n + "  ???", NamedTextColor.GRAY),
                List.of(BunkhouseMenu.line("You haven't met this one yet.", NamedTextColor.GRAY)));
        Material egg = Material.matchMaterial(s.entity + "_SPAWN_EGG");
        long have = w.critters.stream().filter(c -> c.species() == s).count();
        return BunkhouseMenu.item(egg == null ? Material.EGG : egg, Component.text("No. " + n + "  " + s.title(), NamedTextColor.WHITE), List.of(
            BunkhouseMenu.line(s.tier.title + " critter", NamedTextColor.GRAY),
            BunkhouseMenu.line(have == 0 ? "None in your Bunkhouse yet" : have + " in your Bunkhouse", NamedTextColor.GRAY),
            BunkhouseMenu.line("Click to read its page.", NamedTextColor.GRAY)));
    }

    /** A critter's page, as a book: who it is, how to coax it, and its special. */
    static Book page(Species s, Wrangler w) {
        int n = Critterbook.number(s);
        Component first = Component.text()
            .append(Component.text("No. " + n + "\n", NamedTextColor.DARK_GRAY))
            .append(Component.text(s.title() + "\n", NamedTextColor.BLACK, TextDecoration.BOLD))
            .append(Component.text(s.tier.title + " critter\n\n", NamedTextColor.DARK_GRAY))
            .append(Component.text(VOICE.get(s), NamedTextColor.BLACK, TextDecoration.ITALIC))
            .build();
        Component coax = Component.text()
            .append(Component.text("Coaxing\n\n", NamedTextColor.BLACK, TextDecoration.BOLD))
            .append(Component.text(s.hint + "\n\n", NamedTextColor.BLACK))
            .append(Component.text("From Wranglin' level " + s.tier.level + ".", NamedTextColor.DARK_GRAY))
            .build();
        int best = w.critters.stream().filter(c -> c.species() == s).mapToInt(Critter::level).max().orElse(0);
        Component special = Component.text()
            .append(Component.text("Special\n\n", NamedTextColor.BLACK, TextDecoration.BOLD))
            .append(Component.text(s.special + "\n\n", NamedTextColor.BLACK))
            .append(Component.text("It grows at levels 25, 50, 75 and 100." + (best > 0 ? " Your best is level " + best + "." : "") + "\n\n", NamedTextColor.DARK_GRAY))
            .append(Component.text("Spirit coats come later.", NamedTextColor.DARK_GRAY))
            .build();
        return Book.book(Component.text("No. " + n + " " + s.title()), Component.text("La Ménagerie"), first, coax, special);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder(false) instanceof Holder) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder(false) instanceof Holder h)) return;
        event.setCancelled(true); // nothing moves in or out of the journal
        if (!(event.getWhoClicked() instanceof Player p) || !p.getUniqueId().equals(h.viewer)) return;
        if (event.getClickedInventory() != event.getView().getTopInventory() || event.getSlot() >= h.slots.size()) return;
        Species s = h.slots.get(event.getSlot());
        Wrangler w = squads.get(p);
        if (w == null || !w.pages.contains(s)) return;
        Bukkit.getScheduler().runTask(plugin, () -> {
            p.closeInventory();
            p.openBook(page(s, w));
        });
    }
}
