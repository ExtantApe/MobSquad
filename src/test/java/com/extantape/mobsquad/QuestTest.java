package com.extantape.mobsquad;

import static org.junit.jupiter.api.Assertions.*;

import com.extantape.mobsquad.hook.Placeholders.PlaceholderValues;
import com.extantape.mobsquad.model.Critter;
import com.extantape.mobsquad.model.Species;
import com.extantape.mobsquad.model.Tier;
import com.extantape.mobsquad.model.Unlocks;
import com.extantape.mobsquad.model.Unlocks.Mode;
import com.extantape.mobsquad.model.Wrangler;
import com.extantape.mobsquad.store.Store;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Quest chapters (0.9.0): how tiers open, the saved quest unlocks, the setting and the placeholders. */
class QuestTest {
    @Test
    void levelModeIgnoresQuestsSoNothingChangesByDefault() {
        Set<Tier> quested = EnumSet.of(Tier.YONDER);
        assertFalse(Unlocks.open(Mode.LEVEL, Tier.YONDER, 24, quested), "a quest unlock doesn't count in level mode");
        assertTrue(Unlocks.open(Mode.LEVEL, Tier.YONDER, 25, Set.of()));
    }

    @Test
    void eitherModeLetsAQuestOpenATierEarly() {
        assertTrue(Unlocks.open(Mode.EITHER, Tier.YONDER, 1, EnumSet.of(Tier.YONDER)));
        assertTrue(Unlocks.open(Mode.EITHER, Tier.YONDER, 25, Set.of()), "the level still works");
        assertFalse(Unlocks.open(Mode.EITHER, Tier.BACKCOUNTRY, 39, EnumSet.of(Tier.YONDER)), "one tier's quest doesn't open another");
    }

    @Test
    void questModeNeedsTheQuestButYardIsAlwaysOpen() {
        assertFalse(Unlocks.open(Mode.QUEST, Tier.HOLLER, 100, Set.of()), "level alone doesn't open a tier in quest mode");
        assertTrue(Unlocks.open(Mode.QUEST, Tier.HOLLER, 1, EnumSet.of(Tier.HOLLER)));
        for (Mode m : Mode.values()) assertTrue(Unlocks.open(m, Tier.YARD, 1, Set.of()), "Yard is open in " + m + " mode, so nobody is locked out of everything");
    }

    @Test
    void theLockedMessageSaysHowToOpenIt() {
        assertEquals("from Wranglin' level 25", Unlocks.when(Mode.LEVEL, Tier.YONDER));
        assertTrue(Unlocks.when(Mode.EITHER, Tier.YONDER).contains("quest"));
        assertEquals("once a quest opens the Yonder tier", Unlocks.when(Mode.QUEST, Tier.YONDER));
    }

    static final class Warnings extends Handler {
        final List<String> messages = new ArrayList<>();
        @Override public void publish(LogRecord r) { messages.add(r.getMessage()); }
        @Override public void flush() {}
        @Override public void close() {}
    }

    @Test
    void theSettingDefaultsToLevelAndABadValueIsWarnedAbout() throws Exception {
        Logger log = Logger.getAnonymousLogger(); log.setUseParentHandlers(false); Warnings warn = new Warnings(); log.addHandler(warn);
        assertEquals(Mode.LEVEL, Settings.read(new YamlConfiguration(), log).tierUnlock());
        YamlConfiguration y = new YamlConfiguration();
        y.set("quests.tier-unlock", "Either");
        assertEquals(Mode.EITHER, Settings.read(y, log).tierUnlock(), "case doesn't matter");
        y.set("quests.tier-unlock", "sometimes");
        assertEquals(Mode.LEVEL, Settings.read(y, log).tierUnlock());
        assertEquals(1, warn.messages.size(), "a bad value is warned about: " + warn.messages);
        YamlConfiguration bundled = new YamlConfiguration();
        try (InputStream in = QuestTest.class.getResourceAsStream("/config.yml")) {
            bundled.loadFromString(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
        assertEquals("level", bundled.getString("quests.tier-unlock"), "the shipped config keeps tiers on level alone");
    }

    @Test
    void questUnlocksAreSavedWithThePlayer(@TempDir Path dir) throws Exception {
        Store store = new Store(dir);
        UUID p = UUID.randomUUID();
        Wrangler w = new Wrangler(p);
        w.questTiers.add(Tier.HOLLER);
        w.questTiers.add(Tier.BACKCOUNTRY);
        store.save(w);
        Store.Loaded back = store.load(p);
        assertEquals(0, back.skipped());
        assertEquals(EnumSet.of(Tier.HOLLER, Tier.BACKCOUNTRY), back.wrangler().questTiers);
    }

    @Test
    void filesFromBeforeQuestsLoadWithNoneAndABadTierLineIsSkipped(@TempDir Path dir) throws Exception {
        UUID p = UUID.randomUUID();
        Files.writeString(dir.resolve(p + ".txt"), "wranglin 10\npage chicken\n");
        assertTrue(new Store(dir).load(p).wrangler().questTiers.isEmpty());
        UUID q = UUID.randomUUID();
        Files.writeString(dir.resolve(q + ".txt"), "wranglin 10\ntier dragon\ntier yonder\n");
        Store.Loaded l = new Store(dir).load(q);
        assertEquals(1, l.skipped());
        assertEquals(EnumSet.of(Tier.YONDER), l.wrangler().questTiers);
    }

    @Test
    void placeholdersReportOnlyThePlayersOwnProgress() {
        Wrangler w = new Wrangler(UUID.randomUUID());
        w.critters.add(new Critter(UUID.randomUUID(), Species.CHICKEN, "Clucky", 12, 0, 0, 0));
        w.critters.add(new Critter(UUID.randomUUID(), Species.CHICKEN, "Pecky", 30, 0, 0, 0));
        w.pages.add(Species.CHICKEN);
        w.pages.add(Species.FOX);
        var open = (java.util.function.Predicate<Tier>) t -> t == Tier.YARD || t == Tier.HOLLER;
        assertEquals("14", PlaceholderValues.value("level", 14, w, open));
        assertEquals("2", PlaceholderValues.value("critters", 14, w, open));
        assertEquals("2", PlaceholderValues.value("pages", 14, w, open));
        assertEquals("true", PlaceholderValues.value("tier_holler", 14, w, open));
        assertEquals("false", PlaceholderValues.value("TIER_Yonder", 14, w, open), "case doesn't matter");
        assertEquals("true", PlaceholderValues.value("page_fox", 14, w, open));
        assertEquals("false", PlaceholderValues.value("page_creeper", 14, w, open));
        assertEquals("true", PlaceholderValues.value("has_chicken", 14, w, open));
        assertEquals("false", PlaceholderValues.value("has_fox", 14, w, open), "a page isn't a critter");
        assertEquals("30", PlaceholderValues.value("best_chicken", 14, w, open));
        assertEquals("0", PlaceholderValues.value("best_creeper", 14, w, open));
        assertEquals("false", PlaceholderValues.value("has_iron_golem", 14, w, open), "a critter id with an underscore works");
        for (String bad : new String[] {"tier_dragon", "page_dragon", "where", "has_", "owner_chicken", ""})
            assertNull(PlaceholderValues.value(bad, 14, w, open), bad + " is left as written");
    }
}
