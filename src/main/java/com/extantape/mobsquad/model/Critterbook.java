package com.extantape.mobsquad.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Critterbook numbers (brief, "La Ménagerie"): every mob in Java Edition in the order it first appeared, read from
 * {@code critterbook.txt} in the jar. Numbers never change.
 */
public final class Critterbook {
    private static final Map<String, Integer> NUMBERS = load();

    private Critterbook() {}

    private static Map<String, Integer> load() {
        Map<String, Integer> m = new LinkedHashMap<>();
        try (InputStream in = Critterbook.class.getResourceAsStream("/critterbook.txt")) {
            if (in == null) throw new IllegalStateException("critterbook.txt is missing from the jar");
            BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            for (String line; (line = r.readLine()) != null; ) {
                int hash = line.indexOf('#');
                String s = (hash < 0 ? line : line.substring(0, hash)).strip();
                if (s.isEmpty()) continue;
                String[] p = s.split("\\s+");
                m.put(p[1], Integer.parseInt(p[0]));
            }
        } catch (IOException e) {
            throw new IllegalStateException("couldn't read critterbook.txt", e);
        }
        return Collections.unmodifiableMap(m);
    }

    /** Every numbered mob, entity id to number, in number order. */
    public static Map<String, Integer> all() {
        return NUMBERS;
    }

    public static int number(Species s) {
        return NUMBERS.get(s.id());
    }

    /** Launch critters in Critterbook order. */
    public static java.util.List<Species> inOrder() {
        java.util.List<Species> l = new java.util.ArrayList<>(java.util.List.of(Species.values()));
        l.sort(java.util.Comparator.comparingInt(Critterbook::number));
        return l;
    }
}
