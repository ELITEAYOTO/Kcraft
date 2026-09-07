package me.krunsh.kcraft.matching;

import java.util.LinkedHashMap;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * Snapshot d'une matrice cree une seule fois par resolution.
 */
public final class MatrixSnapshot {

    private final ItemSnapshot[] slots;
    private final ItemSnapshot[] occupied;
    private final int gridSize;
    private final int occupiedCount;
    private final Map<Material, Integer> materialOccurrences;

    private MatrixSnapshot(
            ItemSnapshot[] slots,
            ItemSnapshot[] occupied,
            int gridSize,
            Map<Material, Integer> materialOccurrences) {

        this.slots = slots;
        this.occupied = occupied;
        this.gridSize = gridSize;
        this.occupiedCount =
            occupied.length;

        this.materialOccurrences =
            materialOccurrences;
    }

    public static MatrixSnapshot capture(
            ItemStack[] matrix) {

        if (matrix == null) {
            return null;
        }

        int size =
            (int) Math.sqrt(
                matrix.length
            );

        if (size * size != matrix.length) {
            return null;
        }

        ItemSnapshot[] slots =
            new ItemSnapshot[matrix.length];

        int occupiedCount = 0;

        Map<Material, Integer> occurrences =
            new LinkedHashMap<Material, Integer>();

        for (int i = 0;
                i < matrix.length;
                i++) {

            ItemSnapshot snapshot =
                ItemSnapshot.of(matrix[i]);

            slots[i] =
                snapshot;

            if (!snapshot.isPresent()) {
                continue;
            }

            occupiedCount++;

            Material material =
                snapshot.getMaterial();

            Integer current =
                occurrences.get(material);

            occurrences.put(
                material,
                Integer.valueOf(
                    current == null
                        ? 1
                        : current.intValue() + 1
                )
            );
        }

        ItemSnapshot[] occupied =
            new ItemSnapshot[occupiedCount];

        int cursor = 0;

        for (ItemSnapshot slot : slots) {
            if (slot.isPresent()) {
                occupied[cursor++] = slot;
            }
        }

        return new MatrixSnapshot(
            slots,
            occupied,
            size,
            occurrences
        );
    }

    public ItemSnapshot getSlot(
            int index) {

        return slots[index];
    }

    public ItemSnapshot[] getOccupied() {
        return occupied;
    }

    public int getGridSize() {
        return gridSize;
    }

    public int getOccupiedCount() {
        return occupiedCount;
    }

    public Map<Material, Integer> getMaterialOccurrences() {
        return materialOccurrences;
    }

    public int countNbtInitializedSlots() {
        int count = 0;

        for (ItemSnapshot slot : occupied) {
            if (slot.hasInitializedNbt()) {
                count++;
            }
        }

        return count;
    }
}
