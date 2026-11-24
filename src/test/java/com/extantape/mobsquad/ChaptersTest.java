package com.extantape.mobsquad;

import static org.junit.jupiter.api.Assertions.*;

import com.extantape.mobsquad.model.Levels;
import com.extantape.mobsquad.model.Species;
import com.extantape.mobsquad.model.Tier;
import com.extantape.mobsquad.model.Unlocks;
import com.extantape.mobsquad.model.Unlocks.Mode;
import com.extantape.mobsquad.model.Wrangler;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

/**
 * The BetonQuest chapters shipped in quests/mobsquad/package.yml (0.10.0): the unlock rule they rely on, that the file
 * hangs together (every action, objective, journal entry and conversation line it names exists), and that its text is
 * fit for young readers. BetonQuest itself isn't loaded here; the bot suite runs the chapters on a server.
 */
class ChaptersTest {
    private static final List<Tier> CHAPTER_TIERS = List.of(Tier.HOLLER, Tier.YONDER, Tier.BACKCOUNTRY);

    private static YamlConfiguration pack() throws Exception {
        try (InputStream in = ChaptersTest.class.getResourceAsStream("/" + MobSquadPlugin.QUEST_PACKAGE)) {
            assertNotNull(in, "the chapters are in the jar");
            YamlConfiguration y = new YamlConfiguration();
            y.loadFromString(new String(in.readAllBytes(), StandardCharsets.UTF_8));
            return y;
        }
    }

    private static List<String> section(YamlConfiguration y, String name) {
        ConfigurationSection s = y.getConfigurationSection(name);
        assertNotNull(s, name + " is in the package");
        return new ArrayList<>(s.getKeys(false));
    }

    // ---- the unlock rule ----

    @Test
    void eitherOpensATierByLevelOrByQuestAndNothingElse() {
        for (Tier t : CHAPTER_TIERS) {
            assertTrue(Unlocks.open(Mode.EITHER, t, t.level, Set.of()), t + " opens at its level");
            assertTrue(Unlocks.open(Mode.EITHER, t, 1, EnumSet.of(t)), t + " opens by finishing its chapter");
            assertFalse(Unlocks.open(Mode.EITHER, t, t.level - 1, Set.of()), t + " stays shut below its level without the chapter");
        }
    }

    @Test
    void withoutBetonQuestNoTierIsEverQuestedSoEitherActsLikeLevel() {
        // MobSquad never loads BetonQuest. A server without it simply never runs /mobsquad unlock, so a player's quested
        // tiers stay empty, and the rule has to give exactly the level-only answer for every level and tier.
        Wrangler fresh = new Wrangler(UUID.randomUUID());
        assertTrue(fresh.questTiers.isEmpty(), "nobody starts with a quest unlock");
        for (Tier t : Tier.values())
            for (int level = 1; level <= 100; level++)
                assertEquals(Unlocks.open(Mode.LEVEL, t, level, fresh.questTiers), Unlocks.open(Mode.EITHER, t, level, fresh.questTiers),
                    t + " at level " + level);
        assertTrue(Levels.unlocked(Tier.HOLLER, Tier.HOLLER.level), "and the level still opens Holler");
    }

    @Test
    void mobSquadHasNoCodeInTheBetonQuestPlugin() throws Exception {
        try (Stream<Path> files = Files.walk(Path.of("src/main/java"))) {
            for (Path f : files.filter(p -> p.toString().endsWith(".java")).toList())
                assertFalse(Files.readString(f).toLowerCase(Locale.ROOT).contains("org.betonquest"),
                    f + " would stop MobSquad loading where BetonQuest is missing");
        }
        assertFalse(Files.readString(Path.of("src/main/resources/paper-plugin.yml")).contains("BetonQuest:"), "BetonQuest isn't a dependency, soft or hard");
    }

    // ---- the file hangs together ----

