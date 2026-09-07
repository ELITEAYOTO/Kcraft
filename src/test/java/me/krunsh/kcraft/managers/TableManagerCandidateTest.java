package me.krunsh.kcraft.managers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Map;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.Test;

import me.krunsh.kcraft.models.CraftTable;

public class TableManagerCandidateTest {

    @Test
    public void preparesPhysicalAndVirtualTablesWithoutAPlugin() {
        YamlConfiguration config = physical("tier1", "WORKBENCH");
        config.set("Kcraft.custom-blocks.tier1.name", "&6Atelier");
        config.set("Kcraft.command-tables.npc.size", "4x4");
        config.set("Kcraft.command-tables.npc.command", "atelier");
        config.set("Kcraft.command-tables.npc.only-console-npc", true);

        Map<String, CraftTable> prepared = TableManager.prepareTableDefinitions(config);

        assertEquals(2, prepared.size());
        assertEquals(Material.WORKBENCH, prepared.get("tier1").getBlockType());
        assertTrue(prepared.get("tier1").isPhysical());
        assertEquals("\u00a76Atelier", prepared.get("tier1").getName());
        assertEquals("4x4", prepared.get("npc").getSize());
        assertFalse(prepared.get("npc").isPhysical());
        assertTrue(prepared.get("npc").isConsoleOnly());
    }

    @Test
    public void acceptsLegacyBlockData() {
        Map<String, CraftTable> prepared = TableManager.prepareTableDefinitions(
            physical("tier1", "STONE:2"));

        assertEquals(Material.STONE, prepared.get("tier1").getBlockType());
        assertEquals(2, prepared.get("tier1").getBlockDataValue());
    }

    @Test
    public void preparedMapIsReadOnlyAndDetachedFromConfig() {
        YamlConfiguration config = physical("tier1", "WORKBENCH");
        Map<String, CraftTable> prepared = TableManager.prepareTableDefinitions(config);
        config.set("Kcraft.custom-blocks.tier1.block", "STONE");

        assertEquals(Material.WORKBENCH, prepared.get("tier1").getBlockType());
        try {
            prepared.clear();
            fail("Prepared table map must be read-only");
        } catch (UnsupportedOperationException expected) {
            assertEquals(1, prepared.size());
        }
    }

    @Test
    public void failedPreparationDoesNotAlterAnEarlierGeneration() {
        Map<String, CraftTable> active = TableManager.prepareTableDefinitions(
            physical("tier1", "WORKBENCH"));
        YamlConfiguration invalid = physical("replacement", "STONE");
        invalid.set("Kcraft.command-tables.invalid.size", "6x6");

        assertRejected(invalid, "invalid");

        assertEquals(1, active.size());
        assertEquals(Material.WORKBENCH, active.get("tier1").getBlockType());
        assertFalse(active.containsKey("replacement"));
    }

    @Test
    public void rejectsDuplicatePhysicalAndVirtualIds() {
        YamlConfiguration config = physical("shared", "WORKBENCH");
        config.set("Kcraft.command-tables.shared.size", "3x3");

        assertRejected(config, "dupliqu");
    }

    @Test
    public void rejectsMalformedTableSections() {
        String[] paths = {"Kcraft", "Kcraft.custom-blocks", "Kcraft.command-tables",
            "Kcraft.custom-blocks.tier1", "Kcraft.command-tables.npc"};
        for (String path : paths) {
            YamlConfiguration config = new YamlConfiguration();
            config.set(path, "not-a-section");
            assertRejected(config, "section YAML attendue");
        }
    }

    @Test
    public void rejectsUnknownMaterialsAirAndItemsAsPhysicalTables() {
        for (String material : new String[]{"UNKNOWN_MATERIAL", "AIR", "DIAMOND_SWORD"}) {
            assertRejected(physical("tier1", material), "tier1");
        }
    }

    @Test
    public void rejectsMissingBlockAndInvalidBlockData() {
        YamlConfiguration missing = new YamlConfiguration();
        missing.set("Kcraft.custom-blocks.tier1.size", "3x3");
        assertRejected(missing, "block obligatoire");
        assertRejected(physical("tier1", "STONE:invalid"), "tier1");
    }

    @Test
    public void allowsAnIntentionallyEmptyTableConfiguration() {
        assertTrue(TableManager.prepareTableDefinitions(new YamlConfiguration()).isEmpty());
    }

    private static YamlConfiguration physical(String id, String material) {
        YamlConfiguration config = new YamlConfiguration();
        config.set("Kcraft.custom-blocks." + id + ".block", material);
        config.set("Kcraft.custom-blocks." + id + ".size", "3x3");
        return config;
    }

    private static void assertRejected(YamlConfiguration config, String messagePart) {
        try {
            TableManager.prepareTableDefinitions(config);
            fail("Invalid candidate must be rejected");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains(messagePart));
        }
    }
}
