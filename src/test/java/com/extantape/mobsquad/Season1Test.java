package com.extantape.mobsquad;

import static org.junit.jupiter.api.Assertions.*;

import com.extantape.mobsquad.hook.Placeholders.PlaceholderValues;
import com.extantape.mobsquad.model.Critter;
import com.extantape.mobsquad.model.Species;
import com.extantape.mobsquad.model.Wrangler;
import com.extantape.mobsquad.store.Store;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Season 1 (0.13.0): the three tier chapters, unchanged, then the Round-up at the Bunkhouse; the small reward after each
 * chapter and the fox at the end; the later Seasons' switches; and the words, which the owner approves in
 * docs/season1-text.md. The bot suite's "Season 1" section plays it on a server.
 */
class Season1Test {
    private static final List<String> CHAPTERS = List.of("holler", "yonder", "backcountry");

    private static YamlConfiguration load(String resource) throws Exception {
        try (InputStream in = Season1Test.class.getResourceAsStream(resource)) {
            assertNotNull(in, resource);
            YamlConfiguration y = new YamlConfiguration();
            y.loadFromString(new String(in.readAllBytes(), StandardCharsets.UTF_8));
            return y;
        }
    }

    private static YamlConfiguration pack() throws Exception { return load("/" + MobSquadPlugin.QUEST_PACKAGE); }

    /** The package 0.12.0 shipped, which is what a live server has. */
    private static YamlConfiguration before() throws Exception { return load("/package-0.12.0.yml"); }

    private static List<String> keys(YamlConfiguration y, String path) { return new ArrayList<>(y.getConfigurationSection(path).getKeys(false)); }

    private static List<String> conditions(YamlConfiguration y, String npc) {
        return Arrays.asList(y.getString("conversations.ranger.NPC_options." + npc + ".conditions", "").split(","));
    }

    // ---- the progress behind the finale ----

    @Test
    void onlyACritterCoaxedWhileItsOnAndSentFromABunkhouseCounts() {
        Wrangler w = new Wrangler(UUID.randomUUID());
        UUID old = UUID.randomUUID(), fresh = UUID.randomUUID();
        assertFalse(w.roundup.coaxed(old), "a coax before the finale starts is nothing");
        assertTrue(w.roundup.start());
        assertFalse(w.roundup.hasCoaxed());
        assertFalse(w.roundup.sentOut(old), "an older or a given critter doesn't count, however it goes out");
        assertFalse(w.roundup.ready(id -> true), "and being out doesn't make it count");
        assertTrue(w.roundup.coaxed(fresh));
        assertTrue(w.roundup.hasCoaxed());
        assertFalse(w.roundup.ready(id -> true), "out but never sent from a Bunkhouse");
        assertTrue(w.roundup.sentOut(fresh));
        assertTrue(w.roundup.hasSent());
        assertFalse(w.roundup.ready(id -> id.equals(old)), "another critter being out isn't it");
        assertTrue(w.roundup.ready(id -> id.equals(fresh)));
        assertFalse(w.roundup.start(), "asking again changes nothing");
        assertTrue(w.roundup.hasSent(), "and keeps what's done");
        assertTrue(w.roundup.stop());
        assertFalse(w.roundup.hasCoaxed() || w.roundup.hasSent() || w.roundup.ready(id -> true));
    }

    @Test
    void theRoundupIsSavedAndOldFilesLoadWithItOff(@TempDir Path dir) throws Exception {
        Store store = new Store(dir);
        Wrangler w = new Wrangler(UUID.randomUUID());
        Critter a = Critter.fresh(Species.CHICKEN), b = Critter.fresh(Species.FOX);
        w.critters.add(a);
        w.critters.add(b);
        store.save(w);
        assertFalse(Files.readString(dir.resolve(w.player + ".txt")).contains("roundup"), "nothing written while it's off");
        assertFalse(store.load(w.player).wrangler().roundup.on);
        w.roundup.start();
        store.save(w);
        assertTrue(store.load(w.player).wrangler().roundup.on, "on with nothing coaxed yet survives a restart");
        w.roundup.coaxed(a.id());
        w.roundup.coaxed(b.id());
        w.roundup.sentOut(b.id());
        store.save(w);
        Store.Loaded back = store.load(w.player);
        assertEquals(0, back.skipped());
        assertEquals(Set.of(a.id(), b.id()), back.wrangler().roundup.coaxed);
        assertEquals(Set.of(b.id()), back.wrangler().roundup.sent);
    }