    @Test
    void eachTierHasOneChapterThatEndsByUnlockingThatTierAndOpeningAPage() throws Exception {
        YamlConfiguration y = pack();
        for (Tier t : CHAPTER_TIERS) {
            String finish = y.getString("actions." + t.name().toLowerCase(Locale.ROOT) + "Finish");
            assertNotNull(finish, t + " has a finishing action");
            assertTrue(finish.contains("^command mobsquad unlock %player% " + t.name().toLowerCase(Locale.ROOT) + " "), t + " unlocks its own tier: " + finish);
            Matcher page = Pattern.compile("\\^command mobsquad page %player% (\\w+) ").matcher(finish);
            assertTrue(page.find(), t + " opens a page");
            Species s = Species.byId(page.group(1));
            assertNotNull(s, page.group(1) + " is a critter");
            assertEquals(t, s.tier, "the page is for a critter of the same tier");
            assertEquals(2, Pattern.compile("\\^command ").matcher(finish).results().count(), "just the unlock and the page");
        }
        assertEquals(CHAPTER_TIERS.size(), Pattern.compile("\\^command mobsquad unlock").matcher(Files.readString(Path.of("src/main/resources/quests/mobsquad/package.yml"))).results().count(),
            "no chapter unlocks a tier it isn't about");
    }

    @Test
    void everyNameTheFileUsesExists() throws Exception {
        YamlConfiguration y = pack();
        List<String> actions = section(y, "actions"), objectives = section(y, "objectives"), journal = section(y, "journal"),
            conditions = section(y, "conditions"), items = section(y, "items");
        List<String> started = new ArrayList<>();
        for (String o : objectives) {
            String def = y.getString("objectives." + o);
            Matcher a = Pattern.compile("actions:(\\w+)").matcher(def);
            assertTrue(a.find(), o + " runs an action when it's done");
            assertTrue(actions.contains(a.group(1)), o + " names a missing action " + a.group(1));
            String item = def.split("\\s+")[1];
            if (!def.startsWith("breed")) assertTrue(items.contains(item), o + " names a missing item " + item);
        }
        for (String a : actions) {
            String def = y.getString("actions." + a);
            Matcher obj = Pattern.compile("\\^objective start (\\w+)").matcher(def);
            while (obj.find()) { assertTrue(objectives.contains(obj.group(1)), a + " starts a missing objective " + obj.group(1)); started.add(obj.group(1)); }
            Matcher j = Pattern.compile("\\^journal (?:add|delete) (\\w+)").matcher(def);
            while (j.find()) assertTrue(journal.contains(j.group(1)), a + " names a missing journal entry " + j.group(1));
        }
        for (String o : objectives) assertEquals(1, started.stream().filter(o::equals).count(), o + " is started by exactly one action");
        for (String c : conditions) assertTrue(y.getString("conditions." + c).startsWith("tag "), c + " is a plain tag check");
        for (String c : section(y, "conversations.ranger.NPC_options")) {
            String opt = "conversations.ranger.NPC_options." + c;
            for (String p : y.getString(opt + ".pointers", "").split(","))
                if (!p.isBlank()) assertTrue(y.contains("conversations.ranger.player_options." + p.trim()), c + " points at a missing option " + p);
            String act = y.getString(opt + ".actions");
            if (act != null) assertTrue(actions.contains(act), c + " runs a missing action " + act);
            for (String cond : y.getString(opt + ".conditions", "").split(","))
                if (!cond.isBlank()) assertTrue(conditions.contains(cond.replace("!", "").trim()), c + " checks a missing condition " + cond);
        }
        for (String c : section(y, "conversations.ranger.player_options"))
            for (String p : y.getString("conversations.ranger.player_options." + c + ".pointers", "").split(","))
                if (!p.isBlank()) assertTrue(y.contains("conversations.ranger.NPC_options." + p.trim()), c + " points at a missing line " + p);
        for (String f : y.getString("conversations.ranger.first").split(","))
            assertTrue(y.contains("conversations.ranger.NPC_options." + f.trim()), "first names a missing line " + f);
    }

