package me.krunsh.kcraft.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import me.krunsh.kcraft.Kcraft;

/**
 * Gestionnaire de configuration multi-fichiers
 * Gère config.yml principal + fichiers séparés dans crafts/
 */
public class ConfigManager {
    
    private final Kcraft plugin;
    
    // Configs
    private FileConfiguration config;
    private FileConfiguration messagesConfig;
    
    // Dossiers
    private File craftsFolder;
    private File logsFolder;
    private DefaultCraftBootstrap defaultCraftBootstrap;
    private boolean startupBootstrapCompleted;
    
    public ConfigManager(Kcraft plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Charge toutes les configurations
     */
    public void loadConfigs() {
        // 1. Config principal
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();
        
        // 2. Créer dossiers si nécessaires
        createDirectories();
        
        // 3. Config des messages
        loadMessagesConfig();
        
        // 4. Configs par défaut
        if (!startupBootstrapCompleted) {
            initializeDefaultConfigs();
            startupBootstrapCompleted = true;
        }
        
        plugin.getLogger().info("Configurations chargées (crafts: " + getCraftFiles().size() + " fichiers)");
    }
    
    private void createDirectories() {
        craftsFolder = new File(plugin.getDataFolder(), "crafts");
        logsFolder = new File(plugin.getDataFolder(), "logs");
        
        if (!craftsFolder.exists()) {
            craftsFolder.mkdirs();
            plugin.getLogger().info("Dossier crafts/ créé");
        }
        
        if (!logsFolder.exists()) {
            logsFolder.mkdirs();
            plugin.getLogger().info("Dossier logs/ créé");
        }
    }
    
    private void loadMessagesConfig() {
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        
        // Force UTF-8 pour messages.yml aussi
        try {
            messagesConfig = new YamlConfiguration();
            InputStreamReader reader = new InputStreamReader(
                new FileInputStream(messagesFile),
                StandardCharsets.UTF_8
            );
            messagesConfig.load(reader);
            reader.close();
        } catch (Exception e) {
            plugin.getLogger().warning("Erreur chargement UTF-8 pour messages.yml, fallback mode standard");
            messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        }
    }
    
    private void createDefaultConfigs() {
        // Créer les fichiers de crafts par défaut s'ils n'existent pas
        createDefaultCraftFile("compacting.yml");
        createDefaultCraftFile("azurite.yml");
        createDefaultCraftFile("hunter.yml");
        createDefaultCraftFile("evolutive_tools.yml");
        createDefaultCraftFile("tables.yml");
        createDefaultCraftFile("collectors.yml");
        createDefaultCraftFile("weapons.yml");
        createDefaultCraftFile("tools.yml");
        createDefaultCraftFile("evolution.yml");
    }
    
    private void createDefaultCraftFile(String fileName) {
        File craftFile = new File(craftsFolder, fileName);
        if (!craftFile.exists()) {
            try {
                // Copier depuis les resources du plugin
                plugin.saveResource("crafts/" + fileName, false);
                plugin.getLogger().info("Fichier de craft créé: " + fileName);
            } catch (IllegalArgumentException e) {
                // Fichier pas trouvé dans resources, ignorer
                plugin.getLogger().warning("Fichier de craft non trouvé dans resources: " + fileName);
            } catch (Exception e) {
                plugin.getLogger().warning("Erreur création " + fileName + ": " + e.getMessage());
            }
        }
    }
    
    /**
     * Obtient tous les fichiers de craft
     */
    private void initializeDefaultConfigs() {
        ensureDefaultBootstrap();
        try {
            DefaultCraftBootstrap.Initialization result = defaultCraftBootstrap.initializeOnce();
            if (result.isExistingInstallation()) {
                plugin.getLogger().info("Installation KCraft existante: exemples marques initialises sans ajout.");
            } else if (result.getExportedCount() > 0) {
                plugin.getLogger().info(result.getExportedCount() + " fichiers de craft par defaut exportes.");
            }
        } catch (Exception e) {
            throw new IllegalStateException("Initialisation des exemples KCraft impossible", e);
        }
    }

    public List<String> getDefaultCraftFiles() {
        ensureDefaultBootstrap();
        return defaultCraftBootstrap.listDefaults();
    }

    public DefaultCraftBootstrap.ExportStatus exportDefaultCraft(String fileName) throws java.io.IOException {
        ensureDefaultBootstrap();
        return defaultCraftBootstrap.export(fileName);
    }

    private void ensureDefaultBootstrap() {
        if (defaultCraftBootstrap != null) return;
        defaultCraftBootstrap = new DefaultCraftBootstrap(plugin.getDataFolder(), craftsFolder,
            new DefaultCraftBootstrap.ResourceSource() {
                @Override
                public InputStream open(String path) {
                    return plugin.getResource(path);
                }
            });
    }

    public List<File> getCraftFiles() {
        List<File> craftFiles = new ArrayList<>();
        File[] files = craftsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        
        if (files != null) {
            craftFiles.addAll(Arrays.asList(files));
            craftFiles.sort(Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));
        }
        
        return craftFiles;
    }
    
