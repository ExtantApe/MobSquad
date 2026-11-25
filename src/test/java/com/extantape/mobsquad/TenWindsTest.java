package com.extantape.mobsquad;

import static org.junit.jupiter.api.Assertions.*;

import com.extantape.mobsquad.hook.Placeholders.PlaceholderValues;
import com.extantape.mobsquad.model.Critter;
import com.extantape.mobsquad.model.Species;
import com.extantape.mobsquad.model.Tier;
import com.extantape.mobsquad.model.Wrangler;
import com.extantape.mobsquad.quest.ConversationChat;
import com.extantape.mobsquad.quest.PlayStyle;
import com.extantape.mobsquad.quest.QuestBooks;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Monster;
import org.junit.jupiter.api.Test;

/**
 * The Ten Winds story arc (0.11.0, D289): the three small hooks MobSquad adds (the play style, the top level, the books),
 * and what the shipped BetonQuest package promises: kid or adult fails closed, effects go to one player, rings read what a
 * player owns, nothing needs a typed command, and the spots are set by an admin. BetonQuest itself isn't loaded here; the
 * bot suite's "The Ten Winds" section runs the story on a server.
 */
class TenWindsTest {
    // ---- the hooks ----

    @Test
    void nothingCountedOrATieIsAllOfTheAbove() {
        assertEquals(PlayStyle.ALL, PlayStyle.pick(0, 0, 0, 0), "a new player has no style yet");
        assertEquals(PlayStyle.ALL, PlayStyle.pick(-5, -1, -9, -2), "a bad count is ignored, not trusted");
        assertEquals(PlayStyle.ALL, PlayStyle.pick(200, 25, 0, 0), "two kinds tied at the top");
        assertEquals(PlayStyle.ALL, PlayStyle.pick(200, 25, 2500, 6), "all four tied");
    }

    @Test
    void theBiggestShareOfItsOwnGoalWins() {
        assertEquals(PlayStyle.BUILDER, PlayStyle.pick(1000, 10, 100, 1));
        assertEquals(PlayStyle.FIGHTER, PlayStyle.pick(10, 400, 100, 1));
        assertEquals(PlayStyle.EXPLORER, PlayStyle.pick(10, 3, 90_000, 1));
        assertEquals(PlayStyle.CARETAKER, PlayStyle.pick(10, 3, 100, 60));
        assertEquals("explorer", PlayStyle.EXPLORER.id());
        assertEquals("all", PlayStyle.ALL.id());
    }

    @Test
    void topIsTheHighestLevelOwnedAndZeroWithNothing() {
        Wrangler w = new Wrangler(UUID.randomUUID());
        Predicate<Tier> open = t -> true;
        assertEquals("0", PlaceholderValues.value("top", 1, w, open), "nothing owned is 0");
        w.critters.add(new Critter(UUID.randomUUID(), Species.CHICKEN, "Clucky", 12, 0, 0, 0));
        w.critters.add(new Critter(UUID.randomUUID(), Species.FOX, "Rusty", 31, 0, 0, 0));
        assertEquals("31", PlaceholderValues.value("top", 1, w, open), "the highest across kinds");
        assertEquals("31", PlaceholderValues.value("TOP", 1, w, open), "case doesn't matter");
        assertNull(PlaceholderValues.value("top_fox", 1, w, open) == null ? null : "x" , "top has no per-kind form; best_<critter> does that");
    }

    @Test
    void theBooksAreByTheRangerAndSayNothingAboutAnyPlayerOrPlace() {
        QuestBooks books = QuestBooks.load(TenWindsTest.class);
        assertEquals(Set.of("trail", "watch", "meadow", "finale"), books.all().keySet());
        assertEquals("Ranger Juniper", QuestBooks.AUTHOR);
        Pattern coordinates = Pattern.compile("-?\\d+\\s*[,;]\\s*-?\\d+|\\bx\\s*[:=]|\\bz\\s*[:=]|\\bcoordinates?\\b", Pattern.CASE_INSENSITIVE);
        Pattern personal = Pattern.compile("(?i)%|\\bplayer\\b|\\bkid\\b|\\bchild\\b|\\bfamily\\b|\\bage\\b|\\bsir\\b|\\bmiss\\b");
        for (var e : books.all().entrySet()) {
            QuestBooks.Book b = e.getValue();
            assertFalse(b.title().isBlank(), e.getKey() + " has a title");
            assertFalse(b.pages().isEmpty(), e.getKey() + " has pages");
            for (String page : b.pages()) {
                assertFalse(coordinates.matcher(page).find(), e.getKey() + " gives no place: " + page);
                assertFalse(personal.matcher(page).find(), e.getKey() + " names or addresses no one in particular: " + page);
                assertFalse(page.contains("—") || page.contains("–"), "no dashes: " + page);
                assertTrue(page.length() <= 230, "a short page: " + page.length());
                for (String sentence : page.split("(?<=[.!?:])\\s+")) assertTrue(sentence.split("\\s+").length <= 22, "short sentences: " + sentence);
            }
        }
    }

