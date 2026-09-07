package me.krunsh.kcraft.gui;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Registre de GUI dirty, sans dépendance Bukkit.
 *
 * mark(uuid) retourne true uniquement lors de la première marque.
 * drain() vide atomiquement le snapshot logique sur le main thread.
 */
public final class GuiDirtyRegistry {

    private final Set<UUID> dirty =
        new LinkedHashSet<UUID>();

    public boolean mark(
            UUID playerId) {

        if (playerId == null) {
            return false;
        }

        return dirty.add(playerId);
    }

    public boolean remove(
            UUID playerId) {

        return playerId != null
            && dirty.remove(playerId);
    }

    public List<UUID> drain() {

        if (dirty.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        List<UUID> snapshot =
            new ArrayList<UUID>(dirty);

        dirty.clear();

        return snapshot;
    }

    public int size() {
        return dirty.size();
    }

    public boolean isEmpty() {
        return dirty.isEmpty();
    }

    public void clear() {
        dirty.clear();
    }
}
