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
 * by accident. The check is a PlaceholderAPI value (quests.hold-chat-when) that BetonQuest answers; only an answer of exactly
 * {@code true} holds the message, so anything unreadable lets chat through as usual. A bare number is left alone, since
 * BetonQuest's own chat conversations use it to answer and it says nothing.
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

    /** Whether a typed message is held: the value is exactly true, and the message isn't just a number. */
    public static boolean holds(String message, String value) {
        return "true".equals(value) && !JUST_A_NUMBER.matcher(message).matches();
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        String when = squads.settings().holdChatWhen();
        if (when.isEmpty()) return;
        Player p = event.getPlayer();
        String message = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(event.originalMessage());
        if (JUST_A_NUMBER.matcher(message).matches()) return;
        String value;
        try {
            value = Bukkit.isPrimaryThread() ? parse.apply(p, when)
                : Bukkit.getScheduler().callSyncMethod(plugin, () -> parse.apply(p, when)).get(2, TimeUnit.SECONDS);
        } catch (Exception | LinkageError e) {
            return;
        }
        if (!holds(message, value)) return;
        event.setCancelled(true);
        p.sendMessage(Component.text("You're talking with the Ranger. Click an answer to go on.", NamedTextColor.YELLOW));
    }
}
