package me.krunsh.kcraft.managers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import me.krunsh.kcraft.Kcraft;
import me.krunsh.kcraft.models.CraftTable;
import me.krunsh.kcraft.models.PlacedTableData;
import me.krunsh.kcraft.utils.MaterialResolver;
import me.krunsh.kcraft.utils.MessageUtil;

/**
 * Gestionnaire des tables de craft (physiques et virtuelles)
 */
public class TableManager {
    
    private final Kcraft plugin;
    private final Map<String, CraftTable> tables;
    // Cache des tables custom placées dans le monde (coordonnées -> table_id)
    private final Map<String, String> placedCustomTables;
    // Données complètes des tables placées (pour sauvegarde JSON)
    private final Map<String, PlacedTableData> placedTablesData;
    private final Gson gson;
    private final File dataFile;
    
    public TableManager(Kcraft plugin) {
        this.plugin = plugin;
        this.tables = new HashMap<>();
        this.placedCustomTables = new HashMap<>();
        this.placedTablesData = new HashMap<>();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.dataFile = new File(plugin.getDataFolder(), "placed_tables.json");
        
        loadTables();
        loadPlacedTables();
    }
    
    /**
     * Charge toutes les tables depuis la config
     */
    public void loadTables() {
        tables.clear();
        
        // Tables physiques (blocs dans le monde)
        loadPhysicalTables();
        
        // Tables virtuelles (commandes)
        loadVirtualTables();
        
        MessageUtil.log("Tables chargées: " + tables.size());
    }
    
