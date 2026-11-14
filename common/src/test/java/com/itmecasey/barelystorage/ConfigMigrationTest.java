package com.itmecasey.barelystorage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The first start with the new .toml files, against the real files every released version wrote (copied from the tagged
 * builds): the values land in the toml, the old file becomes a .back with a header line, and anything that cannot be migrated
 * cleanly stays where it is.
 */
class ConfigMigrationTest {
    static final String[] RELEASES = {"v2026.11.1", "v2026.11.2", "v2026.11.3", "v2026.11.4"};
    static final String VERSION = "2026.11.5";

    @TempDir
    Path dir;

    static byte[] fixture(String name) throws IOException {
        try (InputStream in = ConfigMigrationTest.class.getResourceAsStream("/config-fixtures/" + name)) {
            assertTrue(in != null, "missing fixture " + name);
            return in.readAllBytes();
        }
    }

    /** What the old code itself would have read: every known key of the file, rounded like the old Settings.getInt did. */
    static Map<String, Integer> expected(byte[] old, List<Settings.Def> defs) throws IOException {
        Properties p = new Properties();
        p.load(new java.io.StringReader(new String(old, StandardCharsets.UTF_8)));
        Map<String, Integer> out = new LinkedHashMap<>();
        for (Settings.Def d : defs) {
            String v = p.getProperty(d.key());
            out.put(d.key(), v == null ? d.fallback() : (int) Math.round(Double.parseDouble(v.trim())));
        }
        return out;
    }

    static Map<String, Integer> defaults(List<Settings.Def> defs) {
        Map<String, Integer> out = new LinkedHashMap<>();
        for (Settings.Def d : defs) {
            out.put(d.key(), d.fallback());
        }
        return out;
    }

    Path write(String name, byte[] bytes) throws IOException {
        Path f = dir.resolve(name);
        Files.write(f, bytes);
        return f;
    }

    boolean seed(Path old, Path toml, List<Settings.Def> defs, List<String> log) {
        return ConfigMigration.seed(old, toml, defs, "BarelyStorage", VERSION, log::add);
    }

    static byte[] withHeader(String tomlName, byte[] original) {
        String header = "# Migrated by BarelyStorage " + VERSION + " on " + LocalDate.now() + " to config/" + tomlName
                + "; this file is no longer read. Kept as a backup.\n";
        byte[] h = header.getBytes(StandardCharsets.UTF_8);
        byte[] all = new byte[h.length + original.length];
        System.arraycopy(h, 0, all, 0, h.length);
        System.arraycopy(original, 0, all, h.length, original.length);
        return all;
    }

    /** The success outcome: toml with the values, a .back with header plus the original bytes, and no original file. */
    void assertMigrated(Path old, Path toml, byte[] original, Map<String, Integer> want, String why) throws IOException {
        assertEquals(want, ConfigMigration.parseToml(Files.readString(toml)), why);
        Path back = old.resolveSibling(old.getFileName() + ".back");
        assertTrue(Files.exists(back), why + ": no .back");
        assertArrayEquals(withHeader(toml.getFileName().toString(), original), Files.readAllBytes(back), why);
        assertFalse(Files.exists(old), why + ": the original is still there");
    }

    /** The failure outcome: the original stays byte for byte and no backup is made. */
    void assertKept(Path old, byte[] original, String why) throws IOException {
        assertArrayEquals(original, Files.readAllBytes(old), why);
        try (var files = Files.list(dir)) {
            assertEquals(0, files.filter(f -> f.getFileName().toString().contains(".back")).count(), why + ": a backup was made");
        }
    }

