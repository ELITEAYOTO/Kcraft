package me.krunsh.kcraft.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import me.krunsh.kcraft.Kcraft;
import me.krunsh.kcraft.models.CraftTable;
import me.krunsh.kcraft.utils.MessageUtil;

/**
 * GUI pour les tables de craft custom
 * Interface adaptative selon la taille de la table
 */
public class CraftTableGUI {
    
    private final Kcraft plugin;
    private final UUID playerId;
    private final CraftTable table;
    private final Inventory inventory;
    
    // Layout de la GUI
    private final int[] craftSlots;
    private final int resultSlot;
    private final int craftButtonSlot;
    
    // Bordures decoratives calculees une seule fois par taille
    private final int[] craftAreaBorder;
    private final int[] resultAreaBorder;
    private final int[] buttonAreaBorder;

    public CraftTableGUI(Kcraft plugin, Player player, CraftTable table) {
        this.plugin = plugin;
        this.playerId = player.getUniqueId();
        this.table = table;

        // Inventaire 54 slots (6 lignes) pour toutes les tailles supportees
        String title = MessageUtil.colorize(plugin.getConfigManager().getTitlePrefix() + table.getName());
        this.inventory = Bukkit.createInventory(null, 54, title);

        // Selection du layout selon la taille declaree (3x3, 4x4, 5x5)
        // Tailles > 5x5 retombent sur 4x4 avec un warning
        String size = table.getSize() == null ? "4x4" : table.getSize().toLowerCase();
        switch (size) {
            case "3x3":
                this.craftSlots = new int[]{
                    10, 11, 12,
                    19, 20, 21,
                    28, 29, 30
                };
                this.resultSlot = 24;
                this.craftButtonSlot = 49;
                this.craftAreaBorder = new int[]{9, 13, 18, 22, 27, 31};
                this.resultAreaBorder = new int[]{15, 16, 17, 25, 26, 33, 34, 35};
                this.buttonAreaBorder = new int[]{48, 50};
                break;
            case "5x5":
                this.craftSlots = new int[]{
                    0,  1,  2,  3,  4,
                    9, 10, 11, 12, 13,
                    18, 19, 20, 21, 22,
                    27, 28, 29, 30, 31,
                    36, 37, 38, 39, 40
                };
                this.resultSlot = 26;
                this.craftButtonSlot = 49;
                this.craftAreaBorder = new int[]{5, 14, 23, 32, 41};
                this.resultAreaBorder = new int[]{7, 8, 16, 17, 25, 33, 34, 35};
                this.buttonAreaBorder = new int[]{48, 50};
                break;
            case "4x4":
                this.craftSlots = new int[]{
                    10, 11, 12, 13,
                    19, 20, 21, 22,
                    28, 29, 30, 31,
                    37, 38, 39, 40
                };
                this.resultSlot = 24;
                this.craftButtonSlot = 49;
                this.craftAreaBorder = new int[]{9, 14, 18, 23, 27, 32, 36, 41};
                this.resultAreaBorder = new int[]{15, 16, 17, 25, 26, 33, 34, 35};
                this.buttonAreaBorder = new int[]{48, 50};
                break;
            default:
                plugin.getLogger().warning("Taille de table non supportee: '" + size
                        + "' pour la table '" + table.getId() + "' (supportees: 3x3, 4x4, 5x5). Fallback 4x4.");
                this.craftSlots = new int[]{
                    10, 11, 12, 13,
                    19, 20, 21, 22,
                    28, 29, 30, 31,
                    37, 38, 39, 40
                };
                this.resultSlot = 24;
                this.craftButtonSlot = 49;
                this.craftAreaBorder = new int[]{9, 14, 18, 23, 27, 32, 36, 41};
                this.resultAreaBorder = new int[]{15, 16, 17, 25, 26, 33, 34, 35};
                this.buttonAreaBorder = new int[]{48, 50};
                break;
        }

        setupGUI();
    }
    
