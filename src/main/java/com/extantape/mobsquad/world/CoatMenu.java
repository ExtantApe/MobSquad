package com.extantape.mobsquad.world;

import com.extantape.mobsquad.Squads;
import com.extantape.mobsquad.model.CoatData;
import com.extantape.mobsquad.model.Coats;
import com.extantape.mobsquad.model.Critter;
import com.extantape.mobsquad.model.Wrangler;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Where an owner picks the spirit coat a critter wears, from the Bunkhouse (right-click the critter). Ten coats in two
 * rows of five: earned ones are named, locked ones say the level that earns them, and the one it wears is marked in
 * words as well as with a glow. Nothing can be taken from it, and only a coat the critter has earned can be picked.
 */
public final class CoatMenu implements Listener {
    static final int SIZE = 36, INFO_SLOT = 4, NEWEST_SLOT = 25, BACK_SLOT = 31;
    private final Squads squads;

    public CoatMenu(Squads squads) {
        this.squads = squads;
    }

    static final class Holder implements InventoryHolder {
        final UUID viewer, critter;
        final Consumer<Player> back;
        Inventory inventory;

        Holder(UUID viewer, UUID critter, Consumer<Player> back) {
            this.viewer = viewer;
            this.critter = critter;
            this.back = back;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    /** The slot for a coat: 1 to 5 across the second row, 6 to 10 across the third. */
    static int slotOf(int coat) {
        return coat <= 5 ? 10 + coat - 1 : 19 + coat - 6;
    }

    /** The coat in a slot, or 0 if it isn't one. */
    static int coatAt(int slot) {
        for (int i = 1; i <= Coats.COUNT; i++) if (slotOf(i) == slot) return i;
        return 0;
    }

    public void open(Player p, UUID critter, Consumer<Player> back) {
        Wrangler w = squads.get(p);
        Critter c = w == null ? null : w.find(critter);
        if (c == null) return;
        Holder h = new Holder(p.getUniqueId(), critter, back);
        h.inventory = Bukkit.createInventory(h, SIZE, Component.text("Spirit coats: " + c.name()));
        draw(h, c);
        p.openInventory(h.inventory);
    }

    private void draw(Holder h, Critter c) {
        Inventory inv = h.inventory;
        inv.clear();
        CoatData data = squads.coatData();
        int earned = Coats.earned(c.level()), worn = Coats.worn(c);
        List<Component> info = new ArrayList<>();
        info.add(BunkhouseMenu.line("Level " + c.level() + ", " + earned + " of " + Coats.COUNT + " coats earned", NamedTextColor.GRAY));
        info.add(BunkhouseMenu.line("A new coat every " + Coats.EVERY + " levels. Coats are never lost.", NamedTextColor.GRAY));
        info.add(BunkhouseMenu.line(squads.settings().coatWorlds().isEmpty()
            ? "Coats show in magic worlds only, and this server has none yet."
            : "Coats show in magic worlds only.", NamedTextColor.GRAY));
        inv.setItem(INFO_SLOT, BunkhouseMenu.item(Material.BOOK, Component.text(c.name() + "'s spirit coats", NamedTextColor.GOLD), info));
        for (int i = 1; i <= Coats.COUNT; i++) {
            CoatData.Coat look = data.coat(i);
            ItemStack it;
            if (i > earned) {
                it = BunkhouseMenu.item(Material.GRAY_STAINED_GLASS_PANE, Component.text("Coat " + i + ": Level " + Coats.levelFor(i), NamedTextColor.GRAY),
                    List.of(BunkhouseMenu.line("Locked. " + c.name() + " earns it at level " + Coats.levelFor(i) + ".", NamedTextColor.GRAY)));
            } else {
                List<Component> lore = new ArrayList<>();
                lore.add(BunkhouseMenu.line(i == worn ? "Wearing now" : "Click to wear it.", i == worn ? NamedTextColor.GREEN : NamedTextColor.GRAY));
                it = BunkhouseMenu.item(dye(look.color()), Component.text("Coat " + i + ": " + look.name(), Coats.readable(look.color())), lore);
                if (i == worn) it.editMeta(m -> m.setEnchantmentGlintOverride(true));
            }
            inv.setItem(slotOf(i), it);
        }
        if (earned > 0) {
            boolean auto = c.coat() == 0 || c.coat() > earned;
            ItemStack newest = BunkhouseMenu.item(Material.NETHER_STAR, Component.text("Newest coat", NamedTextColor.WHITE), List.of(
                BunkhouseMenu.line("Wears each new coat as it's earned.", NamedTextColor.GRAY),
                BunkhouseMenu.line(auto ? "Chosen now" : "Click to choose.", auto ? NamedTextColor.GREEN : NamedTextColor.GRAY)));
            if (auto) newest.editMeta(m -> m.setEnchantmentGlintOverride(true));
            inv.setItem(NEWEST_SLOT, newest);
        }
        inv.setItem(BACK_SLOT, BunkhouseMenu.item(Material.ARROW, Component.text("Back to the Bunkhouse", NamedTextColor.GOLD), List.of()));
    }

    /** A dye that matches a coat's glow color, for the menu icon. The name beside it always says which coat it is. */
    static Material dye(NamedTextColor c) {
        if (c == NamedTextColor.RED || c == NamedTextColor.DARK_RED) return Material.RED_DYE;
        if (c == NamedTextColor.GOLD) return Material.ORANGE_DYE;
        if (c == NamedTextColor.YELLOW) return Material.YELLOW_DYE;
        if (c == NamedTextColor.GREEN) return Material.LIME_DYE;
        if (c == NamedTextColor.DARK_GREEN) return Material.GREEN_DYE;
        if (c == NamedTextColor.AQUA) return Material.LIGHT_BLUE_DYE;
        if (c == NamedTextColor.DARK_AQUA) return Material.CYAN_DYE;
        if (c == NamedTextColor.BLUE || c == NamedTextColor.DARK_BLUE) return Material.BLUE_DYE;
        if (c == NamedTextColor.DARK_PURPLE) return Material.PURPLE_DYE;
        if (c == NamedTextColor.LIGHT_PURPLE) return Material.PINK_DYE;
        if (c == NamedTextColor.GRAY) return Material.LIGHT_GRAY_DYE;
        if (c == NamedTextColor.DARK_GRAY) return Material.GRAY_DYE;
        if (c == NamedTextColor.BLACK) return Material.BLACK_DYE;
        return Material.WHITE_DYE;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder(false) instanceof Holder) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder(false) instanceof Holder h)) return;
        event.setCancelled(true); // nothing moves in or out of this menu, from either side
        if (!(event.getWhoClicked() instanceof Player p) || !p.getUniqueId().equals(h.viewer)) return;
        if (event.getClickedInventory() != event.getView().getTopInventory()) return;
        Wrangler w = squads.get(p);
        Critter c = w == null ? null : w.find(h.critter);
        if (c == null) return;
        int slot = event.getSlot();
        if (slot == BACK_SLOT) {
            Bukkit.getScheduler().runTask(JavaPlugin.getProvidingPlugin(CoatMenu.class), () -> h.back.accept(p));
            return;
        }
        int coat = slot == NEWEST_SLOT ? 0 : coatAt(slot);
        if (slot != NEWEST_SLOT && coat == 0) return;
        int earned = Coats.earned(c.level());
        if (coat > earned || (slot == NEWEST_SLOT && earned == 0)) {
            p.sendMessage(Component.text(squads.coatData().name(coat) + " comes at level " + Coats.levelFor(coat) + ".", NamedTextColor.YELLOW));
            return;
        }
        if (!squads.pickCoat(p, c.id(), coat)) return;
        Critter now = w.find(c.id());
        int shown = Coats.worn(now);
        String name = squads.coatData().name(shown);
        p.sendMessage(Component.text(c.name() + " wears " + name + (coat == 0 ? " and will wear each new coat." : "."), NamedTextColor.GREEN)
            .append(Component.text(squads.settings().coatWorlds().isEmpty() ? " Coats show in magic worlds only, and this server has none yet." : "", NamedTextColor.GRAY)));
        draw(h, now);
    }
}
