package me.krunsh.kcraft.managers;

import java.util.Collections;
import java.util.List;

import org.bukkit.inventory.ItemStack;

import me.krunsh.kcraft.Kcraft;
import me.krunsh.kcraft.compiled.CompiledRecipe;
import me.krunsh.kcraft.compiled.CompiledRecipeCatalog;
import me.krunsh.kcraft.matching.MatchingMetrics;
import me.krunsh.kcraft.matching.MatrixSnapshot;
import me.krunsh.kcraft.matching.RecipeIndex;
import me.krunsh.kcraft.matching.RecipeMatcher;
import me.krunsh.kcraft.models.CraftRecipe;
import me.krunsh.kcraft.models.CraftTable;

/**
 * CraftManager V2.3.
 *
 * Le parsing/execution historique reste dans CraftManager.
 * Le hot path de matching est remplace ici sans recopier tout le manager.
 */
public final class IndexedCraftManager
        extends CraftManager {

    private final Kcraft plugin;
    private final RecipeMatcher matcher =
        new RecipeMatcher();

    private final MatchingMetrics metrics =
        new MatchingMetrics();

    private volatile RecipeIndex index;

    public IndexedCraftManager(
            Kcraft plugin) {

        super(plugin);
        this.plugin = plugin;
    }

    public void rebuildIndex(
            CompiledRecipeCatalog catalog) {

        if (catalog == null) {
            index = null;
            return;
        }

        long started =
            System.nanoTime();

        RecipeIndex next =
            RecipeIndex.build(catalog);

        index = next;

        long micros =
            (System.nanoTime() - started)
                / 1000L;

        plugin.getLogger().info(
            "RecipeIndex V2.3 prêt - recipes="
                + next.getRecipeCount()
                + ", buckets="
                + next.getBucketCount()
                + ", avgBucket="
                + String.format(
                    java.util.Locale.US,
                    "%.2f",
                    next.getAverageBucketSize()
                )
                + ", maxBucket="
                + next.getLargestBucket()
                + ", build="
                + micros
                + "us"
        );
    }

    @Override
    public CraftRecipe findMatchingRecipe(
            ItemStack[] matrix,
            CraftTable table) {

        if (matrix == null
                || table == null) {

            return null;
        }

        RecipeIndex currentIndex =
            index;

        CompiledRecipeCatalog catalog =
            plugin.getCompiledRecipeCatalog();

        if (currentIndex == null
                || catalog == null) {

            /*
             * Uniquement securite bootstrap/reload.
             * En runtime normal V2.3 l'index doit toujours etre actif.
             */
            return super.findMatchingRecipe(
                matrix,
                table
            );
        }

        long started =
            System.nanoTime();

        MatrixSnapshot snapshot =
            MatrixSnapshot.capture(matrix);

        if (snapshot == null) {
            metrics.record(
                0,
                0,
                false,
                0,
                System.nanoTime() - started
            );

            return null;
        }

        List<CompiledRecipe> candidates =
            currentIndex.candidates(
                snapshot,
                table.getId()
            );

        int checks = 0;
        CraftRecipe found = null;

        for (CompiledRecipe candidate
                : candidates) {

            checks++;

            if (matcher.matches(
                    candidate,
                    snapshot)) {

                found =
                    candidate.getSource();

                break;
            }
        }

        metrics.record(
            candidates.size(),
            checks,
            found != null,
            snapshot.countNbtInitializedSlots(),
            System.nanoTime() - started
        );

        return found;
    }

    @Override
    public boolean matchesRecipe(
            CraftRecipe recipe,
            ItemStack[] matrix) {

        if (recipe == null
                || matrix == null) {

            return false;
        }

        CompiledRecipeCatalog catalog =
            plugin.getCompiledRecipeCatalog();

        if (catalog == null) {
            return super.matchesRecipe(
                recipe,
                matrix
            );
        }

        CompiledRecipe compiled =
            catalog.get(
                recipe.getId()
            );

        if (compiled == null) {
            /*
             * Les mutations API dynamiques seront refondues en V2.6.
             * Durant V2.3, une recette ajoutee apres compilation utilise
             * encore le moteur historique.
             */
            return super.matchesRecipe(
                recipe,
                matrix
            );
        }

        MatrixSnapshot snapshot =
            MatrixSnapshot.capture(matrix);

        return snapshot != null
            && matcher.matches(
                compiled,
                snapshot
            );
    }

    public MatchingMetrics getMatchingMetrics() {
        return metrics;
    }

    public RecipeIndex getRecipeIndex() {
        return index;
    }
}
