package me.krunsh.kcraft.managers;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import me.krunsh.kcraft.Kcraft;
import me.krunsh.kcraft.api.events.KcraftPostCraftEvent;
import me.krunsh.kcraft.models.CraftIngredient;
import me.krunsh.kcraft.models.CraftMatrixTransaction;
import me.krunsh.kcraft.models.CraftRecipe;
import me.krunsh.kcraft.models.CraftResult;
import me.krunsh.kcraft.models.CraftType;
import me.krunsh.kcraft.models.RecipeAccessPolicy;
import me.krunsh.kcraft.models.VanillaRecipeResolver;
import me.krunsh.kcraft.utils.MaterialResolver;
import me.krunsh.kcraft.utils.MessageUtil;

/**
 * Gestionnaire principal des crafts
 * Charge, parse et gère toutes les recettes
 */
public class CraftManager {
    
    private final Kcraft plugin;
    private final Map<String, CraftRecipe> recipes;
    private final RecipeSourceRegistry<CraftRecipe> recipeSources;
    
    public CraftManager(Kcraft plugin) {
        this.plugin = plugin;
        this.recipes = new LinkedHashMap<>();
        this.recipeSources = new RecipeSourceRegistry<CraftRecipe>();
        loadCandidateCrafts();
    }
    
    /**
     * Charge tous les crafts depuis les fichiers de config
     */
    public void loadAllCrafts() {
        // L'API historique ne doit pas publier une moitié du runtime.
        plugin.reload();
    }

    private void loadCandidateCrafts() {
        recipes.clear();
        recipeSources.clear();
        
        List<File> craftFiles = plugin.getConfigManager().getCraftFiles();
        int totalLoaded = 0;
        
        for (File craftFile : craftFiles) {
            try {
                int loaded = loadCraftsFromFile(craftFile);
                totalLoaded += loaded;
                MessageUtil.log("Chargé " + loaded + " crafts depuis " + craftFile.getName());
            } catch (Exception e) {
                throw new IllegalStateException("Recettes invalides dans "
                    + craftFile.getName() + ": " + e.getMessage(), e);
            }
        }

        warnAboutAmbiguousVanillaRecipes();
        
        plugin.getLogger().info("Total crafts chargés: " + recipes.size()
            + (totalLoaded == recipes.size() ? "" : " (doublons refusés)"));
    }
    
    /**
     * Charge les crafts d'un fichier spécifique
     */
    private int loadCraftsFromFile(File craftFile) {
        FileConfiguration config = plugin.getConfigManager().loadCraftConfig(craftFile);
        int loaded = 0;
        
        // Parser chaque section principale du fichier
        for (String sectionKey : config.getKeys(false)) {
            ConfigurationSection mainSection = config.getConfigurationSection(sectionKey);
            if (mainSection == null) {
                throw new IllegalArgumentException("Section attendue: " + sectionKey);
            }
            
            // Parser chaque craft dans la section
            for (String craftId : mainSection.getKeys(false)) {
                try {
                    ConfigurationSection craftSection = mainSection.getConfigurationSection(craftId);
                    if (craftSection != null) {
                        CraftRecipe recipe = parseCraftRecipe(craftId, craftSection);
                        if (recipe != null) {
                            RecipeSourceRegistry.Registration registration = recipeSources.register(
                                craftId, recipe, craftFile.getName());
                            if (registration == RecipeSourceRegistry.Registration.ACCEPTED) {
                                recipes.put(craftId, recipe);
                                loaded++;
                                MessageUtil.debug("Craft chargé: " + craftId, 2);
                            } else {
                                throw new IllegalArgumentException("Identifiant dupliqué '"
                                    + craftId + "' dans " + recipeSources.getDuplicateSources(craftId));
                            }
                        }
                    } else {
                        throw new IllegalArgumentException("Section de recette attendue");
                    }
                } catch (Exception e) {
                    throw new IllegalArgumentException("Recette '" + craftId + "': " + e.getMessage(), e);
                }
            }
        }
        
        return loaded;
    }
    
