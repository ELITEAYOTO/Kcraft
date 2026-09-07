package me.krunsh.kcraft.batch;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import me.krunsh.kcraft.compiled.CompiledIngredient;
import me.krunsh.kcraft.compiled.CompiledPatternCell;
import me.krunsh.kcraft.compiled.CompiledRecipe;
import me.krunsh.kcraft.matching.ItemSnapshot;
import me.krunsh.kcraft.matching.MatrixSnapshot;
import me.krunsh.kcraft.matching.RecipeMatcher;
import me.krunsh.kcraft.models.CraftRecipe;

/**
 * Planner du shift-craft V2.4.
 *
 * Aucun scan RecipeIndex dans la boucle.
 * Aucun ItemStack.clone par craft.
 * Aucun NBTItem reconstruit par craft.
 *
 * Le planner travaille avec :
 * - un seul MatrixSnapshot ;
 * - des quantites primitives int[] ;
 * - les ItemSnapshot/NBT caches de V2.3.
 */
public final class CraftBatchPlanner {

    private final RecipeMatcher matcher;

    public CraftBatchPlanner(
            RecipeMatcher matcher) {

        if (matcher == null) {
            throw new IllegalArgumentException(
                "matcher ne peut pas etre null."
            );
        }

        this.matcher = matcher;
    }

    public CraftBatchPlan plan(
            CraftRecipe source,
            CompiledRecipe recipe,
            ItemStack[] matrix,
            int requestedMaxCrafts) {

        if (source == null
                || recipe == null
                || matrix == null
                || requestedMaxCrafts <= 0) {

            return null;
        }

        MatrixSnapshot snapshot =
            MatrixSnapshot.capture(matrix);

        if (snapshot == null
                || !matcher.matches(
                    recipe,
                    snapshot)) {

            return null;
        }

        int limit =
            Math.max(
                1,
                Math.min(
                    256,
                    requestedMaxCrafts
                )
            );

        int[] remaining =
            new int[matrix.length];

        for (int slot = 0;
                slot < matrix.length;
                slot++) {

            ItemStack item =
                matrix[slot];

            remaining[slot] =
                item == null
                    || item.getType() == Material.AIR
                    ? 0
                    : Math.max(
                        0,
                        item.getAmount()
                    );
        }

        int[][] planned =
            new int[limit][];

        int plannedCount = 0;

        for (int craft = 0;
                craft < limit;
                craft++) {

            int[] consumption =
                recipe.getType()
                    .name()
                    .equals("SHAPELESS")
                    ? planShapelessOnce(
                        recipe,
                        snapshot,
                        remaining
                    )
                    : planShapedOnce(
                        recipe,
                        snapshot,
                        remaining
                    );

            if (consumption == null) {
                break;
            }

            subtract(
                remaining,
                consumption
            );

            planned[plannedCount++] =
                consumption;
        }

        if (plannedCount <= 0) {
            return null;
        }

        int[][] exact =
            new int[plannedCount][];

        System.arraycopy(
            planned,
            0,
            exact,
            0,
            plannedCount
        );

        return new CraftBatchPlan(
            source,
            exact,
            cloneMatrixOnce(matrix)
        );
    }

    private int[] planShapedOnce(
            CompiledRecipe recipe,
            MatrixSnapshot snapshot,
            int[] remaining) {

        int grid =
            snapshot.getGridSize();

        int rows =
            recipe.getPatternHeight();

        int columns =
            recipe.getPatternWidth();

        for (int startRow = 0;
                startRow <= grid - rows;
                startRow++) {

            for (int startColumn = 0;
                    startColumn <= grid - columns;
                    startColumn++) {

                int[] normal =
                    shapedAt(
                        recipe,
                        snapshot,
                        remaining,
                        startRow,
                        startColumn,
                        false
                    );

                if (normal != null) {
                    return normal;
                }

                if (recipe.isAllowMirror()) {
                    int[] mirrored =
                        shapedAt(
                            recipe,
                            snapshot,
                            remaining,
                            startRow,
                            startColumn,
                            true
                        );

                    if (mirrored != null) {
                        return mirrored;
                    }
                }
            }
        }

        return null;
    }

    private int[] shapedAt(
            CompiledRecipe recipe,
            MatrixSnapshot snapshot,
            int[] remaining,
            int startRow,
            int startColumn,
            boolean mirror) {

        int[] consumption =
            new int[remaining.length];

        int grid =
            snapshot.getGridSize();

        int width =
            recipe.getPatternWidth();

        for (CompiledPatternCell cell
                : recipe.getShapedCells()) {

            int relativeColumn =
                mirror
                    ? width - 1 - cell.getColumn()
                    : cell.getColumn();

            int matrixIndex =
                (startRow + cell.getRow())
                    * grid
                    + startColumn
                    + relativeColumn;

            CompiledIngredient ingredient =
                cell.getIngredient();

            if (!matchesIdentity(
                    ingredient,
                    snapshot.getSlot(
                        matrixIndex
                    ))) {

                return null;
            }

            int required =
                ingredient.getAmount();

            if (remaining[matrixIndex]
                    < consumption[matrixIndex]
                        + required) {

                return null;
            }

            consumption[matrixIndex] +=
                required;
        }

        return consumption;
    }

