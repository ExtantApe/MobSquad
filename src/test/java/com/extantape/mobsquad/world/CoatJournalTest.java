package com.extantape.mobsquad.world;

import static org.junit.jupiter.api.Assertions.*;

import com.extantape.mobsquad.model.CoatData;
import com.extantape.mobsquad.model.Critter;
import com.extantape.mobsquad.model.Growth;
import com.extantape.mobsquad.model.Species;
import com.extantape.mobsquad.model.Wrangler;
import java.util.UUID;
import java.util.logging.Logger;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

class CoatJournalTest {
    static CoatData data() {
        return CoatData.parse("coats:\n  1: { name: Ember, color: red }\n  2: { name: Marigold, color: gold }\n  3: { name: Sunbeam, color: yellow }\n"
            + "  4: { name: Meadow, color: green }\n  5: { name: Pine, color: dark_green }\n  10: { name: Starlight, color: white }\n",
            new CoatData.Validator() {
                @Override public boolean particle(String key) { return true; }
                @Override public boolean variant(String species, String key) { return true; }
            }, Logger.getAnonymousLogger(), CoatData.EMPTY);
    }

    static String text(Component c) {
        return PlainTextComponentSerializer.plainText().serialize(c);
    }

    @Test
    void earnedCoatsAreNamedAndLockedOnesSayTheirLevel() {
        String page = text(Journal.coatsPage(45, data()));
        assertTrue(page.contains("1. Ember"), page);
        assertTrue(page.contains("4. Meadow"), page);
        assertTrue(page.contains("5. Level 50"), "locked: " + page);
        assertTrue(page.contains("9. Level 90"), page);
        assertTrue(page.contains("10. Level 100"), page);
        assertFalse(page.contains("Pine") || page.contains("Starlight"), "a locked coat's name stays hidden");
    }

    @Test
    void aCritterBelowLevelTenHasEveryCoatLocked() {
        String page = text(Journal.coatsPage(9, data()));
        assertTrue(page.contains("1. Level 10"), page);
        assertFalse(page.contains("Ember"), page);
    }

    @Test
    void theTopLevelShowsAllTen() {
        String page = text(Journal.coatsPage(100, data()));
        assertTrue(page.contains("10. Starlight"), page);
        assertFalse(page.contains("Level 100"), "nothing left locked: " + page);
    }

    @Test
    void thePageFitsTheBookAndTheOldLineIsGone() {
        String page = text(Journal.coatsPage(1, data()));
        long wrapped = page.lines().mapToLong(l -> Math.max(1, (l.length() + 18) / 19)).sum(); // about 19 characters a line
        assertTrue(wrapped <= 14, "a book page holds 14 lines: " + wrapped);
        Wrangler w = new Wrangler(UUID.randomUUID());
        w.critters.add(new Critter(UUID.randomUUID(), Species.CHICKEN, "Clucky", 45, Growth.xpFor(45), 0, 0));
        Book book = Journal.page(Species.CHICKEN, w, data());
        assertEquals(4, book.pages().size(), "its page, how to coax it, its special and its coats");
        for (Component p : book.pages()) assertFalse(text(p).contains("come later"));
        assertTrue(text(book.pages().get(3)).contains("4. Meadow"));
    }
}
