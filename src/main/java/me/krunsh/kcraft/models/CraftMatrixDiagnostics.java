package me.krunsh.kcraft.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.inventory.ItemStack;

import me.krunsh.kcraft.utils.ItemStackUtil;

/** Diagnostic lisible de la même matrice que la transaction atomique. */
public final class CraftMatrixDiagnostics {
    private CraftMatrixDiagnostics() {}

    public static String describe(CraftRecipe recipe, ItemStack[] matrix) {
        if (recipe == null) return "RECETTE_ABSENTE";
        if (matrix == null) return "MATRICE_ABSENTE";
        int size = (int) Math.sqrt(matrix.length);
        if (size * size != matrix.length) return "TAILLE_MATRICE_NON_CARREE=" + matrix.length;
        if (!RecipeAccessPolicy.fitsGrid(recipe, size)) return "RECETTE_HORS_GRILLE_" + size + "x" + size;
        if (CraftMatrixTransaction.matchesVanilla(recipe, matrix)) return "MATCH";
        if (recipe.getType() == CraftType.SHAPELESS) return describeShapeless(recipe, matrix);
        return describeShaped(recipe, matrix, size);
    }

    private static String describeShapeless(CraftRecipe recipe, ItemStack[] matrix) {
        List<CraftIngredient> expected = recipe.getShapelessIngredients();
        List<ItemStack> actual = new ArrayList<ItemStack>();
        for (ItemStack item : matrix) if (!empty(item)) actual.add(item);
        if (actual.size() != expected.size()) {
            return "NOMBRE_SLOTS attendu=" + expected.size() + " actuel=" + actual.size();
        }
        for (int slot = 0; slot < matrix.length; slot++) {
            ItemStack item = matrix[slot];
            if (empty(item)) continue;
            boolean possible = false;
            List<String> reasons = new ArrayList<String>();
            for (CraftIngredient ingredient : expected) {
                String reason = ingredient.describeVanillaMismatch(item, recipe.isVanillaLooseMatch());
                if ("MATCH".equals(reason)) { possible = true; break; }
                reasons.add(reason);
            }
            if (!possible) return "SLOT_" + slot + "_SANS_INGREDIENT " + reasons;
        }
        return "AFFECTATION_SHAPELESS_IMPOSSIBLE (ingrédients concurrents ou quantités)";
    }

    private static String describeShaped(CraftRecipe recipe, ItemStack[] matrix, int size) {
        List<String> pattern = recipe.getPattern();
        int rows = pattern.size();
        int cols = 0;
        for (String row : pattern) cols = Math.max(cols, row == null ? 0 : row.length());
        Attempt best = null;
        int mirrorCount = recipe.isAllowMirror() ? 2 : 1;
        for (int mirrorIndex = 0; mirrorIndex < mirrorCount; mirrorIndex++) {
            boolean mirror = mirrorIndex == 1;
            for (int startRow = 0; startRow <= size - rows; startRow++) {
                for (int startCol = 0; startCol <= size - cols; startCol++) {
                    Attempt attempt = inspectAt(recipe, matrix, size, cols, startRow, startCol, mirror);
                    if (best == null || attempt.matchedSlots > best.matchedSlots) best = attempt;
                }
            }
        }
        return best == null ? "PATTERN_INVALIDE" : best.reason;
    }

    private static Attempt inspectAt(CraftRecipe recipe, ItemStack[] matrix, int size, int cols,
                                     int startRow, int startCol, boolean mirror) {
        int matched = 0;
        Map<Character, CraftIngredient> ingredients = recipe.getIngredients();
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                int index = row * size + col;
                boolean inside = row >= startRow && row < startRow + recipe.getPattern().size()
                    && col >= startCol && col < startCol + cols;
                char symbol = ' ';
                if (inside) {
                    String patternRow = recipe.getPattern().get(row - startRow);
                    int raw = col - startCol;
                    int patternCol = mirror ? cols - 1 - raw : raw;
                    if (patternRow != null && patternCol < patternRow.length()) symbol = patternRow.charAt(patternCol);
                }
                ItemStack item = matrix[index];
                if (symbol == ' ') {
                    if (!empty(item)) return new Attempt(matched, "SLOT_" + index + "_DEVRAIT_ETRE_VIDE actuel=" + item.getType());
                    matched++;
                    continue;
                }
                CraftIngredient ingredient = ingredients.get(symbol);
                if (ingredient == null) return new Attempt(matched, "SYMBOLE_SANS_INGREDIENT=" + symbol);
                String reason = ingredient.describeVanillaMismatch(item, recipe.isVanillaLooseMatch());
                if (!"MATCH".equals(reason)) return new Attempt(matched, "SLOT_" + index + " " + reason);
                matched++;
            }
        }
        return new Attempt(matched, "ECHEC_PATTERN_INCONNU");
    }

    private static boolean empty(ItemStack item) {
        return !ItemStackUtil.isPresent(item);
    }

    private static final class Attempt {
        private final int matchedSlots;
        private final String reason;
        private Attempt(int matchedSlots, String reason) { this.matchedSlots = matchedSlots; this.reason = reason; }
    }
}
