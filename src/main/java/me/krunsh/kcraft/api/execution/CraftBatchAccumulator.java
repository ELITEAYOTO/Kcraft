package me.krunsh.kcraft.api.execution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * Accumulate les résultats réels d'un batch.
 *
 * Un clone n'est gardé que pour chaque résultat distinct (isSimilar),
 * pas pour chaque craft.
 */
public final class CraftBatchAccumulator {

    private final List<MutableResult> groups =
        new ArrayList<MutableResult>();

    private int successfulCrafts;
    private int totalResultItems;

    public void addSuccess(
            ItemStack result) {

        successfulCrafts++;

        if (result == null
                || result.getType() == Material.AIR
                || result.getAmount() <= 0) {

            return;
        }

        int amount =
            result.getAmount();

        totalResultItems += amount;

        for (MutableResult group : groups) {
            if (group.sample.isSimilar(result)) {
                group.craftOccurrences++;
                group.itemCount += amount;
                return;
            }
        }

        groups.add(
            new MutableResult(
                result.clone(),
                1,
                amount
            )
        );
    }

    public int getSuccessfulCrafts() {
        return successfulCrafts;
    }

    public int getTotalResultItems() {
        return totalResultItems;
    }

    public List<CraftBatchResult> snapshot() {
        if (groups.isEmpty()) {
            return Collections.emptyList();
        }

        List<CraftBatchResult> snapshot =
            new ArrayList<CraftBatchResult>(
                groups.size()
            );

        for (MutableResult group : groups) {
            snapshot.add(
                new CraftBatchResult(
                    group.sample,
                    group.craftOccurrences,
                    group.itemCount
                )
            );
        }

        return Collections.unmodifiableList(
            snapshot
        );
    }

    private static final class MutableResult {
        private final ItemStack sample;
        private int craftOccurrences;
        private int itemCount;

        private MutableResult(
                ItemStack sample,
                int craftOccurrences,
                int itemCount) {

            this.sample = sample;
            this.craftOccurrences = craftOccurrences;
            this.itemCount = itemCount;
        }
    }
}
