package com.extantape.mobsquad.store;

import com.extantape.mobsquad.model.Critter;
import com.extantape.mobsquad.model.Species;
import com.extantape.mobsquad.model.Wrangler;
import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Each player's MobSquad in a plain text file of its own, {@code players/<uuid>.txt}:
 *
 * <pre>
 * wranglin 150
 * critter 6b1f...  chicken 1 0 0 Clucky
 * page chicken
 * sidekick 6b1f...
 * sidekick-roll 20407
 * </pre>
 *
 * A critter line is its id, species, level, experience, when it's rested (epoch milliseconds, 0 when ready) and its
 * name, URL-encoded, then its minutes out with its owner (missing in files from before 0.5.0, read as 0). A page line is a La Ménagerie page the player has opened; owning a critter opens its page too. Written to a temporary file and moved into place, so a crash leaves the old file or the new one.
 * Lines that can't be read are skipped and counted, never guessed at.
 */
public final class Store {
    private final Path dir;

    public Store(Path dir) {
        this.dir = dir;
    }

    public record Loaded(Wrangler wrangler, int skipped) {}

    private Path file(UUID player) {
        return dir.resolve(player + ".txt");
    }

    public Loaded load(UUID player) throws IOException {
        Wrangler w = new Wrangler(player);
        Path f = file(player);
        if (!Files.exists(f)) return new Loaded(w, 0);
        int skipped = 0;
        for (String line : Files.readAllLines(f, StandardCharsets.UTF_8)) {
            line = line.strip();
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] p = line.split(" ");
            try {
                switch (p[0]) {
                    case "wranglin" -> w.wranglinXp = Long.parseLong(p[1]);
                    case "critter" -> {
                        Species s = Species.byId(p[2]);
                        if (s == null || p.length < 7) throw new IllegalArgumentException();
                        w.critters.add(new Critter(UUID.fromString(p[1]), s, URLDecoder.decode(p[6], StandardCharsets.UTF_8),
                            Integer.parseInt(p[3]), Long.parseLong(p[4]), Long.parseLong(p[5]), p.length > 7 ? Long.parseLong(p[7]) : 0));
                    }
                    case "sidekick" -> w.sidekick = UUID.fromString(p[1]);
                    case "sidekick-roll" -> w.sidekickRoll = Long.parseLong(p[1]);
                    case "page" -> {
                        Species s = Species.byId(p[1]);
                        if (s == null) throw new IllegalArgumentException();
                        w.pages.add(s);
                    }
                    default -> skipped++;
                }
            } catch (RuntimeException e) {
                skipped++;
            }
        }
        for (Critter c : w.critters) w.pages.add(c.species());
        if (w.sidekick != null && w.find(w.sidekick) == null) w.sidekick = null; // never a sidekick that isn't in the list // files from before 0.4.0 have no page lines
        return new Loaded(w, skipped);
    }

    public void save(Wrangler w) throws IOException {
        Files.createDirectories(dir);
        StringBuilder sb = new StringBuilder("# MobSquad: one player's critters. Plain text; don't edit it while the server is running.\n");
        sb.append("wranglin ").append(w.wranglinXp).append('\n');
        for (Critter c : w.critters)
            sb.append("critter ").append(c.id()).append(' ').append(c.species().id()).append(' ').append(c.level()).append(' ')
                .append(c.xp()).append(' ').append(c.tuckeredUntil()).append(' ')
                .append(URLEncoder.encode(c.name(), StandardCharsets.UTF_8)).append(' ').append(c.minutes()).append('\n');
        if (w.sidekick != null) sb.append("sidekick ").append(w.sidekick).append('\n');
        if (w.sidekickRoll > 0) sb.append("sidekick-roll ").append(w.sidekickRoll).append('\n');
        for (Species s : w.pages) sb.append("page ").append(s.id()).append('\n');
        Path tmp = dir.resolve(w.player + ".txt.tmp");
        Files.writeString(tmp, sb.toString(), StandardCharsets.UTF_8);
        Files.move(tmp, file(w.player), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }
}