    @Test
    void everyReleasedCommonFileIsMovedToTheTomlAndKeptAsBack() throws IOException {
        for (String release : RELEASES) {
            for (String kind : new String[] {"default", "edited"}) {
                byte[] bytes = fixture(release + "-barelystorage-" + kind + ".properties");
                Path sub = Files.createDirectories(dir.resolve(release + kind));
                Path old = sub.resolve("barelystorage.properties");
                Files.write(old, bytes);
                Path toml = sub.resolve("barelystorage-common.toml");
                List<String> log = new ArrayList<>();
                assertTrue(seed(old, toml, Settings.defs(), log), release + " " + kind);
                Map<String, Integer> want = expected(bytes, Settings.defs());
                assertEquals(32, want.size());
                assertMigrated(old, toml, bytes, want, release + " " + kind);
                String line = log.get(log.size() - 1);
                assertTrue(line.contains(old.toString()) && line.contains(old + ".back") && line.contains(toml.toString()), line);
            }
        }
    }

    @Test
    void editedValuesAreTheOnesTheUserSetNotTheDefaults() throws IOException {
        byte[] bytes = fixture("v2026.11.1-barelystorage-edited.properties");
        Path toml = dir.resolve("edited.toml");
        seed(write("edited.properties", bytes), toml, Settings.defs(), new ArrayList<>());
        Map<String, Integer> got = ConfigMigration.parseToml(Files.readString(toml));
        assertEquals(1000000, got.get("power.buffer"));
        assertEquals(12000, got.get("power.max_receive"));
        assertEquals(0, got.get("power.base_per_tick"));
        assertEquals(16384, got.get("tier.iron.capacity"));
        assertEquals(2000000000, got.get("tier.quantum.capacity"));
        assertEquals(100, got.get("tier.gold.types"));
        assertEquals(6, got.get("rack.drive_bays"));
        assertEquals(2, got.get("rack.blade_slots"));
        assertEquals(2, got.get("hardware.basic.depth"));
        assertEquals(64, got.get("crafting.max_batch"));
        assertEquals(4, got.get("nodes.interval_ticks"));
        assertEquals(128, got.get("nodes.items_per_move"));
        assertEquals(65536, got.get("tier.steel.capacity"));
    }

    @Test
    void everyReleasedClientFileIsMovedToTheTomlAndKeptAsBack() throws IOException {
        for (String release : RELEASES) {
            byte[] bytes = fixture(release + "-barelystorage-client.properties");
            Path sub = Files.createDirectories(dir.resolve(release + "-client"));
            Path old = sub.resolve("barelystorage-client.properties");
            Files.write(old, bytes);
            Path toml = sub.resolve("barelystorage-client.toml");
            assertTrue(seed(old, toml, ConfigMigration.CLIENT, new ArrayList<>()));
            assertMigrated(old, toml, bytes, Map.of("theme", 1, "height", 2, "sort", 1, "order", 1, "view", 2), release);
            assertEquals(expected(bytes, ConfigMigration.CLIENT), ConfigMigration.parseToml(Files.readString(toml)));
        }
    }

    @Test
    void anExistingTomlIsNeverOverwrittenAndNothingIsMoved() throws IOException {
        byte[] bytes = fixture("v2026.11.4-barelystorage-edited.properties");
        Path old = write("barelystorage.properties", bytes);
        Path toml = dir.resolve("barelystorage-common.toml");
        Files.writeString(toml, "[power]\n\tbuffer = 123\n");
        assertFalse(seed(old, toml, Settings.defs(), new ArrayList<>()));
        assertEquals("[power]\n\tbuffer = 123\n", Files.readString(toml));
        assertKept(old, bytes, "existing toml");
    }

    @Test
    void withoutAnOldFileNothingIsWritten() {
        Path other = dir.resolve("other.toml");
        assertFalse(seed(dir.resolve("none.properties"), other, Settings.defs(), new ArrayList<>()));
        assertFalse(Files.exists(other));
    }