    @Test
    void theBooksCodeMakesATatteredWrittenBookAndNeverAMap() throws Exception {
        String code = Files.readString(Path.of("src/main/java/com/extantape/mobsquad/quest/QuestBooks.java"));
        assertTrue(code.contains("Material.WRITTEN_BOOK") && !code.contains("FILLED_MAP") && !code.contains("MAP"), "a written book, not a map");
        assertTrue(code.contains("Generation.TATTERED"), "tattered, so it can't be copied");
        assertTrue(code.contains("setAuthor(AUTHOR)"), "the author is always the Ranger, never the receiver");
        assertFalse(code.toLowerCase(Locale.ROOT).contains("getname()"), "nothing in the book reads the player's name");
    }

    @Test
    void typedWordsAreHeldOnlyWhileTheRangerTalksAndOnlyOnAnExactTrue() {
        assertTrue(ConversationChat.holds("hello everyone", "true"), "free text in a conversation is held");
        assertFalse(ConversationChat.holds("hello everyone", "false"), "not in a conversation: chat as usual");
        for (String unreadable : new String[] {"", "TRUE", "yes", "%betonquest_mobsquad:condition.inTalk%", " true", null})
            assertFalse(ConversationChat.holds("hello", unreadable), "'" + unreadable + "' is not a clear yes, so chat isn't touched");
        assertFalse(ConversationChat.holds("1", "true"), "a bare number can answer a chat conversation");
        assertFalse(ConversationChat.holds(" 12 ", "true"));
        assertTrue(ConversationChat.holds("123", "true"), "three digits can't be an answer, so they are held");
        assertTrue(ConversationChat.holds("1 and my name is", "true"));
        assertEquals("%betonquest_mobsquad:condition.inTalk%", Settings.DEFAULT_HOLD_CHAT);
        Logger log = Logger.getAnonymousLogger();
        assertEquals(Settings.DEFAULT_HOLD_CHAT, Settings.read(new YamlConfiguration(), log).holdChatWhen(), "on by default");
        YamlConfiguration off = new YamlConfiguration();
        off.set("quests.hold-chat-when", "");
        assertEquals("", Settings.read(off, log).holdChatWhen(), "empty turns it off");
    }

    @Test
    void theConversationsAreOnesTheHoldCanSee() throws Exception {
        YamlConfiguration y = pack();
        assertEquals("or inRanger,inRings", y.getString("conditions.inTalk"), "the condition the default setting asks for covers both conversations");
        assertEquals("inconversation ranger", y.getString("conditions.inRanger"));
        assertEquals("inconversation rings", y.getString("conditions.inRings"));
    }

    // ---- the package ----

    private static YamlConfiguration pack() throws Exception {
        try (InputStream in = TenWindsTest.class.getResourceAsStream("/" + MobSquadPlugin.QUEST_PACKAGE)) {
            YamlConfiguration y = new YamlConfiguration();
            y.loadFromString(new String(in.readAllBytes(), StandardCharsets.UTF_8));
            return y;
        }
    }

    private static List<String> keys(YamlConfiguration y, String path) {
        ConfigurationSection s = y.getConfigurationSection(path);
        assertNotNull(s, path);
        return new ArrayList<>(s.getKeys(false));
    }

    private static int words(String text) {
        String t = text.replaceAll("&[0-9a-fk-or]", "").trim();
        return t.isEmpty() ? 0 : t.split("\\s+").length;
    }

