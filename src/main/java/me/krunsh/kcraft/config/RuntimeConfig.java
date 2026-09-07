package me.krunsh.kcraft.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Snapshot immutable de la configuration runtime KCraft V2.
 *
 * Aucun hot-path ne doit reparcourir le YAML pour lire ces valeurs.
 * Le snapshot est reconstruit uniquement au démarrage / reload.
 */
public final class RuntimeConfig {

    public static final int SUPPORTED_SCHEMA_VERSION = 2;

    private final int schemaVersion;
    private final String prefix;
    private final int debugLevel;
    private final Set<String> enabledWorlds;
    private final boolean opsBypassPermissions;

    private final boolean asyncLogging;

    private final Set<Material> blockedVanillaCraftResults;
    private final Set<Material> blockedGeneratedLoot;
    private final boolean cleanGeneratedLoot;

    private final boolean particlesEnabled;
    private final boolean soundsEnabled;
    private final boolean allowPerCraftParticles;
    private final boolean allowPerCraftSounds;

    private final boolean guiBorders;
    private final String guiBorderColor;
    private final String guiTitlePrefix;
    private final String guiBorderItem;
    private final String guiCraftAreaColor;
    private final String guiResultAreaColor;
    private final String guiButtonAreaColor;

    private final boolean loggingEnabled;
    private final String logFile;
    private final boolean perPlayerLogging;
    private final boolean factionStatsEnabled;
    private final String logRotation;
    private final int keepDays;

    private final boolean strictFactionLevel;
    private final boolean batchCraftEnabled;
    private final int maxBatchCrafts;

    private RuntimeConfig(
            int schemaVersion,
            String prefix,
            int debugLevel,
            Set<String> enabledWorlds,
            boolean opsBypassPermissions,
            boolean asyncLogging,
            Set<Material> blockedVanillaCraftResults,
            Set<Material> blockedGeneratedLoot,
            boolean cleanGeneratedLoot,
            boolean particlesEnabled,
            boolean soundsEnabled,
            boolean allowPerCraftParticles,
            boolean allowPerCraftSounds,
            boolean guiBorders,
            String guiBorderColor,
            String guiTitlePrefix,
            String guiBorderItem,
            String guiCraftAreaColor,
            String guiResultAreaColor,
            String guiButtonAreaColor,
            boolean loggingEnabled,
            String logFile,
            boolean perPlayerLogging,
            boolean factionStatsEnabled,
            String logRotation,
            int keepDays,
            boolean strictFactionLevel,
            boolean batchCraftEnabled,
            int maxBatchCrafts) {
        this.schemaVersion = schemaVersion;
        this.prefix = prefix;
        this.debugLevel = debugLevel;
        this.enabledWorlds = immutableStringSet(enabledWorlds);
        this.opsBypassPermissions = opsBypassPermissions;
        this.asyncLogging = asyncLogging;
        this.blockedVanillaCraftResults = immutableMaterialSet(blockedVanillaCraftResults);
        this.blockedGeneratedLoot = immutableMaterialSet(blockedGeneratedLoot);
        this.cleanGeneratedLoot = cleanGeneratedLoot;
        this.particlesEnabled = particlesEnabled;
        this.soundsEnabled = soundsEnabled;
        this.allowPerCraftParticles = allowPerCraftParticles;
        this.allowPerCraftSounds = allowPerCraftSounds;
        this.guiBorders = guiBorders;
        this.guiBorderColor = guiBorderColor;
        this.guiTitlePrefix = guiTitlePrefix;
        this.guiBorderItem = guiBorderItem;
        this.guiCraftAreaColor = guiCraftAreaColor;
        this.guiResultAreaColor = guiResultAreaColor;
        this.guiButtonAreaColor = guiButtonAreaColor;
        this.loggingEnabled = loggingEnabled;
        this.logFile = logFile;
        this.perPlayerLogging = perPlayerLogging;
        this.factionStatsEnabled = factionStatsEnabled;
        this.logRotation = logRotation;
        this.keepDays = keepDays;
        this.strictFactionLevel = strictFactionLevel;
        this.batchCraftEnabled = batchCraftEnabled;
        this.maxBatchCrafts = maxBatchCrafts;
    }

