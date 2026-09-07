package me.krunsh.kcraft.batch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.Test;

import me.krunsh.kcraft.compiled.CompiledRecipe;
import me.krunsh.kcraft.compiled.RecipeCompiler;
import me.krunsh.kcraft.matching.RecipeMatcher;
import me.krunsh.kcraft.models.CraftIngredient;
import me.krunsh.kcraft.models.CraftRecipe;
import me.krunsh.kcraft.models.CraftType;

public class CraftBatchPlannerTest {

    private final CraftBatchPlanner planner =
        new CraftBatchPlanner(
            new RecipeMatcher()
        );

    @Test
    public void shapedPlansExactCraftCount() {
        CraftRecipe recipe =
            shapedRecipe();

        CompiledRecipe compiled =
            new RecipeCompiler()
                .compile(recipe);

        ItemStack[] matrix =
            new ItemStack[9];

        matrix[0] =
            new ItemStack(
                Material.DIAMOND,
                10
            );

        matrix[3] =
            new ItemStack(
                Material.STICK,
                5
            );

        CraftBatchPlan plan =
            planner.plan(
                recipe,
                compiled,
                matrix,
                64
            );

        assertNotNull(plan);

        assertEquals(
            5,
            plan.getCraftCount()
        );
    }

    @Test
    public void appliesConsumptionWithoutTouchingSource() {
        CraftRecipe recipe =
            shapedRecipe();

        CompiledRecipe compiled =
            new RecipeCompiler()
                .compile(recipe);

        ItemStack[] matrix =
            new ItemStack[9];

        matrix[0] =
            new ItemStack(
                Material.DIAMOND,
                4
            );

        matrix[3] =
            new ItemStack(
                Material.STICK,
                2
            );

        CraftBatchPlan plan =
            planner.plan(
                recipe,
                compiled,
                matrix,
                64
            );

        assertNotNull(plan);
        assertEquals(2, plan.getCraftCount());

        plan.applyConsumption(0);
        plan.applyConsumption(1);

        assertNull(
            plan.getWorkingMatrix()[0]
        );

        assertNull(
            plan.getWorkingMatrix()[3]
        );

        assertEquals(
            4,
            matrix[0].getAmount()
        );

        assertEquals(
            2,
            matrix[3].getAmount()
        );
    }

    @Test
    public void obeysConfiguredLimit() {
        CraftRecipe recipe =
            shapedRecipe();

        CompiledRecipe compiled =
            new RecipeCompiler()
                .compile(recipe);

        ItemStack[] matrix =
            new ItemStack[9];

        matrix[0] =
            new ItemStack(
                Material.DIAMOND,
                64
            );

        matrix[3] =
            new ItemStack(
                Material.STICK,
                64
            );

        CraftBatchPlan plan =
            planner.plan(
                recipe,
                compiled,
                matrix,
                7
            );

        assertEquals(
            7,
            plan.getCraftCount()
        );
    }

    private static CraftRecipe shapedRecipe() {
        CraftRecipe recipe =
            new CraftRecipe("batch");

        recipe.setType(
            CraftType.SHAPED
        );

        recipe.setRequiredTable(
            "tier1"
        );

        recipe.setPattern(
            Arrays.asList(
                "A",
                "B"
            )
        );

        Map<Character, CraftIngredient> ingredients =
            new LinkedHashMap<Character, CraftIngredient>();

        ingredients.put(
            Character.valueOf('A'),
            new CraftIngredient(
                Material.DIAMOND,
                2
            )
        );

        ingredients.put(
            Character.valueOf('B'),
            new CraftIngredient(
                Material.STICK,
                1
            )
        );

        recipe.setIngredients(
            ingredients
        );

        return recipe;
    }
}