    /**
     * Parse une recette depuis sa configuration
     */
    private CraftRecipe parseCraftRecipe(String id, ConfigurationSection section) {
        if (!section.getBoolean("enabled", true)) {
            MessageUtil.debug("Craft désactivé ignoré: " + id, 2);
            return null;
        }

        CraftRecipe recipe = new CraftRecipe(id);
        
        // Type de craft
        String typeStr = section.getString("type", "SHAPED");
        recipe.setType(CraftType.valueOf(typeStr.toUpperCase(java.util.Locale.ROOT)));
        
        // Table requise
        recipe.setRequiredTable(section.getString("table", "tier1"));
        recipe.setAllowVanillaWorkbench(section.getBoolean("allow-vanilla-workbench", true));
        String vanillaMatchMode = section.getString("vanilla-match-mode", "EXACT");
        recipe.setVanillaLooseMatch(section.getBoolean("allow-custom-ingredients", false)
            || "MATERIAL".equalsIgnoreCase(vanillaMatchMode)
            || "LOOSE".equalsIgnoreCase(vanillaMatchMode));
        recipe.setAllowMirror(section.getBoolean("allow-mirror", false));
        Set<String> recipeTags = new LinkedHashSet<String>(section.getStringList("tags"));
        for (String key : section.getKeys(false)) {
            String normalized = key == null ? "" : key.trim().toLowerCase();
            if (normalized.startsWith("kcraft_") && normalized.endsWith("_only")
                    && section.getBoolean(key, false)) recipeTags.add(normalized);
        }
        recipe.setTags(recipeTags);
        
        // Optimisations
        recipe.setInstantCraft(section.getBoolean("instant-craft", false));
        recipe.setCached(section.getBoolean("cached", false));
        
        // Taux de succès
        recipe.setSuccessRate(section.getDouble("success-rate", 100.0));
        boolean returnItemsOnFail = section.contains("return-items-on-fail")
            ? section.getBoolean("return-items-on-fail", true)
            : section.getBoolean("fail-return-items", true);
        recipe.setReturnItemsOnFail(returnItemsOnFail);
        
        // Conditions
        recipe.setPermission(section.getString("permission"));
        recipe.setFactionLevelRequired(section.getInt("faction-level-required", 0));
        recipe.setEnabledWorlds(section.getStringList("worlds"));
        recipe.setEnabledBiomes(section.getStringList("biomes"));
        recipe.setRequiredPlugin(section.getString("enabled-if-plugin"));
        
        // Pattern (pour SHAPED)
        if (section.contains("pattern")) {
            recipe.setPattern(section.getStringList("pattern"));
        }
        
        // Ingrédients
        parseIngredients(recipe, section);
        
        // Résultats
        parseResults(recipe, section);
        
        // Actions
        parseActions(recipe, section);
        
        return recipe;
    }
    
