package me.krunsh.kcraft.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.bukkit.Material;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import me.krunsh.kcraft.Kcraft;

/**
 * Gestionnaire de configuration KCraft V2.1.
 *
 * La source YAML est lue au boot/reload puis transformée en RuntimeConfig immutable.
 * Les getters utilisés par le code historique restent présents pendant la migration V2,
 * mais ne relisent plus le YAML dans les hot-paths.
 */
public class ConfigManager {

    private final File dataFolder;
    private final Logger logger;
    private final DefaultCraftBootstrap.ResourceSource resources;
    private final File craftsFolder;
    private final File logsFolder;
    private volatile Snapshot activeSnapshot;
    private DefaultCraftBootstrap defaultCraftBootstrap;
    private boolean startupBootstrapCompleted;

    public ConfigManager(final Kcraft plugin) {
        this(plugin.getDataFolder(), plugin.getLogger(), new DefaultCraftBootstrap.ResourceSource() {
            @Override public InputStream open(String path) {
                return plugin.getResource(path);
            }
        });
    }

    ConfigManager(File dataFolder, Logger logger, DefaultCraftBootstrap.ResourceSource resources) {
        this.dataFolder = dataFolder;
        this.logger = logger;
        this.resources = resources;
        this.craftsFolder = new File(dataFolder, "crafts");
        this.logsFolder = new File(dataFolder, "logs");
    }

    public void loadConfigs() {
        if (!startupBootstrapCompleted) {
            createDirectories();
            copyResourceIfAbsent("config.yml");
            copyResourceIfAbsent("messages.yml");
            initializeDefaultConfigs();
            startupBootstrapCompleted = true;
        }

        Snapshot next = prepareReload();
        int craftFileCount = getCraftFiles().size();
        activate(next);
        logger.info("Configs V2 chargees - schema=" + next.getRuntimeConfig().getSchemaVersion()
            + ", crafts=" + craftFileCount + " fichier(s), runtime snapshot=ON");
    }

    /** Reads and validates a candidate without changing the active configuration or disk. */
    public Snapshot prepareReload() {
        return Snapshot.read(new File(dataFolder, "config.yml"), new File(dataFolder, "messages.yml"));
    }

    /** Publishes a previously prepared, non-null snapshot; no loading or validation occurs here. */
    public void activate(Snapshot snapshot) {
        activeSnapshot = snapshot;
    }

    public boolean isLoaded() {
        return activeSnapshot != null;
    }

    public void logValidationReport() {
        ConfigValidationReport validationReport = getValidationReport();
        if (validationReport == null) return;
        for (String warning : validationReport.getWarnings()) {
            logger.warning("[Config V2] " + warning);
        }
        for (String error : validationReport.getErrors()) {
            logger.severe("[Config V2] " + error);
        }
    }

    private void createDirectories() {
        try {
            Files.createDirectories(craftsFolder.toPath());
            Files.createDirectories(logsFolder.toPath());
        } catch (IOException failure) {
            throw new IllegalStateException("Creation des dossiers KCraft impossible: "
                + dataFolder.getAbsolutePath(), failure);
        }
    }

    private void copyResourceIfAbsent(String path) {
        File destination = new File(dataFolder, path);
        if (destination.exists()) return;
        try (InputStream input = resources.open(path)) {
            if (input == null) throw new IOException("Ressource embarquee absente: " + path);
            Files.copy(input, destination.toPath());
        } catch (IOException failure) {
            throw new IllegalStateException("Copie de la configuration par defaut impossible: "
                + path, failure);
        }
    }

    private void initializeDefaultConfigs() {
        ensureDefaultBootstrap();
        try {
            DefaultCraftBootstrap.Initialization result = defaultCraftBootstrap.initializeOnce();
            if (result.isExistingInstallation()) {
                logger.info("Installation KCraft existante: exemples inchanges.");
            } else if (result.getExportedCount() > 0) {
                logger.info(result.getExportedCount()
                    + " fichiers de craft par defaut exportes.");
            }
        } catch (Exception e) {
            throw new IllegalStateException("Initialisation des exemples KCraft impossible", e);
        }
    }

    public List<String> getDefaultCraftFiles() {
        ensureDefaultBootstrap();
        return defaultCraftBootstrap.listDefaults();
    }

    public DefaultCraftBootstrap.ExportStatus exportDefaultCraft(String fileName)
            throws java.io.IOException {
        ensureDefaultBootstrap();
        return defaultCraftBootstrap.export(fileName);
    }

    private void ensureDefaultBootstrap() {
        if (defaultCraftBootstrap != null) return;
        defaultCraftBootstrap = new DefaultCraftBootstrap(dataFolder, craftsFolder, resources);
    }