    @Test
    void chaptersRunInOrderAndEachHasThreeSteps() throws Exception {
        YamlConfiguration y = pack();
        for (Tier t : CHAPTER_TIERS) {
            String n = t.name().toLowerCase(Locale.ROOT);
            assertEquals(3, section(y, "objectives").stream().filter(o -> o.startsWith(n)).count(), n + " has three steps");
            assertTrue(y.getString("conversations.ranger.NPC_options." + n + "StartText.actions", "").equals(n + "Start"), n + " starts from the Ranger");
        }
        assertTrue(y.getString("conversations.ranger.NPC_options.yonderOffer.conditions").contains("hollerDone"));
        assertTrue(y.getString("conversations.ranger.NPC_options.backcountryOffer.conditions").contains("yonderDone"));
    }

    // ---- text for young readers ----

    private static List<String> allText(YamlConfiguration y) {
        List<String> out = new ArrayList<>();
        for (String j : section(y, "journal")) out.add(y.getString("journal." + j));
        for (String c : section(y, "conversations.ranger.NPC_options")) out.add(y.getString("conversations.ranger.NPC_options." + c + ".text"));
        for (String c : section(y, "conversations.ranger.player_options")) out.add(y.getString("conversations.ranger.player_options." + c + ".text"));
        for (String a : section(y, "actions")) {
            Matcher m = Pattern.compile("\\^notify (.*?) io:chat").matcher(y.getString("actions." + a));
            while (m.find()) out.add(m.group(1));
        }
        out.add(y.getString("conversations.ranger.quester"));
        return out;
    }

    @Test
    void textIsShortGentleAndAsksForNothingPersonal() throws Exception {
        YamlConfiguration y = pack();
        List<String> text = allText(y);
        assertTrue(text.size() > 40, "found the text");
        Pattern scary = Pattern.compile("\\b(kill|kills|killed|die|dies|dead|death|blood|bleed|hurt|hurts|fight|fights|attack|attacks|weapon|sword|stab|shoot|shoots|punch|hit|hits|monster|zombie|scary|afraid|explode|explosion|blast|fear)\\b", Pattern.CASE_INSENSITIVE);
        Pattern personal = Pattern.compile("(?i)\\b(your name|real name|age|address|email|phone|password|where do you live|how old|ip|discord|tell me about yourself)\\b|%(?!player%)");
        for (String t : text) {
            assertNotNull(t);
            assertFalse(scary.matcher(t).find(), "gentle wording only: " + t);
            assertFalse(personal.matcher(t).find(), "asks for or shows nothing personal: " + t);
            assertFalse(t.contains("—") || t.contains("–"), "no dashes: " + t);
            assertTrue(t.length() <= 160, "short enough for a young reader (" + t.length() + "): " + t);
            for (String sentence : t.replaceAll("&[0-9a-fk-or]", "").split("(?<=[.!?])\\s+"))
                assertTrue(sentence.split("\\s+").length <= 22, "short sentences: " + sentence);
        }
    }

    @Test
    void thePackageOnlyRunsMobSquadCommandsAndNeverChats() throws Exception {
        YamlConfiguration y = pack();
        for (String a : section(y, "actions")) {
            String def = y.getString("actions." + a);
            Matcher m = Pattern.compile("\\^command (\\S+) (\\S+)").matcher(def);
            while (m.find()) {
                assertEquals("mobsquad", m.group(1), a + " runs only MobSquad commands");
                assertTrue(Set.of("unlock", "page").contains(m.group(2)), a + " runs only unlock and page");
            }
            assertFalse(def.matches(".*\\^(chat|opsudo|sudo|notifyall|runForAll)\\b.*"), a + " doesn't talk for or to other players");
        }
        assertFalse(section(y, "conversations").isEmpty());
    }
}