    /**
     * Charge les tables physiques depuis custom-blocks
     */
    private void loadPhysicalTables() {
        ConfigurationSection customBlocks = plugin.getConfig().getConfigurationSection("Kcraft.custom-blocks");
        if (customBlocks == null) return;
        
        for (String tableId : customBlocks.getKeys(false)) {
            ConfigurationSection tableSection = customBlocks.getConfigurationSection(tableId);
            if (tableSection == null) continue;
            
            try {
                CraftTable table = parsePhysicalTable(tableId, tableSection);
                if (table != null) {
                    tables.put(tableId, table);
                    MessageUtil.debug("Table physique chargée: " + tableId, 2);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Erreur lors du chargement de la table " + tableId + ": " + e.getMessage());
            }
        }
    }
    
    /**
     * Charge les tables virtuelles depuis command-tables
     */
    private void loadVirtualTables() {
        ConfigurationSection commandTables = plugin.getConfig().getConfigurationSection("Kcraft.command-tables");
        if (commandTables == null) return;
        
        for (String tableId : commandTables.getKeys(false)) {
            ConfigurationSection tableSection = commandTables.getConfigurationSection(tableId);
            if (tableSection == null) continue;
            
            try {
                CraftTable table = parseVirtualTable(tableId, tableSection);
                if (table != null) {
                    tables.put(tableId, table);
                    MessageUtil.debug("Table virtuelle chargée: " + tableId, 2);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Erreur lors du chargement de la table " + tableId + ": " + e.getMessage());
            }
        }
    }
    
    /**
     * Parse une table physique
     */
    private CraftTable parsePhysicalTable(String id, ConfigurationSection section) {
        CraftTable table = new CraftTable(id);
        table.setPhysical(true);
        
        // Bloc associé (supporte format MATERIAL:DATA pour 1.8.8)
        String blockStr = section.getString("block");
        if (blockStr != null) {
            try {
                if (blockStr.contains(":")) {
                    // Format MATERIAL:DATA (ex: STONE:2 pour Polished Granite)
                    String[] parts = blockStr.split(":", 2);
                    Material blockType = MaterialResolver.resolve(parts[0]);
                    if (blockType == null) {
                        throw new IllegalArgumentException("Materiau inconnu: " + parts[0]);
                    }
                    byte dataValue = Byte.parseByte(parts[1]);
                    table.setBlockType(blockType);
                    table.setBlockDataValue(dataValue);
                } else {
                    Material blockType = MaterialResolver.resolve(blockStr);
                    if (blockType == null) {
                        throw new IllegalArgumentException("Materiau inconnu: " + blockStr);
                    }
                    table.setBlockType(blockType);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Bloc invalide pour table " + id + ": " + blockStr);
                return null;
            }
        }
        
        // Propriétés communes
        parseCommonProperties(table, section);
        
        return table;
    }
    
    /**
     * Parse une table virtuelle
     */
    private CraftTable parseVirtualTable(String id, ConfigurationSection section) {
        CraftTable table = new CraftTable(id);
        table.setPhysical(false);
        
        // Commande
        table.setCommand(section.getString("command"));
        table.setConsoleOnly(section.getBoolean("only-console-npc", false));
        
        // Propriétés communes
        parseCommonProperties(table, section);
        
        return table;
    }
    
    /**
     * Parse les propriétés communes aux deux types de tables
     */
    private void parseCommonProperties(CraftTable table, ConfigurationSection section) {
        // Nom affiché (compat: "name" prioritaire, fallback "title")
        String name = section.getString("name", section.getString("title"));
        if (name != null) {
            table.setName(MessageUtil.colorize(name));
        }
        
        // Taille
        table.setSize(section.getString("size", "3x3"));
        
        // Permission
        table.setPermission(section.getString("permission"));
        
        // Effets
        table.setParticle(section.getString("particle"));
        table.setSoundOpen(section.getString("sound-open"));
        table.setSoundClose(section.getString("sound-close"));
    }
    
    /**
     * Obtient une table par son ID
     */
    public CraftTable getTable(String id) {
        return tables.get(id);
    }
    
    /**
     * Obtient une table par type de bloc (pour detection physique)
     */
    public CraftTable getTableByBlock(Material blockType) {
        for (CraftTable table : tables.values()) {
            if (table.isPhysical() && table.getBlockType() == blockType) {
                return table;
            }
        }
        return null;
    }
    
    /**
     * Obtient toutes les tables
     */
    public Map<String, CraftTable> getAllTables() {
        return new HashMap<>(tables);
    }
    
    /**
     * Obtient toutes les tables physiques
     */
    public Map<String, CraftTable> getPhysicalTables() {
        Map<String, CraftTable> physicalTables = new HashMap<>();
        for (Map.Entry<String, CraftTable> entry : tables.entrySet()) {
            if (entry.getValue().isPhysical()) {
                physicalTables.put(entry.getKey(), entry.getValue());
            }
        }
        return physicalTables;
    }
    
    /**
     * Obtient toutes les tables virtuelles
     */
    public Map<String, CraftTable> getVirtualTables() {
        Map<String, CraftTable> virtualTables = new HashMap<>();
        for (Map.Entry<String, CraftTable> entry : tables.entrySet()) {
            if (!entry.getValue().isPhysical()) {
                virtualTables.put(entry.getKey(), entry.getValue());
            }
        }
        return virtualTables;
    }
    
    /**
     * Vérifie si un bloc est une table de craft
     */
    public boolean isTable(Material blockType) {
        return getTableByBlock(blockType) != null;
    }
    
    /**
     * Recharge toutes les tables
     */
    public void reloadTables() {
        MessageUtil.log("Rechargement des tables...");
        loadTables();
    }
    
    // === MÉTHODES POUR TABLES CUSTOM PLACÉES ===
    
    /**
     * Enregistre une table custom placée dans le monde
     */
    public void registerPlacedCustomTable(org.bukkit.block.Block block, String tableId, String playerName) {
        String key = getBlockKey(block);
        
        // Créer les données
        PlacedTableData data = new PlacedTableData(
            block.getWorld().getName(),
            block.getX(),
            block.getY(),
            block.getZ(),
            tableId,
            playerName
        );
        
        placedCustomTables.put(key, tableId);
        placedTablesData.put(key, data);
        
        MessageUtil.debug("Table custom enregistrée: " + key + " -> " + tableId + " par " + playerName, 2);
        
        // Sauvegarder immédiatement
        savePlacedTables();
    }
    
    /**
     * Supprime une table custom du cache
     */
    public void unregisterPlacedCustomTable(org.bukkit.block.Block block, String playerName) {
        String key = getBlockKey(block);
        
        // Marquer comme cassée au lieu de supprimer (pour historique)
        PlacedTableData data = placedTablesData.get(key);
        if (data != null) {
            data.setBrokenBy(playerName);
            data.setBrokenAt(System.currentTimeMillis());
            MessageUtil.debug("Table custom cassée: " + key + " par " + playerName, 2);
        }
        
        // Retirer du cache actif
        placedCustomTables.remove(key);
        
        // Sauvegarder immédiatement
        savePlacedTables();
    }
    
    /**
     * Vérifie si un bloc est une table custom placée
     */
    @SuppressWarnings("deprecation")
    public boolean isCustomTableBlock(org.bukkit.block.Block block) {
        if (block == null) {
            return false;
        }
        
        // Vérifier d'abord si c'est enregistré dans le cache
        String key = getBlockKey(block);
        if (!placedCustomTables.containsKey(key)) {
            return false;
        }
        
        // Vérifier que le type de bloc correspond à une table configurée
        String tableId = placedCustomTables.get(key);
        CraftTable table = tables.get(tableId);
        if (table == null || !table.isPhysical()) {
            return false;
        }
        
        // Vérifier le type de bloc et la data value
        if (block.getType() != table.getBlockType()) {
            return false;
        }
        
        // Vérifier la data value si spécifiée (pour STONE:2 = Polished Granite)
        if (table.getBlockDataValue() != 0) {
            return block.getData() == table.getBlockDataValue();
        }
        
        return true;
    }
    
    /**
     * Récupère l'ID de la table custom d'un bloc
     */
    public String getCustomTableId(org.bukkit.block.Block block) {
        String key = getBlockKey(block);
        return placedCustomTables.get(key);
    }
    
    /**
     * Crée une clé unique pour un bloc (monde:x:y:z)
     */
    private String getBlockKey(org.bukkit.block.Block block) {
        return block.getWorld().getName() + ":" + 
               block.getX() + ":" + 
               block.getY() + ":" + 
               block.getZ();
    }
    
    // === SAUVEGARDE/CHARGEMENT PERSISTANT ===
    
    /**
     * Charge les tables placées depuis le fichier JSON
     */
    private void loadPlacedTables() {
        if (!dataFile.exists()) {
            MessageUtil.debug("Aucune donnée de tables placées trouvée", 1);
            return;
        }
        
        try (FileReader reader = new FileReader(dataFile)) {
            List<PlacedTableData> dataList = gson.fromJson(reader, 
                new TypeToken<List<PlacedTableData>>(){}.getType());
            
            if (dataList == null) {
                MessageUtil.debug("Fichier de tables placées vide", 1);
                return;
            }
            
            int loaded = 0;
            for (PlacedTableData data : dataList) {
                // Ignorer les tables cassées (optionnel, pour historique)
                if (data.getBrokenBy() != null) {
                    continue;
                }
                
                String key = data.getLocationKey();
                placedCustomTables.put(key, data.getTableId());
                placedTablesData.put(key, data);
                loaded++;
            }
            
            MessageUtil.log("Tables placées chargées: " + loaded);
            
        } catch (IOException e) {
            plugin.getLogger().warning("Erreur lors du chargement des tables placées: " + e.getMessage());
        }
    }
    
    /**
     * Sauvegarde les tables placées dans le fichier JSON
     */
    public void savePlacedTables() {
        try {
            // Créer le dossier si nécessaire
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            
            // Convertir la map en liste pour JSON
            List<PlacedTableData> dataList = new ArrayList<>(placedTablesData.values());
            
            // Écrire dans le fichier avec UTF-8 explicite
            try (OutputStreamWriter writer = new OutputStreamWriter(
                    new FileOutputStream(dataFile), 
                    StandardCharsets.UTF_8)) {
                gson.toJson(dataList, writer);
            }
            
            MessageUtil.debug("Tables placées sauvegardées: " + dataList.size(), 1);
            
        } catch (IOException e) {
            plugin.getLogger().warning("Erreur lors de la sauvegarde des tables placées: " + e.getMessage());
        }
    }
}