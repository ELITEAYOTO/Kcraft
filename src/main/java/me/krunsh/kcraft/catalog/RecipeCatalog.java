package me.krunsh.kcraft.catalog;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import me.krunsh.kcraft.models.CraftRecipe;

/**
 * Snapshot immutable des recettes actives.
 *
 * V2.1 pose cette frontière pour que V2.2 puisse compiler les recettes sans exposer
 * la Map mutable du CraftManager et sans recopier tout le catalogue dans les hot-paths.
 */
public final class RecipeCatalog {

    private final Map<String, CraftRecipe> byId;
    private final Collection<CraftRecipe> recipes;

    private RecipeCatalog(Map<String, CraftRecipe> source) {
        LinkedHashMap<String, CraftRecipe> copy = new LinkedHashMap<String, CraftRecipe>(source);
        this.byId = Collections.unmodifiableMap(copy);
        this.recipes = Collections.unmodifiableCollection(new ArrayList<CraftRecipe>(copy.values()));
    }

    public static RecipeCatalog snapshot(Map<String, CraftRecipe> source) {
        if (source == null) source = Collections.emptyMap();
        return new RecipeCatalog(source);
    }

    public CraftRecipe get(String id) { return byId.get(id); }
    public Collection<CraftRecipe> recipes() { return recipes; }
    public Map<String, CraftRecipe> asMap() { return byId; }
    public int size() { return byId.size(); }
    public boolean isEmpty() { return byId.isEmpty(); }
}
