package me.krunsh.kcraft.managers;

import me.krunsh.kcraft.Kcraft;
import me.krunsh.kcraft.models.CraftRecipe;
import me.krunsh.kcraft.utils.MessageUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestionnaire de cache pour optimiser les crafts fréquents
 */
public class CacheManager {
    
    private final Kcraft plugin;
    
    // Cache des crafts populaires
    private final Map<String, CraftRecipe> popularCrafts;
    
    // Stats d'utilisation
    private final Map<String, Integer> craftUsageCount;
    
    // Cache des crafts de compactage (instantané)
    private final Map<String, CraftRecipe> compactingCache;
    
    private int maxCacheSize;
    
    public CacheManager(Kcraft plugin) {
        this.plugin = plugin;
        this.popularCrafts = new ConcurrentHashMap<>();
        this.craftUsageCount = new ConcurrentHashMap<>();
        this.compactingCache = new ConcurrentHashMap<>();
        this.maxCacheSize = plugin.getConfigManager().getCacheSize();
    }
    
    /**
     * Construit le cache initial
     */
    public void buildCache() {
        if (!plugin.getConfigManager().isCacheEnabled()) {
            MessageUtil.debug("Cache désactivé", 2);
            return;
        }
        
        clearCache();
        
        // Mettre en cache tous les crafts de compactage
        for (CraftRecipe recipe : plugin.getCraftManager().getAllRecipes().values()) {
            if (isCompactingCraft(recipe)) {
                compactingCache.put(recipe.getId(), recipe);
                MessageUtil.debug("Craft de compactage mis en cache: " + recipe.getId(), 3);
            }
        }
        
        MessageUtil.log("Cache construit - Compactage: " + compactingCache.size() + " crafts");
    }
    
    /**
     * Reconstruit le cache (après rechargement)
     */
    public void rebuildCache() {
        MessageUtil.debug("Reconstruction du cache...", 1);
        buildCache();
    }
    
    /**
     * Vide le cache
     */
    public void clearCache() {
        popularCrafts.clear();
        compactingCache.clear();
        MessageUtil.debug("Cache vidé", 2);
    }
    
    /**
     * Obtient un craft depuis le cache
     */
    public CraftRecipe getCachedRecipe(String craftId) {
        // D'abord checker le cache de compactage (prioritaire)
        CraftRecipe recipe = compactingCache.get(craftId);
        if (recipe != null) {
            return recipe;
        }
        
        // Ensuite le cache populaire
        recipe = popularCrafts.get(craftId);
        if (recipe != null) {
            incrementUsage(craftId);
            return recipe;
        }
        
        return null;
    }
    
    /**
     * Ajoute un craft au cache populaire
     */
    public void addToPopularCache(CraftRecipe recipe) {
        if (!plugin.getConfigManager().isCacheEnabled()) {
            return;
        }
        
        if (popularCrafts.size() >= maxCacheSize) {
            evictLeastUsed();
        }
        
        popularCrafts.put(recipe.getId(), recipe);
        MessageUtil.debug("Craft ajouté au cache populaire: " + recipe.getId(), 3);
    }
    
    /**
     * Incrémente l'usage d'un craft
     */
    public void incrementUsage(String craftId) {
        craftUsageCount.merge(craftId, 1, Integer::sum);
        
        // Si le craft devient populaire, l'ajouter au cache
        int usage = craftUsageCount.get(craftId);
        if (usage >= 10 && !popularCrafts.containsKey(craftId)) {
            CraftRecipe recipe = plugin.getCraftManager().getRecipe(craftId);
            if (recipe != null) {
                addToPopularCache(recipe);
            }
        }
    }
    
    /**
     * Vérifie si un craft est de type compactage
     */
    private boolean isCompactingCraft(CraftRecipe recipe) {
        // Craft marqué comme instantané
        if (recipe.isInstantCraft()) {
            return true;
        }
        
        // Craft dont l'ID contient "compact"
        if (recipe.getId().toLowerCase().contains("compact")) {
            return true;
        }
        
        // Patterns de compactage configurés
        for (String pattern : plugin.getConfig().getStringList("Kcraft.optimizations.instant-compact-items")) {
            if (recipe.getId().matches(pattern.replace("*", ".*"))) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Évince le craft le moins utilisé du cache
     */
    private void evictLeastUsed() {
        if (popularCrafts.isEmpty()) {
            return;
        }
        
        String leastUsed = null;
        int minUsage = Integer.MAX_VALUE;
        
        for (String craftId : popularCrafts.keySet()) {
            int usage = craftUsageCount.getOrDefault(craftId, 0);
            if (usage < minUsage) {
                minUsage = usage;
                leastUsed = craftId;
            }
        }
        
        if (leastUsed != null) {
            popularCrafts.remove(leastUsed);
            MessageUtil.debug("Craft évincé du cache: " + leastUsed + " (usage: " + minUsage + ")", 3);
        }
    }
    
    /**
     * Vérifie si un craft est en cache de compactage
     */
    public boolean isInCompactingCache(String craftId) {
        return compactingCache.containsKey(craftId);
    }
    
    /**
     * Obtient les statistiques du cache
     */
    public Map<String, Integer> getCacheStats() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("popular_cache_size", popularCrafts.size());
        stats.put("compacting_cache_size", compactingCache.size());
        stats.put("max_cache_size", maxCacheSize);
        stats.put("total_usage_tracked", craftUsageCount.size());
        return stats;
    }
    
    /**
     * Obtient les crafts les plus utilisés
     */
    public Map<String, Integer> getTopCrafts(int limit) {
        return craftUsageCount.entrySet()
                .stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limit)
                .collect(java.util.stream.Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue,
                    (e1, e2) -> e1,
                    java.util.LinkedHashMap::new
                ));
    }
    
    /**
     * Met à jour la taille max du cache
     */
    public void updateMaxCacheSize(int newSize) {
        this.maxCacheSize = newSize;
        
        // Éviction si nécessaire
        while (popularCrafts.size() > maxCacheSize && maxCacheSize > 0) {
            evictLeastUsed();
        }
    }
}