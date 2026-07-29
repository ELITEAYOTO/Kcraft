package me.krunsh.kcraft.utils;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/** Convention unique pour distinguer un item réel d'un emplacement vide. */
public final class ItemStackUtil {
    private ItemStackUtil() {
    }

    public static boolean isPresent(ItemStack item) {
        if (item == null || item.getAmount() <= 0) return false;
        Material type = item.getType();
        return type != null && type != Material.AIR;
    }
}
