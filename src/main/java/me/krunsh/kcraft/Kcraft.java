package me.krunsh.kcraft;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import me.krunsh.kcraft.api.KcraftAPI;
import me.krunsh.kcraft.catalog.RecipeCatalog;
import me.krunsh.kcraft.compiled.CompiledRecipeCatalog;
import me.krunsh.kcraft.compiled.CompilationStats;
import me.krunsh.kcraft.commands.KcraftCommand;
import me.krunsh.kcraft.commands.KcraftCommandV2;
import me.krunsh.kcraft.commands.KcraftNPCCommand;
import me.krunsh.kcraft.config.ConfigManager;
import me.krunsh.kcraft.config.ConfigValidationReport;
import me.krunsh.kcraft.hooks.PluginHookManager;
import me.krunsh.kcraft.listeners.CraftGUIListener;
import me.krunsh.kcraft.listeners.GeneratedLootRestrictionListener;
import me.krunsh.kcraft.listeners.TableListener;
import me.krunsh.kcraft.listeners.VanillaCraftListener;
import me.krunsh.kcraft.managers.CacheManager;
import me.krunsh.kcraft.managers.CraftManager;
import me.krunsh.kcraft.managers.IndexedCraftManager;
import me.krunsh.kcraft.managers.LoggingManager;
import me.krunsh.kcraft.managers.TableManager;
import me.krunsh.kcraft.managers.VanillaDebugManager;
import me.krunsh.kcraft.utils.MessageUtil;
import me.krunsh.kcraft.validation.RecipeValidator;

/** KCraft 2.8.2 - rechargement préparé et diagnostics. */
public final class Kcraft extends JavaPlugin {

    private static Kcraft instance;
    private static KcraftAPI api;

    private ConfigManager configManager;
    private CraftManager craftManager;
    private TableManager tableManager;
    private CacheManager cacheManager;
    private LoggingManager loggingManager;
    private VanillaDebugManager vanillaDebugManager;
    private PluginHookManager hookManager;
    private CraftGUIListener craftGUIListener;
    private VanillaCraftListener vanillaCraftListener;
    private RecipeCatalog recipeCatalog;
    private CompiledRecipeCatalog compiledRecipeCatalog;
    private boolean reloading;