    /**
     * Charge une configuration de craft spécifique avec encodage UTF-8 forcé
     */
    public FileConfiguration loadCraftConfig(File craftFile) {
        try {
            FileConfiguration config = new YamlConfiguration();
            // Force UTF-8 pour éviter les problèmes d'encodage avec autres plugins (HolographicDisplays, Kharvester, etc.)
            InputStreamReader reader = new InputStreamReader(
                new FileInputStream(craftFile), 
                StandardCharsets.UTF_8
            );
            config.load(reader);
            reader.close();
            return config;
        } catch (Exception e) {
            plugin.getLogger().warning("Erreur chargement UTF-8 pour " + craftFile.getName() + ", fallback mode standard");
            return YamlConfiguration.loadConfiguration(craftFile);
        }
    }
    
    // === GETTERS CONFIG PRINCIPAL ===
    
    public String getPrefix() {
        return config.getString("Kcraft.settings.prefix", "&8[&6Kcraft&8]");
    }
    
    public int getDebugLevel() {
        return config.getInt("Kcraft.settings.debug-level", 2);
    }
    
    public List<String> getEnabledWorlds() {
        return config.getStringList("Kcraft.settings.enabled-worlds");
    }
    
    public boolean isCacheEnabled() {
        return config.getBoolean("Kcraft.performance.cache-popular-crafts", true);
    }
    
    public boolean isInstantCompactEnabled() {
        return config.getBoolean("Kcraft.performance.instant-compact", true);
    }
    
    public boolean isAsyncLoggingEnabled() {
        return config.getBoolean("Kcraft.performance.async-logging", true);
    }
    
    public int getCacheSize() {
        return config.getInt("Kcraft.performance.smart-cache-size", 50);
    }

    public Set<Material> getBlockedVanillaCraftResults() {
        return materialSet("Kcraft.restrictions.blocked-vanilla-craft-results");
    }

    public Set<Material> getBlockedGeneratedLoot() {
        return materialSet("Kcraft.restrictions.blocked-generated-loot");
    }

    public boolean shouldCleanGeneratedLoot() {
        return config.getBoolean("Kcraft.restrictions.clean-generated-container-loot", true);
    }

    private Set<Material> materialSet(String path) {
        Set<Material> result = new LinkedHashSet<Material>();
        for (String name : config.getStringList(path)) {
            if (name == null) continue;
            try { result.add(Material.valueOf(name.trim().toUpperCase())); }
            catch (IllegalArgumentException invalid) {
                plugin.getLogger().warning("Materiau de restriction invalide: " + name);
            }
        }
        return result;
    }
    
    // === EFFETS ===
    
    public boolean areParticlesEnabled() {
        return config.getBoolean("Kcraft.effects.particles.enabled", false);
    }
    
    public boolean areSoundsEnabled() {
        return config.getBoolean("Kcraft.effects.sounds.enabled", true);
    }
    
