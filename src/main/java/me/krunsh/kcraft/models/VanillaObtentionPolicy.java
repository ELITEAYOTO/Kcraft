package me.krunsh.kcraft.models;

import java.util.Set;

import org.bukkit.Material;

/** Decision commune aux crafts vanilla et au loot de structures. */
public final class VanillaObtentionPolicy {
    private VanillaObtentionPolicy() {}

    public static boolean isBlocked(Material material, Set<Material> blacklist) {
        return material != null && material != Material.AIR
                && blacklist != null && blacklist.contains(material);
    }
}
