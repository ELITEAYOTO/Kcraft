package me.krunsh.kcraft.models;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.bukkit.inventory.ItemStack;

import me.krunsh.kcraft.utils.ItemStackUtil;

/** Match et consommation d'un craft dans une seule decision atomique. */
public final class CraftMatrixTransaction {
    private CraftMatrixTransaction() {}

    public static ItemStack[] consumeOnce(CraftRecipe recipe, ItemStack[] matrix) {
        return consumeOnce(recipe, matrix, false);
    }

    /** Match securise utilise exclusivement par la table vanilla. */
    public static ItemStack[] consumeOnceVanilla(CraftRecipe recipe, ItemStack[] matrix) {
        return consumeOnce(recipe, matrix, true);
    }

    private static ItemStack[] consumeOnce(CraftRecipe recipe, ItemStack[] matrix, boolean vanilla) {
        if (recipe == null || matrix == null) return null;
        int size = (int) Math.sqrt(matrix.length);
        if (size * size != matrix.length) return null;
        if (recipe.getType() == CraftType.SHAPELESS) return consumeShapeless(recipe, matrix, vanilla);
        ItemStack[] direct = consumeShaped(recipe, matrix, size, vanilla, false);
        if (direct != null || !recipe.isAllowMirror()) return direct;
        return consumeShaped(recipe, matrix, size, vanilla, true);
    }

    public static boolean matches(CraftRecipe recipe, ItemStack[] matrix) {
        return consumeOnce(recipe, matrix) != null;
    }

    public static boolean matchesVanilla(CraftRecipe recipe, ItemStack[] matrix) {
        return consumeOnceVanilla(recipe, matrix) != null;
    }

    private static ItemStack[] consumeShaped(CraftRecipe recipe, ItemStack[] matrix, int size,
                                             boolean vanilla, boolean mirror) {
        List<String> pattern = recipe.getPattern();
        Map<Character, CraftIngredient> ingredients = recipe.getIngredients();
        if (pattern == null || pattern.isEmpty() || ingredients == null) return null;
        int rows = pattern.size();
        int cols = 0;
        for (String row : pattern) cols = Math.max(cols, row == null ? 0 : row.length());
        if (rows > size || cols <= 0 || cols > size) return null;

        for (int startRow = 0; startRow <= size - rows; startRow++) {
            for (int startCol = 0; startCol <= size - cols; startCol++) {
                ItemStack[] result = cloneMatrix(matrix);
                if (consumeShapedAt(result, pattern, ingredients, startRow, startCol,
                        size, cols, vanilla, recipe.isVanillaLooseMatch(), mirror)) {
                    return result;
                }
            }
        }
        return null;
    }

    private static boolean consumeShapedAt(ItemStack[] result, List<String> pattern,
                                           Map<Character, CraftIngredient> ingredients,
                                           int startRow, int startCol, int size, int cols,
                                           boolean vanilla, boolean loose, boolean mirror) {
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                int index = row * size + col;
                boolean inside = row >= startRow && row < startRow + pattern.size()
                        && col >= startCol && col < startCol + cols;
                char symbol = ' ';
                if (inside) {
                    String patternRow = pattern.get(row - startRow);
                    int rawPatternCol = col - startCol;
                    int patternCol = mirror ? cols - 1 - rawPatternCol : rawPatternCol;
                    if (patternRow != null && patternCol < patternRow.length()) {
                        symbol = patternRow.charAt(patternCol);
                    }
                }
                ItemStack item = result[index];
                if (symbol == ' ') {
                    if (!isEmpty(item)) return false;
                    continue;
                }
                CraftIngredient ingredient = ingredients.get(symbol);
                if (ingredient == null || !matches(ingredient, item, vanilla, loose)) return false;
                result[index] = consume(item, ingredient.getAmount());
            }
        }
        return true;
    }

    private static ItemStack[] consumeShapeless(CraftRecipe recipe, ItemStack[] matrix, boolean vanilla) {
        List<CraftIngredient> ingredients = recipe.getShapelessIngredients();
        if (ingredients == null || ingredients.isEmpty()) return null;
        List<Integer> slots = new ArrayList<Integer>();
        for (int i = 0; i < matrix.length; i++) if (!isEmpty(matrix[i])) slots.add(i);
        if (slots.size() != ingredients.size()) return null;

        List<CraftIngredient> ordered = new ArrayList<CraftIngredient>(ingredients);
        ordered.sort(Comparator.comparingInt(CraftMatrixTransaction::specificity).reversed());
        int[] assignment = new int[ordered.size()];
        Arrays.fill(assignment, -1);
        if (!assign(0, ordered, slots, matrix, new boolean[slots.size()], assignment,
                vanilla, recipe.isVanillaLooseMatch())) return null;

        ItemStack[] result = cloneMatrix(matrix);
        for (int i = 0; i < ordered.size(); i++) {
            int slot = slots.get(assignment[i]);
            result[slot] = consume(result[slot], ordered.get(i).getAmount());
        }
        return result;
    }

    private static boolean assign(int ingredientIndex, List<CraftIngredient> ingredients,
                                  List<Integer> slots, ItemStack[] matrix,
                                  boolean[] used, int[] assignment,
                                  boolean vanilla, boolean loose) {
        if (ingredientIndex >= ingredients.size()) return true;
        CraftIngredient ingredient = ingredients.get(ingredientIndex);
        for (int slotIndex = 0; slotIndex < slots.size(); slotIndex++) {
            if (used[slotIndex] || !matches(ingredient, matrix[slots.get(slotIndex)], vanilla, loose)) continue;
            used[slotIndex] = true;
            assignment[ingredientIndex] = slotIndex;
            if (assign(ingredientIndex + 1, ingredients, slots, matrix, used, assignment,
                    vanilla, loose)) return true;
            assignment[ingredientIndex] = -1;
            used[slotIndex] = false;
        }
        return false;
    }

    private static int specificity(CraftIngredient ingredient) {
        return ingredient.getIdentitySpecificity();
    }

    private static boolean matches(CraftIngredient ingredient, ItemStack item,
                                   boolean vanilla, boolean loose) {
        return vanilla ? ingredient.matchesForVanilla(item, loose) : ingredient.matches(item);
    }

    private static ItemStack consume(ItemStack item, int amount) {
        int left = item.getAmount() - Math.max(1, amount);
        if (left <= 0) return null;
        ItemStack clone = item.clone();
        clone.setAmount(left);
        return clone;
    }

    private static ItemStack[] cloneMatrix(ItemStack[] matrix) {
        ItemStack[] copy = new ItemStack[matrix.length];
        for (int i = 0; i < matrix.length; i++) copy[i] = matrix[i] == null ? null : matrix[i].clone();
        return copy;
    }

    private static boolean isEmpty(ItemStack item) {
        return !ItemStackUtil.isPresent(item);
    }
}
