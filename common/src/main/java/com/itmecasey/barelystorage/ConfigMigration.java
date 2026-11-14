package com.itmecasey.barelystorage;

import java.io.IOException;
import java.io.Reader;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.io.StringReader;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.function.Consumer;

/**
 * Brings the values of the old hand-written config files over to the loader's {@code .toml} files.
 *
 * <p>Every released version (2026.11.1 to 2026.11.4) wrote {@code config/barelystorage.properties} (the 32 numbers of
 * {@link Settings}) and {@code config/barelystorage-client.properties} (the five screen choices). No key was ever renamed.
 * On the first start where a {@code .toml} does not exist yet and the old file does, {@link #seed} writes the {@code .toml}
 * with the old values in it, before the loader reads it. After a verified move the old file becomes
 * {@code <name>.back} (a header line plus the original bytes); a file that cannot be migrated cleanly stays where it is. A
 * {@code .toml} that already exists is never touched.
 */
public final class ConfigMigration {
    /** The screen choices the client keeps: dark or light theme, terminal height, sort, order and view. */
    public static final List<Settings.Def> CLIENT = List.of(
            new Settings.Def("theme", 0, 0, 1, "Terminal color theme: 0 dark, 1 light."),
            new Settings.Def("height", 0, 0, 2, "Terminal height: 0 small, 1 medium, 2 large."),
            new Settings.Def("sort", 0, 0, 2, "Terminal sort: 0 quantity, 1 mod name, 2 item name."),
            new Settings.Def("order", 0, 0, 1, "Terminal sort order: 0 the usual order, 1 reversed."),
            new Settings.Def("view", 0, 0, 2, "Terminal list view: 0 crafting and available, 1 available only, 2 craftable only."));

    public static final String COMMON_OLD = "barelystorage.properties";
    public static final String COMMON_TOML = "barelystorage-common.toml";
    public static final String CLIENT_OLD = "barelystorage-client.properties";
    public static final String CLIENT_TOML = "barelystorage-client.toml";

    /** The values read from an old file (a value for every key of the list) and what happened to each odd one. */
    public record Result(Map<String, Integer> values, List<String> log, int imported, boolean clean) {
    }

    private ConfigMigration() {
    }

    /** Reads an old properties file against a list of settings. Missing, empty and unreadable files give the defaults. */
    public static Result read(Path old, List<Settings.Def> defs) {
        try {
            return read(Files.readAllBytes(old), old.getFileName().toString(), defs);
        } catch (IOException | RuntimeException e) {
            return new Result(defaults(defs), new ArrayList<>(List.of("could not read " + old.getFileName() + " (" + e + ")")), 0, false);
        }
    }

    static Result read(byte[] bytes, String name, List<Settings.Def> defs) {
        Map<String, Integer> values = defaults(defs);
        List<String> log = new ArrayList<>();
        Properties p = new Properties();
        try {
            String text = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
            try (Reader r = new StringReader(text)) {
                p.load(r);
            }
        } catch (CharacterCodingException e) {
            log.add(name + " is not valid text, every setting keeps its default");
            return new Result(values, log, 0, false);
        } catch (IOException | RuntimeException e) {
            log.add("could not read " + name + " (" + e + "), every setting keeps its default");
            return new Result(values, log, 0, false);
        }
        boolean clean = true;
        int imported = 0;
        for (Settings.Def d : defs) {
            String text = p.getProperty(d.key());
            if (text == null) {
                continue;
            }
            double number;
            try {
                number = Double.parseDouble(text.trim());
            } catch (NumberFormatException e) {
                number = Double.NaN;
            }
            if (Double.isNaN(number) || Double.isInfinite(number)) {
                log.add(d.key() + "=" + text.trim() + " is not a number, using the default " + d.fallback());
                clean = false;
                continue;
            }
            long whole = Math.round(number);
            long clamped = Math.max(d.min(), Math.min(d.max(), whole));
            if (clamped != whole) {
                log.add(d.key() + "=" + text.trim() + " is outside " + d.min() + " to " + d.max() + ", using " + clamped);
            }
            values.put(d.key(), (int) clamped);
            imported++;
        }
        for (String key : new java.util.TreeSet<>(p.stringPropertyNames())) {
            boolean known = false;
            for (Settings.Def d : defs) {
                known |= d.key().equals(key);
            }
            if (!known) {
                log.add("ignored unknown key " + key);
            }
        }
        return new Result(values, log, imported, clean);
    }

