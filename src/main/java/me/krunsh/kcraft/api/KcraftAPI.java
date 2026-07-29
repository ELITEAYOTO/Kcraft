package me.krunsh.kcraft.api;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import me.krunsh.kcraft.Kcraft;
import me.krunsh.kcraft.api.events.KcraftPostCraftEvent;
import me.krunsh.kcraft.api.events.KcraftPreCraftEvent;
import me.krunsh.kcraft.api.events.KcraftRecipeRegisterEvent;
import me.krunsh.kcraft.api.events.KcraftTableOpenEvent;
import me.krunsh.kcraft.models.CraftRecipe;
import me.krunsh.kcraft.models.CraftTable;

/**
 * API publique de Kcraft pour les add-ons externes
 * Permet l'intégration avec d'autres plugins
 */
public class KcraftAPI {
    
    private final Kcraft plugin;
    
    public KcraftAPI(Kcraft plugin) {
        this.plugin = plugin;
    }
    
    // === GESTION DES CRAFTS ===
    
    /**
     * Enregistre une nouvelle recette de craft
     * @param recipe La recette à ajouter
     * @throws IllegalArgumentException Si l'ID existe déjà
     */
    public void registerCraft(CraftRecipe recipe) {
        if (plugin.getCraftManager().getRecipe(recipe.getId()) != null) {
            throw new IllegalArgumentException("Une recette avec l'ID '" + recipe.getId() + "' existe déjà");
        }
        
        plugin.getCraftManager().addRecipe(recipe);
        
        // Event pour notifier
        KcraftRecipeRegisterEvent event = new KcraftRecipeRegisterEvent(recipe);
        plugin.getServer().getPluginManager().callEvent(event);
    }
    
    /**
     * Supprime une recette de craft
     * @param craftId L'ID de la recette à supprimer
     * @return true si la recette a été supprimée
     */
    public boolean unregisterCraft(String craftId) {
        return plugin.getCraftManager().removeRecipe(craftId);
    }
    
    /**
     * Obtient une recette par son ID
     * @param craftId L'ID de la recette
     * @return La recette ou null si introuvable
     */
    public CraftRecipe getCraft(String craftId) {
        return plugin.getCraftManager().getRecipe(craftId);
    }
    
    /**
     * Obtient toutes les recettes disponibles pour un joueur
     * @param player Le joueur
     * @return Liste des recettes accessibles
     */
    public List<CraftRecipe> getAvailableCrafts(Player player) {
        return plugin.getCraftManager().getAllRecipes().values().stream()
                .filter(recipe -> canCraft(player, recipe.getId()))
                .collect(java.util.stream.Collectors.toList());
    }
    
    // === VÉRIFICATIONS ===
    
    /**
     * Vérifie si un joueur peut exécuter un craft
     * @param player Le joueur
     * @param craftId L'ID du craft
     * @return true si le craft est possible
     */
    public boolean canCraft(Player player, String craftId) {
        CraftRecipe recipe = getCraft(craftId);
        if (recipe == null) {
            return false;
        }
        
        // Vérifications basiques
        if (!recipe.canCraft(player)) {
            return false;
        }
        
        // Vérifier plugin requis
        if (!plugin.getHookManager().isPluginAvailable(recipe.getRequiredPlugin())) {
            return false;
        }
        
        // Vérifier niveau faction
        if (!plugin.getHookManager().checkFactionLevel(player, recipe.getFactionLevelRequired())) {
            return false;
        }
        
        return true;
    }
    
    // === CRAFTING FORCÉ ===
    
    /**
     * Force l'exécution d'un craft (bypass toutes les conditions)
     * @param craftId L'ID du craft
     * @param crafter Le joueur qui craft (pour les logs)
     * @return L'item résultat ou null si échec
     */
    public ItemStack forceCraft(String craftId, Player crafter) {
        CraftRecipe recipe = getCraft(craftId);
        if (recipe == null) {
            return null;
        }
        
        try {
            // Event pré-craft
            KcraftPreCraftEvent preEvent = new KcraftPreCraftEvent(crafter, recipe, true);
            plugin.getServer().getPluginManager().callEvent(preEvent);
            
            if (preEvent.isCancelled()) {
                return null;
            }
            
            // Créer le résultat
            ItemStack result = recipe.getResult().createItem();
            
            // Event post-craft
            KcraftPostCraftEvent postEvent = new KcraftPostCraftEvent(crafter, recipe, result, true, true);
            plugin.getServer().getPluginManager().callEvent(postEvent);
            
            // Logger
            plugin.getLoggingManager().logCraft(crafter, craftId, result.getType().name(), true);
            
            return result;
            
        } catch (Exception e) {
            plugin.getLogger().warning("Erreur lors du craft forcé: " + e.getMessage());
            return null;
        }
    }
    
