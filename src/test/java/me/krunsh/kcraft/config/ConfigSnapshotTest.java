package me.krunsh.kcraft.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ConfigSnapshotTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test public void preparedValuesStayInactiveUntilPublicationAndNeedNoFurtherIo() throws Exception {
        File data = validFiles();
        ConfigManager manager = manager(data);
        assertFalse(manager.isLoaded());
        ConfigManager.Snapshot previous = manager.prepareReload();
        assertFalse(manager.isLoaded());
        manager.activate(previous);

        write(data, "config.yml", config("Nouveau", 3));
        write(data, "messages.yml", "messages:\n  ready: 'Prêt à jouer'\n");
        ConfigManager.Snapshot next = manager.prepareReload();
        assertActive(manager, previous);
        assertEquals("Ancien", manager.getPrefix());
        assertEquals("Nouveau", next.getRuntimeConfig().getPrefix());
        assertEquals("Prêt à jouer", next.getMessagesConfig().getString("messages.ready"));

        // Publication only installs values already read and validated.
        Files.delete(new File(data, "config.yml").toPath());
        Files.delete(new File(data, "messages.yml").toPath());
        manager.activate(next);
        assertActive(manager, next);
        assertEquals("Nouveau", manager.getPrefix());
        assertEquals(3, manager.getDebugLevel());
        assertEquals("Prêt à jouer", manager.getMessage("ready"));
    }

    @Test public void malformedMessagesDoNotPublishValidMainConfig() throws Exception {
        File data = validFiles();
        ConfigManager manager = manager(data);
        ConfigManager.Snapshot previous = manager.prepareReload();
        manager.activate(previous);
        write(data, "config.yml", config("Nouveau", 2));
        write(data, "messages.yml", "messages: [unterminated\n");

        IllegalStateException failure = assertThrows(IllegalStateException.class, manager::prepareReload);
        assertTrue(failure.getMessage().contains("messages.yml"));
        assertActive(manager, previous);
        assertEquals("Ancien", manager.getPrefix());
        assertEquals("Ancien message", manager.getMessage("ready"));
    }

    @Test public void malformedMainConfigDoesNotReplaceActiveValues() throws Exception {
        File data = validFiles();
        ConfigManager manager = manager(data);
        ConfigManager.Snapshot previous = manager.prepareReload();
        manager.activate(previous);
        write(data, "config.yml", "schema_version: [unterminated\n");

        IllegalStateException failure = assertThrows(IllegalStateException.class, manager::prepareReload);
        assertTrue(failure.getMessage().contains("config.yml"));
        assertActive(manager, previous);
    }

    @Test public void validationFailureDoesNotReplaceActiveValuesOrReport() throws Exception {
        File data = validFiles();
        ConfigManager manager = manager(data);
        ConfigManager.Snapshot previous = manager.prepareReload();
        manager.activate(previous);
        write(data, "config.yml", config("Nouveau", 9));

        IllegalStateException failure = assertThrows(IllegalStateException.class, manager::prepareReload);
        assertTrue(failure.getMessage().contains("debug-level"));
        assertActive(manager, previous);
        assertTrue(manager.getValidationReport().isValid());
    }

    @Test public void bothMainFilesRejectInvalidUtf8InsteadOfReplacingBytes() throws Exception {
        for (String fileName : new String[] {"config.yml", "messages.yml"}) {
            File data = validFiles();
            Files.write(new File(data, fileName).toPath(), new byte[] {
                'v', 'a', 'l', 'u', 'e', ':', ' ', (byte) 0xc3, (byte) 0x28, '\n'
            });
            IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> ConfigManager.Snapshot.read(new File(data, "config.yml"),
                    new File(data, "messages.yml")));
            assertTrue(failure.getMessage().contains(fileName));
        }
    }

    @Test public void missingMainFilesAreNotReplacedDuringPreparation() throws Exception {
        for (String fileName : new String[] {"config.yml", "messages.yml"}) {
            File data = validFiles();
            ConfigManager manager = manager(data);
            ConfigManager.Snapshot previous = manager.prepareReload();
            manager.activate(previous);
            File missing = new File(data, fileName);
            Files.delete(missing.toPath());

            IllegalStateException failure = assertThrows(IllegalStateException.class, manager::prepareReload);
            assertTrue(failure.getMessage().contains(fileName));
            assertFalse(missing.exists());
            assertActive(manager, previous);
        }
    }

    @Test public void malformedUnreadableAndInvalidUtf8CraftFilesAbortLoading() throws Exception {
        File data = validFiles();
        ConfigManager manager = manager(data);
        File malformed = write(data, "broken.yml", "recipes: [unterminated\n");
        File missing = new File(data, "missing.yml");
        File directory = new File(data, "directory.yml");
        assertTrue(directory.mkdir());
        File invalidUtf8 = new File(data, "bad-encoding.yml");
        Files.write(invalidUtf8.toPath(), new byte[] {'#', (byte) 0xc3, (byte) 0x28});

        for (File invalid : new File[] {malformed, missing, directory, invalidUtf8}) {
            IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> manager.loadCraftConfig(invalid));
            assertTrue(failure.getMessage().contains(invalid.getName()));
        }
    }

    @Test public void unlistableCraftDirectoryIsRejectedButAnEmptyDirectoryIsValid() throws Exception {
        File data = validFiles();
        ConfigManager manager = manager(data);
        assertThrows(IllegalStateException.class, manager::getCraftFiles);
        write(data, "crafts", "not a directory");
        assertThrows(IllegalStateException.class, manager::getCraftFiles);
        Files.delete(new File(data, "crafts").toPath());
        assertTrue(new File(data, "crafts").mkdir());
        assertTrue(manager.getCraftFiles().isEmpty());
    }

    @Test public void startupPreservesExistingFilesAndLaterLoadsDoNotRestoreDeletedDefaults() throws Exception {
        File data = validFiles();
        File crafts = new File(data, "crafts");
        assertTrue(crafts.mkdir());
        File custom = write(crafts, "custom.yml", "custom: unchanged\n");
        ConfigManager manager = manager(data);
        manager.loadConfigs();
        assertEquals("Ancien", manager.getPrefix());
        assertEquals("Ancien message", manager.getMessage("ready"));
        assertEquals("custom: unchanged\n", new String(Files.readAllBytes(custom.toPath()), StandardCharsets.UTF_8));
        assertFalse(new File(crafts, "armor.yml").exists());
        assertTrue(new File(data, DefaultCraftBootstrap.STATE_FILE).isFile());

        ConfigManager.Snapshot previous = manager.prepareReload();
        manager.activate(previous);
        File messages = new File(data, "messages.yml");
        Files.delete(messages.toPath());
        assertThrows(IllegalStateException.class, manager::loadConfigs);
        assertFalse(messages.exists());
        assertActive(manager, previous);
    }

    @Test public void freshStartupCopiesBundledFilesExactlyOnce() throws Exception {
        File data = temporary.newFolder();
        AtomicInteger opened = new AtomicInteger();
        ConfigManager manager = new ConfigManager(data, Logger.getAnonymousLogger(), path -> {
            opened.incrementAndGet();
            String content = "config.yml".equals(path) ? config("Default", 1)
                : "messages.yml".equals(path) ? "messages:\n  ready: 'Default message'\n"
                : "recipe: example\n";
            return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        });
        manager.loadConfigs();
        int firstCopies = opened.get();
        assertEquals(manager.getDefaultCraftFiles().size() + 2, firstCopies);
        File deletedExample = new File(manager.getCraftsFolder(), "armor.yml");
        Files.delete(deletedExample.toPath());

        manager.loadConfigs();
        assertEquals(firstCopies, opened.get());
        assertFalse(deletedExample.exists());
        assertEquals("Default", manager.getPrefix());
    }

    private File validFiles() throws Exception {
        File data = temporary.newFolder();
        write(data, "config.yml", config("Ancien", 1));
        write(data, "messages.yml", "messages:\n  ready: 'Ancien message'\n");
        return data;
    }

    private static ConfigManager manager(File data) {
        return new ConfigManager(data, Logger.getAnonymousLogger(), path -> {
            throw new AssertionError("Unexpected bundled resource access: " + path);
        });
    }

    private static File write(File directory, String name, String content) throws Exception {
        File file = new File(directory, name);
        Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
        return file;
    }

    private static String config(String prefix, int debugLevel) {
        return "schema_version: 2\nKcraft:\n  settings:\n    prefix: '" + prefix
            + "'\n    debug-level: " + debugLevel + "\n";
    }

    private static void assertActive(ConfigManager manager, ConfigManager.Snapshot expected) {
        assertTrue(manager.isLoaded());
        assertSame(expected.getConfig(), manager.getConfig());
        assertSame(expected.getMessagesConfig(), manager.getMessagesConfig());
        assertSame(expected.getRuntimeConfig(), manager.getRuntimeConfig());
        assertSame(expected.getValidationReport(), manager.getValidationReport());
    }
}