    /**
     * Parse les ingrédients d'une recette
     */
    private void parseIngredients(CraftRecipe recipe, ConfigurationSection section) {
        ConfigurationSection ingredientsSection = section.getConfigurationSection("ingredients");
        if (ingredientsSection == null) {
            // Pour les recettes SHAPELESS, ingredients peut être une liste
            if (recipe.getType() == CraftType.SHAPELESS && section.isList("ingredients")) {
                List<?> ingredientsList = section.getList("ingredients");
                List<CraftIngredient> shapelessIngredients = new ArrayList<>();
                
                for (Object rawIngredient : ingredientsList) {
                    if (!(rawIngredient instanceof Map)) {
                        throw new IllegalArgumentException("Chaque ingrédient SHAPELESS doit être une section");
                    }
                    Map<?, ?> ingredientMap = (Map<?, ?>) rawIngredient;
                    String materialStr = (String) ingredientMap.get("material");
                    if (materialStr == null) throw new IllegalArgumentException("Matériau SHAPELESS absent");
                    
                    Material material = MaterialResolver.resolve(materialStr);
                    if (material == null) {
                        throw new IllegalArgumentException("Matériau SHAPELESS invalide: " + materialStr);
                    }
                    
                    int amount = ingredientMap.containsKey("amount") ? 
                        ((Number) ingredientMap.get("amount")).intValue() : 1;
                    
                    CraftIngredient ingredient = new CraftIngredient(material, amount);

                    // Data value 1.8.8 (optionnelle)
                    if (ingredientMap.containsKey("data") && ingredientMap.get("data") instanceof Number) {
                        ingredient.setDataValue(((Number) ingredientMap.get("data")).shortValue());
                    }
                    
                    // NBT
                    if (ingredientMap.containsKey("nbt") && ingredientMap.get("nbt") instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> nbtMap = (Map<String, Object>) ingredientMap.get("nbt");
                        ingredient.setRequiredNBT(nbtMap);
                    }

                    if (ingredientMap.containsKey("cit")) {
                        String citKey = ingredientMap.containsKey("cit-key")
                                ? String.valueOf(ingredientMap.get("cit-key")) : "sparrowmc-item";
                        ingredient.addNBTRequirement(citKey, ingredientMap.get("cit"));
                    }

                    if (ingredientMap.containsKey("name")) {
                        ingredient.setName(String.valueOf(ingredientMap.get("name")));
                    }
                    if (ingredientMap.containsKey("lore") && ingredientMap.get("lore") instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<String> lore = (List<String>) ingredientMap.get("lore");
                        ingredient.setLore(lore);
                    }

                    // Mode strict materiau (refuse items avec NBT custom)
                    if (ingredientMap.containsKey("strict-material")) {
                        Object sm = ingredientMap.get("strict-material");
                        if (sm instanceof Boolean) {
                            ingredient.setStrictMaterial((Boolean) sm);
                        } else if (sm != null) {
                            ingredient.setStrictMaterial(Boolean.parseBoolean(sm.toString()));
                        }
                    }
                    if (ingredientMap.containsKey("allow-custom-items")) {
                        ingredient.setAllowCustomItems(Boolean.parseBoolean(
                            String.valueOf(ingredientMap.get("allow-custom-items"))));
                    }
                    
                    shapelessIngredients.add(ingredient);
                }
                
                recipe.setShapelessIngredients(shapelessIngredients);
            }
            return;
        }
        
        // Pour SHAPED: ingredients est une map avec symbols
        Map<Character, CraftIngredient> ingredients = new HashMap<>();
        
        for (String key : ingredientsSection.getKeys(false)) {
            if (key.length() != 1) throw new IllegalArgumentException("Symbole ingrédient invalide: " + key);
            
            char symbol = key.charAt(0);
            ConfigurationSection ingredientSection = ingredientsSection.getConfigurationSection(key);
            
            if (ingredientSection != null) {
                CraftIngredient ingredient = parseIngredient(ingredientSection);
                if (ingredient != null) {
                    ingredients.put(symbol, ingredient);
                }
            } else {
                throw new IllegalArgumentException("Section ingrédient attendue pour: " + key);
            }
        }
        
        recipe.setIngredients(ingredients);
    }
    
    /**
     * Parse un ingrédient individuel
     */
    private CraftIngredient parseIngredient(ConfigurationSection section) {
        String materialStr = section.getString("material");
        if (materialStr == null) throw new IllegalArgumentException("Matériau ingrédient absent");
        
        Material material = MaterialResolver.resolve(materialStr);
        if (material == null) {
            throw new IllegalArgumentException("Matériau ingrédient invalide: " + materialStr);
        }
        
        int amount = section.getInt("amount", 1);
        CraftIngredient ingredient = new CraftIngredient(material, amount);

        // Data value 1.8.8 (optionnelle)
        if (section.contains("data")) {
            ingredient.setDataValue((short) section.getInt("data"));
        }
        
        // Nom et lore
        ingredient.setName(section.getString("name"));
        ingredient.setLore(section.getStringList("lore"));
        
        // NBT requis
        ConfigurationSection nbtSection = section.getConfigurationSection("nbt");
        if (nbtSection != null) {
            Map<String, Object> nbtData = new HashMap<>();
            for (String nbtKey : nbtSection.getKeys(false)) {
                nbtData.put(nbtKey, nbtSection.get(nbtKey));
            }
            ingredient.setRequiredNBT(nbtData);
        }

        // Alias CIT explicite : equivalent a nbt.sparrowmc-item.
        if (section.contains("cit")) {
            ingredient.addNBTRequirement(
                    section.getString("cit-key", "sparrowmc-item"), section.get("cit"));
        }

        // Mode strict materiau (refuse items avec NBT custom)
        if (section.contains("strict-material")) {
            ingredient.setStrictMaterial(section.getBoolean("strict-material", false));
        }
        ingredient.setAllowCustomItems(section.getBoolean("allow-custom-items", false));
        
        return ingredient;
    }
    