    // === STATISTIQUES ===
    
    /**
     * Obtient les statistiques de craft d'un joueur
     * @param playerId L'UUID du joueur
     * @return Map des statistiques ou null si aucune donnée
     */
    public Map<String, Object> getPlayerStats(UUID playerId) {
        return plugin.getLoggingManager().getPlayerStats(playerId);
    }
    
    /**
     * Obtient les statistiques globales
     * @return Map des statistiques globales
     */
    public Map<String, Object> getGlobalStats() {
        return plugin.getLoggingManager().getGlobalStats();
    }
    
    // === TABLES ===
    
    /**
     * Obtient une table de craft par son ID
     * @param tableId L'ID de la table
     * @return La table ou null si introuvable
     */
    public CraftTable getTable(String tableId) {
        return plugin.getTableManager().getTable(tableId);
    }
    
    /**
     * Ouvre une table de craft pour un joueur
     * @param player Le joueur
     * @param tableId L'ID de la table
     * @return true si la table a été ouverte
     */
    public boolean openTable(Player player, String tableId) {
        CraftTable table = getTable(tableId);
        if (table == null || !table.canUse(player)) {
            return false;
        }
        
        try {
            // Event d'ouverture
            KcraftTableOpenEvent event = new KcraftTableOpenEvent(player, table);
            plugin.getServer().getPluginManager().callEvent(event);
            
            if (event.isCancelled()) {
                return false;
            }

            me.krunsh.kcraft.gui.CraftTableGUI gui = new me.krunsh.kcraft.gui.CraftTableGUI(plugin, player, table);
            gui.open();

            return true;
            
        } catch (Exception e) {
            plugin.getLogger().warning("Erreur ouverture table API: " + e.getMessage());
            return false;
        }
    }
    
    // === CACHE ===
    
    /**
     * Obtient les statistiques du cache
     * @return Map des stats du cache
     */
    public Map<String, Integer> getCacheStats() {
        return plugin.getCacheManager().getCacheStats();
    }
    
    /**
     * Force la reconstruction du cache
     */
    public void rebuildCache() {
        plugin.getCacheManager().rebuildCache();
    }
    
    // === UTILITAIRES ===
    
    /**
     * Vérifie si Kcraft est correctement initialisé
     * @return true si le plugin est prêt
     */
    public boolean isReady() {
        return plugin.getCraftManager() != null && 
               plugin.getTableManager() != null &&
               plugin.getCacheManager() != null;
    }
    
    /**
     * Obtient la version de l'API
     * @return Version de l'API
     */
    public String getAPIVersion() {
        return "1.0.0";
    }
    
    /**
     * Obtient le nombre total de crafts chargés
     * @return Nombre de crafts
     */
    public int getTotalCrafts() {
        return plugin.getCraftManager().getCraftCount();
    }
    
    /**
     * Obtient le nombre total de tables
     * @return Nombre de tables
     */
    public int getTotalTables() {
        return plugin.getTableManager().getAllTables().size();
    }
    
    // === HOOKS POUR ADD-ONS ===
    
    /**
     * Enregistre un hook personnalisé
     * @param pluginName Nom du plugin qui s'enregistre
     * @param hook Interface du hook
     */
    public void registerHook(String pluginName, CraftHook hook) {
        // TODO: Implémenter système de hooks personnalisés
        plugin.getLogger().info("Hook enregistré pour " + pluginName);
    }
    
    /**
     * Interface pour les hooks personnalisés
     */
    public interface CraftHook {
        
        /**
         * Appelé avant qu'un craft soit exécuté
         * @param player Le joueur
         * @param recipe La recette
         * @return true pour autoriser le craft
         */
        boolean onPreCraft(Player player, CraftRecipe recipe);
        
        /**
         * Appelé après qu'un craft soit exécuté
         * @param player Le joueur
         * @param recipe La recette
         * @param result L'item résultat
         * @param success Si le craft a réussi
         */
        void onPostCraft(Player player, CraftRecipe recipe, ItemStack result, boolean success);
    }
}