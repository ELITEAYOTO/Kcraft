package me.krunsh.kcraft.compiled;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import me.krunsh.kcraft.catalog.RecipeCatalog;
import me.krunsh.kcraft.models.CraftRecipe;

/**
 * Snapshot immutable des recettes compilees.
 */
public final class CompiledRecipeCatalog {

    private final Map<String, CompiledRecipe> byId;
    private final Collection<CompiledRecipe> recipes;
    private final Map<String, List<CompiledRecipe>> byTable;

    private CompiledRecipeCatalog(
            Map<String, CompiledRecipe> byId,
            Map<String, List<CompiledRecipe>> byTable) {

        this.byId =
            Collections.unmodifiableMap(
                new LinkedHashMap<String, CompiledRecipe>(
                    byId
                )
            );

        this.recipes =
            Collections.unmodifiableCollection(
                new ArrayList<CompiledRecipe>(
                    byId.values()
                )
            );

        Map<String, List<CompiledRecipe>> frozen =
            new LinkedHashMap<String, List<CompiledRecipe>>();

        for (Map.Entry<String, List<CompiledRecipe>> entry
                : byTable.entrySet()) {

            frozen.put(
                entry.getKey(),
                Collections.unmodifiableList(
                    new ArrayList<CompiledRecipe>(
                        entry.getValue()
                    )
                )
            );
        }

        this.byTable =
            Collections.unmodifiableMap(frozen);
    }

    public static CompiledRecipeCatalog compile(
            RecipeCatalog source) {

        if (source == null) {
            throw new IllegalArgumentException(
                "RecipeCatalog absent."
            );
        }

        RecipeCompiler compiler =
            new RecipeCompiler();

        Map<String, CompiledRecipe> byId =
            new LinkedHashMap<String, CompiledRecipe>();

        Map<String, List<CompiledRecipe>> byTable =
            new LinkedHashMap<String, List<CompiledRecipe>>();

        for (CraftRecipe recipe
                : source.recipes()) {

            CompiledRecipe compiled =
                compiler.compile(recipe);

            byId.put(
                compiled.getId(),
                compiled
            );

            String table =
                compiled.getRequiredTable();

            List<CompiledRecipe> tableRecipes =
                byTable.get(table);

            if (tableRecipes == null) {
                tableRecipes =
                    new ArrayList<CompiledRecipe>();

                byTable.put(
                    table,
                    tableRecipes
                );
            }

            tableRecipes.add(compiled);
        }

        return new CompiledRecipeCatalog(
            byId,
            byTable
        );
    }

    public CompiledRecipe get(String id) {
        return byId.get(id);
    }

    public Collection<CompiledRecipe> recipes() {
        return recipes;
    }

    public List<CompiledRecipe> recipesForTable(
            String tableId) {

        List<CompiledRecipe> found =
            byTable.get(tableId);

        return found == null
            ? Collections.<CompiledRecipe>emptyList()
            : found;
    }

    public int size() {
        return byId.size();
    }

    public int tableCount() {
        return byTable.size();
    }
}
