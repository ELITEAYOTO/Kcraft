package me.krunsh.kcraft.config;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

/** Validation stricte de la configuration principale KCraft V2. */
public final class V2ConfigValidator {

    private static final Set<String> SUPPORTED_TABLE_SIZES =
        new HashSet<String>(Arrays.asList("3x3", "4x4", "5x5"));

    private V2ConfigValidator() {}

    public static ConfigValidationReport validate(FileConfiguration config) {
        ConfigValidationReport report = new ConfigValidationReport();
        if (config == null) {
            report.error("config.yml introuvable ou non charge.");
            return report;
        }

        int schema = config.getInt("schema_version", 0);
        if (schema != RuntimeConfig.SUPPORTED_SCHEMA_VERSION) {
            report.error("schema_version=" + schema + " invalide; attendu="
                + RuntimeConfig.SUPPORTED_SCHEMA_VERSION + ".");
        }

        // V2: plus aucun namespace GUI legacy à la racine.
        if (config.contains("gui")) {
            report.error("Le namespace legacy 'gui:' est interdit en V2. Utiliser uniquement Kcraft.gui.");
        }

        validateMaterialList(config, "Kcraft.restrictions.blocked-vanilla-craft-results", report);
        validateMaterialList(config, "Kcraft.restrictions.blocked-generated-loot", report);

        int debugLevel = config.getInt("Kcraft.settings.debug-level", 1);
        if (debugLevel < 0 || debugLevel > 3) {
            report.error("Kcraft.settings.debug-level doit être compris entre 0 et 3.");
        }

        int maxBatch = config.getInt("Kcraft.optimizations.batch-craft.max-batch", 64);
        if (maxBatch < 1 || maxBatch > 256) {
            report.error("Kcraft.optimizations.batch-craft.max-batch doit être compris entre 1 et 256.");
        }

        validateTables(config.getConfigurationSection("Kcraft.custom-blocks"), true, report);
        validateTables(config.getConfigurationSection("Kcraft.command-tables"), false, report);

        // Options V1 retirées car elles ne constituaient pas un vrai cache de matching.
        String[] removed = {
            "Kcraft.performance.cache-popular-crafts",
            "Kcraft.performance.instant-compact",
            "Kcraft.performance.smart-cache-size",
            "Kcraft.optimizations.smart-cache",
            "Kcraft.optimizations.instant-compact-items",
            "Kcraft.optimizations.async-operations",
            "Kcraft.optimizations.batch-craft.delay-between"
        };
        for (String path : removed) {
            if (config.contains(path)) {
                report.error("Option V1 retirée en V2: " + path
                    + ". Elle doit être supprimée de config.yml.");
            }
        }

        return report;
    }

    private static void validateTables(ConfigurationSection section, boolean physical,
                                       ConfigValidationReport report) {
        if (section == null) return;
        for (String id : section.getKeys(false)) {
            ConfigurationSection table = section.getConfigurationSection(id);
            if (table == null) {
                report.error("Table '" + id + "' invalide: section YAML attendue.");
                continue;
            }
            String size = table.getString("size", "3x3").toLowerCase();
            if (!SUPPORTED_TABLE_SIZES.contains(size)) {
                report.error("Table '" + id + "': taille '" + size
                    + "' non supportée. V2 supporte uniquement 3x3, 4x4 et 5x5.");
            }
            if (physical) {
                String block = table.getString("block");
                if (block == null || block.trim().isEmpty()) {
                    report.error("Table physique '" + id + "': champ block obligatoire.");
                }
            }
        }
    }

    private static void validateMaterialList(FileConfiguration config, String path,
                                             ConfigValidationReport report) {
        List<String> values = config.getStringList(path);
        for (String raw : values) {
            if (raw == null) continue;
            try {
                Material.valueOf(raw.trim().toUpperCase());
            } catch (IllegalArgumentException invalid) {
                report.error("Matériau invalide dans " + path + ": '" + raw + "'.");
            }
        }
    }
}