    public boolean allowPerCraftEffects() {
        return config.getBoolean("Kcraft.effects.particles.allow-per-craft", true) ||
               config.getBoolean("Kcraft.effects.sounds.allow-per-craft", true);
    }
    
    // === GUI ===
    
    public boolean useBorders() {
        if (config.contains("gui.use-borders")) {
            return config.getBoolean("gui.use-borders", true);
        }
        return config.getBoolean("Kcraft.gui.use-borders", true);
    }
    
    public String getBorderColor() {
        return config.getString("Kcraft.gui.border-color", "GRAY");
    }
    
    public String getTitlePrefix() {
        return config.getString("Kcraft.gui.title-prefix", "&8");
    }
    
    // === LOGGING ===
    
    public boolean isLoggingEnabled() {
        return config.getBoolean("Kcraft.logging.enabled", true);
    }
    
    public String getLogFile() {
        return config.getString("Kcraft.logging.file", "crafts-history.json");
    }
    
    public boolean isPerPlayerLogging() {
        return config.getBoolean("Kcraft.logging.log-per-player", true);
    }
    
    public boolean isFactionStatsEnabled() {
        return config.getBoolean("Kcraft.logging.log-faction-stats", true);
    }
    
    public String getLogRotation() {
        return config.getString("Kcraft.logging.rotation", "DAILY");
    }
    
    public int getKeepDays() {
        return config.getInt("Kcraft.logging.keep-days", 30);
    }
    
    // === MESSAGES ===
    
    public String getMessage(String key) {
        return messagesConfig.getString("messages." + key, "&cMessage manquant: " + key);
    }
    
    public String getMessage(String key, String defaultValue) {
        return messagesConfig.getString("messages." + key, defaultValue);
    }
    
    // === GETTERS ===
    
    public FileConfiguration getConfig() {
        return config;
    }
    
    public FileConfiguration getMessagesConfig() {
        return messagesConfig;
    }
    
    public File getCraftsFolder() {
        return craftsFolder;
    }
    
    public File getLogsFolder() {
        return logsFolder;
    }
    
    // === NOUVELLES MÉTHODES GUI ===
    
    public String getGlassBorderColor() {
        return getStringWithFallback("Kcraft.gui.glass-colors.border", "gui.glass-colors.border", "GRAY");
    }
    
    public String getGlassCraftAreaColor() {
        return getStringWithFallback("Kcraft.gui.glass-colors.craft-area", "gui.glass-colors.craft-area", "GREEN");
    }
    
    public String getGlassResultAreaColor() {
        return getStringWithFallback("Kcraft.gui.glass-colors.result-area", "gui.glass-colors.result-area", "YELLOW");
    }
    
    public String getGlassButtonAreaColor() {
        return getStringWithFallback("Kcraft.gui.glass-colors.button-area", "gui.glass-colors.button-area", "BLUE");
    }
    
    public boolean useGUIBorders() {
        if (config.contains("gui.use-borders")) {
            return config.getBoolean("gui.use-borders", true);
        }
        return config.getBoolean("Kcraft.gui.use-borders", true);
    }
    
    public String getBorderItem() {
        return getStringWithFallback("Kcraft.gui.border-item", "gui.border-item", "STAINED_GLASS_PANE");
    }
    
    public boolean opsCanBypassPermissions() {
        return getBooleanWithFallback("Kcraft.settings.ops-bypass-permissions", "gui.ops-bypass-permissions", true);
    }

    private String getStringWithFallback(String primaryPath, String fallbackPath, String defaultValue) {
        if (config.contains(primaryPath)) {
            return config.getString(primaryPath, defaultValue);
        }
        return config.getString(fallbackPath, defaultValue);
    }

    private boolean getBooleanWithFallback(String primaryPath, String fallbackPath, boolean defaultValue) {
        if (config.contains(primaryPath)) {
            return config.getBoolean(primaryPath, defaultValue);
        }
        return config.getBoolean(fallbackPath, defaultValue);
    }
}