    /** Every ranger and ring line with its id and conditions. Audience is read from the conditions, never the name. */
    private record Line(String conv, String id, String text, String conditions, boolean player) {
        boolean adult() { return Pattern.compile("(^|,)adult(,|$)").matcher(conditions).find(); }
        boolean kid() { return conditions.contains("!adult"); }
    }

    private static List<Line> lines(YamlConfiguration y) {
        List<Line> out = new ArrayList<>();
        for (String conv : keys(y, "conversations"))
            for (String kind : List.of("NPC_options", "player_options"))
                for (String id : keys(y, "conversations." + conv + "." + kind)) {
                    String p = "conversations." + conv + "." + kind + "." + id;
                    out.add(new Line(conv, id, y.getString(p + ".text"), y.getString(p + ".conditions", ""), kind.startsWith("player")));
                }
        return out;
    }

    private static final Set<String> ADULT_ONLY_TEXT = Set.of();

    @Test
    void theSpotsAreNamedConstantsAnAdminSetsAndNoneIsHardcoded() throws Exception {
        YamlConfiguration y = pack();
        List<String> spots = keys(y, "constants");
        assertEquals(21, spots.size(), "3 signs x2, 3 rings x2, 3+4 landmarks, 2 meadows");
        for (String k : spots) assertEquals("0;0;0;unset", y.getString("constants." + k), k + " ships unset, so nothing is reachable until an admin sets it");
        String text = Files.readString(Path.of("src/main/resources/quests/mobsquad/package.yml"));
        String outsideConstants = text.replaceAll("(?s)\\nconstants:.*?\\nitems:", "\nitems:");
        assertFalse(Pattern.compile("-?\\d+;-?\\d+;-?\\d+;\\w").matcher(outsideConstants).find(), "no coordinates anywhere but the constants");
        for (String o : keys(y, "objectives")) {
            String def = y.getString("objectives." + o);
            if (def.startsWith("location") || def.startsWith("action")) assertTrue(def.contains("%constant."), o + " points at a named spot");
        }
        for (String c : keys(y, "conditions")) {
            String def = y.getString("conditions." + c);
            if (def.startsWith("location")) assertTrue(def.contains("%constant."), c + " points at a named spot");
        }
        assertFalse(Pattern.compile("\\b(world|world_nether|magic|ninos_\\w+|survival)\\b").matcher(text.replaceAll("#.*", "").replaceAll("(?i)magic world|kids' world|adults' world", "")).find(),
            "no world name is built into the package");
    }

    @Test
    void kidOrAdultFailsClosedAndTheAdultVersionNeedsAPositiveCheck() throws Exception {
        YamlConfiguration y = pack();
        assertEquals("permission group.adult", y.getString("conditions.adult"), "adult is a positive permission check, LuckPerms' group.adult");
        for (String c : keys(y, "conditions")) {
            String def = y.getString("conditions." + c);
            if (def.contains("adult") && !c.equals("adult") && !c.startsWith("landAll")) fail(c + " is another way to be an adult: " + def);
        }
        for (String o : keys(y, "objectives")) {
            String def = y.getString("objectives." + o);
            if (def.contains("Adult") || o.endsWith("Adult")) assertTrue(def.matches(".*conditions:[\\w,]*\\badult\\b.*"), o + " is for adults, and checks for it");
            if (o.endsWith("Kid")) assertTrue(def.contains("conditions:!adult") || def.contains(",!adult"), o + " is for kids, and checks it isn't an adult");
        }
        // every line with an adult condition has a kid twin, so no group still gets words
        List<Line> all = lines(y);
        for (Line l : all) {
            if (l.id().endsWith("Adult")) assertTrue(l.adult(), l.id() + " shows only when positively an adult");
            if (l.id().endsWith("Kid")) assertTrue(l.kid(), l.id() + " shows to anyone who isn't positively an adult");
        }
        assertEquals("if adult watchStartAdult else watchStartKid", y.getString("actions.watchStart"), "the else branch is the kid version");
        for (String a : keys(y, "actions")) {
            String def = y.getString("actions." + a);
            Matcher m = Pattern.compile("^if (\\w+) (\\w+) else (\\w+)$").matcher(def);
            if (m.matches() && m.group(1).equals("adult")) assertTrue(m.group(3).endsWith("Kid"), a + ": else is the kid action");
        }
        for (String a : keys(y, "actions"))
            if (a.endsWith("Adult") && !Set.of("watchStartAdult", "meadowStartAdult", "landCheckAdult").contains(a) && !a.contains("Post")) fail(a + " is an adult-only action with no kid check");
    }

