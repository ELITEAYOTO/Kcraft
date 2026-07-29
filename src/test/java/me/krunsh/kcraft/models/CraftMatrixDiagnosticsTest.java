package me.krunsh.kcraft.models;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.Test;

public class CraftMatrixDiagnosticsTest {
    @Test public void reportsMatchUsingTransactionSourceOfTruth() {
        CraftRecipe recipe = recipe(new CraftIngredient(Material.DIAMOND));
        ItemStack[] matrix = new ItemStack[9];
        matrix[4] = new ItemStack(Material.DIAMOND);
        assertEquals("MATCH", CraftMatrixDiagnostics.describe(recipe, matrix));
    }

    @Test public void reportsMaterialMismatchWithSlot() {
        CraftRecipe recipe = recipe(new CraftIngredient(Material.DIAMOND));
        ItemStack[] matrix = new ItemStack[9];
        matrix[4] = new ItemStack(Material.EMERALD);
        String reason = CraftMatrixDiagnostics.describe(recipe, matrix);
        assertTrue(reason, reason.contains("SLOT_4"));
        assertTrue(reason, reason.contains("MATERIAU"));
    }

    @Test public void reportsRequiredPhysicalAmount() {
        CraftIngredient ingredient = new CraftIngredient(Material.DIAMOND, 2);
        CraftRecipe recipe = recipe(ingredient);
        ItemStack[] matrix = new ItemStack[9];
        matrix[4] = new ItemStack(Material.DIAMOND, 1);
        assertTrue(CraftMatrixDiagnostics.describe(recipe, matrix).contains("QUANTITE"));
    }

    private static CraftRecipe recipe(CraftIngredient ingredient) {
        // Les tests unitaires tournent sans serveur NMS; cette option evite
        // volontairement la sonde NBT reservee aux tests d'integration Spigot.
        ingredient.setAllowCustomItems(true);
        return CraftRecipe.builder("diagnostic")
            .pattern("D")
            .ingredient('D', ingredient)
            .result(new CraftResult(Material.STONE))
            .build();
    }
}