    /** The mod version the loader expanded into its mods.toml, or "unknown". */
    public static String modVersion() {
        for (String name : new String[] {"META-INF/neoforge.mods.toml", "META-INF/mods.toml"}) {
            try (InputStream in = ConfigMigration.class.getResourceAsStream("/" + name)) {
                if (in != null) {
                    String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                    int own = text.indexOf("\"" + BarelyStorage.MODID + "\"");
                    Matcher m = Pattern.compile("(?m)^\\s*version\\s*=\\s*\"([^\"]+)\"").matcher(text);
                    if (own >= 0 && m.find(own)) {
                        return m.group(1);
                    }
                }
            } catch (IOException | RuntimeException e) {
                // Try the next name.
            }
        }
        return "unknown";
    }

    /**
     * Moves the settings of {@code old} into {@code toml} when the {@code toml} does not exist and the old file does. Steps, in
     * order: write the toml; read it back and check every value; check that the old file parsed cleanly; write the backup
     * ({@code old + ".back"}, or {@code .back2}, {@code .back3} when one exists) with a header line and the exact original bytes;
     * check the backup; only then remove the original. If a step fails, or the old file is corrupt or only partly readable, the
     * original stays where it is (the toml still gets the readable values and the defaults) and no backup is made. Never
     * overwrites an existing toml or backup. Returns whether the toml was written.
     */
    public static boolean seed(Path old, Path toml, List<Settings.Def> defs, String modName, String version, Consumer<String> log) {
        if (Files.exists(toml) || !Files.isRegularFile(old)) {
            return false;
        }
        byte[] original;
        try {
            original = Files.readAllBytes(old);
        } catch (IOException | RuntimeException e) {
            log.accept("could not read " + old + " (" + e + "); it stays where it is and the defaults are used");
            return write(toml, defs, defaults(defs), log);
        }
        Result r = read(original, old.getFileName().toString(), defs);
        if (!write(toml, defs, r.values(), log)) {
            return false;
        }
        for (String line : r.log()) {
            log.accept(old.getFileName() + ": " + line);
        }
        // 2. The toml reads back with every migrated value.
        try {
            if (!parseToml(Files.readString(toml, StandardCharsets.UTF_8)).equals(r.values())) {
                log.accept("the new " + toml.getFileName() + " does not hold the migrated values; " + old + " stays where it is");
                return true;
            }
        } catch (IOException | RuntimeException e) {
            log.accept("could not check " + toml + " (" + e + "); " + old + " stays where it is");
            return true;
        }
        // 3. The old file parsed cleanly.
        if (!r.clean()) {
            log.accept(old.getFileName() + " is corrupt or only partly readable, so it stays where it is (unreadable settings use their defaults)");
            return true;
        }
        // 4. and 5. The backup: header line plus the exact original bytes, then checked.
        String header = "# Migrated by " + modName + " " + version + " on " + LocalDate.now() + " to config/" + toml.getFileName()
                + "; this file is no longer read. Kept as a backup.\n";
        byte[] headerBytes = header.getBytes(StandardCharsets.UTF_8);
        byte[] expected = new byte[headerBytes.length + original.length];
        System.arraycopy(headerBytes, 0, expected, 0, headerBytes.length);
        System.arraycopy(original, 0, expected, headerBytes.length, original.length);
        Path back = null;
        for (int n = 1; n < 1000 && back == null; n++) {
            Path candidate = old.resolveSibling(old.getFileName() + (n == 1 ? ".back" : ".back" + n));
            if (!Files.exists(candidate)) {
                back = candidate;
            }
        }
        if (back == null) {
            log.accept("no free .back name next to " + old + "; it stays where it is");
            return true;
        }
        try {
            Files.write(back, expected, StandardOpenOption.CREATE_NEW);
            if (!java.util.Arrays.equals(expected, Files.readAllBytes(back))) {
                log.accept("the backup " + back + " does not match " + old + "; the original stays where it is");
                return true;
            }
            // 6. Only now the original goes.
            Files.delete(old);
        } catch (IOException | RuntimeException e) {
            log.accept("could not finish moving " + old + " to " + back + " (" + e + "); the original stays where it is");
            return true;
        }
        log.accept("migrated " + r.imported() + " of " + defs.size() + " settings: " + old + " -> " + back + ", values now in " + toml);
        return true;
    }

