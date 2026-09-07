package me.krunsh.kcraft.gui;

import me.krunsh.kcraft.models.CraftRecipe;
import me.krunsh.kcraft.models.CraftResult;

/**
 * Selection de preview verrouillee pour une recette.
 *
 * Le resultat peut etre null pour les recettes consume-result:false.
 */
public final class PreviewSelection {

    private final String recipeId;
    private final CraftResult result;

    public PreviewSelection(
            String recipeId,
            CraftResult result) {

        if (recipeId == null
                || recipeId.trim().isEmpty()) {

            throw new IllegalArgumentException(
                "recipeId"
            );
        }

        this.recipeId = recipeId;
        this.result = result;
    }

    public boolean matches(
            CraftRecipe recipe) {

        return recipe != null
            && recipeId.equals(
                recipe.getId()
            );
    }

    public String getRecipeId() {
        return recipeId;
    }

    public CraftResult getResult() {
        return result;
    }
}
