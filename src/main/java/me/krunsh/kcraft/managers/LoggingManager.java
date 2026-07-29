package me.krunsh.kcraft.managers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.entity.Player;

import me.krunsh.kcraft.Kcraft;
import me.krunsh.kcraft.utils.MessageUtil;

/**
 * Gestionnaire de logs JSON pour tracer l'activité des crafts
 */
public class LoggingManager {
    
    private final Kcraft plugin;
    private final SimpleDateFormat dateFormat;
    private final Map<UUID, Map<String, Object>> playerStats;
    private final Map<String, Object> globalStats;
    
    public LoggingManager(Kcraft plugin) {
        this.plugin = plugin;
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        this.playerStats = new ConcurrentHashMap<>();
        this.globalStats = new ConcurrentHashMap<>();
        
        initializeStats();
    }
    
    /**
     * Initialise les statistiques
     */
    private void initializeStats() {
        globalStats.put("total_crafts", 0);
        globalStats.put("unique_players", 0);
        globalStats.put("most_crafted", "");
        globalStats.put("rare_crafts", 0);
        globalStats.put("failed_crafts", 0);
        globalStats.put("last_update", dateFormat.format(new Date()));
    }
    
    /**
     * Log un craft réussi
     */
    public void logCraft(Player player, String craftId, String result, boolean success) {
        if (!plugin.getConfigManager().isLoggingEnabled()) {
            return;
        }
        
        try {
            // Créer l'entrée de log
            Map<String, Object> logEntry = createLogEntry(player, craftId, result, success);
            
            // Mettre à jour les stats
            updateStats(player, craftId, success);
            
            // Écrire en async si activé
            if (plugin.getConfigManager().isAsyncLoggingEnabled()) {
                plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                    writeLogEntry(logEntry);
                });
            } else {
                writeLogEntry(logEntry);
            }
            
        } catch (Exception e) {
            MessageUtil.debug("Erreur lors du log du craft: " + e.getMessage(), 1);
        }
    }
    
    /**
     * Crée une entrée de log détaillée
     */
    private Map<String, Object> createLogEntry(Player player, String craftId, String result, boolean success) {
        Map<String, Object> entry = new HashMap<>();
        
        // ID unique
        entry.put("id", "craft_" + String.format("%06d", (Integer) globalStats.get("total_crafts") + 1));
        
        // Timestamp
        entry.put("timestamp", dateFormat.format(new Date()));
        
        // Informations joueur
        Map<String, Object> playerInfo = new HashMap<>();
        playerInfo.put("name", player.getName());
        playerInfo.put("uuid", player.getUniqueId().toString());
        
        // Faction si disponible
        String faction = getFactionName(player);
        if (faction != null) {
            playerInfo.put("faction", faction);
            playerInfo.put("faction_power", getFactionPower(player));
        }
        
        entry.put("player", playerInfo);
        
        // Informations craft
        Map<String, Object> craftInfo = new HashMap<>();
        craftInfo.put("recipe_id", craftId);
        craftInfo.put("result", result);
        craftInfo.put("success", success);
        
        // TODO: Ajouter rareté si disponible
        craftInfo.put("rarity", "COMMON");
        
        entry.put("craft", craftInfo);
        
        // Localisation
        Map<String, Object> location = new HashMap<>();
        location.put("world", player.getWorld().getName());
        location.put("x", player.getLocation().getBlockX());
        location.put("y", player.getLocation().getBlockY());
        location.put("z", player.getLocation().getBlockZ());
        location.put("biome", player.getLocation().getBlock().getBiome().name());
        
        entry.put("location", location);
        
        return entry;
    }
    
    /**
     * Met à jour les statistiques
     */
    private void updateStats(Player player, String craftId, boolean success) {
        // Stats globales
        globalStats.put("total_crafts", (Integer) globalStats.get("total_crafts") + 1);
        globalStats.put("last_update", dateFormat.format(new Date()));
        
        if (!success) {
            globalStats.put("failed_crafts", (Integer) globalStats.get("failed_crafts") + 1);
        }
        
        // Stats par joueur
        UUID playerId = player.getUniqueId();
        Map<String, Object> playerStat = playerStats.computeIfAbsent(playerId, k -> new HashMap<>());
        
        playerStat.put("name", player.getName());
        playerStat.put("total_crafts", (Integer) playerStat.getOrDefault("total_crafts", 0) + 1);
        playerStat.put("last_craft", dateFormat.format(new Date()));
        
        // Faction
        String faction = getFactionName(player);
        if (faction != null) {
            playerStat.put("faction", faction);
        }
        
        // Craft favori (simple)
        String currentFavorite = (String) playerStat.get("favorite_craft");
        if (currentFavorite == null || currentFavorite.equals(craftId)) {
            playerStat.put("favorite_craft", craftId);
        }
        
        // Taux de réussite
        int totalCrafts = (Integer) playerStat.get("total_crafts");
        int successCount = (Integer) playerStat.getOrDefault("success_count", 0);
        if (success) {
            successCount++;
            playerStat.put("success_count", successCount);
        }
        
        double successRate = (double) successCount / totalCrafts * 100;
        playerStat.put("success_rate", String.format("%.1f%%", successRate));
        
        // Mettre à jour le nombre de joueurs uniques
        globalStats.put("unique_players", playerStats.size());
    }
    
    /**
     * Écrit une entrée de log dans le fichier JSON
     */
    private void writeLogEntry(Map<String, Object> logEntry) {
        // TODO: Implémenter l'écriture JSON proprement
        // Pour l'instant, log simple
        MessageUtil.debug("LOG: " + logEntry.get("player") + " -> " + logEntry.get("craft"), 1);
    }
    
    /**
     * Sauvegarde toutes les stats
     */
    public void saveAll() {
        if (!plugin.getConfigManager().isLoggingEnabled()) {
            return;
        }
        
        try {
            saveGlobalStats();
            if (plugin.getConfigManager().isPerPlayerLogging()) {
                savePlayerStats();
            }
            MessageUtil.debug("Stats sauvegardées", 2);
        } catch (Exception e) {
            plugin.getLogger().warning("Erreur lors de la sauvegarde des stats: " + e.getMessage());
        }
    }
    
    /**
     * Sauvegarde les stats globales
     */
    private void saveGlobalStats() throws IOException {
        File statsFile = new File(plugin.getConfigManager().getLogsFolder(), "stats-global.json");
        
        try (OutputStreamWriter writer = new OutputStreamWriter(
                new FileOutputStream(statsFile), 
                StandardCharsets.UTF_8)) {
            // TODO: Utiliser une vraie lib JSON (Gson par exemple)
            writer.write("{\n");
            writer.write("  \"global_stats\": {\n");
            
            boolean first = true;
            for (Map.Entry<String, Object> entry : globalStats.entrySet()) {
                if (!first) writer.write(",\n");
                writer.write("    \"" + entry.getKey() + "\": ");
                
                if (entry.getValue() instanceof String) {
                    writer.write("\"" + entry.getValue() + "\"");
                } else {
                    writer.write(entry.getValue().toString());
                }
                
                first = false;
            }
            
            writer.write("\n  }\n}");
        }
    }
    
    /**
     * Sauvegarde les stats par joueur
     */
    private void savePlayerStats() throws IOException {
        File playerStatsFile = new File(plugin.getConfigManager().getLogsFolder(), "stats-players.json");
        
        // TODO: Implémenter sauvegarde complète des stats joueurs
        MessageUtil.debug("Sauvegarde stats joueurs: " + playerStats.size() + " joueurs", 2);
    }
    
    /**
     * Obtient les stats d'un joueur
     */
    public Map<String, Object> getPlayerStats(UUID playerId) {
        return playerStats.get(playerId);
    }
    
    /**
     * Obtient les stats globales
     */
    public Map<String, Object> getGlobalStats() {
        return new HashMap<>(globalStats);
    }
    
    // === MÉTHODES UTILITAIRES ===
    
    /**
     * Obtient le nom de faction d'un joueur (via hook)
     */
    private String getFactionName(Player player) {
        // TODO: Implémenter via PluginHookManager quand disponible
        return null;
    }
    
    /**
     * Obtient la puissance de faction d'un joueur
     */
    private int getFactionPower(Player player) {
        // TODO: Implémenter via PluginHookManager quand disponible
        return 0;
    }
}