package me.krunsh.kcraft.matching;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.Test;

import me.krunsh.kcraft.catalog.RecipeCatalog;
import me.krunsh.kcraft.compiled.CompiledRecipeCatalog;
import me.krunsh.kcraft.models.CraftIngredient;
import me.krunsh.kcraft.models.CraftRecipe;
import me.krunsh.kcraft.models.CraftType;

public class RecipeIndexTest {

    @Test
    public void indexNarrowsCandidatesByMaterialMultiset() {
        CraftRecipe sticks =
            new CraftRecipe("sticks");

        sticks.setType(CraftType.SHAPED);
        sticks.setRequiredTable("tier1");
        sticks.setPattern(java.util.Arrays.asList("A", "A"));

        Map<Character, CraftIngredient> stickIngredients =
            new LinkedHashMap<Character, CraftIngredient>();

        stickIngredients.put(
            Character.valueOf('A'),
            new CraftIngredient(Material.STICK)
        );

        sticks.setIngredients(stickIngredients);

        CraftRecipe diamonds =
            new CraftRecipe("diamonds");

        diamonds.setType(CraftType.SHAPED);
        diamonds.setRequiredTable("tier1");
        diamonds.setPattern(java.util.Arrays.asList("A", "A"));

        Map<Character, CraftIngredient> diamondIngredients =
            new LinkedHashMap<Character, CraftIngredient>();

        diamondIngredients.put(
            Character.valueOf('A'),
            new CraftIngredient(Material.DIAMOND)
        );

        diamonds.setIngredients(diamondIngredients);

        Map<String, CraftRecipe> recipes =
            new LinkedHashMap<String, CraftRecipe>();

        recipes.put(sticks.getId(), sticks);
        recipes.put(diamonds.getId(), diamonds);

        CompiledRecipeCatalog compiled =
            CompiledRecipeCatalog.compile(
                RecipeCatalog.snapshot(recipes)
            );

        RecipeIndex index =
            RecipeIndex.build(compiled);

        ItemStack[] matrix =
            new ItemStack[9];

        matrix[0] =
            new ItemStack(Material.STICK);

        matrix[3] =
            new ItemStack(Material.STICK);

        MatrixSnapshot snapshot =
            MatrixSnapshot.capture(matrix);

        assertEquals(
            1,
            index.candidates(
                snapshot,
                "tier1"
            ).size()
        );

        assertEquals(
            "sticks",
            index.candidates(
                snapshot,
                "tier1"
            ).get(0).getId()
        );

        assertTrue(
            index.getBucketCount() >= 2
        );
    }
}