    /**
     * Configure l'interface de la GUI avec vitres colorées
     */
    private void setupGUI() {
        // Remplir d'abord toute la GUI avec des bordures grises
        if (plugin.getConfigManager().useGUIBorders()) {
            fillWithColoredGlass();
        }
        
        // Bouton de craft
        setupCraftButton();
        
        // Slot de résultat (vide pour l'instant)
        inventory.setItem(resultSlot, createResultPlaceholder());

        MessageUtil.debug("GUI " + table.getSize() + " configuree - Slots craft: " + craftSlots.length +
                         ", Resultat: " + resultSlot +
                         ", Bouton: " + craftButtonSlot, 2);
    }
    
    /**
     * Remplit la GUI avec des vitres colorées selon les zones
     */
    private void fillWithColoredGlass() {
        // Remplir d'abord avec les bordures grises générales
        ItemStack borderGlass = createColoredGlass(plugin.getConfigManager().getGlassBorderColor());
        for (int i = 0; i < inventory.getSize(); i++) {
            if (!isCraftSlot(i) && i != resultSlot && i != craftButtonSlot) {
                inventory.setItem(i, borderGlass);
            }
        }
        
        // Vitres autour de la zone de craft (taille dependante)
        ItemStack craftAreaGlass = createColoredGlass(plugin.getConfigManager().getGlassCraftAreaColor());
        for (int slot : craftAreaBorder) {
            if (slot < inventory.getSize() && !isCraftSlot(slot) && slot != resultSlot && slot != craftButtonSlot) {
                inventory.setItem(slot, craftAreaGlass);
            }
        }

        // Vitres autour du resultat
        ItemStack resultAreaGlass = createColoredGlass(plugin.getConfigManager().getGlassResultAreaColor());
        for (int slot : resultAreaBorder) {
            if (slot < inventory.getSize() && !isCraftSlot(slot) && slot != resultSlot && slot != craftButtonSlot) {
                inventory.setItem(slot, resultAreaGlass);
            }
        }

        // Vitres autour du bouton
        ItemStack buttonAreaGlass = createColoredGlass(plugin.getConfigManager().getGlassButtonAreaColor());
        for (int slot : buttonAreaBorder) {
            if (slot < inventory.getSize() && !isCraftSlot(slot) && slot != resultSlot && slot != craftButtonSlot) {
                inventory.setItem(slot, buttonAreaGlass);
            }
        }
    }
    
    /**
     * Crée une vitre colorée selon la couleur spécifiée (1.8.8 compatible)
     */
    private ItemStack createColoredGlass(String colorName) {
        Material glassMaterial;
        short durability = 0;
        
        try {
            // 1.8.8 utilise STAINED_GLASS_PANE avec durability pour la couleur
            glassMaterial = Material.valueOf("STAINED_GLASS_PANE");
            
            // Correspondance couleur -> durability pour 1.8.8
            switch (colorName.toUpperCase()) {
                case "WHITE": durability = 0; break;
                case "ORANGE": durability = 1; break;
                case "MAGENTA": durability = 2; break;
                case "LIGHT_BLUE": durability = 3; break;
                case "YELLOW": durability = 4; break;
                case "LIME": durability = 5; break;
                case "PINK": durability = 6; break;
                case "GRAY": durability = 7; break;
                case "LIGHT_GRAY": durability = 8; break;
                case "CYAN": durability = 9; break;
                case "PURPLE": durability = 10; break;
                case "BLUE": durability = 11; break;
                case "BROWN": durability = 12; break;
                case "GREEN": durability = 13; break;
                case "RED": durability = 14; break;
                case "BLACK": durability = 15; break;
                default: durability = 7; break; // GRAY par défaut
            }
            
        } catch (IllegalArgumentException e) {
            // Fallback sur GLASS_PANE normal
            glassMaterial = Material.valueOf("GLASS_PANE");
        }
        
        ItemStack glass = new ItemStack(glassMaterial, 1, durability);
        
        // Nom vide pour éviter l'affichage
        ItemMeta meta = glass.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§r");
            glass.setItemMeta(meta);
        }
        
