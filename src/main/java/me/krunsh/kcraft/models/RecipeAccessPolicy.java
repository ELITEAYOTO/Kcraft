package me.krunsh.kcraft.models;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

/** Regles pures d'acces d'une recette a la grille vanilla 3x3. */
public final class RecipeAccessPolicy {
    private RecipeAccessPolicy() {}

    public static boolean fitsGrid(CraftRecipe recipe, int gridSize) {
        if (recipe == null || gridSize <= 0) return false;
        if (recipe.getType() == CraftType.SHAPELESS) {
            List<CraftIngredient> ingredients = recipe.getShapelessIngredients();
            return ingredients != null && !ingredients.isEmpty()
                    && ingredients.size() <= gridSize * gridSize;
        }
        List<String> pattern = recipe.getPattern();
        if (pattern == null || pattern.isEmpty() || pattern.size() > gridSize) return false;
        int maxColumns = 0;
        for (String row : pattern) {
            if (row == null) return false;
            maxColumns = Math.max(maxColumns, row.length());
        }
        return maxColumns > 0 && maxColumns <= gridSize;
    }

    public static boolean hasCustomTableOnlyTag(Collection<String> tags) {
        if (tags == null) return false;
        for (String raw : tags) {
            if (raw == null) continue;
            String tag = raw.trim().toLowerCase(Locale.ROOT);
            if (tag.startsWith("kcraft_") && tag.endsWith("_only")
                    && tag.length() > "kcraft__only".length()) return true;
        }
        return false;
    }

    public static boolean allowsVanilla(CraftRecipe recipe) {
        return recipe != null && recipe.isAllowVanillaWorkbench()
                && fitsGrid(recipe, 3)
                && !hasCustomTableOnlyTag(recipe.getTags());
    }
}
