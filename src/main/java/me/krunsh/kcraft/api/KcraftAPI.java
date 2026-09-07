package me.krunsh.kcraft.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import me.krunsh.kcraft.Kcraft;
import me.krunsh.kcraft.api.events.KcraftPostCraftEvent;
import me.krunsh.kcraft.api.events.KcraftPreCraftEvent;
import me.krunsh.kcraft.api.events.KcraftRecipeRegisterEvent;
import me.krunsh.kcraft.api.events.KcraftTableOpenEvent;
import me.krunsh.kcraft.api.execution.CraftExecutionContext;
import me.krunsh.kcraft.api.execution.CraftExecutionSource;
import me.krunsh.kcraft.models.CraftRecipe;
import me.krunsh.kcraft.models.CraftResult;
import me.krunsh.kcraft.models.CraftTable;

/**
 * API publique KCraft V2.
 *
 * V2.6 :
 * - catalogue immutable pour les lectures ;
 * - mutation recipe -> rebuild atomique compiled/index ;
 * - forceCraft null-safe ;
 * - contexte d'execution standardise ;
 * - API version 2.0.
 */
public class KcraftAPI {

    private final Kcraft plugin;

    public KcraftAPI(Kcraft plugin) {
        this.plugin = plugin;
    }

    public void registerCraft(
            CraftRecipe recipe) {

        if (recipe == null) {
            throw new IllegalArgumentException(
                "recipe"
            );
        }

        if (plugin.getCraftManager()
                .getRecipe(recipe.getId()) != null) {

            throw new IllegalArgumentException(
                "Une recette avec l'ID '"
                    + recipe.getId()
                    + "' existe déjà"
            );
        }

        plugin.getCraftManager()
            .addRecipe(recipe);

        try {
            plugin.rebuildRecipeRuntime();
        } catch (RuntimeException error) {
            plugin.getCraftManager()
                .removeRecipe(recipe.getId());

            plugin.rebuildRecipeRuntime();
            throw error;
        }

        plugin.getServer()
            .getPluginManager()
            .callEvent(
                new KcraftRecipeRegisterEvent(
                    recipe
                )
            );
    }

    public boolean unregisterCraft(
            String craftId) {

        CraftRecipe existing =
            plugin.getCraftManager()
                .getRecipe(craftId);

        if (existing == null) {
            return false;
        }

        boolean removed =
            plugin.getCraftManager()
                .removeRecipe(craftId);

        if (removed) {
            plugin.rebuildRecipeRuntime();
        }

        return removed;
    }

    public CraftRecipe getCraft(
            String craftId) {

        return plugin.getRecipeCatalog()
            .get(craftId);
    }

    public List<CraftRecipe> getAvailableCrafts(
            Player player) {

        List<CraftRecipe> available =
            new ArrayList<CraftRecipe>();

        for (CraftRecipe recipe
                : plugin.getRecipeCatalog()
                    .recipes()) {

            if (canCraft(
                    player,
                    recipe.getId())) {

                available.add(recipe);
            }
        }

        return available;
    }

    public boolean canCraft(
            Player player,
            String craftId) {

        CraftRecipe recipe =
            getCraft(craftId);

        if (player == null
                || recipe == null) {

            return false;
        }

        if (!recipe.canCraft(player)) {
            return false;
        }

        if (!plugin.getHookManager()
                .isPluginAvailable(
                    recipe.getRequiredPlugin())) {

            return false;
        }

        return plugin.getHookManager()
            .checkFactionLevel(
                player,
                recipe.getFactionLevelRequired()
            );
    }

