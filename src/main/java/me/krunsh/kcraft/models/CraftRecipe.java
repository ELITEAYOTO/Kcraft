package me.krunsh.kcraft.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.Set;

import org.bukkit.entity.Player;

/**
 * Modèle pour une recette de craft complète
 */
public class CraftRecipe {
    
    // Identification
    private String id;
    private String name;
    private String description;
    
    // Configuration craft
    private CraftType type;
    private String requiredTable;
    private boolean allowVanillaWorkbench;
    private boolean vanillaLooseMatch;
    private boolean allowMirror;
    private boolean instantCraft;
    private boolean cached;
    private Set<String> tags;
    
    // Pattern et ingrédients
    private List<String> pattern;
    private Map<Character, CraftIngredient> ingredients;
    private List<CraftIngredient> shapelessIngredients; // Pour recettes SHAPELESS
    
    // Résultats
    private List<CraftResult> results;
    private double successRate;
    private boolean returnItemsOnFail;
    
    // Conditions
    private String permission;
    private int factionLevelRequired;
    private List<String> enabledWorlds;
    private List<String> enabledBiomes;
    private String requiredPlugin;
    
    // Actions
    private Map<String, Object> onSuccess;
    private Map<String, Object> onFail;
    
    // Cooldown
    private long cooldownMillis;
    
    public CraftRecipe(String id) {
        this.id = id;
        this.type = CraftType.SHAPED;
        this.allowVanillaWorkbench = true;
        this.vanillaLooseMatch = false;
        this.allowMirror = false;
        this.instantCraft = false;
        this.cached = false;
        this.tags = new LinkedHashSet<String>();
        this.pattern = new ArrayList<>();
        this.ingredients = new HashMap<>();
        this.shapelessIngredients = new ArrayList<>();
        this.results = new ArrayList<>();
        this.successRate = 100.0;
        this.returnItemsOnFail = true;
        this.factionLevelRequired = 0;
        this.enabledWorlds = new ArrayList<>();
        this.enabledBiomes = new ArrayList<>();
        this.onSuccess = new HashMap<>();
        this.onFail = new HashMap<>();
        this.cooldownMillis = 0;
    }
    
    /**
     * Vérifie si un joueur peut exécuter ce craft
     */
    public boolean canCraft(Player player) {
        // Les OP bypass uniquement les permissions explicites du craft.
        boolean opBypassPermission = player.isOp();
        
        // Permission
        if (permission != null && !permission.isEmpty() && !player.hasPermission(permission) && !opBypassPermission) {
            return false;
        }
        
        // Monde
        if (!enabledWorlds.isEmpty() && !enabledWorlds.contains(player.getWorld().getName())) {
            return false;
        }
        
        // Biome
        if (!enabledBiomes.isEmpty()) {
            String biome = player.getLocation().getBlock().getBiome().name();
            if (!enabledBiomes.contains(biome)) {
                return false;
            }
        }
        
        // Plugin requis sera vérifié par le manager
        
        // Faction sera vérifiée par le hook Kfaction
        
        return true;
    }
    
    /**
     * Détermine si le craft réussit selon le taux de succès
     */
    public boolean isSuccessful() {
        if (successRate >= 100.0) {
            return true;
        }
        
        double roll = Math.random() * 100.0;
        return roll <= successRate;
    }
    
    /**
     * Obtient le résultat du craft (en tenant compte des probabilités)
     */
    public CraftResult getResult() {
        if (results.isEmpty()) {
            return null;
        }
        
        if (results.size() == 1) {
            return results.get(0);
        }
        
        // Choisir selon les probabilités
        double totalWeight = 0;
        for (CraftResult result : results) {
            totalWeight += result.getChance();
        }
        
        double roll = Math.random() * totalWeight;
        double currentWeight = 0;
        
        for (CraftResult result : results) {
            currentWeight += result.getChance();
            if (roll <= currentWeight) {
                return result;
            }
        }
        
        // Fallback sur le premier
        return results.get(0);
    }
    
    /**
     * Vérifie si le pattern correspond à une grille
     */
    public boolean matchesPattern(org.bukkit.inventory.ItemStack[][] grid) {
        // TODO: Implémenter la vérification de pattern
        // Comparaison avec la grille d'inventaire
        return false;
    }
    
    // === BUILDER PATTERN ===
    
    public static Builder builder(String id) {
        return new Builder(id);
    }
    
    public static class Builder {
        private CraftRecipe recipe;
        
        public Builder(String id) {
            this.recipe = new CraftRecipe(id);
        }
        
        public Builder type(CraftType type) {
            recipe.type = type;
            return this;
        }
        
        public Builder table(String table) {
            recipe.requiredTable = table;
            return this;
        }
        
        public Builder instant(boolean instant) {
            recipe.instantCraft = instant;
            return this;
        }
        
        public Builder cached(boolean cached) {
            recipe.cached = cached;
            return this;
        }
        
        public Builder pattern(String... lines) {
            recipe.pattern.clear();
            for (String line : lines) {
                recipe.pattern.add(line);
            }
            return this;
        }
        
        public Builder ingredient(char symbol, CraftIngredient ingredient) {
            recipe.ingredients.put(symbol, ingredient);
            return this;
        }
        
        public Builder result(CraftResult result) {
            recipe.results.add(result);
            return this;
        }
        
        public Builder successRate(double rate) {
            recipe.successRate = rate;
            return this;
        }
        
        public Builder permission(String perm) {
            recipe.permission = perm;
            return this;
        }
        
