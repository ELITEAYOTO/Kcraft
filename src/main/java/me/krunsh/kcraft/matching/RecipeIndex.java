package me.krunsh.kcraft.matching;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import me.krunsh.kcraft.compiled.CompiledRecipe;
import me.krunsh.kcraft.compiled.CompiledRecipeCatalog;

/**
 * Index immutable fingerprint -> candidats.
 */
public final class RecipeIndex {

    private final Map<String, List<CompiledRecipe>> buckets;
    private final int recipeCount;
    private final int largestBucket;

    private RecipeIndex(
            Map<String, List<CompiledRecipe>> buckets,
            int recipeCount,
            int largestBucket) {

        this.buckets = buckets;
        this.recipeCount = recipeCount;
        this.largestBucket =
            largestBucket;
    }

    public static RecipeIndex build(
            CompiledRecipeCatalog catalog) {

        Map<String, List<CompiledRecipe>> mutable =
            new LinkedHashMap<String, List<CompiledRecipe>>();

        int recipes = 0;
        int largest = 0;

        for (CompiledRecipe recipe
                : catalog.recipes()) {

            String key =
                MatrixFingerprint.forRecipe(
                    recipe
                );

            List<CompiledRecipe> bucket =
                mutable.get(key);

            if (bucket == null) {
                bucket =
                    new ArrayList<CompiledRecipe>();

                mutable.put(
                    key,
                    bucket
                );
            }

            bucket.add(recipe);
            recipes++;

            if (bucket.size() > largest) {
                largest =
                    bucket.size();
            }
        }

        Map<String, List<CompiledRecipe>> frozen =
            new LinkedHashMap<String, List<CompiledRecipe>>();

        for (Map.Entry<String, List<CompiledRecipe>> entry
                : mutable.entrySet()) {

            frozen.put(
                entry.getKey(),
                Collections.unmodifiableList(
                    new ArrayList<CompiledRecipe>(
                        entry.getValue()
                    )
                )
            );
        }

        return new RecipeIndex(
            Collections.unmodifiableMap(
                frozen
            ),
            recipes,
            largest
        );
    }

    public List<CompiledRecipe> candidates(
            MatrixSnapshot matrix,
            String tableId) {

        String key =
            MatrixFingerprint.forMatrix(
                matrix,
                tableId
            );

        List<CompiledRecipe> bucket =
            buckets.get(key);

        return bucket == null
            ? Collections.<CompiledRecipe>emptyList()
            : bucket;
    }

    public int getRecipeCount() {
        return recipeCount;
    }

    public int getBucketCount() {
        return buckets.size();
    }

    public int getLargestBucket() {
        return largestBucket;
    }

    public double getAverageBucketSize() {

        if (buckets.isEmpty()) {
            return 0D;
        }

        return recipeCount
            / (double) buckets.size();
    }
}
