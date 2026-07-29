package me.krunsh.kcraft.hooks;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import me.krunsh.kcraft.utils.MessageUtil;

/**
 * Gestionnaire des hooks avec autres plugins.
 * Kfaction est l'unique source faction supportee.
 */
public class PluginHookManager {

    // Hooks disponibles
    private boolean kfactionEnabled = false;
    private boolean vaultEnabled = false;
    private boolean placeholderAPIEnabled = false;
    private boolean kharvestEnabled = false;
    private boolean outilsEvolutifEnabled = false;
    private boolean pluginCITEnabled = false;

    // Instances des hooks
    private KfactionHook kfactionHook;
    public PluginHookManager() {
    }

    /**
     * Charge tous les hooks disponibles.
     */
    public void loadHooks() {
        MessageUtil.log("Detection des plugins...");

        // Reset complet avant re-detection (utile sur /kcraft reload)
        kfactionEnabled = false;
        vaultEnabled = false;
        placeholderAPIEnabled = false;
        kharvestEnabled = false;
        outilsEvolutifEnabled = false;
        pluginCITEnabled = false;

        kfactionHook = null;
        // Kfaction (source unique pour les checks faction)
        if (Bukkit.getPluginManager().isPluginEnabled("Kfaction")) {
            kfactionEnabled = true;
            kfactionHook = new KfactionHook();
            MessageUtil.log("✓ Kfaction detecte - Conditions niveau faction actives!");
        }

        // Vault
        if (Bukkit.getPluginManager().isPluginEnabled("Vault")) {
            vaultEnabled = true;
            MessageUtil.log("✓ Vault detecte - Economie activee!");
        }

        // PlaceholderAPI
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            placeholderAPIEnabled = true;
            MessageUtil.log("✓ PlaceholderAPI detecte - Placeholders actives!");
        }

        // Kharvester
        if (Bukkit.getPluginManager().isPluginEnabled("Kharvester")) {
            kharvestEnabled = true;
            registerKharvestCrafts();
            MessageUtil.log("✓ Kharvester detecte - Crafts collecteurs ajoutes!");
        }

        // OutilsEvolutif
        if (Bukkit.getPluginManager().isPluginEnabled("OutilsEvolutif")) {
            outilsEvolutifEnabled = true;
            registerEvolutionCrafts();
            MessageUtil.log("✓ OutilsEvolutif detecte - Crafts evolutifs ajoutes!");
        }

        // PluginCIT
        if (Bukkit.getPluginManager().isPluginEnabled("PluginCIT")) {
            pluginCITEnabled = true;
            MessageUtil.log("✓ PluginCIT detecte - Textures custom actives!");
        }

        int totalHooks = 0;
        if (kfactionEnabled) totalHooks++;
        if (vaultEnabled) totalHooks++;
        if (placeholderAPIEnabled) totalHooks++;
        if (kharvestEnabled) totalHooks++;
        if (outilsEvolutifEnabled) totalHooks++;
        if (pluginCITEnabled) totalHooks++;