        public Builder factionLevel(int level) {
            recipe.factionLevelRequired = level;
            return this;
        }
        
        public Builder worlds(String... worlds) {
            recipe.enabledWorlds.clear();
            for (String world : worlds) {
                recipe.enabledWorlds.add(world);
            }
            return this;
        }
        
        public CraftRecipe build() {
            return recipe;
        }
    }
    
    // === GETTERS & SETTERS ===
    
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public CraftType getType() {
        return type;
    }
    
    public void setType(CraftType type) {
        this.type = type;
    }
    
    public String getRequiredTable() {
        return requiredTable;
    }
    
    public void setRequiredTable(String requiredTable) {
        this.requiredTable = requiredTable;
    }

    public boolean isAllowVanillaWorkbench() {
        return allowVanillaWorkbench;
    }

    public void setAllowVanillaWorkbench(boolean allowVanillaWorkbench) {
        this.allowVanillaWorkbench = allowVanillaWorkbench;
    }

    public boolean isVanillaLooseMatch() {
        return vanillaLooseMatch;
    }

    public void setVanillaLooseMatch(boolean vanillaLooseMatch) {
        this.vanillaLooseMatch = vanillaLooseMatch;
    }

    public boolean isAllowMirror() {
        return allowMirror;
    }

    public void setAllowMirror(boolean allowMirror) {
        this.allowMirror = allowMirror;
    }

    public Set<String> getTags() {
        return new LinkedHashSet<String>(tags);
    }

    public void setTags(Set<String> tags) {
        this.tags = tags == null ? new LinkedHashSet<String>() : new LinkedHashSet<String>(tags);
    }
    
    public boolean isInstantCraft() {
        return instantCraft;
    }
    
    public void setInstantCraft(boolean instantCraft) {
        this.instantCraft = instantCraft;
    }
    
    public boolean isCached() {
        return cached;
    }
    
    public void setCached(boolean cached) {
        this.cached = cached;
    }
    
    public List<String> getPattern() {
        return pattern;
    }
    
    public void setPattern(List<String> pattern) {
        this.pattern = pattern;
    }
    
    public Map<Character, CraftIngredient> getIngredients() {
        return ingredients;
    }
    
    public void setIngredients(Map<Character, CraftIngredient> ingredients) {
        this.ingredients = ingredients;
    }
    
    public List<CraftIngredient> getShapelessIngredients() {
        return shapelessIngredients;
    }
    
    public void setShapelessIngredients(List<CraftIngredient> shapelessIngredients) {
        this.shapelessIngredients = shapelessIngredients;
    }
    
    public List<CraftResult> getResults() {
        return results;
    }
    
    public void setResults(List<CraftResult> results) {
        this.results = results;
    }
    
    public double getSuccessRate() {
        return successRate;
    }
    
    public void setSuccessRate(double successRate) {
        this.successRate = Math.max(0.0, Math.min(100.0, successRate));
    }
    
    public boolean isReturnItemsOnFail() {
        return returnItemsOnFail;
    }
    
    public void setReturnItemsOnFail(boolean returnItemsOnFail) {
        this.returnItemsOnFail = returnItemsOnFail;
    }
    
    public String getPermission() {
        return permission;
    }
    
    public void setPermission(String permission) {
        this.permission = permission;
    }
    
    public int getFactionLevelRequired() {
        return factionLevelRequired;
    }
    
    public void setFactionLevelRequired(int factionLevelRequired) {
        this.factionLevelRequired = factionLevelRequired;
    }
    
    public List<String> getEnabledWorlds() {
        return enabledWorlds;
    }
    
    public void setEnabledWorlds(List<String> enabledWorlds) {
        this.enabledWorlds = enabledWorlds;
    }
    
    public List<String> getEnabledBiomes() {
        return enabledBiomes;
    }
    
    public void setEnabledBiomes(List<String> enabledBiomes) {
        this.enabledBiomes = enabledBiomes;
    }
    
    public String getRequiredPlugin() {
        return requiredPlugin;
    }
    
    public void setRequiredPlugin(String requiredPlugin) {
        this.requiredPlugin = requiredPlugin;
    }
    
    public Map<String, Object> getOnSuccess() {
        return onSuccess;
    }
    
    public void setOnSuccess(Map<String, Object> onSuccess) {
        this.onSuccess = onSuccess;
    }
    
    public Map<String, Object> getOnFail() {
        return onFail;
    }
    
    public void setOnFail(Map<String, Object> onFail) {
        this.onFail = onFail;
    }
    
    public long getCooldownMillis() {
        return cooldownMillis;
    }
    
    public void setCooldownMillis(long cooldownMillis) {
        this.cooldownMillis = cooldownMillis;
    }
    
    /**
     * Vérifie si le résultat doit être donné au joueur
     * Retourne false si consume-result: false (commande donne l'item à la place)
     */
    public boolean shouldGiveResult() {
        if (onSuccess != null && onSuccess.containsKey("consume-result")) {
            Object value = onSuccess.get("consume-result");
            // Si consume-result: false, on ne donne PAS le result
            return !(value instanceof Boolean) || ((Boolean) value);
        }
        return true; // Par défaut, on donne le résultat
    }
    
    @Override
    public String toString() {
        return "CraftRecipe{" +
                "id='" + id + '\'' +
                ", type=" + type +
                ", table='" + requiredTable + '\'' +
                ", results=" + results.size() +
                '}';
    }
}
