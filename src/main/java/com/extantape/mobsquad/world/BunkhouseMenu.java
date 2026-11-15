package com.extantape.mobsquad.world;

import com.extantape.mobsquad.Squads;
import com.extantape.mobsquad.model.Critter;
import com.extantape.mobsquad.model.Levels;
import com.extantape.mobsquad.model.Wrangler;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

/**
 * The Bunkhouse menu. Right-click a marked vault and you see your own critters, never anyone else's. Click a critter
 * to send it out or call it back. Nothing in the menu can be taken: the icons are pictures, and critters never exist
 * as items.
 */
public final class BunkhouseMenu implements Listener {
    public static final int SIZE = 54, CRITTER_SLOTS = 45, INFO_SLOT = 49, CALL_BACK_SLOT = 53, JOURNAL_SLOT = 45;
    private final Squads squads;
    private final Bunkhouses bunkhouses;

    private final Journal journal;

    public BunkhouseMenu(Squads squads, Bunkhouses bunkhouses, Journal journal) {
        this.squads = squads;
        this.bunkhouses = bunkhouses;
        this.journal = journal;
    }

    /** Ties an open menu to its viewer and the critters on each slot when it was drawn. */
    static final class Holder implements InventoryHolder {
        final UUID viewer;
        final List<UUID> slots = new ArrayList<>();
        Inventory inventory;

        Holder(UUID viewer) {
            this.viewer = viewer;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) return;
        Block b = event.getClickedBlock();
        if (b == null || b.getType() != Material.VAULT || !bunkhouses.is(b)) return;
        event.setCancelled(true); // a Bunkhouse never hands out vault loot
        Player p = event.getPlayer();
        if (!p.hasPermission("mobsquad.use")) return;
        if (!squads.settings().enabledIn(p.getWorld().getName())) {
            p.sendMessage(Component.text("MobSquad is off in this world.", NamedTextColor.YELLOW));
            return;
        }
        open(p);
    }

