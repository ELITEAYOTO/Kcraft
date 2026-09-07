package me.krunsh.kcraft.api.execution;

import org.bukkit.inventory.ItemStack;

/**
 * Groupe de résultats similaires dans un batch.
 *
 * Exemple :
 * 64 crafts donnant chacun 4 flèches :
 * craftOccurrences=64
 * itemCount=256
 */
public final class CraftBatchResult {

    private final ItemStack sample;
    private final int craftOccurrences;
    private final int itemCount;

    public CraftBatchResult(
            ItemStack sample,
            int craftOccurrences,
            int itemCount) {

        this.sample =
            sample == null
                ? null
                : sample.clone();

        this.craftOccurrences =
            Math.max(0, craftOccurrences);

        this.itemCount =
            Math.max(0, itemCount);
    }

    public ItemStack getSample() {
        return sample == null
            ? null
            : sample.clone();
    }

    public int getCraftOccurrences() {
        return craftOccurrences;
    }

    public int getItemCount() {
        return itemCount;
    }
}