    /**
     * Parse les résultats d'une recette
     */
    private void parseResults(CraftRecipe recipe, ConfigurationSection section) {
        List<CraftResult> results = new ArrayList<>();
        
        // Résultat simple
        if (section.contains("result")) {
            CraftResult result = parseResult(section.getConfigurationSection("result"));
            if (result != null) {
                results.add(result);
            }
        }
        
        // Résultats multiples avec probabilités
        if (section.contains("results")) {
            List<?> resultsList = section.getList("results");
            if (resultsList == null) throw new IllegalArgumentException("results doit être une liste");
            if (resultsList != null) {
                for (Object obj : resultsList) {
                    if (obj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> resultMap = (Map<String, Object>) obj;
                        CraftResult result = parseResultFromMap(resultMap);
                        results.add(result);
                    } else {
                        throw new IllegalArgumentException("Chaque résultat doit être une section");
                    }
                }
            }
        }
        
        recipe.setResults(results);
    }
    
    /**
     * Parse un résultat depuis une section
     */
    private CraftResult parseResult(ConfigurationSection section) {
        if (section == null) throw new IllegalArgumentException("Section de résultat attendue");
        
        String materialStr = section.getString("material");
        if (materialStr == null) throw new IllegalArgumentException("Matériau résultat absent");
        
        Material material = MaterialResolver.resolve(materialStr);
        if (material == null) {
            throw new IllegalArgumentException("Matériau résultat invalide: " + materialStr);
        }
        
        int amount = section.getInt("amount", 1);
        CraftResult result = new CraftResult(material, amount);
        
        // Data value (pour blocs comme STONE:2 = Polished Granite)
        short dataValue = (short) section.getInt("data", 0);
        result.setDataValue(dataValue);
        
        // Nom et lore
        result.setName(section.getString("name"));
        result.setLore(section.getStringList("lore"));
        
        // NBT
        ConfigurationSection nbtSection = section.getConfigurationSection("nbt");
        if (nbtSection != null) {
            Map<String, Object> nbtData = new HashMap<>();
            for (String nbtKey : nbtSection.getKeys(false)) {
                nbtData.put(nbtKey, nbtSection.get(nbtKey));
            }
            result.setNbtData(nbtData);
        }
        
        // Enchantement visuel
        result.setEnchanted(section.getBoolean("enchanted", false));
        
        // === NOUVEAUX ATTRIBUTS KCRAFT ===
        
        // Attributes (attack_damage, armor, max_health, etc.)
        ConfigurationSection attributesSection = section.getConfigurationSection("attributes");
        if (attributesSection != null) {
            Map<String, Double> attributes = new HashMap<>();
            for (String attrKey : attributesSection.getKeys(false)) {
                double value = attributesSection.getDouble(attrKey, 0);
                if (value != 0) {
                    attributes.put(attrKey, value);
                }
            }
            result.setAttributes(attributes);
            MessageUtil.debug("Attributs chargés pour " + materialStr + ": " + attributes, 2);
        }
        
        // Modificateur de durabilité
        double durabilityMod = section.getDouble("durability_modifier", 1.0);
        if (durabilityMod != 1.0) {
            result.setDurabilityModifier(durabilityMod);
            MessageUtil.debug("Durabilité modifier pour " + materialStr + ": " + durabilityMod + "x", 2);
        }
        
        return result;
    }
    
    /**
     * Parse un résultat depuis une Map (pour résultats multiples)
     */
    private CraftResult parseResultFromMap(Map<String, Object> map) {
        if (map == null || !map.containsKey("material")) {
            throw new IllegalArgumentException("Matériau résultat absent");
        }

        String materialStr = String.valueOf(map.get("material"));
        Material material = MaterialResolver.resolve(materialStr);
        if (material == null) {
            throw new IllegalArgumentException("Matériau résultat invalide: " + materialStr);
        }

        int amount = 1;
        if (map.containsKey("amount") && map.get("amount") instanceof Number) {
            amount = ((Number) map.get("amount")).intValue();
        }

        CraftResult result = new CraftResult(material, amount);

        if (map.containsKey("data") && map.get("data") instanceof Number) {
            result.setDataValue(((Number) map.get("data")).shortValue());
        }

        if (map.containsKey("name")) {
            result.setName(String.valueOf(map.get("name")));
        }

        if (map.containsKey("lore") && map.get("lore") instanceof List) {
            @SuppressWarnings("unchecked")
            List<String> lore = (List<String>) map.get("lore");
            result.setLore(lore);
        }

        if (map.containsKey("chance") && map.get("chance") instanceof Number) {
            result.setChance(((Number) map.get("chance")).doubleValue());
        }

        if (map.containsKey("enchanted") && map.get("enchanted") instanceof Boolean) {
            result.setEnchanted((Boolean) map.get("enchanted"));
        }

        if (map.containsKey("nbt") && map.get("nbt") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> nbt = (Map<String, Object>) map.get("nbt");
            result.setNbtData(nbt);
        }

        if (map.containsKey("attributes") && map.get("attributes") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> attrsRaw = (Map<String, Object>) map.get("attributes");
            Map<String, Double> attrs = new HashMap<>();
            for (Map.Entry<String, Object> entry : attrsRaw.entrySet()) {
                if (entry.getValue() instanceof Number) {
                    attrs.put(entry.getKey(), ((Number) entry.getValue()).doubleValue());
                }
            }
            result.setAttributes(attrs);
        }

        if (map.containsKey("durability_modifier") && map.get("durability_modifier") instanceof Number) {
            result.setDurabilityModifier(((Number) map.get("durability_modifier")).doubleValue());
        }

        return result;
    }
    