    @Override
    public void onEnable() {
        long started = System.nanoTime();
        instance = this;

        getLogger().info("VOLKARIA • KCraft " + getDescription().getVersion() + " | initialisation");

        if (!checkSpigotVersion()) {
            getLogger().severe("Kcraft nécessite CraftBukkit/Spigot v1_8_R3 compatible.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        try {
            configManager = new ConfigManager(this);
            try {
                configManager.loadConfigs();
            } catch (RuntimeException invalidConfig) {
                configManager.logValidationReport();
                throw invalidConfig;
            }
            MessageUtil.init(configManager);

            craftManager = new IndexedCraftManager(this);
            tableManager = new TableManager(this);

            recipeCatalog = RecipeCatalog.snapshot(craftManager.getAllRecipes());
            ConfigValidationReport recipeReport = RecipeValidator.validate(
                recipeCatalog, tableManager.getAllTables());
            for (String warning : recipeReport.getWarnings()) {
                getLogger().warning("[Recipes V2] " + warning);
            }
            if (!recipeReport.isValid()) {
                for (String error : recipeReport.getErrors()) {
                    getLogger().severe("[Recipes V2] " + error);
                }
                throw new IllegalStateException("Validation recettes V2 échouée: "
                    + recipeReport.errorCount() + " erreur(s)");
            }
            getLogger().info("Validation V2 OK - " + recipeCatalog.size()
                + " recette(s), " + tableManager.getAllTables().size() + " table(s)");

            long compileStarted = System.nanoTime();
            compiledRecipeCatalog =
                CompiledRecipeCatalog.compile(recipeCatalog);

            CompilationStats compilationStats =
                CompilationStats.from(compiledRecipeCatalog);

            long compileMicros =
                (System.nanoTime() - compileStarted) / 1000L;

            getLogger().info(
                "CompiledRecipe V2.3 prêt - "
                    + compilationStats.summary()
                    + ", tables="
                    + compiledRecipeCatalog.tableCount()
                    + ", compile="
                    + compileMicros
                    + "us"
            );

            ((IndexedCraftManager) craftManager)
                .rebuildIndex(
                    compiledRecipeCatalog
                );

            // CacheManager V1 conservé uniquement comme shim API durant V2.3.
            // ConfigManager désactive volontairement son faux cache de matching.
            cacheManager = new CacheManager(this);
            loggingManager = new LoggingManager(this);
            vanillaDebugManager = new VanillaDebugManager(this);

            hookManager = new PluginHookManager();
            hookManager.loadHooks();

            registerEvents();
            registerCommands();
            api = new KcraftAPI(this);
            cacheManager.buildCache();

            long elapsedMs = (System.nanoTime() - started) / 1_000_000L;
            getLogger().info("Kcraft " + getDescription().getVersion() + " actif - recipes=" + recipeCatalog.size()
                + ", tables=" + tableManager.getAllTables().size()
                + ", compiled=" + compiledRecipeCatalog.size()
                + ", runtime-config=IMMUTABLE, legacy-cache=OFF, boot=" + elapsedMs + "ms");
        } catch (Exception e) {
            getLogger().severe("Erreur démarrage Kcraft " + getDescription().getVersion() + ": " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (craftGUIListener != null) craftGUIListener.shutdown();
        if (vanillaCraftListener != null) vanillaCraftListener.shutdown();
        if (vanillaDebugManager != null) vanillaDebugManager.shutdown();
        if (loggingManager != null) loggingManager.saveAll();
        if (tableManager != null) tableManager.savePlacedTables();
        if (cacheManager != null) cacheManager.clearCache();
        compiledRecipeCatalog = null;
        recipeCatalog = null;
        getLogger().info("Kcraft " + getDescription().getVersion() + " désactivé.");
        instance = null;
        api = null;
    }

    private boolean checkSpigotVersion() {
        String packageName = getServer().getClass().getPackage().getName();
        return packageName.endsWith("v1_8_R3") || Bukkit.getVersion().contains("1.8.8");
    }

    private void registerEvents() {
        getServer().getPluginManager().registerEvents(new TableListener(this), this);
        craftGUIListener = new CraftGUIListener(this);
        getServer().getPluginManager().registerEvents(craftGUIListener, this);
        vanillaCraftListener = new VanillaCraftListener(this);
        getServer().getPluginManager().registerEvents(vanillaCraftListener, this);
        boolean unknownCraftExtension = vanillaCraftListener.registerUnknownCraftExtension();
        getLogger().info("Santé extension KHopeSpigot: " + vanillaDebugManager.healthSummary()
            + (unknownCraftExtension ? "; réception runtime en attente" : "; extension indisponible"));
        getServer().getPluginManager().registerEvents(new GeneratedLootRestrictionListener(this), this);
        getServer().getPluginManager().registerEvents(
            new me.krunsh.kcraft.listeners.DurabilityListener(this), this);
    }

    private void registerCommands() {
        KcraftCommand kcraftCommand = new KcraftCommandV2(this);
        getCommand("kcraft").setExecutor(kcraftCommand);
        getCommand("kcraft").setTabCompleter(kcraftCommand);
        getCommand("kcraft-npc").setExecutor(new KcraftNPCCommand(this));
    }

    /**
     * Reconstruit atomiquement les snapshots runtime après une mutation API.
     *
     * Le parsing YAML n'est pas relancé : on repart du CraftManager courant.
     */
    public void rebuildRecipeRuntime() {

        RecipeCatalog nextCatalog =
            RecipeCatalog.snapshot(
                craftManager.getAllRecipes()
            );

        ConfigValidationReport report =
            RecipeValidator.validate(
                nextCatalog,
                tableManager.getAllTables()
            );

        for (String warning
                : report.getWarnings()) {

            getLogger().warning(
                "[Recipes V2] " + warning
            );
        }

        if (!report.isValid()) {
            for (String error
                    : report.getErrors()) {

                getLogger().severe(
                    "[Recipes V2] " + error
                );
            }

            throw new IllegalStateException(
                "Runtime recipe rebuild refusé: "
                    + report.errorCount()
                    + " erreur(s)"
            );
        }

        CompiledRecipeCatalog nextCompiled =
            CompiledRecipeCatalog.compile(
                nextCatalog
            );

        /*
         * L'index est reconstruit avant publication des nouveaux catalogues.
         * IndexedCraftManager garde son propre volatile RecipeIndex.
         */
        ((IndexedCraftManager) craftManager)
            .rebuildIndex(nextCompiled);

        recipeCatalog = nextCatalog;
        compiledRecipeCatalog = nextCompiled;

        getLogger().info(
            "Recipe runtime V2.6 reconstruit - recipes="
                + recipeCatalog.size()
                + ", compiled="
                + compiledRecipeCatalog.size()
        );
    }

    public void reload() {
        if (!getServer().isPrimaryThread()) {
            throw new IllegalStateException("Le reload KCraft exige le thread principal.");
        }
        if (reloading) throw new IllegalStateException("Un reload KCraft est déjà en cours.");
        reloading = true;
        try {
            // Toutes les opérations susceptibles d'échouer restent sur des candidats.
            ConfigManager.Snapshot nextConfig = configManager.prepareReload();
            java.util.Map<String, me.krunsh.kcraft.models.CraftTable> nextTables =
                tableManager.prepareTables(nextConfig.getConfig());
            IndexedCraftManager nextCrafts = new IndexedCraftManager(this);
            RecipeCatalog nextCatalog = RecipeCatalog.snapshot(nextCrafts.getAllRecipes());
            ConfigValidationReport report = RecipeValidator.validate(nextCatalog, nextTables);
            for (String warning : report.getWarnings()) getLogger().warning("[Recipes V2] " + warning);
            if (!report.isValid()) {
                throw new IllegalStateException("Reload refusé: " + String.join("; ", report.getErrors()));
            }
            CompiledRecipeCatalog nextCompiled = CompiledRecipeCatalog.compile(nextCatalog);
            nextCrafts.rebuildIndex(nextCompiled);
            PluginHookManager nextHooks = new PluginHookManager();
            nextHooks.loadHooks();

            // Commit sans I/O ni callback Bukkit : aucune exécution gameplay ne peut
            // s'intercaler sur le thread principal. La persistance placée reste intacte.
            tableManager.activateTables(nextTables);
            craftManager = nextCrafts;
            recipeCatalog = nextCatalog;
            compiledRecipeCatalog = nextCompiled;
            hookManager = nextHooks;
            configManager.activate(nextConfig);
            MessageUtil.init(configManager);

            // Après commit seulement. Une erreur de rafraîchissement ne signifie pas
            // que la configuration précédente a été conservée : ne pas annoncer un rollback.
            afterReload("menus", () -> { if (craftGUIListener != null) craftGUIListener.shutdown(); });
            afterReload("aperçus vanilla", () -> { if (vanillaCraftListener != null) vanillaCraftListener.refreshAfterReload(); });
            afterReload("logs", () -> { if (loggingManager != null) loggingManager.ensureWorker(); });
            getLogger().info("Kcraft " + getDescription().getVersion() + " rechargé - recipes="
                + nextCatalog.size() + ", tables=" + nextTables.size());
        } finally {
            reloading = false;
        }
    }

    private void afterReload(String component, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException error) {
            getLogger().log(java.util.logging.Level.SEVERE,
                "Reload appliqué, mais rafraîchissement " + component + " en erreur", error);
        }
    }

    /** Les consommateurs historiques lisent la configuration réellement active. */
    @Override
    public FileConfiguration getConfig() {
        return configManager != null && configManager.isLoaded()
            ? configManager.getConfig() : super.getConfig();
    }

    @Override
    public void saveConfig() {
        if (configManager == null || !configManager.isLoaded()) {
            super.saveConfig();
            return;
        }
        try {
            getConfig().save(new java.io.File(getDataFolder(), "config.yml"));
        } catch (java.io.IOException error) {
            getLogger().log(java.util.logging.Level.SEVERE, "Écriture config.yml impossible", error);
        }
    }

    public static Kcraft getInstance() { return instance; }
    public static KcraftAPI getAPI() { return api; }
    public ConfigManager getConfigManager() { return configManager; }
    public CraftManager getCraftManager() { return craftManager; }
    public TableManager getTableManager() { return tableManager; }
    public CacheManager getCacheManager() { return cacheManager; }
    public LoggingManager getLoggingManager() { return loggingManager; }
    public VanillaDebugManager getVanillaDebugManager() { return vanillaDebugManager; }
    public PluginHookManager getHookManager() { return hookManager; }
    public CraftGUIListener getCraftGUIListener() { return craftGUIListener; }
    public RecipeCatalog getRecipeCatalog() { return recipeCatalog; }
    public CompiledRecipeCatalog getCompiledRecipeCatalog() { return compiledRecipeCatalog; }
    public IndexedCraftManager getIndexedCraftManager() { return (IndexedCraftManager) craftManager; }
}
