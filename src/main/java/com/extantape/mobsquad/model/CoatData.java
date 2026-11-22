package com.extantape.mobsquad.model;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * What each spirit coat looks like, read from {@code coats.yml} (bundled in the jar, overridable from the plugin
 * folder). Each of the ten coats has a plain name, a glow color and a particle; the six critters with real variants
 * (wolf, cat, pig, cow, chicken, frog) also name the variant each coat wears. A bad entry is skipped with a warning and
 * the rest still load, so a typo in the data file never stops the plugin.
 */
public final class CoatData {
    /** The critters that have real data-driven variants. */
    public static final Set<String> VARIANT_SPECIES = Set.of("wolf", "cat", "pig", "cow", "chicken", "frog");

    /** One coat: its plain name, its glow color (a team color) and the particle for its trail (a key, or null). */
    public record Coat(int index, String name, NamedTextColor color, String particle) {}

    /** Checks entries against what this server knows. In the plugin it asks Paper's registries. */
    public interface Validator {
        boolean particle(String key);

        boolean variant(String species, String key);
    }

    public static final CoatData EMPTY = new CoatData(Map.of(), Map.of());

    private final Map<Integer, Coat> coats;
    private final Map<String, Map<Integer, String>> variants;

    private CoatData(Map<Integer, Coat> coats, Map<String, Map<Integer, String>> variants) {
        this.coats = coats;
        this.variants = variants;
    }

    /** The coat's data; a plain "Coat 3" with no particle when the file has nothing usable for it. */
    public Coat coat(int index) {
        Coat c = coats.get(index);
        return c != null ? c : new Coat(index, "Coat " + index, NamedTextColor.WHITE, null);
    }

    public String name(int index) {
        return coat(index).name();
    }

    /** The variant key (such as "ashen") a species wears in a coat, or null when it has none there. */
    public String variant(String species, int coat) {
        Map<Integer, String> m = variants.get(species);
        return m == null ? null : m.get(coat);
    }

    /** Whether a species has any variant set, so it wears variants instead of a glow. */
    public boolean hasVariants(String species) {
        return variants.containsKey(species);
    }

    /**
     * Reads {@code yaml} over {@code base}: each good entry replaces the base's, each bad one is skipped with a warning.
     * A file that isn't YAML at all is skipped whole, leaving the base.
     */
    public static CoatData parse(String yaml, Validator v, Logger log, CoatData base) {
        Map<Integer, Coat> coats = new HashMap<>(base.coats);
        Map<String, Map<Integer, String>> variants = new HashMap<>();
        base.variants.forEach((k, m) -> variants.put(k, new HashMap<>(m)));
        YamlConfiguration y = new YamlConfiguration();
        try {
            y.loadFromString(yaml);
        } catch (InvalidConfigurationException | RuntimeException e) {
            log.warning("coats.yml can't be read, so the built-in coats are used: " + e.getMessage());
            return base;
        }
        ConfigurationSection cs = y.getConfigurationSection("coats");
        if (cs != null) for (String k : cs.getKeys(false)) {
            Integer i = index(k);
            if (i == null) { log.warning("coats.yml: coat \"" + k + "\" isn't a number from 1 to " + Coats.COUNT + ". Skipped."); continue; }
            ConfigurationSection c = cs.getConfigurationSection(k);
            String name = c == null ? null : c.getString("name");
            if (name == null || name.isBlank() || name.length() > 24) { log.warning("coats.yml: coat " + i + " needs a name of 1 to 24 characters. Skipped."); continue; }
            NamedTextColor color = NamedTextColor.NAMES.value(String.valueOf(c.getString("color", "")).toLowerCase(Locale.ROOT));
            if (color == null) { log.warning("coats.yml: coat " + i + " has a color that isn't one of the 16 team colors. Skipped."); continue; }
            String particle = c.getString("particle");
            if (particle != null && !v.particle(particle)) {
                log.warning("coats.yml: coat " + i + " has a particle \"" + particle + "\" this server can't use here. It gets no trail.");
                particle = null;
            }
            coats.put(i, new Coat(i, name.strip(), color, particle));
        }
        ConfigurationSection vs = y.getConfigurationSection("variants");
        if (vs != null) for (String species : vs.getKeys(false)) {
            if (!VARIANT_SPECIES.contains(species)) { log.warning("coats.yml: \"" + species + "\" has no variants to set. Skipped."); continue; }
            ConfigurationSection m = vs.getConfigurationSection(species);
            if (m == null) { log.warning("coats.yml: variants for " + species + " should list coats by number. Skipped."); continue; }
            for (String k : m.getKeys(false)) {
                Integer i = index(k);
                String key = m.getString(k);
                if (i == null) { log.warning("coats.yml: " + species + " coat \"" + k + "\" isn't a number from 1 to " + Coats.COUNT + ". Skipped."); continue; }
                if (key == null || !v.variant(species, key)) { log.warning("coats.yml: " + species + " coat " + i + " names a variant \"" + key + "\" this server doesn't have. Skipped."); continue; }
                variants.computeIfAbsent(species, s -> new HashMap<>()).put(i, key);
            }
        }
        Map<String, Map<Integer, String>> frozen = new HashMap<>();
        variants.forEach((k, m) -> frozen.put(k, Map.copyOf(m)));
        return new CoatData(Map.copyOf(coats), Map.copyOf(frozen));
    }

    private static Integer index(String k) {
        try {
            int i = Integer.parseInt(k.strip());
            return i >= 1 && i <= Coats.COUNT ? i : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