        return glass;
    }
    
    /**
     * Configure le bouton de craft
     */
    private void setupCraftButton() {
        ItemStack craftButton = new ItemStack(Material.WORKBENCH);
        ItemMeta meta = craftButton.getItemMeta();
        
        meta.setDisplayName(MessageUtil.colorize(plugin.getConfigManager().getMessage("gui.craft-button")));
        
        List<String> lore = new ArrayList<>();
        for (String line : plugin.getConfigManager().getMessagesConfig().getStringList("messages.gui.craft-button-lore")) {
            lore.add(MessageUtil.colorize(line));
        }
        meta.setLore(lore);
        
        craftButton.setItemMeta(meta);
        inventory.setItem(craftButtonSlot, craftButton);
    }
    
    /**
     * Crée le placeholder du slot de résultat
     */
    private ItemStack createResultPlaceholder() {
        ItemStack placeholder = new ItemStack(Material.BARRIER);
        ItemMeta meta = placeholder.getItemMeta();
        
        meta.setDisplayName(MessageUtil.colorize(plugin.getConfigManager().getMessage("gui.result-empty")));
        placeholder.setItemMeta(meta);
        
        return placeholder;
    }
    
    /**
     * Vérifie si un slot est un slot de craft
     */
    private boolean isCraftSlot(int slot) {
        for (int craftSlot : craftSlots) {
            if (craftSlot == slot) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Ouvre la GUI pour un joueur
     */
    public void open() {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline()) {
            throw new IllegalStateException("Impossible d'ouvrir une GUI KCraft pour un joueur hors ligne");
        }

        // Enregistrer la GUI dans le listener
        plugin.getCraftGUIListener().registerGUI(player, this);
        
        player.openInventory(inventory);
        
        // Jouer le son d'ouverture si configuré
        if (table.getSoundOpen() != null && plugin.getConfigManager().areSoundsEnabled()) {
            me.krunsh.kcraft.utils.SoundUtil.playSafe(player, table.getSoundOpen());
        }
        
        MessageUtil.debug("GUI ouverte pour " + player.getName() + " - Table: " + table.getId(), 2);
    }
    
    /**
     * Met à jour l'affichage du résultat
     */
    public void updateResult(ItemStack result) {
        if (result != null) {
            inventory.setItem(resultSlot, result);
        } else {
            inventory.setItem(resultSlot, createResultPlaceholder());
        }
    }
    
    /**
     * Obtient les items de la grille de craft
     */
    public ItemStack[] getCraftMatrix() {
        ItemStack[] matrix = new ItemStack[craftSlots.length];
        
        for (int i = 0; i < craftSlots.length; i++) {
            matrix[i] = inventory.getItem(craftSlots[i]);
        }
        
        return matrix;
    }
    
    /**
     * Vérifie si un slot peut être modifié par le joueur
     */
    public boolean isModifiableSlot(int slot) {
        return isCraftSlot(slot);
    }
    
    /**
     * Vérifie si c'est le bouton de craft
     */
    public boolean isCraftButton(int slot) {
        return slot == craftButtonSlot;
    }
    
    /**
     * Vérifie si c'est le slot de résultat
     */
    public boolean isResultSlot(int slot) {
        return slot == resultSlot;
    }
    
    // === GETTERS ===
    
    public Player getPlayer() {
        return Bukkit.getPlayer(playerId);
    }
    
    public CraftTable getTable() {
        return table;
    }
    
    public Inventory getInventory() {
        return inventory;
    }
    
    public int[] getCraftSlots() {
        return craftSlots.clone();
    }
    
    public int getResultSlot() {
        return resultSlot;
    }
    
    public int getCraftButtonSlot() {
        return craftButtonSlot;
    }
}
