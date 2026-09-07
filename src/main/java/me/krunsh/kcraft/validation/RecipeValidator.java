package me.krunsh.kcraft.validation;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import me.krunsh.kcraft.catalog.RecipeCatalog;
import me.krunsh.kcraft.config.ConfigValidationReport;
import me.krunsh.kcraft.models.CraftIngredient;
import me.krunsh.kcraft.models.CraftRecipe;
import me.krunsh.kcraft.models.CraftTable;
import me.krunsh.kcraft.models.CraftType;

/** Validation structurelle des recettes déjà parsées. */
public final class RecipeValidator {

    private RecipeValidator() {}

    public static ConfigValidationReport validate(RecipeCatalog catalog,
                                                  Map<String, CraftTable> tables) {
        ConfigValidationReport report = new ConfigValidationReport();
        if (catalog == null) {
            report.error("RecipeCatalog absent.");
            return report;
        }

        for (CraftRecipe recipe : catalog.recipes()) {
            validateRecipe(recipe, tables, report);
        }
        return report;
    }

    private static void validateRecipe(CraftRecipe recipe, Map<String, CraftTable> tables,
                                       ConfigValidationReport report) {
        String id = recipe == null ? "<null>" : recipe.getId();
        if (recipe == null) {
            report.error("Recette null dans le catalogue.");
            return;
        }

        CraftTable table = tables == null ? null : tables.get(recipe.getRequiredTable());
        if (table == null) {
            report.error("Recette '" + id + "': table requise inconnue '"
                + recipe.getRequiredTable() + "'.");
            return;
        }

        int[] dimensions = table.getDimensions();
        int capacity = dimensions[0] * dimensions[1];

        if (recipe.getType() == CraftType.SHAPELESS) {
            List<CraftIngredient> ingredients = recipe.getShapelessIngredients();
            if (ingredients == null || ingredients.isEmpty()) {
                report.error("Recette SHAPELESS '" + id + "': aucun ingredient.");
            } else if (ingredients.size() > capacity) {
                report.error("Recette SHAPELESS '" + id + "': " + ingredients.size()
                    + " ingredients pour une table " + table.getSize() + ".");
            }
        } else {
            validateShaped(recipe, table, report);
        }

        if (recipe.shouldGiveResult()
                && (recipe.getResults() == null || recipe.getResults().isEmpty())) {
            report.error("Recette '" + id + "': aucun résultat alors que consume-result n'est pas false.");
        }
    }

    private static void validateShaped(CraftRecipe recipe, CraftTable table,
                                       ConfigValidationReport report) {
        String id = recipe.getId();
        List<String> pattern = recipe.getPattern();
        Map<Character, CraftIngredient> ingredients = recipe.getIngredients();
        if (pattern == null || pattern.isEmpty()) {
            report.error("Recette SHAPED '" + id + "': pattern absent.");
            return;
        }

        int width = -1;
        Set<Character> used = new HashSet<Character>();
        for (int rowIndex = 0; rowIndex < pattern.size(); rowIndex++) {
            String row = pattern.get(rowIndex);
            if (row == null || row.isEmpty()) {
                report.error("Recette '" + id + "': ligne de pattern " + rowIndex + " vide.");
                continue;
            }
            if (width < 0) width = row.length();
            if (row.length() != width) {
                report.error("Recette '" + id + "': toutes les lignes du pattern doivent avoir la même largeur.");
            }
            for (int col = 0; col < row.length(); col++) {
                char symbol = row.charAt(col);
                if (symbol != ' ') used.add(symbol);
            }
        }

        int[] dims = table.getDimensions();
        if (pattern.size() > dims[0] || width > dims[1]) {
            report.error("Recette '" + id + "': pattern " + width + "x" + pattern.size()
                + " trop grand pour table " + table.getSize() + ".");
        }

        if (ingredients == null) {
            report.error("Recette '" + id + "': map ingredients absente.");
            return;
        }

        for (Character symbol : used) {
            if (!ingredients.containsKey(symbol)) {
                report.error("Recette '" + id + "': symbole '" + symbol
                    + "' utilisé dans pattern mais non déclaré dans ingredients.");
            }
        }
        for (Character declared : ingredients.keySet()) {
            if (!used.contains(declared)) {
                report.warning("Recette '" + id + "': ingredient '" + declared
                    + "' déclaré mais jamais utilisé dans pattern.");
            }
        }
    }
}
