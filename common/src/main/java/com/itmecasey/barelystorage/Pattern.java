package com.itmecasey.barelystorage;

import java.util.ArrayList;
import java.util.List;

/** An encoded recipe: what one run consumes and what it produces. Crafting patterns run in an Assembler, processing patterns in an outside machine. */
public record Pattern(Kind kind, List<Stack> inputs, List<Stack> outputs) {
    public enum Kind { CRAFTING, PROCESSING }

    /** One ingredient or product. {@code alts} are other items that may stand in for {@code key} (items sharing a tag with it). */
    public record Stack(ItemKey key, int count, List<ItemKey> alts) {
        public Stack {
            alts = List.copyOf(alts);
        }

        public Stack(ItemKey key, int count) {
            this(key, count, List.of());
        }

        /** The item itself first, then its stand-ins. */
        public List<ItemKey> candidates() {
            if (alts.isEmpty()) {
                return List.of(key);
            }
            List<ItemKey> all = new ArrayList<>(alts.size() + 1);
            all.add(key);
            all.addAll(alts);
            return all;
        }
    }

    public Pattern {
        inputs = List.copyOf(inputs);
        outputs = List.copyOf(outputs);
    }

    public int outputCount(ItemKey key) {
        int n = 0;
        for (Stack s : outputs) {
            if (s.key().equals(key)) {
                n += s.count();
            }
        }
        return n;
    }

    public boolean makes(ItemKey key) {
        return outputCount(key) > 0;
    }

    /** Text form: kind;count*key,count*key;count*key. Keys contain neither ';' nor ',' (the separators are escaped by the callers' encoders). */
    public String encode() {
        return kind.name() + ";" + join(inputs) + ";" + join(outputs);
    }

    private static String join(List<Stack> stacks) {
        StringBuilder sb = new StringBuilder();
        for (Stack s : stacks) {
            if (sb.length() > 0) {
                sb.append('\u001d');
            }
            sb.append(s.count()).append('*').append(s.key().encode());
            for (ItemKey alt : s.alts()) {
                sb.append('').append(alt.encode());
            }
        }
        return sb.toString();
    }

    public static Pattern decode(String text) {
        String[] parts = text.split(";", -1);
        return new Pattern(Kind.valueOf(parts[0]), split(parts[1]), split(parts[2]));
    }

    private static List<Stack> split(String text) {
        List<Stack> out = new ArrayList<>();
        if (text.isEmpty()) {
            return out;
        }
        for (String part : text.split("\u001d")) {
            int star = part.indexOf('*');
            String[] keys = part.substring(star + 1).split("");
            List<ItemKey> alts = new ArrayList<>();
            for (int i = 1; i < keys.length; i++) {
                alts.add(ItemKey.decode(keys[i]));
            }
            out.add(new Stack(ItemKey.decode(keys[0]), Integer.parseInt(part.substring(0, star)), alts));
        }
        return out;
    }
}