    public static RuntimeConfig from(FileConfiguration config) {
        if (config == null) throw new IllegalArgumentException("config");

        return new RuntimeConfig(
            config.getInt("schema_version", 0),
            config.getString("Kcraft.settings.prefix", "&8[&6Kcraft&8]"),
            clamp(config.getInt("Kcraft.settings.debug-level", 1), 0, 3),
            new LinkedHashSet<String>(config.getStringList("Kcraft.settings.enabled-worlds")),
            config.getBoolean("Kcraft.settings.ops-bypass-permissions", true),
            config.getBoolean("Kcraft.performance.async-logging", true),
            materialSet(config.getStringList("Kcraft.restrictions.blocked-vanilla-craft-results")),
            materialSet(config.getStringList("Kcraft.restrictions.blocked-generated-loot")),
            config.getBoolean("Kcraft.restrictions.clean-generated-container-loot", true),
            config.getBoolean("Kcraft.effects.particles.enabled", false),
            config.getBoolean("Kcraft.effects.sounds.enabled", true),
            config.getBoolean("Kcraft.effects.particles.allow-per-craft", true),
            config.getBoolean("Kcraft.effects.sounds.allow-per-craft", true),
            config.getBoolean("Kcraft.gui.use-borders", true),
            config.getString("Kcraft.gui.border-color", "GRAY"),
            config.getString("Kcraft.gui.title-prefix", "&8"),
            config.getString("Kcraft.gui.border-item", "STAINED_GLASS_PANE"),
            config.getString("Kcraft.gui.glass-colors.craft-area", "GREEN"),
            config.getString("Kcraft.gui.glass-colors.result-area", "YELLOW"),
            config.getString("Kcraft.gui.glass-colors.button-area", "BLUE"),
            config.getBoolean("Kcraft.logging.enabled", true),
            config.getString("Kcraft.logging.file", "crafts-history.json"),
            config.getBoolean("Kcraft.logging.log-per-player", true),
            config.getBoolean("Kcraft.logging.log-faction-stats", true),
            config.getString("Kcraft.logging.rotation", "DAILY"),
            Math.max(1, config.getInt("Kcraft.logging.keep-days", 30)),
            config.getBoolean("Kcraft.hooks.strict-faction-level", false),
            config.getBoolean("Kcraft.optimizations.batch-craft.enabled", true),
            clamp(config.getInt("Kcraft.optimizations.batch-craft.max-batch", 64), 1, 256)
        );
    }

    private static Set<Material> materialSet(List<String> names) {
        Set<Material> result = new LinkedHashSet<Material>();
        if (names == null) return result;
        for (String name : names) {
            if (name == null) continue;
            try {
                result.add(Material.valueOf(name.trim().toUpperCase()));
            } catch (IllegalArgumentException ignored) {
                // Le validateur V2 signale l'erreur. Le snapshot ne garde pas la valeur invalide.
            }
        }
        return result;
    }

    private static Set<String> immutableStringSet(Set<String> values) {
        return Collections.unmodifiableSet(new LinkedHashSet<String>(values));
    }

    private static Set<Material> immutableMaterialSet(Set<Material> values) {
        return Collections.unmodifiableSet(new LinkedHashSet<Material>(values));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public int getSchemaVersion() { return schemaVersion; }
    public String getPrefix() { return prefix; }
    public int getDebugLevel() { return debugLevel; }
    public List<String> getEnabledWorlds() { return new ArrayList<String>(enabledWorlds); }
    public boolean isOpsBypassPermissions() { return opsBypassPermissions; }
    public boolean isAsyncLogging() { return asyncLogging; }
    public Set<Material> getBlockedVanillaCraftResults() { return blockedVanillaCraftResults; }
    public Set<Material> getBlockedGeneratedLoot() { return blockedGeneratedLoot; }
    public boolean isCleanGeneratedLoot() { return cleanGeneratedLoot; }
    public boolean isParticlesEnabled() { return particlesEnabled; }
    public boolean isSoundsEnabled() { return soundsEnabled; }
    public boolean isAllowPerCraftEffects() { return allowPerCraftParticles || allowPerCraftSounds; }
    public boolean isGuiBorders() { return guiBorders; }
    public String getGuiBorderColor() { return guiBorderColor; }
    public String getGuiTitlePrefix() { return guiTitlePrefix; }
    public String getGuiBorderItem() { return guiBorderItem; }
    public String getGuiCraftAreaColor() { return guiCraftAreaColor; }
    public String getGuiResultAreaColor() { return guiResultAreaColor; }
    public String getGuiButtonAreaColor() { return guiButtonAreaColor; }
    public boolean isLoggingEnabled() { return loggingEnabled; }
    public String getLogFile() { return logFile; }
    public boolean isPerPlayerLogging() { return perPlayerLogging; }
    public boolean isFactionStatsEnabled() { return factionStatsEnabled; }
    public String getLogRotation() { return logRotation; }
    public int getKeepDays() { return keepDays; }
    public boolean isStrictFactionLevel() { return strictFactionLevel; }
    public boolean isBatchCraftEnabled() { return batchCraftEnabled; }
    public int getMaxBatchCrafts() { return maxBatchCrafts; }
}
