package me.krunsh.kcraft;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import me.krunsh.kcraft.api.KcraftAPI;
import me.krunsh.kcraft.commands.KcraftCommand;
import me.krunsh.kcraft.commands.KcraftNPCCommand;
import me.krunsh.kcraft.config.ConfigManager;
import me.krunsh.kcraft.hooks.PluginHookManager;
import me.krunsh.kcraft.listeners.CraftGUIListener;
import me.krunsh.kcraft.listeners.GeneratedLootRestrictionListener;
import me.krunsh.kcraft.listeners.TableListener;
import me.krunsh.kcraft.listeners.VanillaCraftListener;
import me.krunsh.kcraft.managers.CacheManager;
import me.krunsh.kcraft.managers.CraftManager;
import me.krunsh.kcraft.managers.LoggingManager;
import me.krunsh.kcraft.managers.TableManager;
import me.krunsh.kcraft.managers.VanillaDebugManager;
import me.krunsh.kcraft.utils.MessageUtil;

/**
 * Kcraft - Plugin de craft custom pour serveur PvP faction 1.8.8
 * Architecture modulaire avec support NBT et intégrations multi-plugins
 * 
 * @author Krunsh
 * @version 1.0.0
 */
public final class Kcraft extends JavaPlugin {
    
    private static Kcraft instance;
    private static KcraftAPI api;
    
    // Core Managers
    private ConfigManager configManager;
    private CraftManager craftManager;
    private TableManager tableManager;
    private CacheManager cacheManager;
    private LoggingManager loggingManager;
    private VanillaDebugManager vanillaDebugManager;
    
    // Integrations
    private PluginHookManager hookManager;
    
    // Listeners
    private CraftGUIListener craftGUIListener;
    private VanillaCraftListener vanillaCraftListener;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Bannière de démarrage
        getLogger().info("╔══════════════════════════════╗");
        getLogger().info("║        Kcraft v1.0.0         ║");
        getLogger().info("║    Custom Crafting Plugin    ║");
        getLogger().info("║     Loading components...    ║");
        getLogger().info("╚══════════════════════════════╝");
        
        // Vérification version Spigot
        if (!checkSpigotVersion()) {
            getLogger().severe("Kcraft nécessite Spigot 1.8.8 ou compatible!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        try {
            // 1. Configuration
            configManager = new ConfigManager(this);
            configManager.loadConfigs();
            
            // 2. Utilitaires
            MessageUtil.init(configManager);
            
            // 3. Core Managers
            craftManager = new CraftManager(this);
            tableManager = new TableManager(this);
            cacheManager = new CacheManager(this);
            loggingManager = new LoggingManager(this);
            vanillaDebugManager = new VanillaDebugManager(this);
            
            // 4. Hooks pour autres plugins
            hookManager = new PluginHookManager();
            hookManager.loadHooks();
            
            // 5. Events
            registerEvents();
            
            // 6. Commandes
            registerCommands();
            
            // 7. API
            api = new KcraftAPI(this);
            
            // 8. Cache initial
            cacheManager.buildCache();
            
            getLogger().info("Kcraft activé avec succès! (" + craftManager.getCraftCount() + " crafts chargés)");
            
        } catch (Exception e) {
            getLogger().severe("Erreur lors du démarrage de Kcraft: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }
    
    @Override
    public void onDisable() {
        if (craftGUIListener != null) {
            craftGUIListener.shutdown();
        }

        if (vanillaCraftListener != null) {
            vanillaCraftListener.shutdown();
        }

        if (vanillaDebugManager != null) {
            vanillaDebugManager.shutdown();
        }

        if (loggingManager != null) {
            loggingManager.saveAll();
        }
        
        if (tableManager != null) {
            tableManager.savePlacedTables();
        }
        
        if (cacheManager != null) {
            cacheManager.clearCache();
        }
        
        getLogger().info("Kcraft désactivé proprement.");
        instance = null;
        api = null;
    }
    
    private boolean checkSpigotVersion() {
        String version = Bukkit.getVersion();
        return version.contains("1.8") || version.contains("Spigot");
    }
    
    private void registerEvents() {
        getServer().getPluginManager().registerEvents(new TableListener(this), this);
        craftGUIListener = new CraftGUIListener(this);
        getServer().getPluginManager().registerEvents(craftGUIListener, this);
        vanillaCraftListener = new VanillaCraftListener(this);
        getServer().getPluginManager().registerEvents(vanillaCraftListener, this);
        boolean unknownCraftExtension = vanillaCraftListener.registerUnknownCraftExtension();
        getLogger().info("Santé extension KHopeSpigot: "
            + vanillaDebugManager.healthSummary()
            + (unknownCraftExtension ? "; réception runtime en attente"
                : "; extension indisponible"));
        getServer().getPluginManager().registerEvents(new GeneratedLootRestrictionListener(this), this);
        
        // Listener pour le système de durabilité custom
        getServer().getPluginManager().registerEvents(new me.krunsh.kcraft.listeners.DurabilityListener(this), this);
        
        getLogger().info("Events enregistrés");
    }
    
    private void registerCommands() {
        // Commande principale /kcraft
        KcraftCommand kcraftCommand = new KcraftCommand(this);
        getCommand("kcraft").setExecutor(kcraftCommand);
        getCommand("kcraft").setTabCompleter(kcraftCommand);
        
        // Commande spéciale NPCs
        getCommand("kcraft-npc").setExecutor(new KcraftNPCCommand(this));
        
        getLogger().info("Commandes enregistrées");
    }
    
    /**
     * Reload complet du plugin
     */
    public void reload() {
        try {
            configManager.loadConfigs();
            craftManager.reloadCrafts();
            tableManager.reloadTables();
            hookManager.loadHooks();
            cacheManager.rebuildCache();
            MessageUtil.init(configManager);
            
            getLogger().info("Kcraft rechargé avec succès!");
        } catch (Exception e) {
            getLogger().severe("Erreur lors du reload: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
    
    // === GETTERS STATIQUES ===
    
    public static Kcraft getInstance() {
        return instance;
    }
    
    public static KcraftAPI getAPI() {
        return api;
    }
    
    // === GETTERS MANAGERS ===
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public CraftManager getCraftManager() {
        return craftManager;
    }
    
    public TableManager getTableManager() {
        return tableManager;
    }
    
    public CacheManager getCacheManager() {
        return cacheManager;
    }
    
    public LoggingManager getLoggingManager() {
        return loggingManager;
    }

    public VanillaDebugManager getVanillaDebugManager() {
        return vanillaDebugManager;
    }
    
    public PluginHookManager getHookManager() {
        return hookManager;
    }
    
    public CraftGUIListener getCraftGUIListener() {
        return craftGUIListener;
    }
}