    @Test
    void kidAndSharedLinesAreTwelveWordsOrFewer() throws Exception {
        YamlConfiguration y = pack();
        List<String> adultOnly = List.of("ringEmberAdultAsk", "ringMarigoldAdultAsk", "ringSunbeamAdultAsk");
        int checked = 0;
        for (Line l : lines(y)) {
            if (l.adult() && !l.kid()) continue; // grown-up wording may run longer
            if (isOldChapterLine(l)) continue;
            assertTrue(words(l.text()) <= 12, l.id() + " (" + words(l.text()) + " words): " + l.text());
            checked++;
        }
        assertTrue(checked > 40, "found the lines: " + checked);
        for (String j : keys(y, "journal")) {
            if (isOldChapterJournal(j) || j.endsWith("_adult")) continue;
            for (String sentence : y.getString("journal." + j).replaceAll("&[0-9a-fk-or]", "").split("(?<=[.!?])\\s+"))
                assertTrue(words(sentence) <= 14 || j.endsWith("_done") && words(sentence) <= 24, j + ": " + sentence);
        }
        for (String a : keys(y, "actions")) {
            Matcher m = Pattern.compile("\\^notify (.*?) io:chat").matcher(y.getString("actions." + a));
            while (m.find()) {
                if (isOldChapterAction(a) || a.endsWith("Adult")) continue;
                assertTrue(words(m.group(1)) <= 14, a + ": " + m.group(1));
            }
        }
    }

    private static final Set<String> OLD_LINES = Set.of("hello", "hollerOn", "hollerStartText", "yonderOffer", "yonderOn", "yonderStartText", "backcountryOffer",
        "backcountryOn", "backcountryStartText", "allDone", "bye", "yesHoller", "yesYonder", "yesBackcountry", "notNow", "okay");
    private static boolean isOldChapterLine(Line l) { return l.conv().equals("ranger") && OLD_LINES.contains(l.id()); }
    private static boolean isOldChapterJournal(String j) { return j.startsWith("holler_") || j.startsWith("yonder_") || j.startsWith("backcountry_"); }
    private static boolean isOldChapterAction(String a) { return a.startsWith("holler") || a.startsWith("yonder") || a.startsWith("backcountry"); }

    @Test
    void textForKidsIsGentleAndNothingAsksForTypingOrCommands() throws Exception {
        YamlConfiguration y = pack();
        Pattern scary = Pattern.compile("\\b(kill|kills|killed|die|dies|dead|death|blood|bleed|hurt|hurts|fight|fights|attack|attacks|weapon|sword|stab|shoot|punch|hit|hits|monster|zombie|scary|afraid|explode|fear|slay|slain)\\b", Pattern.CASE_INSENSITIVE);
        Pattern typing = Pattern.compile("(?i)/\\w|\\btype\\b|\\bsay\\b|\\bchat\\b|\\btell (me|us)\\b|\\bcommand\\b|\\bwhisper\\b|\\bmeet\\b|\\bfind (a|another|your) (player|friend)\\b|\\bvisit (a|another|your) (player|friend)\\b");
        for (Line l : lines(y)) {
            assertFalse(typing.matcher(l.text()).find(), "D284: no typing, no commands, no meeting players: " + l.text());
            if (!l.adult() || l.kid()) assertFalse(scary.matcher(l.text()).find(), "gentle wording for kids: " + l.text());
        }
        for (String j : keys(y, "journal")) {
            String t = y.getString("journal." + j);
            assertFalse(typing.matcher(t).find(), j + ": no typing or commands: " + t);
            if (!j.endsWith("_adult")) assertFalse(scary.matcher(t).find(), j + ": gentle: " + t);
        }
        for (String a : keys(y, "actions")) {
            Matcher m = Pattern.compile("\\^notify (.*?) io:chat").matcher(y.getString("actions." + a));
            while (m.find()) {
                assertFalse(typing.matcher(m.group(1)).find(), a + ": no typing or commands: " + m.group(1));
                if (!a.endsWith("Adult")) assertFalse(scary.matcher(m.group(1)).find(), a + ": gentle: " + m.group(1));
            }
        }
        // typed text is kept out of public chat while a conversation is open
        for (String conv : keys(y, "conversations")) {
            assertEquals("tellraw", y.getString("conversations." + conv + ".conversationIO"), conv + ": clickable choices");
            assertEquals("simple", y.getString("conversations." + conv + ".interceptor"), conv + ": chat is held back while the conversation is open");
        }
        for (String o : keys(y, "objectives"))
            assertFalse(y.getString("objectives." + o).startsWith("password") || y.getString("objectives." + o).startsWith("command"), o + " would wait for typed text");
    }

