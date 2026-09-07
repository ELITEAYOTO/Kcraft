package me.krunsh.kcraft.compiled;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Material;

import me.krunsh.kcraft.models.CraftIngredient;
import me.krunsh.kcraft.models.CraftRecipe;
import me.krunsh.kcraft.models.CraftResult;
import me.krunsh.kcraft.models.CraftType;

/**
 * Compile CraftRecipe vers CompiledRecipe.
 *
 * Tout ce qui est stable est calcule ici une seule fois au reload.
 */
public final class RecipeCompiler {

    public CompiledRecipe compile(
            CraftRecipe recipe) {

        if (recipe == null) {
            throw new IllegalArgumentException(
                "recipe ne peut pas etre null."
            );
        }

        if (recipe.getType() == CraftType.SHAPELESS) {
            return compileShapeless(recipe);
        }

        return compileShaped(recipe);
    }

    private CompiledRecipe compileShaped(
            CraftRecipe recipe) {

        List<String> pattern =
            recipe.getPattern();

        Map<Character, CraftIngredient> sourceIngredients =
            recipe.getIngredients();

        Map<Character, CompiledIngredient> compiledBySymbol =
            new LinkedHashMap<Character, CompiledIngredient>();

        for (Map.Entry<Character, CraftIngredient> entry
                : sourceIngredients.entrySet()) {

            compiledBySymbol.put(
                entry.getKey(),
                CompiledIngredient.compile(
                    entry.getValue()
                )
            );
        }

        int height =
            pattern.size();

        int width = 0;

        List<CompiledPatternCell> cells =
            new ArrayList<CompiledPatternCell>();

        Set<Material> materials =
            new LinkedHashSet<Material>();

        Map<Material, Integer> amounts =
            new LinkedHashMap<Material, Integer>();

        boolean requiresNbt = false;
        boolean requiresIdentity = false;
        int specificity = 0;
        int totalItems = 0;

        for (int row = 0;
                row < pattern.size();
                row++) {

            String line =
                pattern.get(row);

            if (line == null) {
                continue;
            }

            width =
                Math.max(
                    width,
                    line.length()
                );

            for (int column = 0;
                    column < line.length();
                    column++) {

                char symbol =
                    line.charAt(column);

                if (symbol == ' ') {
                    continue;
                }

                CompiledIngredient ingredient =
                    compiledBySymbol.get(symbol);

                if (ingredient == null) {
                    /*
                     * Le RecipeValidator doit deja avoir refuse ce cas.
                     * Fail-fast ici aussi pour ne jamais compiler un artefact
                     * partiellement incoherent.
                     */
                    throw new IllegalStateException(
                        "Recette "
                            + recipe.getId()
                            + " : symbole non compile "
                            + symbol
                    );
                }

                cells.add(
                    new CompiledPatternCell(
                        row,
                        column,
                        symbol,
                        ingredient
                    )
                );

                accumulate(
                    ingredient,
                    materials,
                    amounts
                );

                totalItems +=
                    ingredient.getAmount();

                requiresNbt |=
                    ingredient.requiresNbt();

                requiresIdentity |=
                    ingredient.isIdentityRequired();

                specificity +=
                    ingredient.getIdentitySpecificity();
            }
        }

        return build(
            recipe,
            width,
            height,
            cells.size(),
            totalItems,
            cells,
            Collections.<CompiledIngredient>emptyList(),
            materials,
            amounts,
            requiresNbt,
            requiresIdentity,
            specificity
        );
    }

