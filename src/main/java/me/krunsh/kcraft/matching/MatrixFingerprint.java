package me.krunsh.kcraft.matching;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;

import me.krunsh.kcraft.compiled.CompiledIngredient;
import me.krunsh.kcraft.compiled.CompiledPatternCell;
import me.krunsh.kcraft.compiled.CompiledRecipe;

/**
 * Fingerprint primaire volontairement grossier.
 *
 * Il filtre sans jamais pouvoir exclure un vrai match :
 * table + nombre de slots occupes + multiset de Material.
 *
 * Data/NBT/nom/lore/geometrie sont reserves au RecipeMatcher exact.
 */
public final class MatrixFingerprint {

    private MatrixFingerprint() {}

    public static String forMatrix(
            MatrixSnapshot matrix,
            String tableId) {

        if (matrix == null
                || tableId == null) {

            return "";
        }

        return build(
            tableId,
            matrix.getOccupiedCount(),
            matrix.getMaterialOccurrences()
        );
    }

    public static String forRecipe(
            CompiledRecipe recipe) {

        java.util.LinkedHashMap<Material, Integer> occurrences =
            new java.util.LinkedHashMap<Material, Integer>();

        if (recipe.getType().name().equals("SHAPELESS")) {
            for (CompiledIngredient ingredient
                    : recipe.getShapelessIngredients()) {

                add(
                    occurrences,
                    ingredient.getMaterial()
                );
            }
        } else {
            for (CompiledPatternCell cell
                    : recipe.getShapedCells()) {

                add(
                    occurrences,
                    cell.getIngredient()
                        .getMaterial()
                );
            }
        }

        return build(
            recipe.getRequiredTable(),
            recipe.getOccupiedSlots(),
            occurrences
        );
    }

    private static void add(
            Map<Material, Integer> map,
            Material material) {

        Integer current =
            map.get(material);

        map.put(
            material,
            Integer.valueOf(
                current == null
                    ? 1
                    : current.intValue() + 1
            )
        );
    }

    private static String build(
            String tableId,
            int occupied,
            Map<Material, Integer> occurrences) {

        List<String> parts =
            new ArrayList<String>(
                occurrences.size()
            );

        for (Map.Entry<Material, Integer> entry
                : occurrences.entrySet()) {

            parts.add(
                entry.getKey().name()
                    + "x"
                    + entry.getValue()
            );
        }

        Collections.sort(parts);

        StringBuilder key =
            new StringBuilder(80);

        key.append(tableId);
        key.append('|');
        key.append(occupied);
        key.append('|');

        for (int i = 0;
                i < parts.size();
                i++) {

            if (i > 0) {
                key.append(',');
            }

            key.append(parts.get(i));
        }

        return key.toString();
    }
}
