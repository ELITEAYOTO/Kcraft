package me.krunsh.kcraft.managers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Index pur qui refuse entierement un identifiant defini plusieurs fois. */
public final class RecipeSourceRegistry<T> {
    public enum Registration { ACCEPTED, DUPLICATE_REJECTED }

    private final Map<String, T> active = new LinkedHashMap<String, T>();
    private final Map<String, String> activeSources = new LinkedHashMap<String, String>();
    private final Map<String, LinkedHashSet<String>> duplicates =
        new LinkedHashMap<String, LinkedHashSet<String>>();

    public Registration register(String id, T value, String source) {
        if (id == null || id.trim().isEmpty()) throw new IllegalArgumentException("Identifiant de recette vide");
        String safeSource = source == null || source.trim().isEmpty() ? "<inconnu>" : source;
        LinkedHashSet<String> knownDuplicates = duplicates.get(id);
        if (knownDuplicates != null) {
            knownDuplicates.add(safeSource);
            return Registration.DUPLICATE_REJECTED;
        }
        if (active.containsKey(id)) {
            LinkedHashSet<String> sources = new LinkedHashSet<String>();
            sources.add(activeSources.remove(id));
            sources.add(safeSource);
            duplicates.put(id, sources);
            active.remove(id);
            return Registration.DUPLICATE_REJECTED;
        }
        active.put(id, value);
        activeSources.put(id, safeSource);
        return Registration.ACCEPTED;
    }

    public T get(String id) { return active.get(id); }
    public String getSource(String id) { return activeSources.get(id); }
    public boolean isDuplicate(String id) { return duplicates.containsKey(id); }

    public List<String> getDuplicateSources(String id) {
        Set<String> sources = duplicates.get(id);
        if (sources == null) return Collections.emptyList();
        return Collections.unmodifiableList(new ArrayList<String>(sources));
    }

    public void removeActive(String id) {
        active.remove(id);
        activeSources.remove(id);
    }

    public void clear() {
        active.clear();
        activeSources.clear();
        duplicates.clear();
    }
}