    private CompiledRecipe compileShapeless(
            CraftRecipe recipe) {

        List<CompiledIngredient> ingredients =
            new ArrayList<CompiledIngredient>();

        Set<Material> materials =
            new LinkedHashSet<Material>();

        Map<Material, Integer> amounts =
            new LinkedHashMap<Material, Integer>();

        boolean requiresNbt = false;
        boolean requiresIdentity = false;
        int specificity = 0;
        int totalItems = 0;

        for (CraftIngredient source
                : recipe.getShapelessIngredients()) {

            CompiledIngredient ingredient =
                CompiledIngredient.compile(source);

            ingredients.add(ingredient);

            accumulate(
                ingredient,
                materials,
                amounts
            );

            totalItems +=
                ingredient.getAmount();

            requiresNbt |=
                ingredient.requiresNbt();

            requiresIdentity |=
                ingredient.isIdentityRequired();

            specificity +=
                ingredient.getIdentitySpecificity();
        }

        /*
         * Le matcher shapeless V1 trie a chaque tentative.
         * On paie maintenant ce tri UNE seule fois au reload.
         */
        Collections.sort(
            ingredients,
            new Comparator<CompiledIngredient>() {
                @Override
                public int compare(
                        CompiledIngredient left,
                        CompiledIngredient right) {

                    return Integer.compare(
                        right.getIdentitySpecificity(),
                        left.getIdentitySpecificity()
                    );
                }
            }
        );

        return build(
            recipe,
            0,
            0,
            ingredients.size(),
            totalItems,
            Collections.<CompiledPatternCell>emptyList(),
            ingredients,
            materials,
            amounts,
            requiresNbt,
            requiresIdentity,
            specificity
        );
    }

    private CompiledRecipe build(
            CraftRecipe recipe,
            int patternWidth,
            int patternHeight,
            int occupiedSlots,
            int totalItems,
            List<CompiledPatternCell> shapedCells,
            List<CompiledIngredient> shapeless,
            Set<Material> materials,
            Map<Material, Integer> amounts,
            boolean requiresNbt,
            boolean requiresIdentity,
            int specificity) {

        int resultCount =
            recipe.getResults() == null
                ? 0
                : recipe.getResults().size();

        double totalWeight = 0D;

        if (recipe.getResults() != null) {
            for (CraftResult result
                    : recipe.getResults()) {

                if (result != null) {
                    totalWeight +=
                        Math.max(
                            0D,
                            result.getChance()
                        );
                }
            }
        }

        String signature =
            primarySignature(
                recipe,
                patternWidth,
                patternHeight,
                occupiedSlots,
                amounts
            );

        return new CompiledRecipe(
            recipe.getId(),
            recipe,
            recipe.getType(),
            recipe.getRequiredTable(),
            recipe.isAllowVanillaWorkbench(),
            recipe.isVanillaLooseMatch(),
            recipe.isAllowMirror(),
            patternWidth,
            patternHeight,
            occupiedSlots,
            totalItems,
            shapedCells,
            shapeless,
            materials,
            amounts,
            requiresNbt,
            requiresIdentity,
            specificity,
            resultCount,
            totalWeight,
            signature
        );
    }

    private static void accumulate(
            CompiledIngredient ingredient,
            Set<Material> materials,
            Map<Material, Integer> amounts) {

        Material material =
            ingredient.getMaterial();

        materials.add(material);

        Integer current =
            amounts.get(material);

        amounts.put(
            material,
            Integer.valueOf(
                (current == null ? 0 : current.intValue())
                    + ingredient.getAmount()
            )
        );
    }

    private static String primarySignature(
            CraftRecipe recipe,
            int width,
            int height,
            int occupied,
            Map<Material, Integer> amounts) {

        List<String> materialParts =
            new ArrayList<String>();

        for (Map.Entry<Material, Integer> entry
                : amounts.entrySet()) {

            materialParts.add(
                entry.getKey().name()
                    + "x"
                    + entry.getValue()
            );
        }

        Collections.sort(materialParts);

        StringBuilder out =
            new StringBuilder(96);

        out.append(
            recipe.getRequiredTable()
        );
        out.append('|');
        out.append(
            recipe.getType().name()
        );
        out.append('|');
        out.append(width);
        out.append('x');
        out.append(height);
        out.append('|');
        out.append(occupied);
        out.append('|');

        for (int i = 0;
                i < materialParts.size();
                i++) {

            if (i > 0) {
                out.append(',');
            }

            out.append(
                materialParts.get(i)
            );
        }

        return out.toString();
    }
}