    /** Breaking a Bunkhouse forgets it, so a vault placed there later isn't one by accident. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (event.getBlock().getType() == Material.VAULT && bunkhouses.is(event.getBlock())) bunkhouses.unmark(event.getBlock());
    }

    public void open(Player p) {
        Wrangler w = squads.get(p);
        if (w == null) {
            p.sendMessage(Component.text("Your Bunkhouse list can't be read right now. Ask an admin to check the server log.", NamedTextColor.RED));
            return;
        }
        Holder h = new Holder(p.getUniqueId());
        h.inventory = Bukkit.createInventory(h, SIZE, Component.text("Bunkhouse"));
        draw(h, w, p);
        p.openInventory(h.inventory);
    }

    private void draw(Holder h, Wrangler w, Player p) {
        Inventory inv = h.inventory;
        inv.clear();
        h.slots.clear();
        long now = System.currentTimeMillis();
        int level = squads.wranglinLevel(p);
        int out = squads.posse().of(p.getUniqueId()).size();
        for (int i = 0; i < Math.min(CRITTER_SLOTS, w.critters.size()); i++) {
            Critter c = w.critters.get(i);
            h.slots.add(c.id());
            inv.setItem(i, icon(c, squads.posse().isOut(c.id()), now));
        }
        int max = Levels.posse(level);
        List<Component> lore = new ArrayList<>();
        lore.add(line("Wranglin' level " + level, NamedTextColor.GRAY));
        lore.add(line(out + " of " + max + " critters out", NamedTextColor.GRAY));
        lore.add(line(w.critters.size() + " in the Bunkhouse", NamedTextColor.GRAY));
        if (w.critters.size() > CRITTER_SLOTS) lore.add(line("Only the first " + CRITTER_SLOTS + " fit on this page.", NamedTextColor.YELLOW));
        inv.setItem(INFO_SLOT, item(Material.BOOK, Component.text(p.getName() + "'s Bunkhouse", NamedTextColor.GOLD), lore));
        inv.setItem(CALL_BACK_SLOT, item(Material.BELL, Component.text("Call everyone back", NamedTextColor.GOLD),
            List.of(line("Every critter out with you comes home.", NamedTextColor.GRAY))));
        inv.setItem(JOURNAL_SLOT, item(Material.WRITTEN_BOOK, Component.text("La Ménagerie", NamedTextColor.GOLD),
            List.of(line("Your journal: a page for every critter you've met.", NamedTextColor.GRAY))));
    }

    static Component line(String text, NamedTextColor color) {
        return Component.text(text, color).decoration(TextDecoration.ITALIC, false);
    }

    static ItemStack item(Material m, Component name, List<Component> lore) {
        ItemStack it = new ItemStack(m);
        it.editMeta(meta -> {
            meta.displayName(name.decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
        });
        return it;
    }

    private static ItemStack icon(Critter c, boolean out, long now) {
        Material egg = Material.matchMaterial(c.species().entity + "_SPAWN_EGG");
        List<Component> lore = new ArrayList<>();
        lore.add(line(c.species().title() + ", level " + c.level(), NamedTextColor.GRAY));
        if (c.level() < com.extantape.mobsquad.model.Growth.MAX)
            lore.add(line(String.format(java.util.Locale.US, "%,d experience to level %d", com.extantape.mobsquad.model.Growth.xpFor(c.level() + 1) - c.xp(), c.level() + 1), NamedTextColor.DARK_GRAY));
        lore.add(line(c.species().special, NamedTextColor.LIGHT_PURPLE));
        if (out) {
            lore.add(line("Out with you", NamedTextColor.GREEN));
            lore.add(line("Click to call it back.", NamedTextColor.DARK_GRAY));
        } else if (c.resting(now)) {
            long minutes = Math.max(1, (c.tuckeredUntil() - now + 59_999) / 60_000);
            lore.add(line("Resting, ready in about " + minutes + (minutes == 1 ? " minute" : " minutes"), NamedTextColor.YELLOW));
        } else {
            lore.add(line("Ready", NamedTextColor.GREEN));
            lore.add(line("Click to send it out.", NamedTextColor.DARK_GRAY));
        }
        return item(egg == null ? Material.EGG : egg, Component.text(c.name(), NamedTextColor.WHITE), lore);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder(false) instanceof Holder) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder(false) instanceof Holder h)) return;
        event.setCancelled(true); // nothing moves in or out of the menu, from either side
        if (!(event.getWhoClicked() instanceof Player p) || !p.getUniqueId().equals(h.viewer)) return;
        if (event.getClickedInventory() != event.getView().getTopInventory()) return;
        Wrangler w = squads.get(p);
        if (w == null) return;
        int slot = event.getSlot();
        if (slot == JOURNAL_SLOT) {
            org.bukkit.Bukkit.getScheduler().runTask(org.bukkit.plugin.java.JavaPlugin.getProvidingPlugin(BunkhouseMenu.class), () -> journal.show(p));
            return;
        }
        if (slot == CALL_BACK_SLOT) {
            int n = squads.posse().of(p.getUniqueId()).size();
            squads.posse().backAll(p.getUniqueId());
            p.sendMessage(Component.text(n == 0 ? "Nobody's out with you." : "Everyone's back in the Bunkhouse.", NamedTextColor.GREEN));
        } else if (slot < h.slots.size()) {
            Critter c = w.find(h.slots.get(slot));
            if (c != null) toggle(p, c);
        }
        draw(h, w, p);
    }

    private void toggle(Player p, Critter c) {
        if (squads.posse().isOut(c.id())) {
            squads.posse().back(c.id());
            p.sendMessage(Component.text(c.name() + " is back in the Bunkhouse.", NamedTextColor.GREEN));
            return;
        }
        String refusal = whyNot(p, c);
        if (refusal != null) {
            p.sendMessage(Component.text(refusal, NamedTextColor.YELLOW));
            return;
        }
        squads.posse().send(p, c);
        p.sendMessage(Component.text(c.name() + " is out with you.", NamedTextColor.GREEN));
    }

    /** Why a critter can't go out right now, or null when it can. */
    String whyNot(Player p, Critter c) {
        if (!squads.settings().enabledIn(p.getWorld().getName())) return "MobSquad is off in this world.";
        long now = System.currentTimeMillis();
        if (c.resting(now)) return c.name() + " is still resting.";
        int level = squads.wranglinLevel(p);
        if (!Levels.unlocked(c.species().tier, level))
            return c.name() + " is a " + c.species().tier.title + " critter. It can go out from Wranglin' level " + c.species().tier.level + ".";
        int max = Levels.posse(level);
        if (squads.posse().of(p.getUniqueId()).size() >= max)
            return "You can have " + max + (max == 1 ? " critter" : " critters") + " out at once at Wranglin' level " + level + ". Call one back first.";
        return null;
    }
}
