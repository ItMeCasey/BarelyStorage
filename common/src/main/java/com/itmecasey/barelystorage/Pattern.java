package com.itmecasey.barelystorage;

import java.util.ArrayList;
import java.util.List;

/** An encoded recipe: what one run consumes and what it produces. Crafting patterns run in an Assembler, processing patterns in an outside machine. */
public record Pattern(Kind kind, List<Stack> inputs, List<Stack> outputs) {
    public enum Kind { CRAFTING, PROCESSING }

    public record Stack(ItemKey key, int count) {
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
            out.add(new Stack(ItemKey.decode(part.substring(star + 1)), Integer.parseInt(part.substring(0, star))));
        }
        return out;
    }
}