    /**
     * Parse les actions de succès/échec
     */
    private void parseActions(CraftRecipe recipe, ConfigurationSection section) {
        Map<String, Object> successActions = new HashMap<>();
        Map<String, Object> failActions = new HashMap<>();

        // Actions de succès (format moderne)
        if (section.contains("on-success")) {
            ConfigurationSection successSection = section.getConfigurationSection("on-success");
            if (successSection != null) {
                for (String key : successSection.getKeys(false)) {
                    successActions.put(key, successSection.get(key));
                }
            }
        }

        // Actions d'échec (format moderne)
        if (section.contains("on-fail")) {
            ConfigurationSection failSection = section.getConfigurationSection("on-fail");
            if (failSection != null) {
                for (String key : failSection.getKeys(false)) {
                    failActions.put(key, failSection.get(key));
                }
            }
        }

        // Compat legacy: command-on-success (string ou liste)
        if (!successActions.containsKey("command") && !successActions.containsKey("commands") &&
            section.contains("command-on-success")) {
            List<String> commands = normalizeCommands(section.get("command-on-success"));
            if (!commands.isEmpty()) {
                successActions.put("commands", commands);
                successActions.put("console-command", true);
            }
        }

        // Compat legacy: command-on-fail (string ou liste)
        if (!failActions.containsKey("command") && !failActions.containsKey("commands") &&
            section.contains("command-on-fail")) {
            List<String> commands = normalizeCommands(section.get("command-on-fail"));
            if (!commands.isEmpty()) {
                failActions.put("commands", commands);
                failActions.put("console-command", true);
            }
        }

        recipe.setOnSuccess(successActions);
        recipe.setOnFail(failActions);
    }

    private List<String> normalizeCommands(Object rawCommands) {
        List<String> commands = new ArrayList<>();

        if (rawCommands instanceof List) {
            for (Object entry : (List<?>) rawCommands) {
                if (entry != null) {
                    String value = entry.toString().trim();
                    if (!value.isEmpty()) {
                        commands.add(value);
                    }
                }
            }
            return commands;
        }

        if (rawCommands != null) {
            String value = rawCommands.toString().trim();
            if (!value.isEmpty()) {
                commands.add(value);
            }
        }

        return commands;
    }
    
    /**
     * Recharge tous les crafts
     */
    public void reloadCrafts() {
        plugin.reload();
    }
    
    /**
     * Obtient une recette par son ID
     */
    public CraftRecipe getRecipe(String id) {
        return recipes.get(id);
    }
    
    /**
     * Obtient toutes les recettes
     */
    public Map<String, CraftRecipe> getAllRecipes() {
        return new LinkedHashMap<>(recipes);
    }

    public String getRecipeSource(String id) {
        return recipeSources.getSource(id);
    }

    public boolean isDuplicateRecipe(String id) {
        return recipeSources.isDuplicate(id);
    }

    public List<String> getDuplicateRecipeSources(String id) {
        return recipeSources.getDuplicateSources(id);
    }