        MessageUtil.log("Hooks charges: " + totalHooks + "/6 plugins");
    }

    /**
     * Enregistre les crafts speciaux pour Kharvester.
     */
    private void registerKharvestCrafts() {
        // TODO: Ajouter les crafts de collecteurs automatiquement
        MessageUtil.debug("Crafts Kharvester a implementer", 1);
    }

    /**
     * Enregistre les crafts evolutifs pour OutilsEvolutif.
     */
    private void registerEvolutionCrafts() {
        // TODO: Ajouter les crafts d'outils evolutifs automatiquement
        MessageUtil.debug("Crafts OutilsEvolutif a implementer", 1);
    }

    // === METHODES D'ACCES AUX HOOKS ===

    /**
     * Verifie si un joueur peut craft selon son niveau de faction.
     * Comportement :
     *   - requiredLevel <= 0 : toujours OK
     *   - Kfaction present : verifie reellement le niveau
     *   - Kfaction absent + strict=false (defaut) : autorise (mode tolerant, evite de bloquer
     *     toutes les recettes en l'absence du plugin faction)
     *   - Kfaction absent + strict=true : refuse (mode strict, opt-in via
     *     `Kcraft.hooks.strict-faction-level: true` dans config.yml)
     */
    public boolean checkFactionLevel(Player player, int requiredLevel) {
        if (requiredLevel <= 0) {
            return true;
        }

        if (kfactionEnabled && kfactionHook != null) {
            return kfactionHook.checkFactionLevel(player, requiredLevel);
        }

        boolean strict = me.krunsh.kcraft.Kcraft.getInstance()
            .getConfig()
            .getBoolean("Kcraft.hooks.strict-faction-level", false);

        if (strict) {
            MessageUtil.debug(
                "Recette avec faction-level-required=" + requiredLevel +
                " refusee: Kfaction indisponible et mode strict actif.",
                1
            );
            return false;
        }

        MessageUtil.debug(
            "Recette avec faction-level-required=" + requiredLevel +
            " autorisee: Kfaction indisponible mais mode tolerant (strict-faction-level=false).",
            2
        );
        return true;
    }

    /**
     * Obtient le nom de faction d'un joueur.
     */
    public String getFactionName(Player player) {
        if (kfactionEnabled && kfactionHook != null) {
            return kfactionHook.getFactionName(player);
        }

        return null;
    }

    /**
     * Obtient la puissance de faction d'un joueur.
     */
    public int getFactionPower(Player player) {
        if (kfactionEnabled && kfactionHook != null) {
            return kfactionHook.getFactionPower(player);
        }

        return 0;
    }

    /**
     * Verifie si un plugin est requis et disponible.
     */
    public boolean isPluginAvailable(String pluginName) {
        if (pluginName == null || pluginName.isEmpty()) {
            return true;
        }

        switch (pluginName.toLowerCase()) {
            case "kfaction":
                return kfactionEnabled;
            case "vault":
                return vaultEnabled;
            case "placeholderapi":
                return placeholderAPIEnabled;
            case "kharvester":
                return kharvestEnabled;
            case "outilsevolutif":
                return outilsEvolutifEnabled;
            case "plugincit":
                return pluginCITEnabled;
            default:
                return Bukkit.getPluginManager().isPluginEnabled(pluginName);
        }
    }

    // === GETTERS ===

    public boolean isKfactionEnabled() {
        return kfactionEnabled;
    }

    public boolean isVaultEnabled() {
        return vaultEnabled;
    }

    public boolean isPlaceholderAPIEnabled() {
        return placeholderAPIEnabled;
    }

    public boolean isKharvestEnabled() {
        return kharvestEnabled;
    }

    public boolean isOutilsEvolutifEnabled() {
        return outilsEvolutifEnabled;
    }

    public boolean isPluginCITEnabled() {
        return pluginCITEnabled;
    }

    /**
     * Hook pour Kfaction (via reflection pour eviter une dependance compile-time).
     */
    private static class KfactionHook {

        private static final String PLUGIN_NAME = "Kfaction";

        private Object getApi() {
            try {
                Plugin plugin = Bukkit.getPluginManager().getPlugin(PLUGIN_NAME);
                if (plugin == null) {
                    return null;
                }
                java.lang.reflect.Method getApi = plugin.getClass().getMethod("getAPI");
                return getApi.invoke(plugin);
            } catch (Exception e) {
                MessageUtil.debug("Erreur acces API Kfaction: " + e.getMessage(), 1);
                return null;
            }
        }

        private Object getPlayerFaction(Player player) {
            try {
                Object api = getApi();
                if (api == null) {
                    return null;
                }

                java.lang.reflect.Method getPlayerFaction = api.getClass().getMethod("getPlayerFaction", Player.class);
                return getPlayerFaction.invoke(api, player);
            } catch (Exception e) {
                MessageUtil.debug("Erreur getPlayerFaction Kfaction: " + e.getMessage(), 1);
                return null;
            }
        }

        public boolean checkFactionLevel(Player player, int requiredLevel) {
            try {
                Object faction = getPlayerFaction(player);
                if (faction == null) {
                    return false;
                }

                java.lang.reflect.Method getLevel = faction.getClass().getMethod("getLevel");
                Object value = getLevel.invoke(faction);
                int level = value instanceof Number ? ((Number) value).intValue() : 0;
                return level >= requiredLevel;
            } catch (Exception e) {
                MessageUtil.debug("Erreur check level Kfaction: " + e.getMessage(), 1);
                return false;
            }
        }

        public String getFactionName(Player player) {
            try {
                Object faction = getPlayerFaction(player);
                if (faction == null) {
                    return null;
                }

                java.lang.reflect.Method getName = faction.getClass().getMethod("getName");
                Object value = getName.invoke(faction);
                return value != null ? value.toString() : null;
            } catch (Exception e) {
                return null;
            }
        }

        public int getFactionPower(Player player) {
            try {
                Object faction = getPlayerFaction(player);
                if (faction == null) {
                    return 0;
                }

                java.lang.reflect.Method getPower = faction.getClass().getMethod("getPower");
                Object value = getPower.invoke(faction);
                return value instanceof Number ? (int) Math.round(((Number) value).doubleValue()) : 0;
            } catch (Exception e) {
                return 0;
            }
        }
    }

}
