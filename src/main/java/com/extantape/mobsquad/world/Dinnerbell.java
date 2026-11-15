package com.extantape.mobsquad.world;

import com.extantape.mobsquad.Squads;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/** The dinnerbell: ring it and every critter out with you heads home to the Bunkhouse, wherever you are. */
public final class Dinnerbell implements Listener {
    private final Squads squads;
    private final NamespacedKey key;

    public Dinnerbell(Plugin plugin, Squads squads) {
        this.squads = squads;
        this.key = new NamespacedKey(plugin, "dinnerbell");
    }

    public ItemStack make() {
        ItemStack it = new ItemStack(Material.WARPED_FUNGUS_ON_A_STICK);
        it.editMeta(meta -> {
            meta.displayName(BunkhouseMenu.line("Dinnerbell", NamedTextColor.GOLD));
            meta.lore(List.of(BunkhouseMenu.line("Right-click to call your critters home.", NamedTextColor.GRAY)));
            meta.setMaxStackSize(1);
            meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
        });
        return it;
    }

    public boolean is(ItemStack it) {
        return it != null && it.getType() == Material.WARPED_FUNGUS_ON_A_STICK
            && it.getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onRing(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!is(event.getItem())) return;
        event.setCancelled(true);
        Player p = event.getPlayer();
        int n = squads.posse().recall(p.getUniqueId()); // a sidekick stays
        p.playSound(p.getLocation(), Sound.BLOCK_BELL_USE, 0.8f, 1.2f);
        p.sendMessage(Component.text(n == 0 ? "Nobody's out with you." : n == 1 ? "Your critter is headed home." : "All " + n + " critters are headed home.", NamedTextColor.GREEN));
    }
}
