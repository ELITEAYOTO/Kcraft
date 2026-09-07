package me.krunsh.kcraft.managers;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import me.krunsh.kcraft.Kcraft;
import me.krunsh.kcraft.models.CraftTable;
import me.krunsh.kcraft.models.PlacedTableData;
import me.krunsh.kcraft.persistence.TablePersistenceBuffer;
import me.krunsh.kcraft.utils.MaterialResolver;
import me.krunsh.kcraft.utils.MessageUtil;

/** TableManager V2.7 : persistance placée avec debounce/async. */
public class TableManager {

    private final Kcraft plugin;
    private volatile Map<String, CraftTable> tables = Collections.emptyMap();
    private final Map<String, String> placedCustomTables = new HashMap<String, String>();
    private final Map<String, PlacedTableData> placedTablesData = new HashMap<String, PlacedTableData>();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final File dataFile;
    private final TablePersistenceBuffer persistence;

    public TableManager(Kcraft plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "placed_tables.json");
        this.persistence = new TablePersistenceBuffer(
            plugin, dataFile, gson,
            new TablePersistenceBuffer.SnapshotProvider() {
                @Override public List<PlacedTableData> snapshot() {
                    return snapshotPlacedTables();
                }
            },
            40L
        );
        loadTables();
        loadPlacedTables();
    }

    /** Chargement initial; les rechargements runtime passent par reloadTables(). */
    public void loadTables() {
        activateTables(prepareTables(plugin.getConfig()));
        MessageUtil.log("Tables V2 chargées: " + tables.size());
    }

    /** Prépare les définitions sans toucher aux tables actives ni à leur persistance. */
    public Map<String, CraftTable> prepareTables(FileConfiguration candidateConfig) {
        return prepareTableDefinitions(candidateConfig);
    }

    /** Publication sur le thread principal d'une map retournée par prepareTables(). */
    public void activateTables(Map<String, CraftTable> prepared) {
        tables = prepared;
    }

    static Map<String, CraftTable> prepareTableDefinitions(FileConfiguration candidateConfig) {
        if (candidateConfig == null) throw new IllegalArgumentException("config tables absente");
        if (candidateConfig.contains("Kcraft") && !candidateConfig.isConfigurationSection("Kcraft")) {
            throw new IllegalArgumentException("Kcraft: section YAML attendue");
        }
        Map<String, CraftTable> prepared = new LinkedHashMap<String, CraftTable>();
        prepareSection(candidateConfig, "Kcraft.custom-blocks", true, prepared);
        prepareSection(candidateConfig, "Kcraft.command-tables", false, prepared);
        return Collections.unmodifiableMap(prepared);
    }

    private static void prepareSection(FileConfiguration config, String path, boolean physical,
                                       Map<String, CraftTable> prepared) {
        if (!config.contains(path)) return;
        ConfigurationSection tablesSection = config.getConfigurationSection(path);
        if (tablesSection == null) throw new IllegalArgumentException(path + ": section YAML attendue");
        for (String tableId : tablesSection.getKeys(false)) {
            ConfigurationSection section = tablesSection.getConfigurationSection(tableId);
            if (section == null) {
                throw new IllegalArgumentException("Table '" + tableId + "' dans " + path
                    + ": section YAML attendue");
            }
            if (prepared.containsKey(tableId)) {
                throw new IllegalArgumentException("Table '" + tableId
                    + "': identifiant dupliqué entre tables physiques et virtuelles");
            }
            try {
                prepared.put(tableId, physical ? parsePhysicalTable(tableId, section)
                    : parseVirtualTable(tableId, section));
            } catch (IllegalArgumentException invalid) {
                throw new IllegalArgumentException("Table '" + tableId + "' dans " + path
                    + ": " + invalid.getMessage(), invalid);
            }
        }
    }

    private static CraftTable parsePhysicalTable(String id, ConfigurationSection section) {
        CraftTable table = new CraftTable(id);
        table.setPhysical(true);
        String blockStr = section.getString("block");
        if (blockStr == null || blockStr.trim().isEmpty()) throw new IllegalArgumentException("champ block obligatoire");
        if (blockStr.contains(":")) {
            String[] parts = blockStr.split(":", 2);
            Material type = MaterialResolver.resolve(parts[0]);
            validateBlockMaterial(type, parts[0]);
            table.setBlockType(type);
            table.setBlockDataValue(Byte.parseByte(parts[1]));
        } else {
            Material type = MaterialResolver.resolve(blockStr);
            validateBlockMaterial(type, blockStr);
            table.setBlockType(type);
        }
        parseCommon(table, section);
        return table;
    }

    private static void validateBlockMaterial(Material type, String configured) {
        if (type == null || type == Material.AIR || !type.isBlock()) {
            throw new IllegalArgumentException("materiau de bloc invalide: " + configured);
        }
    }

    private static CraftTable parseVirtualTable(String id, ConfigurationSection section) {
        CraftTable table = new CraftTable(id);
        table.setPhysical(false);
        table.setCommand(section.getString("command"));
        table.setConsoleOnly(section.getBoolean("only-console-npc", false));
        parseCommon(table, section);
        return table;
    }

    private static void parseCommon(CraftTable table, ConfigurationSection section) {
        String name = section.getString("name");
        if (name != null) table.setName(MessageUtil.colorize(name));
        table.setSize(section.getString("size", "3x3"));
        table.setPermission(section.getString("permission"));
        table.setParticle(section.getString("particle"));
        table.setSoundOpen(section.getString("sound-open"));
        table.setSoundClose(section.getString("sound-close"));
    }

    public CraftTable getTable(String id) { return tables.get(id); }

    public CraftTable getTableByBlock(Material type) {
        for (CraftTable table : tables.values()) {
            if (table.isPhysical() && table.getBlockType() == type) return table;
        }
        return null;
    }

    public Map<String, CraftTable> getAllTables() { return new HashMap<String, CraftTable>(tables); }

    public Map<String, CraftTable> getPhysicalTables() {
        Map<String, CraftTable> result = new HashMap<String, CraftTable>();
        for (Map.Entry<String, CraftTable> entry : tables.entrySet()) {
            if (entry.getValue().isPhysical()) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    public Map<String, CraftTable> getVirtualTables() {
        Map<String, CraftTable> result = new HashMap<String, CraftTable>();
        for (Map.Entry<String, CraftTable> entry : tables.entrySet()) {
            if (!entry.getValue().isPhysical()) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }
    public boolean isTable(Material type) { return getTableByBlock(type) != null; }
    public void reloadTables() { plugin.reload(); }

    public void registerPlacedCustomTable(org.bukkit.block.Block block, String tableId, String playerName) {
        String key = getBlockKey(block);
        PlacedTableData data = new PlacedTableData(block.getWorld().getName(), block.getX(), block.getY(), block.getZ(), tableId, playerName);
        placedCustomTables.put(key, tableId);
        placedTablesData.put(key, data);
        persistence.markDirty();
    }

    public void unregisterPlacedCustomTable(org.bukkit.block.Block block, String playerName) {
        String key = getBlockKey(block);
        PlacedTableData data = placedTablesData.get(key);
        if (data != null) {
            data.setBrokenBy(playerName);
            data.setBrokenAt(System.currentTimeMillis());
        }
        placedCustomTables.remove(key);
        persistence.markDirty();
    }

    @SuppressWarnings("deprecation")
    public boolean isCustomTableBlock(org.bukkit.block.Block block) {
        if (block == null) return false;
        String tableId = placedCustomTables.get(getBlockKey(block));
        if (tableId == null) return false;
        CraftTable table = tables.get(tableId);
        if (table == null || !table.isPhysical()) return false;
        if (block.getType() != table.getBlockType()) return false;
        return table.getBlockDataValue() == 0 || block.getData() == table.getBlockDataValue();
    }

    public String getCustomTableId(org.bukkit.block.Block block) { return placedCustomTables.get(getBlockKey(block)); }

    private String getBlockKey(org.bukkit.block.Block block) {
        return block.getWorld().getName() + ":" + block.getX() + ":" + block.getY() + ":" + block.getZ();
    }

    private void loadPlacedTables() {
        if (!dataFile.exists()) return;
        try (FileReader reader = new FileReader(dataFile)) {
            List<PlacedTableData> list = gson.fromJson(reader, new TypeToken<List<PlacedTableData>>(){}.getType());
            if (list == null) return;
            for (PlacedTableData data : list) {
                placedTablesData.put(data.getLocationKey(), data);
                if (data.getBrokenBy() == null) placedCustomTables.put(data.getLocationKey(), data.getTableId());
            }
        } catch (IOException error) {
            plugin.getLogger().warning("Erreur chargement tables placées: " + error.getMessage());
        }
    }

    private List<PlacedTableData> snapshotPlacedTables() {
        return new ArrayList<PlacedTableData>(placedTablesData.values());
    }

    public void savePlacedTables() {
        persistence.flushNow(snapshotPlacedTables());
    }

    public long getPersistenceWrites() { return persistence.getWrites(); }
    public long getPersistenceCoalesced() { return persistence.getCoalesced(); }
    public long getPersistenceFailures() { return persistence.getFailures(); }
    public void resetPersistenceMetrics() { persistence.resetMetrics(); }
}