    @Test
    void thePlaceholdersSayCoaxedAndSent() {
        Wrangler w = new Wrangler(UUID.randomUUID());
        UUID id = UUID.randomUUID();
        assertEquals("false", PlaceholderValues.value("roundup_coaxed", 1, w, t -> true));
        w.roundup.start();
        w.roundup.coaxed(id);
        assertEquals("true", PlaceholderValues.value("roundup_coaxed", 1, w, t -> true));
        assertEquals("false", PlaceholderValues.value("roundup_sent", 1, w, t -> true));
        w.roundup.sentOut(id);
        assertEquals("true", PlaceholderValues.value("ROUNDUP_SENT", 1, w, t -> true));
    }

    // ---- the live chapters stay valid ----

    @Test
    void everythingTheLiveChaptersHadIsStillThereWordForWord() throws Exception {
        YamlConfiguration old = before(), y = pack();
        int checked = 0;
        for (String section : List.of("actions", "journal", "items", "conditions"))
            for (String k : keys(old, section)) {
                if (!CHAPTERS.stream().anyMatch(k::startsWith) && !section.equals("items")) continue;
                assertEquals(old.getString(section + "." + k), y.getString(section + "." + k), section + "." + k + " is unchanged");
                checked++;
            }
        for (String k : keys(old, "conversations.ranger.NPC_options"))
            if (CHAPTERS.stream().anyMatch(k::startsWith) || Set.of("hello", "allDone", "bye").contains(k))
                for (String field : List.of("text", "conditions", "pointers", "actions")) {
                    assertEquals(old.getString("conversations.ranger.NPC_options." + k + "." + field), y.getString("conversations.ranger.NPC_options." + k + "." + field), k + " " + field);
                    checked++;
                }
        assertTrue(checked > 60, "found them: " + checked);
        // each step is the same step; only the three last ones also hand over the small reward, after the old finish
        for (String o : keys(old, "objectives")) {
            if (CHAPTERS.stream().noneMatch(o::startsWith)) continue;
            String was = old.getString("objectives." + o), is = y.getString("objectives." + o);
            if (Set.of("hollerNoteBlock", "yonderCampfire", "backcountryShears").contains(o))
                assertEquals(was + "," + CHAPTERS.stream().filter(o::startsWith).findFirst().orElseThrow() + "Keep", is, o);
            else assertEquals(was, is, o);
        }
        // nothing at all is taken away, so tags, objectives and journal entries a player holds still exist
        for (String section : List.of("constants", "items", "conditions", "actions", "objectives", "journal"))
            assertTrue(y.getConfigurationSection(section).getKeys(false).containsAll(keys(old, section)), section);
    }

    // ---- rewards ----