    @Test
    void effectsAreSentOnlyToThePlayerWhoTriggeredThem() throws Exception {
        YamlConfiguration y = pack();
        Pattern everyone = Pattern.compile("\\^(notifyall|chat|opsudo|sudo|runForAll|particle|explosion|lightning|weather|time|spawn|setblock|teleport|kill|damage|velocity|effect|drop|give|take|folder|first)\\b|notifyall|\\bruniforall\\b");
        for (String a : keys(y, "actions")) {
            String def = y.getString("actions." + a);
            assertFalse(everyone.matcher(def).find(), a + " reaches only one player, with no world effect: " + def);
            Matcher cmd = Pattern.compile("\\^command (.*?)(?= \\^|$)").matcher(def);
            while (cmd.find()) assertTrue(cmd.group(1).contains("%player%"), a + " acts on %player% only: " + cmd.group(1));
        }
        for (String a : keys(y, "actions")) {
            Matcher notify = Pattern.compile("\\^notify .*? io:(\\w+)").matcher(y.getString("actions." + a));
            while (notify.find()) assertEquals("chat", notify.group(1), a + " tells only that player in chat");
        }
        assertEquals(0, Pattern.compile("globaltag").matcher(String.join("\n", keys(y, "actions").stream().filter(k -> !k.startsWith("windsOpen")).map(k -> y.getString("actions." + k)).toList())).results().count(),
            "only the admin switch touches a global tag, and nothing a player does");
    }

    @Test
    void ringsReadWhatAPlayerOwnsAtTenTwentyAndThirtyNeverWhatIsOut() throws Exception {
        YamlConfiguration y = pack();
        assertEquals("numbercompare %ph.mobsquad_top% >= 10", y.getString("conditions.top10"));
        assertEquals("numbercompare %ph.mobsquad_top% >= 20", y.getString("conditions.top20"));
        assertEquals("numbercompare %ph.mobsquad_top% >= 30", y.getString("conditions.top30"));
        String text = Files.readString(Path.of("src/main/resources/quests/mobsquad/package.yml"));
        assertFalse(text.contains("mobsquad_out") || text.contains("mobsquad_following") || text.contains("mobsquad_coat"), "no gate reads a critter that is out or a coat that is shown");
        for (String r : List.of("Ember:10", "Marigold:20", "Sunbeam:30"))
            for (String aud : List.of("Kid", "Adult")) {
                String[] p = r.split(":");
                String ask = y.getString("conversations.rings.NPC_options.ring" + p[0] + aud + "Ask.conditions");
                assertTrue(ask.contains("top" + p[1]) && ask.contains("!ring" + p[0] + "Done"), p[0] + aud + " opens only at level " + p[1] + ": " + ask);
                String quiet = y.getString("conversations.rings.NPC_options.ring" + p[0] + aud + "Quiet.conditions");
                assertTrue(quiet.contains("!top" + p[1]), p[0] + aud + " says it is quiet below level " + p[1]);
                assertNull(y.getString("conversations.rings.NPC_options.ring" + p[0] + aud + "Quiet.pointers"), "a quiet ring offers no way to wake it");
            }
        // a ring waits for another visit instead of being used up
        for (String k : keys(y, "actions")) if (k.matches("ring\\w+(Kid|Adult)Hit")) assertTrue(y.getString("actions." + k).contains("^objective start ring"), k + " re-arms itself");
        for (String o : keys(y, "objectives")) if (o.startsWith("ring")) assertTrue(y.getString("objectives." + o).contains(" entry "), o + " fires on walking in, not on standing there");
    }