    @Test
    void aSecondLaunchDoesNothing() throws IOException {
        byte[] bytes = fixture("v2026.11.4-barelystorage-edited.properties");
        Path old = write("barelystorage.properties", bytes);
        Path toml = dir.resolve("barelystorage-common.toml");
        assertTrue(seed(old, toml, Settings.defs(), new ArrayList<>()));
        Path back = dir.resolve("barelystorage.properties.back");
        byte[] backBytes = Files.readAllBytes(back);
        Files.writeString(toml, Files.readString(toml).replace("buffer = 1000000", "buffer = 777"));
        // The user puts an old-style file there again (a downgrade and back): the toml exists, so nothing happens.
        Files.writeString(old, "power.buffer=5\n");
        List<String> log = new ArrayList<>();
        assertFalse(seed(old, toml, Settings.defs(), log));
        assertTrue(log.isEmpty());
        assertEquals(777, ConfigMigration.parseToml(Files.readString(toml)).get("power.buffer"));
        assertArrayEquals(backBytes, Files.readAllBytes(back));
        assertEquals("power.buffer=5\n", Files.readString(old));
        assertFalse(Files.exists(dir.resolve("barelystorage.properties.back2")));
    }

    @Test
    void anExistingBackIsNeverOverwritten() throws IOException {
        byte[] bytes = fixture("v2026.11.3-barelystorage-edited.properties");
        Path old = write("barelystorage.properties", bytes);
        Path back1 = write("barelystorage.properties.back", "first backup".getBytes(StandardCharsets.UTF_8));
        Path back2 = write("barelystorage.properties.back2", "second backup".getBytes(StandardCharsets.UTF_8));
        Path toml = dir.resolve("barelystorage-common.toml");
        assertTrue(seed(old, toml, Settings.defs(), new ArrayList<>()));
        assertEquals("first backup", Files.readString(back1));
        assertEquals("second backup", Files.readString(back2));
        assertArrayEquals(withHeader("barelystorage-common.toml", bytes), Files.readAllBytes(dir.resolve("barelystorage.properties.back3")));
        assertFalse(Files.exists(old));
    }

    @Test
    void anEmptyOldFileIsMigratedWithTheDefaults() throws IOException {
        Path old = write("empty.properties", new byte[0]);
        Path toml = dir.resolve("empty.toml");
        assertTrue(seed(old, toml, Settings.defs(), new ArrayList<>()));
        assertMigrated(old, toml, new byte[0], defaults(Settings.defs()), "empty");
    }

    @Test
    void aPartlyFilledOldFileChangesOnlyTheKeysThatAreThere() throws IOException {
        byte[] bytes = "power.buffer=900000\nrack.drive_bays=5\n".getBytes(StandardCharsets.UTF_8);
        Path old = write("part.properties", bytes);
        Path toml = dir.resolve("part.toml");
        assertTrue(seed(old, toml, Settings.defs(), new ArrayList<>()));
        Map<String, Integer> expect = defaults(Settings.defs());
        expect.put("power.buffer", 900000);
        expect.put("rack.drive_bays", 5);
        assertMigrated(old, toml, bytes, expect, "partial");
    }

    @Test
    void aCorruptOldFileStaysWhereItIsAndGetsNoBackup() throws IOException {
        byte[] junk = new byte[256];
        for (int i = 0; i < junk.length; i++) {
            junk[i] = (byte) (0x80 + (i * 7) % 100);
        }
        List<String> log = new ArrayList<>();
        Path old = write("junk.properties", junk);
        Path toml = dir.resolve("junk.toml");
        assertTrue(seed(old, toml, Settings.defs(), log));
        assertEquals(defaults(Settings.defs()), ConfigMigration.parseToml(Files.readString(toml)));
        assertKept(old, junk, "binary junk");
        assertTrue(log.stream().anyMatch(l -> l.contains("not valid text")), log.toString());
        assertTrue(log.stream().anyMatch(l -> l.contains("stays where it is")), log.toString());

        byte[] escape = "power.buffer=\\u00zz\n".getBytes(StandardCharsets.UTF_8);
        Path old2 = write("esc.properties", escape);
        Path toml2 = dir.resolve("esc.toml");
        log.clear();
        assertTrue(seed(old2, toml2, Settings.defs(), log));
        assertEquals(defaults(Settings.defs()), ConfigMigration.parseToml(Files.readString(toml2)));
        assertKept(old2, escape, "bad escape");
        assertTrue(log.stream().anyMatch(l -> l.contains("could not read")), log.toString());
    }

