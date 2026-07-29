package me.krunsh.kcraft.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class DefaultCraftBootstrapTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test public void freshInstallationExportsDefaultsOnlyOnce() throws Exception {
        File data = temporary.newFolder("fresh");
        File crafts = new File(data, "crafts");
        DefaultCraftBootstrap bootstrap = bootstrap(data, crafts, new AtomicInteger());
        assertEquals(bootstrap.listDefaults().size(), bootstrap.initializeOnce().getExportedCount());
        Files.delete(new File(crafts, "armor.yml").toPath());
        DefaultCraftBootstrap.Initialization second = bootstrap.initializeOnce();
        assertTrue(second.isAlreadyInitialized());
        assertEquals(0, second.getExportedCount());
        assertFalse(new File(crafts, "armor.yml").exists());
    }

    @Test public void existingInstallationIsMarkedWithoutFillingMissingFiles() throws Exception {
        File data = temporary.newFolder("existing");
        File crafts = new File(data, "crafts");
        assertTrue(crafts.mkdirs());
        Files.write(new File(crafts, "custom.yml").toPath(), "custom: true".getBytes(StandardCharsets.UTF_8));
        AtomicInteger opened = new AtomicInteger();
        DefaultCraftBootstrap.Initialization result = bootstrap(data, crafts, opened).initializeOnce();
        assertTrue(result.isExistingInstallation());
        assertEquals(0, opened.get());
        assertFalse(new File(crafts, "armor.yml").exists());
    }

    @Test public void manualExportNeverOverwritesAndRejectsTraversal() throws Exception {
        File data = temporary.newFolder("manual");
        File crafts = new File(data, "crafts");
        DefaultCraftBootstrap bootstrap = bootstrap(data, crafts, new AtomicInteger());
        assertEquals(DefaultCraftBootstrap.ExportStatus.EXPORTED, bootstrap.export("armor"));
        assertEquals(DefaultCraftBootstrap.ExportStatus.ALREADY_EXISTS, bootstrap.export("armor.yml"));
        assertEquals(DefaultCraftBootstrap.ExportStatus.UNKNOWN_DEFAULT, bootstrap.export("../armor.yml"));
    }

    private static DefaultCraftBootstrap bootstrap(File data, File crafts, AtomicInteger opened) {
        return new DefaultCraftBootstrap(data, crafts, new DefaultCraftBootstrap.ResourceSource() {
            @Override public ByteArrayInputStream open(String path) throws IOException {
                opened.incrementAndGet();
                return new ByteArrayInputStream(("resource: " + path).getBytes(StandardCharsets.UTF_8));
            }
        });
    }
}
