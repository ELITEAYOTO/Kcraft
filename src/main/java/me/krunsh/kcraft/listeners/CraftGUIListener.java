package me.krunsh.kcraft.listeners;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import me.krunsh.kcraft.Kcraft;
import me.krunsh.kcraft.api.events.KcraftPreCraftEvent;
import me.krunsh.kcraft.api.events.KcraftBatchCraftEvent;
import me.krunsh.kcraft.api.execution.CraftBatchAccumulator;
import me.krunsh.kcraft.api.execution.CraftExecutionContext;
import me.krunsh.kcraft.api.execution.CraftExecutionSource;
import me.krunsh.kcraft.batch.CraftBatchMetrics;
import me.krunsh.kcraft.batch.CraftBatchPlan;
import me.krunsh.kcraft.batch.CraftBatchPlanner;
import me.krunsh.kcraft.compiled.CompiledRecipe;
import me.krunsh.kcraft.gui.CraftTableGUI;
import me.krunsh.kcraft.gui.GuiDirtyRegistry;
import me.krunsh.kcraft.gui.GuiRefreshMetrics;
import me.krunsh.kcraft.gui.PreviewSelection;
import me.krunsh.kcraft.matching.RecipeMatcher;
import me.krunsh.kcraft.models.CraftMatrixTransaction;
import me.krunsh.kcraft.models.CraftRecipe;
import me.krunsh.kcraft.models.CraftResult;
import me.krunsh.kcraft.utils.MessageUtil;
import me.krunsh.kcraft.utils.SoundUtil;

/**
 * Listener GUI V2.6.
 *
 * Optimisations :
 * - DirtyQueue globale : plusieurs changements dans le même tick -> 1 refresh ;
 * - Preview Lock : un résultat aléatoire affiché reste identique jusqu'au craft ;
 * - UUID-only : aucun Player n'est retenu dans les registres.
 */