    @Test
    void aPartlyReadableOldFileStaysAndTheReadableValuesAreStillUsed() throws IOException {
        byte[] bytes = "power.buffer=lots\npower.max_receive=9000\nunknown.key=3\n".getBytes(StandardCharsets.UTF_8);
        List<String> log = new ArrayList<>();
        Path old = write("odd.properties", bytes);
        Path toml = dir.resolve("odd.toml");
        assertTrue(seed(old, toml, Settings.defs(), log));
        Map<String, Integer> got = ConfigMigration.parseToml(Files.readString(toml));
        assertEquals(400000, got.get("power.buffer"));
        assertEquals(9000, got.get("power.max_receive"));
        assertKept(old, bytes, "partly readable");
        String all = String.join("\n", log);
        assertTrue(all.contains("power.buffer=lots is not a number"), all);
        assertTrue(all.contains("ignored unknown key unknown.key"), all);
        assertTrue(all.contains("partly readable"), all);
    }

    @Test
    void outOfRangeValuesAreClampedLoggedAndStillMigrated() throws IOException {
        byte[] bytes = ("rack.drive_bays=2\nrack.blade_slots=99\npower.efficiency_percent=250\ntier.iron.capacity=-5\n"
                + "nodes.interval_ticks=7.6\ncrafting.max_batch=99999999999\n").getBytes(StandardCharsets.UTF_8);
        List<String> log = new ArrayList<>();
        Path old = write("range.properties", bytes);
        Path toml = dir.resolve("range.toml");
        assertTrue(seed(old, toml, Settings.defs(), log));
        Map<String, Integer> expect = defaults(Settings.defs());
        expect.put("rack.drive_bays", 4);
        expect.put("rack.blade_slots", 4);
        expect.put("power.efficiency_percent", 100);
        expect.put("tier.iron.capacity", 1);
        expect.put("nodes.interval_ticks", 8);
        expect.put("crafting.max_batch", Integer.MAX_VALUE);
        assertMigrated(old, toml, bytes, expect, "clamped");
        assertTrue(String.join("\n", log).contains("rack.drive_bays=2 is outside 4 to 8, using 4"), log.toString());
    }

    @Test
    void ifTheTomlCannotBeWrittenTheOriginalStays() throws IOException {
        byte[] bytes = fixture("v2026.11.4-barelystorage-edited.properties");
        Path old = write("barelystorage.properties", bytes);
        Path blocker = write("not-a-folder", new byte[] {1});
        Path toml = blocker.resolve("barelystorage-common.toml");
        List<String> log = new ArrayList<>();
        assertFalse(seed(old, toml, Settings.defs(), log));
        assertKept(old, bytes, "toml not writable");
        assertTrue(log.stream().anyMatch(l -> l.contains("could not write")), log.toString());
    }

    @Test
    void theTomlUsesTheDocumentedLayoutAndEveryKeyIsInIt() {
        String toml = ConfigMigration.toml(Settings.defs(), new LinkedHashMap<>());
        assertTrue(toml.contains("[power]\n"));
        assertTrue(toml.contains("[tier.iron]\n"));
        assertTrue(toml.contains("\t# Range: > 1\n"));
        assertTrue(toml.contains("\t# Range: 4 ~ 8\n"));
        assertEquals(32, ConfigMigration.parseToml(toml).size());
        // A table is written once: TOML does not allow reopening one.
        assertEquals(1, toml.split("\\[tier\\.iron\\]", -1).length - 1);
        assertEquals(5, ConfigMigration.parseToml(ConfigMigration.toml(ConfigMigration.CLIENT, new LinkedHashMap<>())).size());
    }
}