    @Test
    void eachOfTheFourChaptersGivesAKeepsakeAndTwoVittlesAndTheFinaleAFox() throws Exception {
        YamlConfiguration y = pack();
        for (String c : List.of("holler", "yonder", "backcountry", "roundup")) {
            String keep = y.getString("actions." + c + "Keep");
            assertTrue(keep.contains("^journal add " + c + "_keep "), c + " writes its keepsake");
            assertTrue(keep.contains("^command mobsquad vittles %player% 2 "), c + " gives two vittles");
            assertEquals(1, Pattern.compile("\\^command ").matcher(keep).results().count(), c + " gives nothing else");
        }
        String finish = y.getString("actions.roundupFinish");
        assertTrue(finish.startsWith("run ^tag add season1_done "), "done is marked first");
        assertTrue(finish.contains("^command mobsquad give %player% fox "), "the Season reward is the fox");
        assertTrue(finish.contains("^command mobsquad roundup %player% stop "));
        List<String> gives = keys(y, "actions").stream().filter(a -> y.getString("actions." + a).contains("mobsquad give ")).toList();
        assertEquals(List.of("roundupFinish"), gives, "no other action gives a critter");
        assertEquals(4, keys(y, "actions").stream().filter(a -> y.getString("actions." + a).contains("mobsquad vittles")).count());
        // the reward comes once: only the ready line runs it, and it needs season1_done unset
        assertEquals("roundupFinish,roundupKeep", y.getString("conversations.ranger.NPC_options.roundupReady.actions"));
        for (String npc : keys(y, "conversations.ranger.NPC_options"))
            if (!npc.equals("roundupReady")) assertFalse(y.getString("conversations.ranger.NPC_options." + npc + ".actions", "").matches(".*roundup(Finish|Keep).*"), npc);
        assertTrue(conditions(y, "roundupReady").containsAll(List.of("roundupStarted", "!season1Done", "roundupReady")));
        for (String o : keys(y, "objectives")) assertFalse(y.getString("objectives." + o).matches(".*roundup(Finish|Keep).*"), o);
    }

    // ---- the finale ----

    @Test
    void theFinaleIsOfferedToAnyoneWhoFinishedBackcountryWithNoSwitch() throws Exception {
        YamlConfiguration y = pack();
        assertEquals(List.of("backcountryDone", "!roundupStarted", "!season1Done"), conditions(y, "roundupOffer"), "no switch: Season 1 is always on");
        String first = y.getString("conversations.ranger.first");
        assertTrue(first.startsWith("roundupReady,roundupAway,roundupSendOn,roundupOn,roundupOffer,"), first);
        assertTrue(first.endsWith("seasonOneDone,allDone,hello"), first);
        assertEquals("variable %ph.mobsquad_roundup_coaxed% true", y.getString("conditions.roundupCoaxed"));
        assertEquals("variable %ph.mobsquad_roundup_sent% true", y.getString("conditions.roundupSent"));
        assertEquals("variable %ph.mobsquad_roundup_ready% true", y.getString("conditions.roundupReady"));
        assertEquals("delay 1 seconds interval:20 conditions:roundupCoaxed actions:roundupStep2", y.getString("objectives.roundupCoax"));
        assertEquals("delay 1 seconds interval:20 conditions:roundupSent actions:roundupStep3", y.getString("objectives.roundupSend"));
        assertEquals("run ^command mobsquad roundup %player% start", y.getString("actions.roundupRearm"));
        assertEquals("roundupRearm", y.getString("conversations.ranger.NPC_options.roundupOn.actions"), "talking to the Ranger sets the watch up again if it was lost");
    }

    @Test
    void helpOnlyAddsATipAndFailsTowardGivingIt() throws Exception {
        YamlConfiguration y = pack();
        assertEquals("delay 15 actions:roundupCoaxTip", y.getString("objectives.roundupCoaxHint"));
        assertEquals("delay 15 actions:roundupSendTip", y.getString("objectives.roundupSendHint"));
        // the tip is held back only on a clear true; an unreadable answer is not true, so the tip is given
        assertEquals("if roundupCoaxed nop else roundupCoaxTipGive", y.getString("actions.roundupCoaxTip"));
        assertEquals("if roundupSent nop else roundupSendTipGive", y.getString("actions.roundupSendTip"));
        for (String step : List.of("Coax", "Send")) {
            String give = y.getString("actions.roundup" + step + "TipGive");
            assertFalse(give.contains("^command") || give.contains("^tag") || give.contains("^objective"), "a tip changes no goal and no reward: " + give);
        }
    }

