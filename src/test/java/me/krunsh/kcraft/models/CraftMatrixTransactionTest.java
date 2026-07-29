package me.krunsh.kcraft.models;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.HashMap;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.Test;

public class CraftMatrixTransactionTest {

    @Test
    public void shapedConsumptionIsAtomicAndDoesNotMutateSource() {
        CraftRecipe recipe = new CraftRecipe("two-stones");
        recipe.setPattern(Arrays.asList("AA"));
        HashMap<Character, CraftIngredient> ingredients = new HashMap<Character, CraftIngredient>();
        ingredients.put('A', new CraftIngredient(Material.STONE, 1));
        recipe.setIngredients(ingredients);

        ItemStack[] matrix = new ItemStack[9];
        matrix[3] = new ItemStack(Material.STONE, 2);
        matrix[4] = new ItemStack(Material.STONE, 2);
        ItemStack[] consumed = CraftMatrixTransaction.consumeOnce(recipe, matrix);

        assertNotNull(consumed);
        assertEquals(1, consumed[3].getAmount());
        assertEquals(1, consumed[4].getAmount());
        assertEquals(2, matrix[3].getAmount());
        assertEquals(2, matrix[4].getAmount());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void shapelessSpecificIngredientCannotBeStolenByGenericOne() {
        CraftRecipe recipe = new CraftRecipe("two-wools");
        recipe.setType(CraftType.SHAPELESS);
        CraftIngredient generic = new CraftIngredient(Material.WOOL, 1);
        CraftIngredient red = new CraftIngredient(Material.WOOL, 1);
        red.setDataValue((short) 14);
        recipe.setShapelessIngredients(Arrays.asList(generic, red));

        ItemStack[] matrix = new ItemStack[9];
        matrix[0] = new ItemStack(Material.WOOL, 1, (short) 14);
        matrix[1] = new ItemStack(Material.WOOL, 1, (short) 0);
        ItemStack[] consumed = CraftMatrixTransaction.consumeOnce(recipe, matrix);

        assertNotNull(consumed);
        assertNull(consumed[0]);
        assertNull(consumed[1]);
    }

    @Test
    public void refusedMatrixProducesNoConsumptionPlan() {
        CraftRecipe recipe = new CraftRecipe("stone-only");
        recipe.setPattern(Arrays.asList("A"));
        HashMap<Character, CraftIngredient> ingredients = new HashMap<Character, CraftIngredient>();
        ingredients.put('A', new CraftIngredient(Material.STONE, 1));
        recipe.setIngredients(ingredients);
        ItemStack[] matrix = new ItemStack[9];
        matrix[0] = new ItemStack(Material.DIRT, 1);
        assertNull(CraftMatrixTransaction.consumeOnce(recipe, matrix));
        assertEquals(1, matrix[0].getAmount());
    }

    @Test
    public void nullAirAndZeroAmountAreEquivalentEmptySlots() {
        CraftRecipe recipe = new CraftRecipe("stone-with-empty-neighbours");
        recipe.setPattern(Arrays.asList("A"));
        HashMap<Character, CraftIngredient> ingredients = new HashMap<Character, CraftIngredient>();
        ingredients.put('A', new CraftIngredient(Material.STONE, 1));
        recipe.setIngredients(ingredients);

        ItemStack[] matrix = new ItemStack[9];
        matrix[0] = new ItemStack(Material.STONE, 1);
        matrix[1] = new ItemStack(Material.AIR, 1);
        matrix[8] = new ItemStack(Material.DIAMOND, 0);

        ItemStack[] consumed = CraftMatrixTransaction.consumeOnce(recipe, matrix);
        assertNotNull(consumed);
        assertNull(consumed[0]);
    }
}
