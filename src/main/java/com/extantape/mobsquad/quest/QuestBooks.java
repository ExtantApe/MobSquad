package com.extantape.mobsquad.quest;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

/**
 * The Ranger's books, from quest-books.yml in the jar. Every book is written by Ranger Juniper and says nothing about
 * the player who gets it: no name and no place. It is a plain written book, not a map, and its generation is tattered,
 * which a game can't copy. A quest hands one over with {@code /mobsquad book <player> <id>} from the console.
 */
public final class QuestBooks {
    public static final String AUTHOR = "Ranger Juniper";
    public static final String RESOURCE = "quest-books.yml";

    /** One book: its title and pages as plain text. */
    public record Book(String title, List<String> pages) {}

    private final Map<String, Book> books;

    private QuestBooks(Map<String, Book> books) {
        this.books = books;
    }

    public static QuestBooks load(InputStream in) {
        YamlConfiguration y = YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
        Map<String, Book> out = new LinkedHashMap<>();
        for (String id : y.getKeys(false)) {
            ConfigurationSection s = y.getConfigurationSection(id);
            if (s == null) continue;
            out.put(id, new Book(s.getString("title", id), new ArrayList<>(s.getStringList("pages"))));
        }
        return new QuestBooks(out);
    }

    public static QuestBooks load(Class<?> from) {
        try (InputStream in = from.getResourceAsStream("/" + RESOURCE)) {
            if (in == null) return new QuestBooks(Map.of());
            return load(in);
        } catch (java.io.IOException e) {
            return new QuestBooks(Map.of());
        }
    }

    public Map<String, Book> all() {
        return books;
    }

    public boolean has(String id) {
        return books.containsKey(id);
    }

    /** The book for an id, or null if there is none. */
    public ItemStack make(String id) {
        Book b = books.get(id);
        if (b == null) return null;
        ItemStack it = new ItemStack(Material.WRITTEN_BOOK);
        it.editMeta(BookMeta.class, meta -> {
            meta.setTitle(b.title());
            meta.setAuthor(AUTHOR);
            meta.setGeneration(BookMeta.Generation.TATTERED);
            for (String page : b.pages()) meta.addPages(Component.text(page));
        });
        return it;
    }
}