    private int[] planShapelessOnce(
            CompiledRecipe recipe,
            MatrixSnapshot snapshot,
            int[] remaining) {

        List<CompiledIngredient> ingredients =
            recipe.getShapelessIngredients();

        ItemSnapshot[] occupied =
            snapshot.getOccupied();

        int[] occupiedIndexes =
            occupiedIndexes(snapshot);

        int[] consumption =
            new int[remaining.length];

        boolean matched =
            assignShapeless(
                ingredients,
                occupied,
                occupiedIndexes,
                remaining,
                consumption,
                0,
                0
            );

        return matched
            ? consumption
            : null;
    }

    private boolean assignShapeless(
            List<CompiledIngredient> ingredients,
            ItemSnapshot[] occupied,
            int[] occupiedIndexes,
            int[] remaining,
            int[] consumption,
            int ingredientIndex,
            int usedMask) {

        if (ingredientIndex
                >= ingredients.size()) {

            return true;
        }

        CompiledIngredient ingredient =
            ingredients.get(
                ingredientIndex
            );

        for (int localSlot = 0;
                localSlot < occupied.length;
                localSlot++) {

            int bit =
                1 << localSlot;

            if ((usedMask & bit) != 0) {
                continue;
            }

            int matrixSlot =
                occupiedIndexes[localSlot];

            if (!matchesIdentity(
                    ingredient,
                    occupied[localSlot])) {

                continue;
            }

            int required =
                ingredient.getAmount();

            if (remaining[matrixSlot]
                    < consumption[matrixSlot]
                        + required) {

                continue;
            }

            consumption[matrixSlot] +=
                required;

            if (assignShapeless(
                    ingredients,
                    occupied,
                    occupiedIndexes,
                    remaining,
                    consumption,
                    ingredientIndex + 1,
                    usedMask | bit)) {

                return true;
            }

            consumption[matrixSlot] -=
                required;
        }

        return false;
    }

    /**
     * Le planner reutilise la logique exacte V2.3.
     *
     * On construit une mini matrice 1-slot seulement quand une contrainte
     * d'identite complexe doit etre verifiee. Ce chemin est ensuite cache par
     * ItemSnapshot pour le meta/NBT.
     *
     * Pour les ingredients materiau/data purs, fast-path direct.
     */
    private boolean matchesIdentity(
            CompiledIngredient ingredient,
            ItemSnapshot item) {

        if (item == null
                || !item.isPresent()
                || item.getMaterial()
                    != ingredient.getMaterial()) {

            return false;
        }

        Short data =
            ingredient.getDataValue();

        if (data != null
                && item.getData()
                    != data.shortValue()) {

            return false;
        }

        if (ingredient.requiresName()) {
            if (!ingredient.getExpectedName()
                    .equals(
                        item.getDisplayName()
                    )) {

                return false;
            }
        }

        if (ingredient.requiresLore()) {
            if (!ingredient.getExpectedLore()
                    .equals(
                        item.getLore()
                    )) {

                return false;
            }
        }

        if (ingredient.requiresNbt()) {
            for (java.util.Map.Entry<String, Object> requirement
                    : ingredient.getRequiredNbt()
                        .entrySet()) {

                if (!item.hasNbtKey(
                        requirement.getKey())) {

                    return false;
                }

                Object actual =
                    item.getNbtValue(
                        requirement.getKey()
                    );

                if (!me.krunsh.kcraft.utils.NbtValueComparator
                        .equivalent(
                            requirement.getValue(),
                            actual)) {

                    return false;
                }
            }
        } else if (ingredient.isStrictMaterial()
                && item.hasCustomNbt()) {

            return false;
        }

        return true;
    }

    private static int[] occupiedIndexes(
            MatrixSnapshot snapshot) {

        int[] indexes =
            new int[
                snapshot.getOccupiedCount()
            ];

        int cursor = 0;

        int total =
            snapshot.getGridSize()
                * snapshot.getGridSize();

        for (int slot = 0;
                slot < total;
                slot++) {

            if (snapshot.getSlot(slot)
                    .isPresent()) {

                indexes[cursor++] =
                    slot;
            }
        }

        return indexes;
    }

    private static void subtract(
            int[] remaining,
            int[] consumption) {

        for (int slot = 0;
                slot < remaining.length;
                slot++) {

            remaining[slot] -=
                consumption[slot];
        }
    }

    private static ItemStack[] cloneMatrixOnce(
            ItemStack[] matrix) {

        ItemStack[] copy =
            new ItemStack[matrix.length];

        for (int slot = 0;
                slot < matrix.length;
                slot++) {

            ItemStack item =
                matrix[slot];

            copy[slot] =
                item == null
                    ? null
                    : item.clone();
        }

        return copy;
    }
}
