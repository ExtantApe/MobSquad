package com.extantape.mobsquad;

import static org.junit.jupiter.api.Assertions.*;

import com.extantape.mobsquad.model.CoatData;
import com.extantape.mobsquad.model.Coats;
import com.extantape.mobsquad.model.Critter;
import com.extantape.mobsquad.model.Growth;
import com.extantape.mobsquad.model.Species;
import com.extantape.mobsquad.model.Wrangler;
import com.extantape.mobsquad.store.Store;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CoatTest {
    /** Says yes to everything except what a test lists as missing. */
    static CoatData.Validator allow(Set<String> missing) {
        return new CoatData.Validator() {
            @Override public boolean particle(String key) { return !missing.contains("particle:" + key); }
            @Override public boolean variant(String species, String key) { return !missing.contains(species + ":" + key); }
        };
    }

    static final class Warnings extends Handler {
        final List<String> messages = new ArrayList<>();
        @Override public void publish(LogRecord r) { messages.add(r.getMessage()); }
        @Override public void flush() {}
        @Override public void close() {}
    }

    static Logger logger(Warnings w) {
        Logger l = Logger.getAnonymousLogger();
        l.setUseParentHandlers(false);
        l.addHandler(w);
        return l;
    }

    static String bundled() throws Exception {
        try (InputStream in = CoatTest.class.getResourceAsStream("/coats.yml")) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    static Critter at(int level, int coat) {
        long xp = Growth.xpFor(level);
        return new Critter(UUID.randomUUID(), Species.CHICKEN, "Clucky", level, xp, 0, 0, coat);
    }

    // ---- earning ----

    @Test
    void aCoatIsEarnedEveryTenLevels() {
        assertEquals(0, Coats.earned(1));
        assertEquals(0, Coats.earned(9));
        assertEquals(1, Coats.earned(10));
        assertEquals(1, Coats.earned(19));
        assertEquals(2, Coats.earned(20));
        assertEquals(9, Coats.earned(99));
        assertEquals(10, Coats.earned(100));
        assertEquals(10, Coats.earned(150), "never more than ten");
        assertEquals(30, Coats.levelFor(3));
    }

    @Test
    void theNewestEarnedCoatIsTheDefault() {
        assertEquals(0, Coats.worn(at(9, 0)));
        assertEquals(1, Coats.worn(at(10, 0)));
        assertEquals(4, Coats.worn(at(45, 0)));
        assertEquals(10, Coats.worn(at(100, 0)));
    }

    @Test
    void aChosenCoatIsWornOnlyIfEarned() {
        assertEquals(2, Coats.worn(at(45, 2)));
        assertEquals(4, Coats.worn(at(45, 4)));
        assertEquals(4, Coats.worn(at(45, 7)), "not earned yet: the newest earned");
        assertEquals(0, Coats.worn(at(5, 3)), "nothing earned, nothing worn");
    }

    @Test
    void aCoatChoiceSurvivesLevelingAndRestingAndStaysInRange() {
        Critter c = at(45, 2);
        assertEquals(2, c.withXp(Growth.xpFor(60)).coat());
        assertEquals(2, c.tuckered(5).coat());
        assertEquals(2, c.minuteOut(2).coat());
        assertEquals(Coats.COUNT, c.withCoat(99).coat());
        assertEquals(0, c.withCoat(-3).coat());
        assertEquals(0, Critter.fresh(Species.BEE).coat());
    }

    // ---- the choice in the file, and migration ----

    @Test
    void theChoiceIsSavedWithTheCritter(@TempDir Path dir) throws Exception {
        Store store = new Store(dir);
        UUID p = UUID.randomUUID();
        Wrangler w = new Wrangler(p);
        w.critters.add(at(45, 3));
        w.critters.add(at(12, 0));
        store.save(w);
        Store.Loaded back = store.load(p);
        assertEquals(0, back.skipped());
        assertEquals(w.critters, back.wrangler().critters);
        assertEquals(3, back.wrangler().critters.get(0).coat());
    }

    @Test
    void savesFromBeforeCoatsStillLoad(@TempDir Path dir) throws Exception {
        UUID p = UUID.randomUUID(), id = UUID.randomUUID();
        // the 0.7.x format: seven fields after the word critter's id, no coat
        Files.writeString(dir.resolve(p + ".txt"), "wranglin 150\ncritter " + id + " chicken 45 1234 0 Clucky 77\npage chicken\n");
        Store store = new Store(dir);
        Store.Loaded l = store.load(p);
        assertEquals(0, l.skipped());
        Critter c = l.wrangler().critters.get(0);
        assertEquals(0, c.coat(), "no coat picked, so the newest earned");
        assertEquals(45, c.level());
        assertEquals(77, c.minutes());
        assertEquals(4, Coats.worn(c));
        // and the older 0.4.x format, with no minutes either
        Files.writeString(dir.resolve(p + ".txt"), "critter " + id + " chicken 45 1234 0 Clucky\n");
        assertEquals(0, store.load(p).wrangler().critters.get(0).coat());
        // saving writes the new field, and it reads back the same
        store.save(l.wrangler());
        assertTrue(Files.readString(dir.resolve(p + ".txt")).contains("Clucky 77 0"));
        assertEquals(c, store.load(p).wrangler().critters.get(0));
    }

    @Test
    void aBadCoatFieldNeverCostsTheCritter(@TempDir Path dir) throws Exception {
        UUID p = UUID.randomUUID(), id = UUID.randomUUID();
        Store store = new Store(dir);
        for (String bad : new String[] {"banana", "-1", "11", "99999999999"}) {
            Files.writeString(dir.resolve(p + ".txt"), "critter " + id + " chicken 45 1234 0 Clucky 77 " + bad + "\n");
            Store.Loaded l = store.load(p);
            assertEquals(0, l.skipped(), bad);
            assertEquals(1, l.wrangler().critters.size(), bad);
            assertEquals(0, l.wrangler().critters.get(0).coat(), bad);
        }
    }

    // ---- magic worlds ----

    @Test
    void coatsShowNowhereUntilAnAdminListsAWorld() {
        Logger log = Logger.getAnonymousLogger();
        Settings none = Settings.read(new YamlConfiguration(), log);
        assertTrue(none.coatWorlds().isEmpty());
        assertFalse(none.coatsIn("world"));
        assertFalse(none.coatsIn("world_nether"));

        YamlConfiguration y = new YamlConfiguration();
        y.set("spirit-coats.worlds", List.of("storybook", "magic_meadow"));
        Settings some = Settings.read(y, log);
        assertTrue(some.coatsIn("storybook"));
        assertTrue(some.coatsIn("magic_meadow"));
        assertFalse(some.coatsIn("world"), "only the listed worlds");
        assertFalse(some.coatsIn("Storybook"), "world names match exactly");
    }

    @Test
    void theBundledConfigListsNoWorlds() throws Exception {
        YamlConfiguration y = new YamlConfiguration();
        try (InputStream in = CoatTest.class.getResourceAsStream("/config.yml")) {
            y.loadFromString(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
        assertTrue(y.contains("spirit-coats.worlds"));
        assertTrue(Settings.read(y, Logger.getAnonymousLogger()).coatWorlds().isEmpty());
    }

    // ---- the data file ----

    @Test
    void theBundledCoatsLoadCleanlyAndReadWell() throws Exception {
        Warnings warn = new Warnings();
        CoatData d = CoatData.parse(bundled(), allow(Set.of()), logger(warn), CoatData.EMPTY);
        assertEquals(List.of(), warn.messages);
        Set<String> names = new HashSet<>();
        for (int i = 1; i <= Coats.COUNT; i++) {
            CoatData.Coat c = d.coat(i);
            assertFalse(c.name().startsWith("Coat "), "coat " + i + " has a real name");
            assertTrue(names.add(c.name()), "names are all different: " + c.name());
            assertNotNull(c.particle(), "coat " + i);
            NamedTextColor text = Coats.readable(c.color());
            assertFalse(Set.of(NamedTextColor.BLACK, NamedTextColor.DARK_GRAY, NamedTextColor.DARK_BLUE, NamedTextColor.DARK_RED,
                NamedTextColor.DARK_GREEN, NamedTextColor.DARK_AQUA, NamedTextColor.DARK_PURPLE, NamedTextColor.BLUE).contains(text),
                "coat " + i + " is readable on a dark menu");
        }
        assertEquals("Ember", d.name(1));
        assertEquals("Starlight", d.name(10));
        for (String species : CoatData.VARIANT_SPECIES) {
            assertTrue(d.hasVariants(species), species);
            for (int i = 1; i <= Coats.COUNT; i++) assertNotNull(d.variant(species, i), species + " coat " + i);
        }
        assertFalse(d.hasVariants("creeper"), "a creeper glows instead");
        assertTrue(d.glows("creeper"), "a critter without variants glows");
        for (String species : Set.of("pig", "cow", "chicken", "frog")) assertTrue(d.glows(species), species + " glows too (owner, 2026-11-22)");
        for (String species : Set.of("wolf", "cat")) assertFalse(d.glows(species), species + " wears its variant only");
        assertNull(d.variant("creeper", 1));
    }

    @Test
    void badEntriesAreSkippedWithAWarningAndTheRestLoad() throws Exception {
        CoatData base = CoatData.parse(bundled(), allow(Set.of()), logger(new Warnings()), CoatData.EMPTY);
        Warnings warn = new Warnings();
        String yaml = """
            coats:
              1: { name: Cinder, color: red, particle: flame }
              2: { color: gold }
              3: { name: Sunbeam, color: plaid }
              4: { name: Meadow, color: green, particle: nope }
              eleven: { name: Extra, color: red }
              12: { name: Extra, color: red }
              5: just text
            variants:
              wolf: { 1: ashen, 2: not_a_wolf, three: pale }
              dragon: { 1: red }
              cat: oops
            """;
        CoatData d = CoatData.parse(yaml, allow(Set.of("particle:nope", "wolf:not_a_wolf")), logger(warn), base);
        assertEquals("Cinder", d.name(1), "a good entry replaces the built-in one");
        assertEquals("Marigold", d.name(2), "a bad one keeps the built-in");
        assertEquals("Sunbeam", d.name(3));
        assertEquals(NamedTextColor.YELLOW, d.coat(3).color());
        assertEquals("Meadow", d.name(4));
        assertNull(d.coat(4).particle(), "a particle the server can't use gives no trail");
        assertEquals("Pine", d.name(5));
        assertEquals("ashen", d.variant("wolf", 1));
        assertEquals("woods", d.variant("wolf", 3), "a bad variant keeps the built-in one");
        assertEquals("ashen", d.variant("wolf", 6));
        assertEquals("tabby", d.variant("cat", 1));
        assertNull(d.variant("dragon", 1));
        assertTrue(warn.messages.size() >= 9, warn.messages.toString());
        assertTrue(warn.messages.stream().allMatch(m -> m.startsWith("coats.yml")), "each warning says where");
    }

    @Test
    void aFileThatIsNotYamlLeavesTheBuiltInCoats() throws Exception {
        CoatData base = CoatData.parse(bundled(), allow(Set.of()), logger(new Warnings()), CoatData.EMPTY);
        Warnings warn = new Warnings();
        assertSame(base, CoatData.parse("coats: [unclosed\n  : : :", allow(Set.of()), logger(warn), base));
        assertEquals(1, warn.messages.size());
    }

    @Test
    void anEmptyDataSetStillNamesEveryCoat() {
        assertEquals("Coat 3", CoatData.EMPTY.name(3));
        assertNull(CoatData.EMPTY.coat(3).particle());
        assertNull(CoatData.EMPTY.variant("wolf", 3));
    }

    @Test
    void glowTooCanBeChangedAndBadEntriesAreSkipped() throws Exception {
        CoatData base = CoatData.parse(bundled(), allow(Set.of()), logger(new Warnings()), CoatData.EMPTY);
        Warnings warn = new Warnings();
        CoatData d = CoatData.parse("glow-too: [wolf, Cow, creeper, dragon]\n", allow(Set.of()), logger(warn), base);
        assertTrue(d.glows("wolf"));
        assertTrue(d.glows("cow"), "names are read without regard to case");
        assertFalse(d.glows("pig"), "a list replaces the built-in one");
        assertTrue(d.glows("creeper"), "a critter without variants glows anyway");
        assertEquals(2, warn.messages.size(), "creeper and dragon are skipped with a warning: " + warn.messages);

        Warnings warn2 = new Warnings();
        CoatData kept = CoatData.parse("glow-too: pig\n", allow(Set.of()), logger(warn2), base);
        assertTrue(kept.glows("frog"), "a value that isn't a list keeps the built-in list");
        assertEquals(1, warn2.messages.size());

        CoatData none = CoatData.parse("glow-too: []\n", allow(Set.of()), logger(new Warnings()), base);
        assertFalse(none.glows("pig"), "an empty list turns it off");
        assertTrue(CoatData.parse("coats: {}\n", allow(Set.of()), logger(new Warnings()), base).glows("pig"),
            "a file without glow-too keeps the built-in list");
    }
}
