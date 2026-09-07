package me.krunsh.kcraft.matching;

import java.util.List;
import java.util.Map;

import me.krunsh.kcraft.compiled.CompiledIngredient;
import me.krunsh.kcraft.compiled.CompiledPatternCell;
import me.krunsh.kcraft.compiled.CompiledRecipe;
import me.krunsh.kcraft.utils.NbtValueComparator;

/**
 * Matcher exact V2.3.
 *
 * Aucune copie d'ItemStack.
 * Aucune consommation.
 * Aucun tri shapeless.
 */
public final class RecipeMatcher {

    public boolean matches(
            CompiledRecipe recipe,
            MatrixSnapshot matrix) {

        return matches(
            recipe,
            matrix,
            false
        );
    }

    public boolean matchesVanilla(
            CompiledRecipe recipe,
            MatrixSnapshot matrix) {

        return matches(
            recipe,
            matrix,
            true
        );
    }

    private boolean matches(
            CompiledRecipe recipe,
            MatrixSnapshot matrix,
            boolean vanilla) {

        if (recipe == null
                || matrix == null
                || matrix.getOccupiedCount()
                    != recipe.getOccupiedSlots()) {

            return false;
        }

        if (recipe.getType()
                .name()
                .equals("SHAPELESS")) {

            return matchesShapeless(
                recipe,
                matrix,
                vanilla
            );
        }

        return matchesShaped(
            recipe,
            matrix,
            vanilla
        );
    }

    private boolean matchesShaped(
            CompiledRecipe recipe,
            MatrixSnapshot matrix,
            boolean vanilla) {

        int grid =
            matrix.getGridSize();

        int rows =
            recipe.getPatternHeight();

        int columns =
            recipe.getPatternWidth();

        if (rows <= 0
                || columns <= 0
                || rows > grid
                || columns > grid) {

            return false;
        }

        for (int startRow = 0;
                startRow <= grid - rows;
                startRow++) {

            for (int startColumn = 0;
                    startColumn <= grid - columns;
                    startColumn++) {

                if (matchesShapedAt(
                        recipe,
                        matrix,
                        startRow,
                        startColumn,
                        false,
                        vanilla)) {

                    return true;
                }

                if (recipe.isAllowMirror()
                        && matchesShapedAt(
                            recipe,
                            matrix,
                            startRow,
                            startColumn,
                            true,
                            vanilla)) {

                    return true;
                }
            }
        }

        return false;
    }

    private boolean matchesShapedAt(
            CompiledRecipe recipe,
            MatrixSnapshot matrix,
            int startRow,
            int startColumn,
            boolean mirror,
            boolean vanilla) {

        int grid =
            matrix.getGridSize();

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

            ItemSnapshot item =
                matrix.getSlot(
                    matrixIndex
                );

            if (!matchesIngredient(
                    cell.getIngredient(),
                    item,
                    vanilla,
                    recipe.isVanillaLooseMatch())) {

                return false;
            }
        }

        /*
         * occupiedCount == shapedCells.size().
         * Si toutes les cellules requises sont remplies, aucun item
         * supplementaire ne peut exister ailleurs dans la matrice.
         */
        return true;
    }

    private boolean matchesShapeless(
            CompiledRecipe recipe,
            MatrixSnapshot matrix,
            boolean vanilla) {

        List<CompiledIngredient> ingredients =
            recipe.getShapelessIngredients();

        ItemSnapshot[] slots =
            matrix.getOccupied();

        if (ingredients.size() != slots.length
                || slots.length > 31) {

            return false;
        }

        return assign(
            ingredients,
            slots,
            0,
            0,
            vanilla,
            recipe.isVanillaLooseMatch()
        );
    }

    /**
     * Backtracking avec bitmask int.
     *
     * Plus de boolean[], int[] ou liste de slots alloues par candidat.
     * Avec les tables V2 max 5x5, 25 bits suffisent.
     */
    private boolean assign(
            List<CompiledIngredient> ingredients,
            ItemSnapshot[] slots,
            int ingredientIndex,
            int usedMask,
            boolean vanilla,
            boolean loose) {

        if (ingredientIndex
                >= ingredients.size()) {

            return true;
        }

        CompiledIngredient ingredient =
            ingredients.get(
                ingredientIndex
            );

        for (int slotIndex = 0;
                slotIndex < slots.length;
                slotIndex++) {

            int bit =
                1 << slotIndex;

            if ((usedMask & bit) != 0) {
                continue;
            }

            if (!matchesIngredient(
                    ingredient,
                    slots[slotIndex],
                    vanilla,
                    loose)) {

                continue;
            }

            if (assign(
                    ingredients,
                    slots,
                    ingredientIndex + 1,
                    usedMask | bit,
                    vanilla,
                    loose)) {

                return true;
            }
        }

        return false;
    }

    private boolean matchesIngredient(
            CompiledIngredient ingredient,
            ItemSnapshot item,
            boolean vanilla,
            boolean loose) {

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

        if (item.getAmount()
                < ingredient.getAmount()) {

            return false;
        }

        if (ingredient.requiresName()) {
            String actual =
                item.getDisplayName();

            if (!ingredient.getExpectedName()
                    .equals(actual)) {

                return false;
            }
        }

        if (ingredient.requiresLore()) {
            List<String> actual =
                item.getLore();

            if (!ingredient.getExpectedLore()
                    .equals(actual)) {

                return false;
            }
        }

        if (ingredient.requiresNbt()) {
            for (Map.Entry<String, Object> requirement
                    : ingredient.getRequiredNbt()
                        .entrySet()) {

                String key =
                    requirement.getKey();

                if (!item.hasNbtKey(key)) {
                    return false;
                }

                Object actual =
                    item.getNbtValue(key);

                if (!NbtValueComparator.equivalent(
                        requirement.getValue(),
                        actual)) {

                    return false;
                }
            }
        } else if (ingredient.isStrictMaterial()
                && item.hasCustomNbt()) {

            return false;
        }

        if (vanilla
                && !ingredient.isIdentityRequired()
                && !ingredient.isAllowCustomItems()
                && !loose
                && item.hasCustomNbt()) {

            return false;
        }

        return true;
    }
}