    // ---- later Seasons stay hidden ----

    @Test
    void everyLaterOfferNeedsTheOldSwitchItsSeasonsSwitchAndSeasonOne() throws Exception {
        YamlConfiguration y = pack();
        assertEquals("globaltag winds_open", y.getString("conditions.windsOpen"));
        assertEquals("globaltag season2_open", y.getString("conditions.season2Open"));
        assertEquals("globaltag season3_open", y.getString("conditions.season3Open"));
        for (String o : List.of("signsOffer", "watchOffer", "watchOfferFighter", "meadowOffer", "meadowOfferExplorer"))
            assertTrue(conditions(y, o).containsAll(List.of("windsOpen", "season1Done", "season2Open")), o);
        for (String o : List.of("windsOffer", "finaleOffer"))
            assertTrue(conditions(y, o).containsAll(List.of("windsOpen", "season1Done", "season3Open")), o);
        for (String s : List.of("season2", "season3")) {
            assertEquals("globaltag add " + s + "_open", y.getString("actions." + s + "OpenOn"));
            assertEquals("globaltag del " + s + "_open", y.getString("actions." + s + "OpenOff"));
            for (String a : keys(y, "actions")) if (!a.equals(s + "OpenOn")) assertFalse(y.getString("actions." + a).contains(s + "OpenOn"), a + " never opens a Season");
        }
        for (String conv : keys(y, "conversations"))
            for (String o : keys(y, "conversations." + conv + ".NPC_options"))
                assertFalse(y.getString("conversations." + conv + ".NPC_options." + o + ".actions", "").contains("OpenO"), o);
    }

    // ---- the words ----

    private static List<String> newWords() throws Exception {
        YamlConfiguration old = before(), y = pack();
        List<String> out = new ArrayList<>();
        for (String j : keys(y, "journal")) if (!y.getString("journal." + j).equals(old.getString("journal." + j))) out.add(y.getString("journal." + j));
        for (String conv : keys(y, "conversations"))
            for (String kind : List.of("NPC_options", "player_options"))
                for (String id : keys(y, "conversations." + conv + "." + kind)) {
                    String p = "conversations." + conv + "." + kind + "." + id + ".text";
                    if (!y.getString(p).equals(old.getString(p))) out.add(y.getString(p));
                }
        for (String a : keys(y, "actions")) {
            Matcher m = Pattern.compile("\\^notify (.*?) io:chat").matcher(y.getString("actions." + a));
            while (m.find()) if (old.getString("actions." + a) == null) out.add(m.group(1));
        }
        return out.stream().map(t -> t.replaceAll("&[0-9a-fk-or]", "")).toList();
    }

    @Test
    void newKidLinesAreShortPlainAndAskForNoTyping() throws Exception {
        List<String> words = newWords();
        assertTrue(words.size() > 25, "found them: " + words.size());
        for (String t : words) {
            for (String sentence : t.split("(?<=[.!?])\\s+"))
                assertTrue(sentence.split("\\s+").length <= 12, "12 words or fewer: " + sentence);
            if (!t.startsWith("Keepsake")) assertTrue(t.split("\\s+").length <= 12, "the whole line is 12 words or fewer: " + t);
            assertFalse(t.contains("—") || t.contains("–"), "no dashes: " + t);
            assertFalse(Pattern.compile("(?i)/\\w|\\btype\\b|\\bsay\\b|\\bchat\\b|\\bcommand\\b").matcher(t).find(), "nothing to type: " + t);
            assertFalse(Pattern.compile("(?i)\\b(colour|favourite|neighbour|centre|grey|harbour|armour)").matcher(t).find(), "US spelling: " + t);
        }
    }

    @Test
    void everyNewLineIsInTheListForTheOwner() throws Exception {
        String doc = Files.readString(Path.of("docs/season1-text.md"));
        for (String t : newWords()) assertTrue(doc.contains(t), "docs/season1-text.md is missing: " + t);
    }
}