    private void warnAboutAmbiguousVanillaRecipes() {
        List<CraftRecipe> all = new ArrayList<CraftRecipe>(recipes.values());
        for (int i = 0; i < all.size(); i++) {
            CraftRecipe left = all.get(i);
            if (!RecipeAccessPolicy.fitsGrid(left, 3)) continue;
            for (int j = i + 1; j < all.size(); j++) {
                CraftRecipe right = all.get(j);
                if (!RecipeAccessPolicy.fitsGrid(right, 3)) continue;
                if (VanillaRecipeResolver.specificity(left) == VanillaRecipeResolver.specificity(right)
                        && VanillaRecipeResolver.signature(left).equals(VanillaRecipeResolver.signature(right))) {
                    plugin.getLogger().warning("Ambiguite KCraft vanilla entre '" + left.getId()
                        + "' et '" + right.getId() + "' : meme matrice et meme priorite. "
                        + "La table vanilla refusera ce cas au lieu de choisir au hasard.");
                }
            }
        }
    }
    
    /**
     * Obtient le nombre de crafts chargés
     */
    public int getCraftCount() {
        return recipes.size();
    }
    
    /**
     * Obtient les crafts disponibles pour une table
     */
    public List<CraftRecipe> getCraftsForTable(String tableId) {
        List<CraftRecipe> tableCrafts = new ArrayList<>();
        
        for (CraftRecipe recipe : recipes.values()) {
            if (tableId.equals(recipe.getRequiredTable())) {
                tableCrafts.add(recipe);
            }
        }
        
        return tableCrafts;
    }
    
    /**
     * Ajoute une recette (pour API)
     */
    public void addRecipe(CraftRecipe recipe) {
        if (recipe == null) return;
        RecipeSourceRegistry.Registration registration = recipeSources.register(
            recipe.getId(), recipe, "<API>");
        if (registration == RecipeSourceRegistry.Registration.ACCEPTED) {
            recipes.put(recipe.getId(), recipe);
            MessageUtil.debug("Recette ajoutée via API: " + recipe.getId(), 2);
        } else {
            recipes.remove(recipe.getId());
            plugin.getLogger().severe("Recette API refusée: identifiant dupliqué '" + recipe.getId()
                + "' dans " + recipeSources.getDuplicateSources(recipe.getId()));
        }
    }
    
    /**
     * Supprime une recette (pour API)
     */
    public boolean removeRecipe(String id) {
        CraftRecipe removed = recipes.remove(id);
        if (removed != null) {
            recipeSources.removeActive(id);
            MessageUtil.debug("Recette supprimée via API: " + id, 2);
            return true;
        }
        return false;
    }
    
    /**
     * Trouve une recette correspondant à une matrice d'items
     */
    public CraftRecipe findMatchingRecipe(org.bukkit.inventory.ItemStack[] matrix, me.krunsh.kcraft.models.CraftTable table) {
        for (CraftRecipe recipe : recipes.values()) {
            // Vérifier si la table est compatible
            if (!recipe.getRequiredTable().equals(table.getId())) {
                continue;
            }
            
            // Vérifier si le pattern correspond
            if (CraftMatrixTransaction.matches(recipe, matrix)) {
                return recipe;
            }
        }
        return null;
    }

    /**
     * Vérifie si une recette donnée correspond à une matrice d'items.
     * Utile pour les intégrations externes (ex: table vanilla 3x3).
     */
    public boolean matchesRecipe(CraftRecipe recipe, org.bukkit.inventory.ItemStack[] matrix) {
        if (recipe == null || matrix == null) {
            return false;
        }

        return CraftMatrixTransaction.matches(recipe, matrix);
    }
    
    /**
     * Vérifie si une matrice d'items correspond au pattern d'une recette
     */
    private boolean matchesPattern(CraftRecipe recipe, org.bukkit.inventory.ItemStack[] matrix) {
        return CraftMatrixTransaction.matches(recipe, matrix);
    }
    
    /**
     * Vérifie si une matrice correspond à une recette SHAPELESS
     */
    private boolean matchesShapeless(CraftRecipe recipe, org.bukkit.inventory.ItemStack[] matrix) {
        List<CraftIngredient> requiredIngredients = recipe.getShapelessIngredients();
        if (requiredIngredients == null || requiredIngredients.isEmpty()) {
            return false;
        }
        
        // Créer une liste mutable des ingrédients requis
        List<CraftIngredient> remaining = new ArrayList<>(requiredIngredients);
        
        // Pour chaque item dans la matrice
        for (org.bukkit.inventory.ItemStack item : matrix) {
            if (item == null || item.getType() == org.bukkit.Material.AIR) {
                continue;
            }
            
            // Chercher un ingrédient correspondant
            boolean found = false;
            for (int i = 0; i < remaining.size(); i++) {
                CraftIngredient ingredient = remaining.get(i);

                    if (ingredient.matches(item)) {
                        // Match trouvé!
                        remaining.remove(i);
                        found = true;
                        break;
                    }
            }
            
            // Si cet item ne correspond à aucun ingrédient, recette invalide
            if (!found) {
                return false;
            }
        }
        
        // Tous les ingrédients doivent avoir été trouvés
        return remaining.isEmpty();
    }
    