    private static Map<String, Integer> defaults(List<Settings.Def> defs) {
        Map<String, Integer> out = new LinkedHashMap<>();
        for (Settings.Def d : defs) {
            out.put(d.key(), d.fallback());
        }
        return out;
    }

    private static boolean write(Path toml, List<Settings.Def> defs, Map<String, Integer> values, Consumer<String> log) {
        try {
            if (toml.getParent() != null) {
                Files.createDirectories(toml.getParent());
            }
            Files.writeString(toml, toml(defs, values), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
            return true;
        } catch (FileAlreadyExistsException e) {
            if (!Files.exists(toml) || !Files.isRegularFile(toml)) {
                log.accept("could not write " + toml + ": " + e);
            }
            return false;
        } catch (IOException | RuntimeException e) {
            log.accept("could not write " + toml + ": " + e);
            return false;
        }
    }

    /** The {@code .toml} text for a list of settings: one table per dotted prefix, a comment and a range line above each value. */
    public static String toml(List<Settings.Def> defs, Map<String, Integer> values) {
        Map<String, List<Settings.Def>> tables = new LinkedHashMap<>();
        for (Settings.Def d : defs) {
            int dot = d.key().lastIndexOf('.');
            tables.computeIfAbsent(dot < 0 ? "" : d.key().substring(0, dot), k -> new ArrayList<>()).add(d);
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, List<Settings.Def>> t : tables.entrySet()) {
            if (!t.getKey().isEmpty()) {
                sb.append('[').append(t.getKey()).append("]\n");
            }
            for (Settings.Def d : t.getValue()) {
                String name = d.key().substring(d.key().lastIndexOf('.') + 1);
                sb.append("\t#").append(d.comment()).append('\n');
                // The comment layout of the loaders' own writers, so they find nothing to correct when they first load the file.
                sb.append("\t# Default: ").append(d.fallback()).append('\n');
                String range = d.max() == Integer.MAX_VALUE ? "> " + d.min() : d.min() + " ~ " + d.max();
                sb.append("\t# Range: ").append(range).append('\n');
                sb.append('\t').append(name).append(" = ").append(values.getOrDefault(d.key(), d.fallback())).append('\n');
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    /** Reads the whole-number values of a {@code .toml} like the ones above (and the loaders write): dotted table names plus key. */
    public static Map<String, Integer> parseToml(String text) {
        Map<String, Integer> out = new LinkedHashMap<>();
        String table = "";
        for (String raw : text.split("\\R")) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            if (line.startsWith("[") && line.endsWith("]")) {
                table = line.substring(1, line.length() - 1).trim();
                continue;
            }
            int eq = line.indexOf('=');
            if (eq > 0) {
                try {
                    out.put((table.isEmpty() ? "" : table + ".") + line.substring(0, eq).trim(), Integer.parseInt(line.substring(eq + 1).trim()));
                } catch (NumberFormatException e) {
                    // Not a whole number: not one of ours.
                }
            }
        }
        return out;
    }
}