    public List<File> getCraftFiles() {
        File[] files = craftsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            throw new IllegalStateException("Lecture du dossier de crafts impossible: "
                + craftsFolder.getAbsolutePath());
        }
        List<File> craftFiles = new ArrayList<File>(Arrays.asList(files));
        craftFiles.sort(Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));
        return craftFiles;
    }

    public FileConfiguration loadCraftConfig(File craftFile) {
        return readYaml(craftFile);
    }

    private static FileConfiguration readYaml(File file) {
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(file),
                StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT))) {
            FileConfiguration yaml = new YamlConfiguration();
            yaml.load(reader);
            return yaml;
        } catch (IOException | InvalidConfigurationException failure) {
            throw new IllegalStateException("Lecture YAML UTF-8 impossible: "
                + file.getAbsolutePath() + " (" + failure.getMessage() + ")", failure);
        }
    }

    /** A fully prepared configuration and message pair, published together. */
    public static final class Snapshot {
        private final FileConfiguration config;
        private final FileConfiguration messagesConfig;
        private final RuntimeConfig runtimeConfig;
        private final ConfigValidationReport validationReport;

        private Snapshot(FileConfiguration config, FileConfiguration messagesConfig,
                         RuntimeConfig runtimeConfig, ConfigValidationReport validationReport) {
            this.config = config;
            this.messagesConfig = messagesConfig;
            this.runtimeConfig = runtimeConfig;
            this.validationReport = validationReport;
        }

        public static Snapshot read(File configFile, File messagesFile) {
            FileConfiguration config = readYaml(configFile);
            FileConfiguration messages = readYaml(messagesFile);
            ConfigValidationReport report = V2ConfigValidator.validate(config);
            if (!report.isValid()) {
                throw new IllegalStateException("Configuration KCraft V2 invalide: "
                    + report.errorCount() + " erreur(s): " + String.join("; ", report.getErrors()));
            }
            return new Snapshot(config, messages, RuntimeConfig.from(config), report);
        }

        public FileConfiguration getConfig() { return config; }
        public FileConfiguration getMessagesConfig() { return messagesConfig; }
        public RuntimeConfig getRuntimeConfig() { return runtimeConfig; }
        public ConfigValidationReport getValidationReport() { return validationReport; }
    }

    private Snapshot snapshot() {
        Snapshot current = activeSnapshot;
        if (current == null) throw new IllegalStateException("Configuration KCraft non initialisee");
        return current;
    }

    private RuntimeConfig runtime() {
        return snapshot().getRuntimeConfig();
    }

    public RuntimeConfig getRuntimeConfig() { return runtime(); }
    public ConfigValidationReport getValidationReport() {
        Snapshot current = activeSnapshot;
        return current == null ? null : current.getValidationReport();
    }

    public String getPrefix() { return runtime().getPrefix(); }
    public int getDebugLevel() { return runtime().getDebugLevel(); }
    public List<String> getEnabledWorlds() { return runtime().getEnabledWorlds(); }

    /**
     * Compat V2.1: l'ancien cache populaire V1 est volontairement neutralise.
     * Le vrai cache de matching arrive avec RecipeIndex/MatrixFingerprint en V2.3.
     */
    public boolean isCacheEnabled() { return false; }
    public boolean isInstantCompactEnabled() { return false; }
    public int getCacheSize() { return 0; }

    public boolean isAsyncLoggingEnabled() { return runtime().isAsyncLogging(); }
    public Set<Material> getBlockedVanillaCraftResults() {
        return runtime().getBlockedVanillaCraftResults();
    }
    public Set<Material> getBlockedGeneratedLoot() { return runtime().getBlockedGeneratedLoot(); }
    public boolean shouldCleanGeneratedLoot() { return runtime().isCleanGeneratedLoot(); }
    public boolean areParticlesEnabled() { return runtime().isParticlesEnabled(); }
    public boolean areSoundsEnabled() { return runtime().isSoundsEnabled(); }
    public boolean allowPerCraftEffects() { return runtime().isAllowPerCraftEffects(); }
    public boolean useBorders() { return runtime().isGuiBorders(); }
    public String getBorderColor() { return runtime().getGuiBorderColor(); }
    public String getTitlePrefix() { return runtime().getGuiTitlePrefix(); }
    public boolean isLoggingEnabled() { return runtime().isLoggingEnabled(); }
    public String getLogFile() { return runtime().getLogFile(); }
    public boolean isPerPlayerLogging() { return runtime().isPerPlayerLogging(); }
    public boolean isFactionStatsEnabled() { return runtime().isFactionStatsEnabled(); }
    public String getLogRotation() { return runtime().getLogRotation(); }
    public int getKeepDays() { return runtime().getKeepDays(); }
    public String getMessage(String key) {
        return getMessagesConfig().getString("messages." + key, "&cMessage manquant: " + key);
    }
    public String getMessage(String key, String defaultValue) {
        return getMessagesConfig().getString("messages." + key, defaultValue);
    }
    public FileConfiguration getConfig() { return snapshot().getConfig(); }
    public FileConfiguration getMessagesConfig() { return snapshot().getMessagesConfig(); }
    public File getCraftsFolder() { return craftsFolder; }
    public File getLogsFolder() { return logsFolder; }
    public String getGlassBorderColor() { return runtime().getGuiBorderColor(); }
    public String getGlassCraftAreaColor() { return runtime().getGuiCraftAreaColor(); }
    public String getGlassResultAreaColor() { return runtime().getGuiResultAreaColor(); }
    public String getGlassButtonAreaColor() { return runtime().getGuiButtonAreaColor(); }
    public boolean useGUIBorders() { return runtime().isGuiBorders(); }
    public String getBorderItem() { return runtime().getGuiBorderItem(); }
    public boolean opsCanBypassPermissions() { return runtime().isOpsBypassPermissions(); }
    public boolean isStrictFactionLevel() { return runtime().isStrictFactionLevel(); }
    public boolean isBatchCraftEnabled() { return runtime().isBatchCraftEnabled(); }
    public int getMaxBatchCrafts() { return runtime().getMaxBatchCrafts(); }
}
