package me.krunsh.kcraft.listeners;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import me.krunsh.kcraft.Kcraft;
import me.krunsh.kcraft.gui.CraftTableGUI;
import me.krunsh.kcraft.models.CraftTable;
import me.krunsh.kcraft.utils.MessageUtil;
import me.krunsh.kcraft.utils.NBTUtil;

/**
 * Listener pour les interactions avec les tables de craft
 */
public class TableListener implements Listener {
    
    private final Kcraft plugin;
    
    public TableListener(Kcraft plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) {
            return;
        }
        
        Player player = event.getPlayer();
        
        // Vérifier si c'est une table custom placée (enregistrée)
        if (plugin.getTableManager().isCustomTableBlock(clickedBlock)) {
            // C'est une table custom enregistrée - récupérer son ID
            String tableId = plugin.getTableManager().getCustomTableId(clickedBlock);
            CraftTable table = plugin.getTableManager().getTable(tableId);
            
            if (table == null) {
                MessageUtil.sendError(player, "table.invalid-custom");
                return;
            }
            
            // Annuler l'event pour éviter l'ouverture du GUI vanilla
            event.setCancelled(true);
            
            // Vérifications
            if (!canUseTable(player, table)) {
                return;
            }
            
            // Vérifier le monde
            if (!isWorldEnabled(player)) {
                MessageUtil.sendError(player, "craft.wrong-world");
                return;
            }
            
            // Ouvrir la GUI custom
            openTableGUI(player, table);
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        Block placedBlock = event.getBlockPlaced();
        
        // Vérifier si c'est une table custom du plugin
        if (NBTUtil.isCustomCraftTable(item)) {
            String tableId = NBTUtil.getCustomTableId(item);
            if (tableId != null) {
                // Enregistrer la table comme custom avec le nom du joueur
                Player player = event.getPlayer();
                plugin.getTableManager().registerPlacedCustomTable(placedBlock, tableId, player.getName());
                
                MessageUtil.sendInfo(player, "table.custom-placed", 
                    MessageUtil.placeholders("table", tableId));
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        
        // Vérifier si c'est une table custom
        if (plugin.getTableManager().isCustomTableBlock(block)) {
            String tableId = plugin.getTableManager().getCustomTableId(block);
            Player player = event.getPlayer();
            
            // Supprimer du cache avec le nom du joueur
            plugin.getTableManager().unregisterPlacedCustomTable(block, player.getName());
            
            // Optionnel: Dropper l'item avec NBT
            if (tableId != null && !player.getGameMode().equals(org.bukkit.GameMode.CREATIVE)) {
                CraftTable table = plugin.getTableManager().getTable(tableId);
                
                // Créer l'item avec le bon type de bloc et data value
                Material blockType = (table != null && table.getBlockType() != null) 
                    ? table.getBlockType() : Material.WORKBENCH;
                byte blockDataValue = (table != null) ? table.getBlockDataValue() : 0;
                
                ItemStack customTable = NBTUtil.createCustomCraftTable(tableId, blockType, blockDataValue);
                org.bukkit.inventory.meta.ItemMeta meta = customTable.getItemMeta();
                
                if (table != null) {
                    meta.setDisplayName("§6Table de Craft " + table.getName());
                }
                customTable.setItemMeta(meta);
                
                // Empêcher le drop normal du bloc
                event.setCancelled(true);
                block.setType(Material.AIR);
                
                block.getWorld().dropItemNaturally(block.getLocation(), customTable);
            }
        }
    }
    
    /**
     * Vérifie si un joueur peut utiliser une table
     */
    private boolean canUseTable(Player player, CraftTable table) {
        if (!table.canUse(player)) {
            MessageUtil.sendError(player, "table.no-access");
            return false;
        }
        
        return true;
    }
    
    /**
     * Vérifie si le monde est activé pour Kcraft
     */
    private boolean isWorldEnabled(Player player) {
        String worldName = player.getWorld().getName();
        return plugin.getConfigManager().getEnabledWorlds().isEmpty() ||
               plugin.getConfigManager().getEnabledWorlds().contains(worldName);
    }
    
    /**
     * Ouvre la GUI de craft pour une table
     */
    private void openTableGUI(Player player, CraftTable table) {
        try {
            // Jouer le son d'ouverture si configuré
            if (table.getSoundOpen() != null && plugin.getConfigManager().areSoundsEnabled()) {
                me.krunsh.kcraft.utils.SoundUtil.playSafe(player, table.getSoundOpen());
            }
            
            // Créer et ouvrir la GUI
            CraftTableGUI gui = new CraftTableGUI(plugin, player, table);
            gui.open();
            
            // Message d'info
            MessageUtil.sendInfo(player, "table.open", 
                MessageUtil.placeholders("table", table.getName()));
            
            MessageUtil.debug("GUI ouverte pour " + player.getName() + " - table: " + table.getId(), 2);
            
        } catch (Exception e) {
            plugin.getLogger().warning("Erreur lors de l'ouverture de la GUI pour " + player.getName() + ": " + e.getMessage());
            MessageUtil.sendError(player, "table.no-access");
        }
    }
}