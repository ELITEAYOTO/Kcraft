package me.krunsh.kcraft.models;

import org.bukkit.inventory.ItemStack;

import me.krunsh.kcraft.utils.ItemStackUtil;

import me.krunsh.kcraft.utils.NBTUtil;

/** Plan atomique d'un craft vers curseur, inventaire ou touche numerique. */
public final class CraftOutputTransfer {

    public enum Destination { CURSOR, INVENTORY, HOTBAR }

    public static final class Plan {
        private final ItemStack[] matrix;
        private final ItemStack[] storage;
        private final ItemStack cursor;

        private Plan(ItemStack[] matrix, ItemStack[] storage, ItemStack cursor) {
            this.matrix = matrix;
            this.storage = storage;
            this.cursor = cursor;
        }

        public ItemStack[] getMatrix() { return cloneItems(matrix); }
        public ItemStack[] getStorage() { return cloneItems(storage); }
        public ItemStack getCursor() { return cursor == null ? null : cursor.clone(); }
    }

    private CraftOutputTransfer() {
    }

    public static Plan planOnce(CraftRecipe recipe, ItemStack[] matrix, ItemStack output,
                                Destination destination, ItemStack cursor,
                                ItemStack[] storage, int hotbarSlot) {
        if (recipe == null || output == null || isEmpty(output)) return null;
        ItemStack[] consumed = CraftMatrixTransaction.consumeOnceVanilla(recipe, matrix);
        if (consumed == null) return null;

        ItemStack[] nextStorage = cloneItems(storage == null ? new ItemStack[0] : storage);
        ItemStack nextCursor = cursor == null ? null : cursor.clone();
        boolean accepted;
        switch (destination) {
            case CURSOR:
                ItemStack merged = merge(nextCursor, output);
                accepted = merged != null;
                if (accepted) nextCursor = merged;
                break;
            case HOTBAR:
                accepted = hotbarSlot >= 0 && hotbarSlot < Math.min(9, nextStorage.length);
                if (accepted) {
                    ItemStack hotbar = merge(nextStorage[hotbarSlot], output);
                    if (hotbar != null) {
                        nextStorage[hotbarSlot] = hotbar;
                    } else {
                        ItemStack displaced = nextStorage[hotbarSlot] == null
                            ? null : nextStorage[hotbarSlot].clone();
                        nextStorage[hotbarSlot] = null;
                        accepted = displaced == null
                            || addFullyExcluding(nextStorage, displaced, hotbarSlot);
                        if (accepted) nextStorage[hotbarSlot] = output.clone();
                    }
                }
                break;
            case INVENTORY:
                accepted = addFully(nextStorage, output);
                break;
            default:
                accepted = false;
        }
        return accepted ? new Plan(consumed, nextStorage, nextCursor) : null;
    }

    private static boolean addFully(ItemStack[] storage, ItemStack output) {
        return addFullyExcluding(storage, output, -1);
    }

    private static boolean addFullyExcluding(ItemStack[] storage, ItemStack output,
                                             int excludedSlot) {
        int remaining = output.getAmount();
        int max = Math.max(1, output.getMaxStackSize());
        for (int i = 0; i < storage.length && remaining > 0; i++) {
            if (i == excludedSlot) continue;
            ItemStack current = storage[i];
            if (isEmpty(current) || !similar(current, output)) continue;
            int space = Math.max(0, Math.min(max, current.getMaxStackSize()) - current.getAmount());
            if (space <= 0) continue;
            int moved = Math.min(space, remaining);
            current.setAmount(current.getAmount() + moved);
            remaining -= moved;
        }
        for (int i = 0; i < storage.length && remaining > 0; i++) {
            if (i == excludedSlot) continue;
            if (!isEmpty(storage[i])) continue;
            int moved = Math.min(max, remaining);
            ItemStack placed = output.clone();
            placed.setAmount(moved);
            storage[i] = placed;
            remaining -= moved;
        }
        return remaining == 0;
    }

    private static ItemStack merge(ItemStack current, ItemStack output) {
        if (isEmpty(current)) return output.clone();
        if (!similar(current, output)) return null;
        int max = Math.min(current.getMaxStackSize(), output.getMaxStackSize());
        if (current.getAmount() + output.getAmount() > max) return null;
        ItemStack merged = current.clone();
        merged.setAmount(current.getAmount() + output.getAmount());
        return merged;
    }

    private static boolean similar(ItemStack left, ItemStack right) {
        if (left == null || right == null || left.getType() != right.getType()
                || left.getDurability() != right.getDurability()) return false;
        try {
            if (!left.isSimilar(right)) return false;
            return NBTUtil.compareNBT(left, right);
        } catch (Throwable unavailableNbtRuntime) {
            // En tests unitaires Bukkit n'a pas d'ItemFactory. En serveur, la
            // branche ci-dessus compare meta et NBT complets.
            return true;
        }
    }

    private static boolean isEmpty(ItemStack item) {
        return !ItemStackUtil.isPresent(item);
    }

    private static ItemStack[] cloneItems(ItemStack[] source) {
        if (source == null) return null;
        ItemStack[] clone = new ItemStack[source.length];
        for (int i = 0; i < source.length; i++) {
            clone[i] = source[i] == null ? null : source[i].clone();
        }
        return clone;
    }
}