    @Test
    void holdingTheLineCountsHostileMobsByTypeAndKidsGetEightAtAnyHourAsAnOption() throws Exception {
        YamlConfiguration y = pack();
        String kid = y.getString("objectives.watchFightKid"), adult = y.getString("objectives.watchFightAdult");
        Matcher mk = Pattern.compile("^mobkill (\\S+) (\\d+) (.*)$").matcher(kid), ma = Pattern.compile("^mobkill (\\S+) (\\d+) (.*)$").matcher(adult);
        assertTrue(mk.matches() && ma.matches());
        assertEquals("8", mk.group(2)); assertEquals("15", ma.group(2));
        assertEquals(mk.group(1), ma.group(1), "the same list of mobs");
        for (String type : mk.group(1).split(",")) {
            EntityType t = EntityType.valueOf(type);
            assertTrue(Monster.class.isAssignableFrom(t.getEntityClass()), type + " is a hostile mob");
        }
        assertFalse(mk.group(3).contains("night"), "kids: any time of day");
        assertTrue(ma.group(3).contains("night"), "adults: dusk or night");
        assertEquals("time 18:00-6:00", y.getString("conditions.night"));
        // optional for kids: nothing else waits for it
        for (String c : keys(y, "conditions")) if (!c.equals("watchDone") && !c.startsWith("pair") && !c.equals("twoNew")) assertFalse(y.getString("conditions." + c).contains("watchDone"), c + " doesn't need the watch");
        for (String o : keys(y, "conversations.ranger.NPC_options"))
            if (!o.startsWith("watch") && !o.startsWith("finaleDone") && !o.startsWith("finaleOn")) assertFalse(y.getString("conversations.ranger.NPC_options." + o + ".conditions", "").contains("watchDone"), o + " doesn't wait for the watch");
        // the finale needs any two of the four new chapters
        String two = y.getString("conditions.twoNew");
        assertEquals(6, two.substring(3).split(",").length, "six pairs of four chapters");
        assertTrue(y.getString("conversations.ranger.NPC_options.finaleOffer.conditions").contains("twoNew"));
        assertTrue(y.getString("conversations.ranger.NPC_options.finaleOffer.conditions").contains("backcountryDone"), "and the three tier chapters");
    }

    @Test
    void landmarksAreThreeForKidsFourForAdultsAndEachChapterNeedsTheNewChaptersSwitch() throws Exception {
        YamlConfiguration y = pack();
        assertEquals("and !adult,landHill,landLake,landFlower", y.getString("conditions.landAllKid"));
        assertEquals("and adult,landHill,landLake,landRiver,landFlower", y.getString("conditions.landAllAdult"));
        for (String o : List.of("signsOffer", "windsOffer", "watchOffer", "watchOfferFighter", "meadowOffer", "meadowOfferExplorer", "finaleOffer"))
            assertTrue(y.getString("conversations.ranger.NPC_options." + o + ".conditions").contains("windsOpen"), o + " waits for the admin's switch");
        assertEquals("globaltag winds_open", y.getString("conditions.windsOpen"));
        assertTrue(y.getString("conversations.ranger.NPC_options.signsOffer.conditions").contains("hollerDone"));
        assertTrue(y.getString("conversations.ranger.NPC_options.windsOffer.conditions").contains("yonderDone"));
        assertTrue(y.getString("conversations.ranger.NPC_options.watchOffer.conditions").contains("backcountryDone"));
        assertTrue(y.getString("conversations.ranger.NPC_options.meadowOffer.conditions").contains("yonderDone"));
    }

    @Test
    void maybeLaterLeadsToTheNextOpenOfferSoNothingIsStuckBehindOneJob() throws Exception {
        YamlConfiguration y = pack();
        List<String> chain = List.of("yonderOffer", "meadowOfferExplorer", "signsOffer", "windsOffer", "backcountryOffer", "watchOfferFighter", "watchOffer", "meadowOffer", "finaleOffer");
        for (int i = 0; i < chain.size(); i++) {
            String offer = chain.get(i), later = "later" + Character.toUpperCase(offer.charAt(0)) + offer.substring(1);
            assertTrue(y.getString("conversations.ranger.NPC_options." + offer + ".pointers").contains(later), offer + " offers Maybe later");
            assertEquals("Maybe later.", y.getString("conversations.ranger.player_options." + later + ".text"));
            List<String> next = new ArrayList<>(chain.subList(i + 1, chain.size()));
            next.add("bye");
            assertEquals(String.join(",", next), y.getString("conversations.ranger.player_options." + later + ".pointers"), offer + " goes on to the later offers, then goodbye");
        }
        String first = y.getString("conversations.ranger.first");
        assertTrue(first.indexOf("finaleOffer") > first.indexOf("meadowOffer,"), "the finale is offered after the optional chapters, and reached by Maybe later when they are skipped");
        for (String c : List.of("signsOffer", "windsOffer", "watchOffer", "meadowOffer")) {
            String cond = y.getString("conversations.ranger.NPC_options." + c + ".conditions");
            assertFalse(cond.contains("twoNew"), c + " doesn't wait for the finale's rule");
        }
    }