    public ItemStack forceCraft(
            String craftId,
            Player crafter) {

        CraftRecipe recipe =
            getCraft(craftId);

        if (recipe == null
                || crafter == null) {

            return null;
        }

        UUID transactionId =
            UUID.randomUUID();

        try (CraftExecutionContext.Scope ignored =
                CraftExecutionContext.enter(
                    CraftExecutionSource.API_FORCE,
                    transactionId,
                    -1,
                    1)) {

            KcraftPreCraftEvent preEvent =
                new KcraftPreCraftEvent(
                    crafter,
                    recipe,
                    true
                );

            plugin.getServer()
                .getPluginManager()
                .callEvent(preEvent);

            if (preEvent.isCancelled()) {
                return null;
            }

            CraftResult selected =
                recipe.shouldGiveResult()
                    ? recipe.getResult()
                    : null;

            if (recipe.shouldGiveResult()
                    && selected == null) {

                return null;
            }

            ItemStack result =
                selected == null
                    ? null
                    : selected.createItem();

            KcraftPostCraftEvent postEvent =
                new KcraftPostCraftEvent(
                    crafter,
                    recipe,
                    result,
                    true,
                    true
                );

            plugin.getServer()
                .getPluginManager()
                .callEvent(postEvent);

            if (result != null) {
                plugin.getLoggingManager()
                    .logCraft(
                        crafter,
                        craftId,
                        result.getType().name(),
                        true
                    );
            }

            return result;

        } catch (Exception error) {
            plugin.getLogger().warning(
                "Erreur lors du craft forcé: "
                    + error.getMessage()
            );

            return null;
        }
    }

    public Map<String, Object> getPlayerStats(
            UUID playerId) {

        return plugin.getLoggingManager()
            .getPlayerStats(playerId);
    }

    public Map<String, Object> getGlobalStats() {
        return plugin.getLoggingManager()
            .getGlobalStats();
    }

    public CraftTable getTable(
            String tableId) {

        return plugin.getTableManager()
            .getTable(tableId);
    }

    public boolean openTable(
            Player player,
            String tableId) {

        CraftTable table =
            getTable(tableId);

        if (player == null
                || table == null
                || !table.canUse(player)) {

            return false;
        }

        try {
            KcraftTableOpenEvent event =
                new KcraftTableOpenEvent(
                    player,
                    table
                );

            plugin.getServer()
                .getPluginManager()
                .callEvent(event);

            if (event.isCancelled()) {
                return false;
            }

            me.krunsh.kcraft.gui.CraftTableGUI gui =
                new me.krunsh.kcraft.gui.CraftTableGUI(
                    plugin,
                    player,
                    table
                );

            gui.open();
            return true;

        } catch (Exception error) {
            plugin.getLogger().warning(
                "Erreur ouverture table API: "
                    + error.getMessage()
            );

            return false;
        }
    }

    /**
     * Shim legacy. Le CacheManager V1 est neutralisé depuis V2.1.
     */
    public Map<String, Integer> getCacheStats() {
        return plugin.getCacheManager()
            .getCacheStats();
    }

    /**
     * En V2 cette méthode reconstruit le vrai runtime compiled/index.
     */
    public void rebuildCache() {
        plugin.rebuildRecipeRuntime();
    }

    public boolean isReady() {
        return plugin.getRecipeCatalog() != null
            && plugin.getCompiledRecipeCatalog() != null
            && plugin.getIndexedCraftManager() != null
            && plugin.getTableManager() != null;
    }

    public String getAPIVersion() {
        return "2.0.0";
    }

    public int getTotalCrafts() {
        return plugin.getRecipeCatalog()
            .size();
    }

    public int getTotalTables() {
        return plugin.getTableManager()
            .getAllTables()
            .size();
    }

    /**
     * Ancien système de hook maintenu seulement pour compat source.
     *
     * Les integrations V2 doivent utiliser les Bukkit events.
     */
    @Deprecated
    public void registerHook(
            String pluginName,
            CraftHook hook) {

        plugin.getLogger().warning(
            "registerHook("
                + pluginName
                + ") est deprecated en API V2; utilisez les events KCraft."
        );
    }

    @Deprecated
    public interface CraftHook {
        boolean onPreCraft(
            Player player,
            CraftRecipe recipe);

        void onPostCraft(
            Player player,
            CraftRecipe recipe,
            ItemStack result,
            boolean success);
    }
}