    /**
     * Vérifie si le pattern correspond à une position spécifique dans la grille
     */
    private boolean matchesPatternAtPosition(org.bukkit.inventory.ItemStack[] matrix, List<String> pattern, 
                                           Map<Character, CraftIngredient> ingredients,
                                           int startRow, int startCol, int tableSize) {
        
        for (int row = 0; row < pattern.size(); row++) {
            String patternRow = pattern.get(row);
            for (int col = 0; col < patternRow.length(); col++) {
                char patternChar = patternRow.charAt(col);
                int matrixIndex = (startRow + row) * tableSize + (startCol + col);
                
                if (matrixIndex >= matrix.length) return false;
                
                org.bukkit.inventory.ItemStack item = matrix[matrixIndex];
                
                if (patternChar == ' ') {
                    // Espace = doit être vide
                    if (item != null && item.getType() != org.bukkit.Material.AIR) {
                        return false;
                    }
                } else {
                    // Caractère = doit correspondre à un ingrédient
                    CraftIngredient ingredient = ingredients.get(patternChar);
                    if (ingredient == null) return false;

                    if (item == null) {
                        return false;
                    }

                    if (!ingredient.matches(item)) {
                        return false;
                    }
                }
            }
        }
        
        return true;
    }
    
    /**
     * Vérifie que tous les slots en dehors du pattern sont vides
     */
    private boolean areOtherSlotsEmpty(org.bukkit.inventory.ItemStack[] matrix, List<String> pattern, 
                                     int startRow, int startCol, int tableSize) {
        
        for (int row = 0; row < tableSize; row++) {
            for (int col = 0; col < tableSize; col++) {
                int matrixIndex = row * tableSize + col;
                
                // Si ce slot fait partie du pattern, l'ignorer
                boolean isInPattern = false;
                if (row >= startRow && row < startRow + pattern.size() &&
                    col >= startCol && col < startCol + pattern.get(0).length()) {
                    isInPattern = true;
                }
                
                if (!isInPattern) {
                    // Ce slot doit être vide
                    org.bukkit.inventory.ItemStack item = matrix[matrixIndex];
                    if (item != null && item.getType() != org.bukkit.Material.AIR) {
                        return false;
                    }
                }
            }
        }
        
        return true;
    }
    
    /**
     * Exécute un craft (pour événements et logs)
     */
    public boolean executeCraft(org.bukkit.entity.Player player, CraftRecipe recipe, org.bukkit.inventory.ItemStack[] matrix) {
        return executeCraft(player, recipe, matrix, recipe == null ? null : recipe.getResult());
    }

    /** Execute un craft avec le resultat deja selectionne par la transaction. */
    public boolean executeCraft(org.bukkit.entity.Player player, CraftRecipe recipe,
                                org.bukkit.inventory.ItemStack[] matrix,
                                CraftResult selectedResult) {
        org.bukkit.inventory.ItemStack selectedItem = selectedResult == null
            ? null : selectedResult.createItem();
        return executeCraft(player, recipe, matrix, selectedResult, selectedItem);
    }

