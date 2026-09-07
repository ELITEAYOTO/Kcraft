package me.krunsh.kcraft.matching;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.Test;

import me.krunsh.kcraft.compiled.CompiledRecipe;
import me.krunsh.kcraft.compiled.RecipeCompiler;
import me.krunsh.kcraft.models.CraftIngredient;
import me.krunsh.kcraft.models.CraftRecipe;
import me.krunsh.kcraft.models.CraftType;

public class RecipeMatcherTest {

    private final RecipeMatcher matcher =
        new RecipeMatcher();

    @Test
    public void shapedMatchesOffsetWithoutClone() {
        CraftRecipe recipe =
            shapedPickaxe();

        CompiledRecipe compiled =
            new RecipeCompiler().compile(recipe);

        ItemStack[] matrix =
            new ItemStack[16];

        matrix[5] = new ItemStack(Material.DIAMOND);
        matrix[6] = new ItemStack(Material.DIAMOND);
        matrix[7] = new ItemStack(Material.DIAMOND);
        matrix[10] = new ItemStack(Material.STICK);
        matrix[14] = new ItemStack(Material.STICK);

        assertTrue(
            matcher.matches(
                compiled,
                MatrixSnapshot.capture(matrix)
            )
        );
    }

    @Test
    public void shapedRejectsExtraItem() {
        CraftRecipe recipe =
            shapedPickaxe();

        CompiledRecipe compiled =
            new RecipeCompiler().compile(recipe);

        ItemStack[] matrix =
            new ItemStack[16];

        matrix[5] = new ItemStack(Material.DIAMOND);
        matrix[6] = new ItemStack(Material.DIAMOND);
        matrix[7] = new ItemStack(Material.DIAMOND);
        matrix[10] = new ItemStack(Material.STICK);
        matrix[14] = new ItemStack(Material.STICK);
        matrix[0] = new ItemStack(Material.DIRT);

        assertFalse(
            matcher.matches(
                compiled,
                MatrixSnapshot.capture(matrix)
            )
        );
    }

    @Test
    public void shapelessBacktrackingMatches() {
        CraftRecipe recipe =
            new CraftRecipe("mix");

        recipe.setType(CraftType.SHAPELESS);
        recipe.setRequiredTable("tier1");

        CraftIngredient generic =
            new CraftIngredient(
                Material.STONE,
                1
            );

        CraftIngredient data =
            new CraftIngredient(
                Material.STONE,
                1
            );

        data.setDataValue(
            Short.valueOf((short) 1)
        );

        recipe.setShapelessIngredients(
            Arrays.asList(
                generic,
                data
            )
        );

        CompiledRecipe compiled =
            new RecipeCompiler().compile(recipe);

        ItemStack[] matrix =
            new ItemStack[9];

        matrix[0] =
            new ItemStack(
                Material.STONE,
                1,
                (short) 0
            );

        matrix[1] =
            new ItemStack(
                Material.STONE,
                1,
                (short) 1
            );

        assertTrue(
            matcher.matches(
                compiled,
                MatrixSnapshot.capture(matrix)
            )
        );
    }

    private static CraftRecipe shapedPickaxe() {
        CraftRecipe recipe =
            new CraftRecipe("pickaxe");

        recipe.setType(CraftType.SHAPED);
        recipe.setRequiredTable("tier1");

        recipe.setPattern(
            Arrays.asList(
                "AAA",
                " B ",
                " B "
            )
        );

        Map<Character, CraftIngredient> ingredients =
            new LinkedHashMap<Character, CraftIngredient>();

        ingredients.put(
            Character.valueOf('A'),
            new CraftIngredient(Material.DIAMOND)
        );

        ingredients.put(
            Character.valueOf('B'),
            new CraftIngredient(Material.STICK)
        );

        recipe.setIngredients(
            ingredients
        );

        return recipe;
    }
}
