package me.krunsh.kcraft.utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.bukkit.Material;

/**
 * Resolveur de materiaux avec compatibilite legacy 1.8.8.
 */
public final class MaterialResolver {

    private static final Map<String, String> LEGACY_ALIASES = createLegacyAliases();

    private MaterialResolver() {
        // Classe utilitaire
    }

    /**
     * Resolves a material name and supports cross-version aliases.
     */
    public static Material resolve(String rawName) {
        if (rawName == null) {
            return null;
        }

        String normalized = normalize(rawName);
        if (normalized.isEmpty()) {
            return null;
        }

        Material material = Material.matchMaterial(normalized);
        if (material != null) {
            return material;
        }

        String alias = LEGACY_ALIASES.get(normalized);
        if (alias != null) {
            material = Material.matchMaterial(alias);
            if (material != null) {
                return material;
            }
        }

        try {
            return Material.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            // No-op
        }

        if (alias != null) {
            try {
                return Material.valueOf(alias);
            } catch (IllegalArgumentException ignored) {
                // No-op
            }
        }

        return null;
    }

    private static String normalize(String rawName) {
        String normalized = rawName.trim().toUpperCase(Locale.ROOT);
        if (normalized.startsWith("MINECRAFT:")) {
            return normalized.substring("MINECRAFT:".length());
        }
        return normalized;
    }

    private static Map<String, String> createLegacyAliases() {
        Map<String, String> aliases = new HashMap<>();

        // Legacy 1.8.8 names
        aliases.put("PISTON", "PISTON_BASE");
        aliases.put("CRAFTING_TABLE", "WORKBENCH");

        return Collections.unmodifiableMap(aliases);
    }
}