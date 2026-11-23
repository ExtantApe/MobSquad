package com.extantape.mobsquad.world;

import com.extantape.mobsquad.Squads;
import com.extantape.mobsquad.model.Critter;
import com.extantape.mobsquad.model.Levels;
import com.extantape.mobsquad.model.Wrangler;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/**
 * Vittles: right-click a bundle of them and the critter that was tuckered out most recently perks right up, ready to
 * go, and comes out beside you if there's room in your posse. Crafted from bread, a carrot and sweet berries (the
 * lead's proposal, 2026-11-15). They aren't food for people.
 */
public final class Vittles implements Listener {
    private final Squads squads;
    private final NamespacedKey key, recipe;

    public Vittles(Plugin plugin, Squads squads) {
        this.squads = squads;
        this.key = new NamespacedKey(plugin, "vittles");
        this.recipe = new NamespacedKey(plugin, "vittles");
    }

    public NamespacedKey key() {
        return recipe;
    }

    public ItemStack make(int amount) {
        ItemStack it = new ItemStack(Material.PUMPKIN_PIE, amount);
        it.editMeta(meta -> {
            meta.displayName(BunkhouseMenu.line("Vittles", NamedTextColor.GOLD));
            meta.lore(List.of(BunkhouseMenu.line("Right-click to perk up a tuckered-out critter.", NamedTextColor.GRAY),
                BunkhouseMenu.line("Critter food. Not for people.", NamedTextColor.GRAY)));
            meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
        });
        return it;
    }

    public boolean is(ItemStack it) {
        return it != null && it.getType() == Material.PUMPKIN_PIE && it.getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }

    public void register() {
        if (Bukkit.getRecipe(recipe) != null) Bukkit.removeRecipe(recipe);
        ShapelessRecipe r = new ShapelessRecipe(recipe, make(2));
        r.addIngredient(Material.BREAD);
        r.addIngredient(Material.CARROT);
        r.addIngredient(Material.SWEET_BERRIES);
        Bukkit.addRecipe(r);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack hand = event.getItem();
        if (!is(hand)) return;
        event.setCancelled(true); // never eaten, never placed
        Player p = event.getPlayer();
        Wrangler w = squads.get(p);
        if (w == null) return;
        long now = System.currentTimeMillis();
        Critter tired = null;
        for (Critter c : w.critters) if (c.resting(now) && (tired == null || c.tuckeredUntil() > tired.tuckeredUntil())) tired = c;
        if (tired == null) {
            p.sendMessage(Component.text("None of your critters needs vittles right now.", NamedTextColor.YELLOW));
            return;
        }
        Critter ready = tired.tuckered(0);
        w.replace(ready);
        squads.save(w);
        if (p.getGameMode() != GameMode.CREATIVE) hand.subtract();
        p.playSound(p.getLocation(), Sound.ENTITY_GENERIC_EAT, 0.8f, 1.3f);
        int max = Levels.posse(squads.wranglinLevel(p));
        boolean room = squads.settings().enabledIn(p.getWorld().getName()) && squads.posse().counted(p.getUniqueId()) < max
            && squads.tierOpen(p, ready.species().tier);
        if (room) {
            Posse.Out o = squads.posse().send(p, ready);
            o.mob().getWorld().spawnParticle(Particle.HAPPY_VILLAGER, o.mob().getLocation().add(0, 0.6, 0), 8, 0.4, 0.4, 0.4);
            p.sendMessage(Component.text(ready.name() + " perked right up and came running.", NamedTextColor.GREEN));
        } else {
            p.sendMessage(Component.text(ready.name() + " perked right up. It's ready in your Bunkhouse.", NamedTextColor.GREEN));
        }
    }

    /** In case anything lets a pie through to eating. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEat(PlayerItemConsumeEvent event) {
        if (is(event.getItem())) event.setCancelled(true);
    }
}
