package me.krunsh.kcraft.batch;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import me.krunsh.kcraft.models.CraftRecipe;

/**
 * Plan immutable d'un shift-craft.
 *
 * La phase planning a deja :
 * - valide la recette sur la matrice initiale ;
 * - calcule toutes les affectations shapeless ;
 * - calcule jusqu'ou les stacks permettent de crafter ;
 * - prepare une copie de travail unique de la matrice.
 *
 * L'execution ne rematche donc plus la recette entre chaque craft.
 */
public final class CraftBatchPlan {

    private final CraftRecipe recipe;
    private final int[][] consumptionByCraft;
    private final ItemStack[] workingMatrix;

    CraftBatchPlan(
            CraftRecipe recipe,
            int[][] consumptionByCraft,
            ItemStack[] workingMatrix) {

        this.recipe = recipe;
        this.consumptionByCraft =
            consumptionByCraft;
        this.workingMatrix =
            workingMatrix;
    }

    public CraftRecipe getRecipe() {
        return recipe;
    }

    public int getCraftCount() {
        return consumptionByCraft.length;
    }

    /**
     * Matrice courante AVANT la consommation du craft index.
     * executeCraft peut la lire sans toucher a la vraie GUI.
     */
    public ItemStack[] getWorkingMatrix() {
        return workingMatrix;
    }

    public boolean applyConsumption(
            int craftIndex) {

        if (craftIndex < 0
                || craftIndex >= consumptionByCraft.length) {

            return false;
        }

        int[] consumption =
            consumptionByCraft[craftIndex];

        for (int slot = 0;
                slot < consumption.length;
                slot++) {

            int required =
                consumption[slot];

            if (required <= 0) {
                continue;
            }

            ItemStack item =
                workingMatrix[slot];

            if (item == null
                    || item.getType() == Material.AIR
                    || item.getAmount() < required) {

                return false;
            }
        }

        for (int slot = 0;
                slot < consumption.length;
                slot++) {

            int required =
                consumption[slot];

            if (required <= 0) {
                continue;
            }

            ItemStack item =
                workingMatrix[slot];

            int remaining =
                item.getAmount() - required;

            if (remaining <= 0) {
                workingMatrix[slot] = null;
            } else {
                item.setAmount(remaining);
            }
        }

        return true;
    }
}