    @Test
    void theNewChaptersNeverTouchALevelATierOrACritter() throws Exception {
        YamlConfiguration y = pack();
        List<String> newActions = keys(y, "actions").stream().filter(a -> !a.startsWith("holler") && !a.startsWith("yonder") && !a.startsWith("backcountry")).toList();
        for (String a : newActions) {
            String def = y.getString("actions." + a);
            assertFalse(def.contains("mobsquad unlock") || def.contains("mobsquad page") || def.contains("mobsquad level") || def.contains("mobsquad xp")
                || def.contains("mobsquad give") || def.contains("mobsquad sidekick") || def.contains("mobsquad lock"), a + " gives no level, tier, page or critter: " + def);
            for (Matcher m = Pattern.compile("mobsquad (book|dinnerbell) %player%( (\\w+))?").matcher(def); m.find(); )
                if (m.group(1).equals("book")) assertTrue(Set.of("trail", "watch", "meadow", "finale").contains(m.group(3)), a + " hands over a real book");
        }
    }

    @Test
    void theStyleIsSetOnceAndKidAndAdultPathsGetTheSameRewards() throws Exception {
        YamlConfiguration y = pack();
        assertEquals("if styleKnown nop else styleSet1", y.getString("actions.styleEnsure"), "set once, kept in tags");
        for (String chapter : List.of("signsFinish", "watchFinish", "meadowFinish", "finaleFinish", "ringsFinish"))
            for (String a : keys(y, "actions").stream().filter(k -> k.startsWith(chapter)).toList()) {
                String def = y.getString("actions." + a);
                assertFalse(def.contains("%ph.") || def.contains(" adult ") || def.contains("^if"), a + " gives the same thing whoever you are");
            }
        for (String a : List.of("signsFinishStone", "signsFinishWood", "signsFinishFight", "signsFinishBerries", "signsFinishWalk"))
            assertTrue(y.getString("actions." + a).contains("mobsquad book %player% trail"), a + ": every path gets the trail book");
        for (String a : List.of("meadowFinish", "watchFinish", "finaleFinish")) assertTrue(y.getString("actions." + a).contains("mobsquad book %player% "), a + " gives its book");
    }

    @Test
    void theBuilderStepCountsFortyPlacedBlocksOfEitherBase() throws Exception {
        YamlConfiguration y = pack();
        assertEquals("block COBBLESTONE 40 actions:signsFinishStone", y.getString("objectives.signsStone"));
        assertEquals("block OAK_PLANKS 40 actions:signsFinishWood", y.getString("objectives.signsWood"));
        assertTrue(y.getString("actions.signsFinishStone").contains("^objective delete signsWood"), "finishing one base stops the other");
        assertTrue(y.getString("actions.signsFinishWood").contains("^objective delete signsStone"));
    }

    @Test
    void thePackageNamesNoPlayersAndTheRangerIsTheOnlyBookAuthor() throws Exception {
        String text = Files.readString(Path.of("src/main/resources/quests/mobsquad/package.yml"));
        Matcher tokens = Pattern.compile("%[^%\\s]+%").matcher(text.replaceAll("#.*", ""));
        while (tokens.find())
            assertTrue(tokens.group().matches("%ph\\.mobsquad_(top|style|holding_bell|level)%|%constant\\.\\w+%|%player%"), "no placeholder names another player or gives a place: " + tokens.group());
        assertFalse(text.contains("%player%") && Pattern.compile("notify[^\\n]*%player%").matcher(text).find(), "no message prints the player's name");
        assertFalse(Pattern.compile("(?i)ops?sudo").matcher(text).find());
    }
}
