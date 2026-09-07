package me.krunsh.kcraft.compiled;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Material;

import me.krunsh.kcraft.models.CraftRecipe;
import me.krunsh.kcraft.models.CraftType;

/**
 * Representation immutable d'une recette, optimisee pour le futur matcher V2.
 *
 * CraftRecipe reste le contrat historique/execution pendant V2.2.
 * CompiledRecipe devient la source de verite du matching a partir de V2.3.
 */
public final class CompiledRecipe {

    private final String id;
    private final CraftRecipe source;

    private final CraftType type;
    private final String requiredTable;

    private final boolean allowVanillaWorkbench;
    private final boolean vanillaLooseMatch;
    private final boolean allowMirror;

    private final int patternWidth;
    private final int patternHeight;
    private final int occupiedSlots;
    private final int totalRequiredItems;

    private final List<CompiledPatternCell> shapedCells;
    private final List<CompiledIngredient> shapelessIngredients;

    private final Set<Material> distinctMaterials;
    private final Map<Material, Integer> requiredAmountsByMaterial;

    private final boolean requiresNbt;
    private final boolean requiresCustomIdentity;
    private final int identitySpecificity;

    private final int resultCount;
    private final double totalResultWeight;

    private final String primarySignature;

    CompiledRecipe(
            String id,
            CraftRecipe source,
            CraftType type,
            String requiredTable,
            boolean allowVanillaWorkbench,
            boolean vanillaLooseMatch,
            boolean allowMirror,
            int patternWidth,
            int patternHeight,
            int occupiedSlots,
            int totalRequiredItems,
            List<CompiledPatternCell> shapedCells,
            List<CompiledIngredient> shapelessIngredients,
            Set<Material> distinctMaterials,
            Map<Material, Integer> requiredAmountsByMaterial,
            boolean requiresNbt,
            boolean requiresCustomIdentity,
            int identitySpecificity,
            int resultCount,
            double totalResultWeight,
            String primarySignature) {

        this.id = id;
        this.source = source;
        this.type = type;
        this.requiredTable = requiredTable;

        this.allowVanillaWorkbench =
            allowVanillaWorkbench;

        this.vanillaLooseMatch =
            vanillaLooseMatch;

        this.allowMirror =
            allowMirror;

        this.patternWidth = patternWidth;
        this.patternHeight = patternHeight;
        this.occupiedSlots = occupiedSlots;
        this.totalRequiredItems =
            totalRequiredItems;

        this.shapedCells =
            Collections.unmodifiableList(
                new ArrayList<CompiledPatternCell>(
                    shapedCells
                )
            );

        this.shapelessIngredients =
            Collections.unmodifiableList(
                new ArrayList<CompiledIngredient>(
                    shapelessIngredients
                )
            );

        this.distinctMaterials =
            Collections.unmodifiableSet(
                new LinkedHashSet<Material>(
                    distinctMaterials
                )
            );

        this.requiredAmountsByMaterial =
            Collections.unmodifiableMap(
                new LinkedHashMap<Material, Integer>(
                    requiredAmountsByMaterial
                )
            );

        this.requiresNbt = requiresNbt;
        this.requiresCustomIdentity =
            requiresCustomIdentity;

        this.identitySpecificity =
            identitySpecificity;

        this.resultCount = resultCount;
        this.totalResultWeight =
            totalResultWeight;

        this.primarySignature =
            primarySignature;
    }

    public String getId() {
        return id;
    }

    public CraftRecipe getSource() {
        return source;
    }

    public CraftType getType() {
        return type;
    }

    public String getRequiredTable() {
        return requiredTable;
    }

    public boolean isAllowVanillaWorkbench() {
        return allowVanillaWorkbench;
    }

    public boolean isVanillaLooseMatch() {
        return vanillaLooseMatch;
    }

    public boolean isAllowMirror() {
        return allowMirror;
    }

    public int getPatternWidth() {
        return patternWidth;
    }

    public int getPatternHeight() {
        return patternHeight;
    }

    public int getOccupiedSlots() {
        return occupiedSlots;
    }

    public int getTotalRequiredItems() {
        return totalRequiredItems;
    }

    public List<CompiledPatternCell> getShapedCells() {
        return shapedCells;
    }

    public List<CompiledIngredient> getShapelessIngredients() {
        return shapelessIngredients;
    }

    public Set<Material> getDistinctMaterials() {
        return distinctMaterials;
    }

    public Map<Material, Integer> getRequiredAmountsByMaterial() {
        return requiredAmountsByMaterial;
    }

    public boolean isRequiresNbt() {
        return requiresNbt;
    }

    public boolean isRequiresCustomIdentity() {
        return requiresCustomIdentity;
    }

    public int getIdentitySpecificity() {
        return identitySpecificity;
    }

    public int getResultCount() {
        return resultCount;
    }

    public double getTotalResultWeight() {
        return totalResultWeight;
    }

    /**
     * Signature primaire grossiere pour V2.3.
     * Elle ne remplace pas le match exact.
     */
    public String getPrimarySignature() {
        return primarySignature;
    }
}
