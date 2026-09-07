package me.krunsh.kcraft;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.MockedStatic;

import me.krunsh.kcraft.catalog.RecipeCatalog;
import me.krunsh.kcraft.compiled.CompiledRecipeCatalog;
import me.krunsh.kcraft.config.ConfigManager;
import me.krunsh.kcraft.hooks.PluginHookManager;
import me.krunsh.kcraft.listeners.CraftGUIListener;
import me.krunsh.kcraft.listeners.VanillaCraftListener;
import me.krunsh.kcraft.managers.IndexedCraftManager;
import me.krunsh.kcraft.managers.LoggingManager;
import me.krunsh.kcraft.managers.TableManager;
import me.krunsh.kcraft.models.CraftTable;
import me.krunsh.kcraft.utils.MessageUtil;

/** Executes the real reload orchestration; only Bukkit infrastructure is mocked. */
public class ReloadTransactionTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();
    private Kcraft plugin;
    private Server server;
    private ConfigManager config;
    private TableManager tables;
    private IndexedCraftManager crafts;
    private RecipeCatalog catalog;
    private CompiledRecipeCatalog compiled;
    private CraftGUIListener gui;
    private VanillaCraftListener vanilla;
    private LoggingManager logs;
    private File folder;
    private MockedStatic<Bukkit> bukkit;

    @Before public void setup() throws Exception {
        folder = temporary.getRoot();
        temporary.newFolder("crafts");
        write("config.yml", configuration("old", "3x3"));
        write("messages.yml", "messages:\n  marker: old\n");
        write("crafts/test.yml", recipe("STONE"));
        plugin = mock(Kcraft.class, CALLS_REAL_METHODS);
        server = mock(Server.class);
        Logger logger = Logger.getAnonymousLogger();
        logger.setLevel(Level.OFF);
        doReturn(folder).when(plugin).getDataFolder();
        doReturn(logger).when(plugin).getLogger();
        doReturn(server).when(plugin).getServer();
        doReturn(new PluginDescriptionFile("Kcraft", "2.8.2", Kcraft.class.getName()))
            .when(plugin).getDescription();
        when(server.isPrimaryThread()).thenReturn(true);
        bukkit = mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getPluginManager).thenReturn(mock(PluginManager.class));
        config = new ConfigManager(plugin);
        config.activate(config.prepareReload());
        set("configManager", config);
        MessageUtil.init(config);
        tables = new TableManager(plugin);
        crafts = new IndexedCraftManager(plugin);
        catalog = RecipeCatalog.snapshot(crafts.getAllRecipes());
        compiled = CompiledRecipeCatalog.compile(catalog);
        crafts.rebuildIndex(compiled);
        set("tableManager", tables);
        set("craftManager", crafts);
        set("recipeCatalog", catalog);
        set("compiledRecipeCatalog", compiled);
        set("hookManager", new PluginHookManager());
        gui = mock(CraftGUIListener.class);
        vanilla = mock(VanillaCraftListener.class);
        logs = mock(LoggingManager.class);
        set("craftGUIListener", gui);
        set("vanillaCraftListener", vanilla);
        set("loggingManager", logs);
    }

    @After public void teardown() { if (bukkit != null) bukkit.close(); }

    @Test public void invalidConfigKeepsEntireRuntimeAndAllowsFollowingReload() throws Exception {
        write("config.yml", configuration("bad", "9x9"));
        rejectAndAssertUnchanged();
        write("config.yml", configuration("new", "4x4"));
        plugin.reload();
        assertEquals("new", config.getPrefix());
        assertEquals("4x4", tables.getTable("tier1").getSize());
    }

    @Test public void malformedMessagesCannotReplaceConfigOrPrefix() throws Exception {
        write("config.yml", configuration("new", "4x4"));
        write("messages.yml", "messages: [\n");
        rejectAndAssertUnchanged();
    }

    @Test public void malformedRecipeFileRejectsWholeReload() throws Exception {
        write("crafts/test.yml", "recipes: [\n");
        rejectAndAssertUnchanged();
    }

    @Test public void invalidRecipeCannotDisappearSilently() throws Exception {
        write("crafts/test.yml", recipe("NOT_A_MATERIAL"));
        rejectAndAssertUnchanged();
    }

    @Test public void duplicateRecipeIdRejectsWholeReload() throws Exception {
        write("crafts/duplicate.yml", recipe("DIAMOND"));
        rejectAndAssertUnchanged();
    }

    @Test public void scalarInShapelessIngredientsCannotBeSilentlyDropped() throws Exception {
        write("crafts/test.yml", "recipes:\n  one:\n    type: SHAPELESS\n    table: tier1\n"
            + "    ingredients:\n      - material: STONE\n      - invalid\n"
            + "    result:\n      material: DIAMOND\n");
        rejectAndAssertUnchanged();
    }

    @Test public void unusedMalformedShapedIngredientRejectsReload() throws Exception {
        write("crafts/test.yml", recipe("STONE").replace("    result:", "      B: invalid\n    result:"));
        rejectAndAssertUnchanged();
    }

    @Test public void unknownRequiredTableRejectsBeforePublication() throws Exception {
        write("crafts/test.yml", recipe("DIAMOND").replace("table: tier1", "table: missing"));
        rejectAndAssertUnchanged();
    }

    @Test public void hookPreparationFailureKeepsRuntime() throws Exception {
        bukkit.when(Bukkit::getPluginManager).thenThrow(new IllegalStateException("hook failure"));
        rejectAndAssertUnchanged();
    }

    @Test public void successfulReloadPublishesBeforeInvalidatingViews() throws Exception {
        write("config.yml", configuration("new", "4x4"));
        write("messages.yml", "messages:\n  marker: new\n");
        write("crafts/test.yml", recipe("DIAMOND"));
        doAnswer(call -> {
            assertEquals("new", plugin.getConfigManager().getPrefix());
            assertEquals("4x4", tables.getTable("tier1").getSize());
            assertNotSame(catalog, plugin.getRecipeCatalog());
            try { plugin.reload(); fail("Reentrant reload accepted"); }
            catch (IllegalStateException expected) { assertTrue(expected.getMessage().contains("en cours")); }
            return null;
        }).when(gui).shutdown();
        plugin.reload();
        assertSame(config, plugin.getConfigManager());
        assertSame(tables, plugin.getTableManager());
        assertSame(logs, plugin.getLoggingManager());
        assertSame(config.getConfig(), plugin.getConfig());
        assertEquals("new", config.getMessage("marker"));
        assertNotSame(crafts, plugin.getCraftManager());
        assertNotSame(compiled, plugin.getCompiledRecipeCatalog());
        assertSame(plugin.getCraftManager().getRecipe("one"), plugin.getRecipeCatalog().get("one"));
        verify(gui).shutdown();
        verify(vanilla).refreshAfterReload();
        verify(logs).ensureWorker();
        verify(logs, never()).saveAll();
    }

    @Test public void viewFailureAfterCommitDoesNotClaimRollbackOrSkipOthers() throws Exception {
        write("config.yml", configuration("new", "3x3"));
        doThrow(new IllegalStateException("GUI failure")).when(gui).shutdown();
        plugin.reload();
        assertEquals("new", config.getPrefix());
        verify(vanilla).refreshAfterReload();
        verify(logs).ensureWorker();
    }

    @Test public void offThreadCallIsRejectedBeforeReadingOrMutating() throws Exception {
        when(server.isPrimaryThread()).thenReturn(false);
        rejectAndAssertUnchanged();
    }

    @Test public void bundledDefaultsPassStrictParsingAndCompilation() throws Exception {
        File defaults = temporary.newFolder("bundled");
        doReturn(defaults).when(plugin).getDataFolder();
        doAnswer(call -> getClass().getClassLoader().getResourceAsStream(call.getArgument(0)))
            .when(plugin).getResource(anyString());
        ConfigManager bundled = new ConfigManager(plugin);
        bundled.loadConfigs();
        set("configManager", bundled);
        MessageUtil.init(bundled);
        TableManager bundledTables = new TableManager(plugin);
        IndexedCraftManager bundledCrafts = new IndexedCraftManager(plugin);
        RecipeCatalog bundledCatalog = RecipeCatalog.snapshot(bundledCrafts.getAllRecipes());
        me.krunsh.kcraft.config.ConfigValidationReport report =
            me.krunsh.kcraft.validation.RecipeValidator.validate(bundledCatalog, bundledTables.getAllTables());
        assertTrue(report.getErrors().toString(), report.isValid());
        assertTrue(bundledCatalog.size() > 0);
        assertEquals(bundledCatalog.size(), CompiledRecipeCatalog.compile(bundledCatalog).size());
    }

    private void rejectAndAssertUnchanged() throws Exception {
        FileConfiguration oldConfig = config.getConfig();
        FileConfiguration oldMessages = config.getMessagesConfig();
        Map<String, CraftTable> oldTables = tables.getAllTables();
        try { plugin.reload(); fail("Invalid reload accepted"); }
        catch (IllegalArgumentException | IllegalStateException expected) { /* intended rejection */ }
        assertSame(oldConfig, config.getConfig());
        assertSame(oldMessages, config.getMessagesConfig());
        assertSame(oldConfig, plugin.getConfig());
        assertEquals("old", MessageUtil.getPrefix());
        assertSame(oldTables.get("tier1"), tables.getTable("tier1"));
        assertSame(crafts, plugin.getCraftManager());
        assertSame(catalog, plugin.getRecipeCatalog());
        assertSame(compiled, plugin.getCompiledRecipeCatalog());
        verifyNoInteractions(gui, vanilla, logs);
    }

    private void set(String name, Object value) throws Exception {
        Field field = Kcraft.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(plugin, value);
    }
    private void write(String path, String contents) throws Exception {
        Files.write(new File(folder, path).toPath(), contents.getBytes(StandardCharsets.UTF_8));
    }
    private static String configuration(String prefix, String size) {
        return "schema_version: 2\nKcraft:\n  settings:\n    prefix: '" + prefix
            + "'\n    debug-level: 0\n  custom-blocks:\n    tier1:\n      block: WORKBENCH\n      size: "
            + size + "\n";
    }
    private static String recipe(String material) {
        return "recipes:\n  one:\n    type: SHAPED\n    table: tier1\n    pattern: ['A']\n"
            + "    ingredients:\n      A:\n        material: STONE\n    result:\n      material: "
            + material + "\n";
    }
}