    /** Execute avec le clone exact deja montre dans la preview. */
    public boolean executeCraft(org.bukkit.entity.Player player, CraftRecipe recipe,
                                org.bukkit.inventory.ItemStack[] matrix,
                                CraftResult selectedResult,
                                org.bukkit.inventory.ItemStack selectedItem) {
        try {
            boolean success = recipe.isSuccessful();

            // Logs
            if (plugin.getLoggingManager() != null) {
                plugin.getLoggingManager().logCraft(player, recipe.getId(), success ? "SUCCESS" : "FAILED", success);
            }

            if (success) {
                // Exécuter les actions de succès
                executeSuccessActions(player, recipe);
            } else {
                // Exécuter les actions d'échec
                executeFailActions(player, recipe);
            }

            // Déclencher l'événement PostCraft pour les add-ons externes (KjobsUltimate, etc.)
            org.bukkit.inventory.ItemStack resultItem = null;
            if (success && selectedItem != null) {
                try { resultItem = selectedItem.clone(); } catch (Throwable ignored) {}
            }
            KcraftPostCraftEvent postEvent = new KcraftPostCraftEvent(player, recipe, resultItem, success, false);
            plugin.getServer().getPluginManager().callEvent(postEvent);

            return success;
            
        } catch (Exception e) {
            plugin.getLogger().warning("Erreur lors de l'exécution du craft: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Exécute les actions de succès d'une recette
     */
    private void executeSuccessActions(org.bukkit.entity.Player player, CraftRecipe recipe) {
        Map<String, Object> actions = recipe.getOnSuccess();
        if (actions == null || actions.isEmpty()) {
            return;
        }
        
        try {
            // Messages
            if (actions.containsKey("message")) {
                String message = actions.get("message").toString();
                message = applyActionPlaceholders(message, player);
                MessageUtil.sendRaw(player, message);
            }
            
            // Sons
            if (actions.containsKey("sound")) {
                String soundName = actions.get("sound").toString();
                try {
                    org.bukkit.Sound sound = org.bukkit.Sound.valueOf(soundName);
                    player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
                } catch (Exception e) {
                    // Son invalide, ignorer
                }
            }
            
            // Particules
            if (actions.containsKey("particle")) {
                // TODO: Implémenter système de particules pour 1.8.8
            }

            executeActionCommands(actions, player);
            
        } catch (Exception e) {
            plugin.getLogger().warning("Erreur lors de l'exécution des actions de succès: " + e.getMessage());
        }
    }

    /**
     * Exécute les actions d'échec d'une recette.
     */
    private void executeFailActions(org.bukkit.entity.Player player, CraftRecipe recipe) {
        Map<String, Object> actions = recipe.getOnFail();
        if (actions == null || actions.isEmpty()) {
            return;
        }

        try {
            // Messages
            if (actions.containsKey("message")) {
                String message = actions.get("message").toString();
                message = applyActionPlaceholders(message, player);
                MessageUtil.sendRaw(player, message);
            }

            // Sons
            if (actions.containsKey("sound")) {
                String soundName = actions.get("sound").toString();
                try {
                    org.bukkit.Sound sound = org.bukkit.Sound.valueOf(soundName);
                    player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
                } catch (Exception e) {
                    // Son invalide, ignorer
                }
            }

            executeActionCommands(actions, player);

        } catch (Exception e) {
            plugin.getLogger().warning("Erreur lors de l'exécution des actions d'échec: " + e.getMessage());
        }
    }

    private void executeActionCommands(Map<String, Object> actions, org.bukkit.entity.Player player) {
        if (actions == null || actions.isEmpty()) {
            return;
        }

        boolean consoleCommand = Boolean.TRUE.equals(actions.get("console-command"));

        if (actions.containsKey("commands") && actions.get("commands") instanceof List) {
            for (Object cmdObj : (List<?>) actions.get("commands")) {
                if (cmdObj == null) {
                    continue;
                }

                String command = applyActionPlaceholders(cmdObj.toString(), player).trim();
                if (command.isEmpty()) {
                    continue;
                }

                dispatchActionCommand(player, command, consoleCommand);
            }
            return;
        }

        if (actions.containsKey("command") && actions.get("command") != null) {
            String command = applyActionPlaceholders(actions.get("command").toString(), player).trim();
            if (!command.isEmpty()) {
                dispatchActionCommand(player, command, consoleCommand);
            }
        }
    }

    private void dispatchActionCommand(org.bukkit.entity.Player player, String command, boolean consoleCommand) {
        if (consoleCommand) {
            org.bukkit.Bukkit.getServer().dispatchCommand(
                org.bukkit.Bukkit.getServer().getConsoleSender(),
                command
            );
            return;
        }

        org.bukkit.Bukkit.getServer().dispatchCommand(player, command);
    }

    private String applyActionPlaceholders(String text, org.bukkit.entity.Player player) {
        return text
            .replace("{player_name}", player.getName())
            .replace("{player}", player.getName())
            .replace("%player%", player.getName());
    }
    
    /**
     * Récupère tous les IDs de craft disponibles
     * @return Set contenant tous les IDs de craft
     */
    public java.util.Set<String> getAllCraftIds() {
        return recipes.keySet();
    }
    
    /**
     * Récupère le nombre total de crafts chargés
     * @return Nombre de crafts
     */
    public int getTotalCrafts() {
        return recipes.size();
    }
}