public final class CraftGUIListener
        implements Listener {

    private final Kcraft plugin;

    private final PlayerGuiRegistry<CraftTableGUI> activeGUIs =
        new PlayerGuiRegistry<CraftTableGUI>();

    private final Map<UUID, PreviewSelection> previewSelections =
        new HashMap<UUID, PreviewSelection>();

    private final GuiDirtyRegistry dirty =
        new GuiDirtyRegistry();

    private final GuiRefreshMetrics guiMetrics =
        new GuiRefreshMetrics();

    private final CraftBatchPlanner batchPlanner =
        new CraftBatchPlanner(
            new RecipeMatcher()
        );

    private final CraftBatchMetrics batchMetrics =
        new CraftBatchMetrics();

    private BukkitTask dirtyTask;

    public CraftGUIListener(
            Kcraft plugin) {

        this.plugin = plugin;
    }

    public void registerGUI(
            Player player,
            CraftTableGUI gui) {

        UUID uuid =
            player.getUniqueId();

        cleanupPlayer(
            uuid,
            player,
            true
        );

        activeGUIs.put(
            uuid,
            gui
        );

        markDirty(
            uuid
        );
    }

    public void unregisterGUI(
            Player player) {

        cleanupPlayer(
            player.getUniqueId(),
            player,
            false
        );
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(
            InventoryClickEvent event) {

        if (!(event.getWhoClicked()
                instanceof Player)) {

            return;
        }

        Player player =
            (Player) event.getWhoClicked();

        UUID uuid =
            player.getUniqueId();

        CraftTableGUI gui =
            activeGUIs.get(uuid);

        if (gui == null) {
            return;
        }

        int slot =
            event.getRawSlot();

        if (slot >= 0
                && slot < gui.getInventory()
                    .getSize()) {

            if (gui.isCraftButton(slot)) {
                event.setCancelled(true);

                if (event.isLeftClick()) {
                    if (event.isShiftClick()) {
                        handleMassCraft(
                            player,
                            gui
                        );
                    } else {
                        handleCraftClick(
                            player,
                            gui
                        );
                    }
                }

                return;
            }

            if (gui.isResultSlot(slot)) {
                event.setCancelled(true);
                return;
            }

            if (gui.isModifiableSlot(slot)) {
                markDirty(uuid);
                return;
            }

            event.setCancelled(true);
            return;
        }

        /*
         * Clic dans l'inventaire joueur :
         * Bukkit peut déplacer un item vers la GUI via shift-click.
         */
        markDirty(uuid);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(
            InventoryDragEvent event) {

        if (!(event.getWhoClicked()
                instanceof Player)) {

            return;
        }

        Player player =
            (Player) event.getWhoClicked();

        UUID uuid =
            player.getUniqueId();

        CraftTableGUI gui =
            activeGUIs.get(uuid);

        if (gui == null) {
            return;
        }

        for (int slot
                : event.getRawSlots()) {

            if (slot >= 0
                    && slot < gui.getInventory()
                        .getSize()
                    && !gui.isModifiableSlot(slot)) {

                event.setCancelled(true);
                return;
            }
        }

        markDirty(uuid);
    }

    @EventHandler
    public void onInventoryClose(
            InventoryCloseEvent event) {

        if (!(event.getPlayer()
                instanceof Player)) {

            return;
        }

        Player player =
            (Player) event.getPlayer();

        UUID uuid =
            player.getUniqueId();

        CraftTableGUI gui =
            activeGUIs.get(uuid);

        if (gui == null
                || event.getInventory()
                    != gui.getInventory()) {

            return;
        }

        cleanupPlayer(
            uuid,
            player,
            true
        );
    }

    @EventHandler
    public void onPlayerKick(
            PlayerKickEvent event) {

        cleanupPlayer(
            event.getPlayer().getUniqueId(),
            event.getPlayer(),
            true
        );
    }

    @EventHandler
    public void onPlayerQuit(
            PlayerQuitEvent event) {

        cleanupPlayer(
            event.getPlayer().getUniqueId(),
            event.getPlayer(),
            true
        );
    }

    public void shutdown() {

        if (dirtyTask != null) {
            dirtyTask.cancel();
            dirtyTask = null;
        }

        dirty.clear();
        previewSelections.clear();

        for (UUID uuid
                : activeGUIs.playerIds()) {

            Player player =
                plugin.getServer()
                    .getPlayer(uuid);

            cleanupPlayer(
                uuid,
                player,
                player != null
            );

            if (player != null) {
                try {
                    player.closeInventory();
                } catch (RuntimeException ignored) {
                    // Shutdown best-effort.
                }
            }
        }

        activeGUIs.clear();
    }

    private void handleCraftClick(
            Player player,
            CraftTableGUI gui) {

        UUID uuid =
            player.getUniqueId();

        try {
            ItemStack[] matrix =
                gui.getCraftMatrix();

            CraftRecipe recipe =
                plugin.getCraftManager()
                    .findMatchingRecipe(
                        matrix,
                        gui.getTable()
                    );

            if (!checkAccess(
                    player,
                    recipe)) {

                clearPreview(uuid);
                markDirty(uuid);
                return;
            }

            ItemStack[] consumed =
                CraftMatrixTransaction.consumeOnce(
                    recipe,
                    matrix
                );

            if (consumed == null) {
                MessageUtil.sendError(
                    player,
                    "craft.failed"
                );

                markDirty(uuid);
                return;
            }

            CraftResult result =
                resolveLockedResult(
                    uuid,
                    recipe
                );

            if (recipe.shouldGiveResult()
                    && result == null) {

                MessageUtil.sendError(
                    player,
                    "craft.failed"
                );

                clearPreview(uuid);
                markDirty(uuid);
                return;
            }

            if (!callPreEvent(
                    player,
                    recipe)) {

                /*
                 * Le craft n'a pas eu lieu :
                 * on conserve la preview verrouillée.
                 */
                markDirty(uuid);
                return;
            }

            UUID transactionId =
                UUID.randomUUID();

            boolean success;

            try (CraftExecutionContext.Scope ignored =
                    CraftExecutionContext.enter(
                        CraftExecutionSource.CUSTOM_GUI_SINGLE,
                        transactionId,
                        -1,
                        1)) {

                success =
                    plugin.getCraftManager()
                        .executeCraft(
                            player,
                            recipe,
                            matrix,
                            result
                        );
            }

            /*
             * Une vraie tentative a eu lieu.
             * La prochaine preview doit être une nouvelle sélection.
             */
            consumePreview(uuid);

            if (success) {
                applyMatrix(
                    gui,
                    consumed
                );

                if (result != null) {
                    giveResult(
                        player,
                        result.createItem()
                    );
                }

                MessageUtil.sendSuccess(
                    player,
                    "craft.success",
                    MessageUtil.placeholders(
                        "item",
                        recipe.getId()
                    )
                );

                playSuccessSound(
                    player,
                    gui
                );

                markDirty(uuid);
                return;
            }

            if (!recipe.isReturnItemsOnFail()) {
                applyMatrix(
                    gui,
                    consumed
                );
            }

            markDirty(uuid);

            MessageUtil.sendError(
                player,
                "craft.failed"
            );

        } catch (Exception error) {
            plugin.getLogger().warning(
                "Erreur craft "
                    + player.getName()
                    + ": "
                    + error.getMessage()
            );

            clearPreview(uuid);
            markDirty(uuid);

            MessageUtil.sendError(
                player,
                "craft.error"
            );
        }
    }

    private void handleMassCraft(
            Player player,
            CraftTableGUI gui) {

        UUID uuid =
            player.getUniqueId();

        try {
            if (!plugin.getConfigManager()
                    .isBatchCraftEnabled()) {

                handleCraftClick(
                    player,
                    gui
                );
                return;
            }

            ItemStack[] initialMatrix =
                gui.getCraftMatrix();

            CraftRecipe recipe =
                plugin.getCraftManager()
                    .findMatchingRecipe(
                        initialMatrix,
                        gui.getTable()
                    );

            if (!checkAccess(
                    player,
                    recipe)) {

                clearPreview(uuid);
                markDirty(uuid);
                return;
            }

            CompiledRecipe compiled =
                plugin.getCompiledRecipeCatalog()
                    .get(recipe.getId());

            if (compiled == null) {
                handleCraftClick(
                    player,
                    gui
                );
                return;
            }

            long planStarted =
                System.nanoTime();

            CraftBatchPlan plan =
                batchPlanner.plan(
                    recipe,
                    compiled,
                    initialMatrix,
                    plugin.getConfigManager()
                        .getMaxBatchCrafts()
                );

            long planNanos =
                System.nanoTime()
                    - planStarted;

            if (plan == null
                    || plan.getCraftCount() <= 0) {

                batchMetrics.recordPlan(
                    0,
                    planNanos
                );

                MessageUtil.sendError(
                    player,
                    "craft.mass-failed"
                );

                markDirty(uuid);
                return;
            }

            batchMetrics.recordPlan(
                plan.getCraftCount(),
                planNanos
            );

            int successful = 0;
            int attempted = 0;
            int failed = 0;
            boolean cancelled = false;
            boolean matrixChanged = false;

            UUID transactionId =
                UUID.randomUUID();

            CraftBatchAccumulator resultAccumulator =
                new CraftBatchAccumulator();

            /*
             * Le premier craft utilise exactement la preview affichée.
             * Les suivants tirent normalement un nouveau résultat par craft.
             */
            CraftResult firstLockedResult =
                resolveLockedResult(
                    uuid,
                    recipe
                );

            boolean firstResultAvailable =
                !recipe.shouldGiveResult()
                    || firstLockedResult != null;

            if (!firstResultAvailable) {
                clearPreview(uuid);
                markDirty(uuid);

                MessageUtil.sendError(
                    player,
                    "craft.mass-failed"
                );
                return;
            }

            for (int craftIndex = 0;
                    craftIndex < plan.getCraftCount();
                    craftIndex++) {

                CraftResult result =
                    craftIndex == 0
                        ? firstLockedResult
                        : selectFreshResult(recipe);

                if (recipe.shouldGiveResult()
                        && result == null) {

                    batchMetrics.recordFailed();
                    failed++;
                    break;
                }

                if (!callPreEvent(
                        player,
                        recipe)) {

                    batchMetrics.recordCancelled();
                    cancelled = true;
                    break;
                }

                attempted++;

                ItemStack[] beforeConsumption =
                    plan.getWorkingMatrix();

                boolean success;

                try (CraftExecutionContext.Scope ignored =
                        CraftExecutionContext.enter(
                            CraftExecutionSource.SHIFT_BATCH,
                            transactionId,
                            craftIndex,
                            plan.getCraftCount())) {

                    success =
                        plugin.getCraftManager()
                            .executeCraft(
                                player,
                                recipe,
                                beforeConsumption,
                                result
                            );
                }

                /*
                 * Dès la première vraie tentative le preview lock est consommé.
                 */
                if (craftIndex == 0) {
                    consumePreview(uuid);
                }

                if (success) {
                    if (!plan.applyConsumption(
                            craftIndex)) {

                        plugin.getLogger().warning(
                            "CraftBatchPlan incoherent pour "
                                + recipe.getId()
                                + " / "
                                + player.getName()
                        );

                        batchMetrics.recordFailed();
                        break;
                    }

                    matrixChanged = true;
                    successful++;

                    batchMetrics.recordExecuted();

                    ItemStack resultItem =
                        result == null
                            ? null
                            : result.createItem();

                    resultAccumulator.addSuccess(
                        resultItem
                    );

                    if (resultItem != null) {
                        giveResult(
                            player,
                            resultItem
                        );
                    }

                    continue;
                }

                batchMetrics.recordFailed();
                failed++;

                if (!recipe.isReturnItemsOnFail()) {
                    if (plan.applyConsumption(
                            craftIndex)) {

                        matrixChanged = true;
                    }
                }

                break;
            }

            if (matrixChanged) {
                applyMatrix(
                    gui,
                    plan.getWorkingMatrix()
                );
            }

            if (successful > 0) {
                MessageUtil.sendSuccess(
                    player,
                    "craft.mass-success",
                    MessageUtil.placeholders(
                        "amount",
                        String.valueOf(successful),
                        "item",
                        recipe.getId()
                    )
                );

                playSuccessSound(
                    player,
                    gui
                );

            } else {
                MessageUtil.sendError(
                    player,
                    "craft.mass-failed"
                );
            }

            KcraftBatchCraftEvent batchEvent =
                new KcraftBatchCraftEvent(
                    player,
                    recipe,
                    transactionId,
                    CraftExecutionSource.SHIFT_BATCH,
                    plan.getCraftCount(),
                    attempted,
                    successful,
                    failed,
                    resultAccumulator.getTotalResultItems(),
                    cancelled,
                    resultAccumulator.snapshot()
                );

            plugin.getServer()
                .getPluginManager()
                .callEvent(batchEvent);

            markDirty(uuid);

        } catch (Exception error) {
            plugin.getLogger().warning(
                "Erreur mass craft "
                    + player.getName()
                    + ": "
                    + error.getMessage()
            );

            clearPreview(uuid);
            markDirty(uuid);

            MessageUtil.sendError(
                player,
                "craft.error"
            );
        }
    }

    private boolean checkAccess(
            Player player,
            CraftRecipe recipe) {

        if (recipe == null) {
            MessageUtil.sendError(
                player,
                "craft.no-recipe"
            );
            return false;
        }

        if (!recipe.canCraft(player)) {
            MessageUtil.sendError(
                player,
                "craft.no-permission"
            );
            return false;
        }

        if (!plugin.getHookManager()
                .isPluginAvailable(
                    recipe.getRequiredPlugin())) {

            MessageUtil.sendError(
                player,
                "craft.plugin-required"
            );
            return false;
        }

        if (!plugin.getHookManager()
                .checkFactionLevel(
                    player,
                    recipe.getFactionLevelRequired())) {

            MessageUtil.sendError(
                player,
                "craft.faction-level-low",
                MessageUtil.placeholders(
                    "level",
                    String.valueOf(
                        recipe.getFactionLevelRequired()
                    )
                )
            );
            return false;
        }

        return true;
    }

    private CraftResult resolveLockedResult(
            UUID uuid,
            CraftRecipe recipe) {

        PreviewSelection selection =
            previewSelections.get(uuid);

        if (selection != null
                && selection.matches(recipe)) {

            guiMetrics.recordPreviewReused();

            return selection.getResult();
        }

        CraftResult fresh =
            selectFreshResult(recipe);

        previewSelections.put(
            uuid,
            new PreviewSelection(
                recipe.getId(),
                fresh
            )
        );

        guiMetrics.recordPreviewCreated();

        return fresh;
    }

    private CraftResult selectFreshResult(
            CraftRecipe recipe) {

        return recipe.shouldGiveResult()
            ? recipe.getResult()
            : null;
    }

    private void consumePreview(
            UUID uuid) {

        if (previewSelections.remove(uuid)
                != null) {

            guiMetrics.recordPreviewConsumed();
        }
    }

    private void clearPreview(
            UUID uuid) {

        previewSelections.remove(uuid);
    }

    private boolean callPreEvent(
            Player player,
            CraftRecipe recipe) {

        KcraftPreCraftEvent event =
            new KcraftPreCraftEvent(
                player,
                recipe,
                false
            );

        plugin.getServer()
            .getPluginManager()
            .callEvent(event);

        return !event.isCancelled();
    }

    /**
     * Marque une GUI dirty.
     *
     * Une seule tâche Bukkit est planifiée, quelle que soit la quantité de
     * marks avant le prochain tick.
     */
    private void markDirty(
            UUID uuid) {

        boolean first =
            dirty.mark(uuid);

        guiMetrics.recordMark(first);

        if (dirtyTask != null) {
            return;
        }

        dirtyTask =
            plugin.getServer()
                .getScheduler()
                .runTask(
                    plugin,
                    new Runnable() {
                        @Override
                        public void run() {
                            flushDirty();
                        }
                    }
                );
    }

    private void flushDirty() {

        dirtyTask = null;

        List<UUID> players =
            dirty.drain();

        if (players.isEmpty()) {
            return;
        }

        guiMetrics.recordDrain();

        for (UUID uuid : players) {

            CraftTableGUI gui =
                activeGUIs.get(uuid);

            if (gui == null) {
                guiMetrics.recordStaleSkipped();
                continue;
            }

            Player player =
                plugin.getServer()
                    .getPlayer(uuid);

            if (player == null
                    || !player.isOnline()) {

                guiMetrics.recordStaleSkipped();
                continue;
            }

            updateCraftResult(
                uuid,
                gui
            );

            guiMetrics.recordRefreshed();
        }

        /*
         * Un listener déclenché pendant le flush peut re-marquer une GUI.
         * On programme alors un prochain drain, jamais une boucle immédiate.
         */
        if (!dirty.isEmpty()
                && dirtyTask == null) {

            dirtyTask =
                plugin.getServer()
                    .getScheduler()
                    .runTask(
                        plugin,
                        new Runnable() {
                            @Override
                            public void run() {
                                flushDirty();
                            }
                        }
                    );
        }
    }

    private void updateCraftResult(
            UUID uuid,
            CraftTableGUI gui) {

        try {
            ItemStack[] matrix =
                gui.getCraftMatrix();

            CraftRecipe recipe =
                plugin.getCraftManager()
                    .findMatchingRecipe(
                        matrix,
                        gui.getTable()
                    );

            if (recipe == null) {
                clearPreview(uuid);
                gui.updateResult(null);
                return;
            }

            Player player =
                plugin.getServer()
                    .getPlayer(uuid);

            if (player == null
                    || !player.isOnline()
                    || !recipe.canCraft(player)
                    || !plugin.getHookManager()
                        .isPluginAvailable(
                            recipe.getRequiredPlugin())
                    || !plugin.getHookManager()
                        .checkFactionLevel(
                            player,
                            recipe.getFactionLevelRequired())) {

                clearPreview(uuid);
                gui.updateResult(null);
                return;
            }

            CraftResult preview =
                resolveLockedResult(
                    uuid,
                    recipe
                );

            gui.updateResult(
                preview == null
                    ? null
                    : preview.createItem()
            );

        } catch (Exception error) {
            clearPreview(uuid);

            plugin.getLogger().warning(
                "Erreur preview KCraft: "
                    + error.getMessage()
            );
        }
    }

    private void applyMatrix(
            CraftTableGUI gui,
            ItemStack[] matrix) {

        int[] slots =
            gui.getCraftSlots();

        for (int index = 0;
                index < slots.length
                    && index < matrix.length;
                index++) {

            gui.getInventory()
                .setItem(
                    slots[index],
                    matrix[index]
                );
        }
    }

    private void giveResult(
            Player player,
            ItemStack result) {

        if (result == null
                || result.getType() == Material.AIR) {

            return;
        }

        HashMap<Integer, ItemStack> leftover =
            player.getInventory()
                .addItem(result);

        for (ItemStack item
                : leftover.values()) {

            player.getWorld()
                .dropItemNaturally(
                    player.getLocation(),
                    item
                );
        }
    }

    private void playSuccessSound(
            Player player,
            CraftTableGUI gui) {

        if (plugin.getConfigManager()
                .areSoundsEnabled()
                && gui.getTable()
                    .getSoundOpen() != null) {

            SoundUtil.playSafe(
                player,
                gui.getTable().getSoundOpen(),
                1.0F,
                1.4F
            );
        }
    }

    private void cleanupPlayer(
            final UUID uuid,
            final Player player,
            final boolean returnIngredients) {

        dirty.remove(uuid);
        clearPreview(uuid);

        activeGUIs.cleanup(
            uuid,
            new PlayerGuiRegistry.Cleanup<CraftTableGUI>() {
                @Override
                public void run(
                        CraftTableGUI gui) {

                    if (returnIngredients
                            && player != null) {

                        returnIngredients(
                            player,
                            gui
                        );
                    }
                }
            }
        );
    }

    private void returnIngredients(
            Player player,
            CraftTableGUI gui) {

        ItemStack[] matrix =
            gui.getCraftMatrix();

        int[] slots =
            gui.getCraftSlots();

        for (int index = 0;
                index < matrix.length;
                index++) {

            ItemStack item =
                matrix[index];

            if (item == null
                    || item.getType() == Material.AIR
                    || item.getAmount() <= 0) {

                continue;
            }

            HashMap<Integer, ItemStack> leftover =
                player.getInventory()
                    .addItem(
                        item.clone()
                    );

            for (ItemStack drop
                    : leftover.values()) {

                player.getWorld()
                    .dropItemNaturally(
                        player.getLocation(),
                        drop
                    );
            }

            if (index < slots.length) {
                gui.getInventory()
                    .setItem(
                        slots[index],
                        null
                    );
            }
        }
    }

    public CraftBatchMetrics getBatchMetrics() {
        return batchMetrics;
    }

    public GuiRefreshMetrics getGuiRefreshMetrics() {
        return guiMetrics;
    }

    public int getDirtyGuiCount() {
        return dirty.size();
    }

    public int getPreviewLockCount() {
        return previewSelections.size();
    }
}

/** Registre GUI UUID-only. */
final class PlayerGuiRegistry<T> {

    interface Cleanup<T> {
        void run(T value);
    }

    private final Map<UUID, T> values =
        new HashMap<UUID, T>();

    T put(
            UUID uuid,
            T value) {

        if (uuid == null
                || value == null) {

            throw new IllegalArgumentException(
                "uuid/value"
            );
        }

        return values.put(
            uuid,
            value
        );
    }

    T get(UUID uuid) {
        return values.get(uuid);
    }

    void cleanup(
            UUID uuid,
            Cleanup<T> cleanup) {

        T value =
            values.remove(uuid);

        if (value == null) {
            return;
        }

        cleanup.run(value);
    }

    List<UUID> playerIds() {
        return new ArrayList<UUID>(
            values.keySet()
        );
    }

    int size() {
        return values.size();
    }

    void clear() {
        values.clear();
    }
}
