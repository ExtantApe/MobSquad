package com.extantape.mobsquad.quest;

import com.extantape.mobsquad.Squads;
import io.papermc.paper.event.player.AsyncChatEvent;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

/**
 * While a player is talking to the Ranger, what they type stays off public chat. BetonQuest's conversations are answered by
 * clicking, but it lets typed text through to everyone, and in a world kids play in nothing a kid types should reach others
 * by accident. The check is a PlaceholderAPI value (quests.hold-chat-when) that BetonQuest answers. It fails closed: only a
 * clear {@code false} lets a message through, so a missing expansion, an unresolved placeholder, an odd answer or a timeout
 * all hold it. Where BetonQuest isn't installed there are no conversations, so nothing is held. A bare number is
 * left alone, since BetonQuest's own chat conversations use it to answer and it says nothing.
 */
public final class ConversationChat implements Listener {
    private static final Pattern JUST_A_NUMBER = Pattern.compile("\\s*\\d{1,2}\\s*");

    private final Plugin plugin;
    private final Squads squads;
    private final BiFunction<Player, String, String> parse;

    /** {@code parse} turns a placeholder into its value for a player, as PlaceholderAPI does. */
    public ConversationChat(Plugin plugin, Squads squads, BiFunction<Player, String, String> parse) {
        this.plugin = plugin;
        this.squads = squads;
        this.parse = parse;
    }

    private boolean warned;

    /** Whether a typed message is held: the value isn't a clear false, and the message isn't just a number. */
    public static boolean holds(String message, String value) {
        return !isClearFalse(value) && !JUST_A_NUMBER.matcher(message).matches();
    }

    /** Only the exact word false (case aside, spaces aside) says the player isn't in a conversation. */
    static boolean isClearFalse(String value) {
        return value != null && "false".equalsIgnoreCase(value.trim());
    }

    private void warnOnce(String why) {
        if (warned) return;
        warned = true;
        plugin.getLogger().warning("Can't tell whether a player is talking to the Ranger (" + why + "), so typed chat is held for everyone until "
                + "the BetonQuest PlaceholderAPI expansion answers true or false. Run /papi ecloud download BetonQuest, or set quests.hold-chat-when to empty.");
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        String when = squads.settings().holdChatWhen();
        if (when.isEmpty() || Bukkit.getPluginManager().getPlugin("BetonQuest") == null) return;
        Player p = event.getPlayer();
        String message = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(event.originalMessage());
        if (JUST_A_NUMBER.matcher(message).matches()) return;
        String value;
        try {
            value = Bukkit.isPrimaryThread() ? parse.apply(p, when)
                : Bukkit.getScheduler().callSyncMethod(plugin, () -> parse.apply(p, when)).get(2, TimeUnit.SECONDS);
        } catch (Exception | LinkageError e) {
            value = null; // an answer that can't be had is not a clear false
        }
        if (!isClearFalse(value) && !"true".equalsIgnoreCase(value == null ? "" : value.trim())) warnOnce(value == null ? "the check failed" : "the placeholder came back as '" + value + "'");
        if (!holds(message, value)) return;
        event.setCancelled(true);
        p.sendMessage(Component.text("You're talking with the Ranger. Click an answer to go on.", NamedTextColor.YELLOW));
    }
}
