package me.krunsh.kcraft.models;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.LinkedHashSet;

import org.junit.Test;

public class RecipeAccessPolicyTest {

    @Test
    public void shapedThreeByThreeWithoutRestrictionIsVanillaCompatible() {
        CraftRecipe recipe = new CraftRecipe("allowed");
        recipe.setPattern(Arrays.asList("AAA", " B ", " B "));
        assertTrue(RecipeAccessPolicy.allowsVanilla(recipe));
    }

    @Test
    public void largerPatternIsCustomTableOnlyBySize() {
        CraftRecipe recipe = new CraftRecipe("large");
        recipe.setPattern(Arrays.asList("AAAA", "AAAA", "AAAA", "AAAA"));
        assertFalse(RecipeAccessPolicy.allowsVanilla(recipe));
    }

    @Test
    public void kcraftTableOnlyTagDeniesVanillaEvenForSmallRecipe() {
        CraftRecipe recipe = new CraftRecipe("restricted");
        recipe.setPattern(Arrays.asList("A"));
        recipe.setTags(new LinkedHashSet<String>(Arrays.asList("kcraft_tier1_only")));
        assertFalse(RecipeAccessPolicy.allowsVanilla(recipe));
    }

    @Test
    public void legacyExplicitDisableRemainsSupported() {
        CraftRecipe recipe = new CraftRecipe("legacy-disabled");
        recipe.setPattern(Arrays.asList("A"));
        recipe.setAllowVanillaWorkbench(false);
        assertFalse(RecipeAccessPolicy.allowsVanilla(recipe));
    }
}
