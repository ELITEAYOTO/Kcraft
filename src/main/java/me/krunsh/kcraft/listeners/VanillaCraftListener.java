package me.krunsh.kcraft.listeners;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

import me.krunsh.kcraft.Kcraft;
import me.krunsh.kcraft.api.events.KcraftPreCraftEvent;
import me.krunsh.kcraft.models.CraftOutputTransfer;
import me.krunsh.kcraft.models.CraftIngredient;
import me.krunsh.kcraft.models.CraftMatrixDiagnostics;
import me.krunsh.kcraft.models.CraftRecipe;
import me.krunsh.kcraft.models.CraftResult;
import me.krunsh.kcraft.models.RecipeAccessPolicy;
import me.krunsh.kcraft.models.VanillaObtentionPolicy;
import me.krunsh.kcraft.models.VanillaRecipeResolver;
import me.krunsh.kcraft.managers.VanillaDebugManager;
import me.krunsh.kcraft.utils.MessageUtil;
import me.krunsh.kcraft.utils.ItemStackUtil;

/** Pont transactionnel prioritaire entre KCraft et la grille vanilla 3x3. */
public final class VanillaCraftListener implements Listener {

    private static final int MAX_SHIFT_CRAFTS = 256;

    private final Kcraft plugin;
    private final Map<UUID, PreparedPreview> previews = new HashMap<UUID, PreparedPreview>();
    private final Map<UUID, TraceObservation> pendingTraces = new HashMap<UUID, TraceObservation>();
    private final Set<String> loggedAmbiguities = new HashSet<String>();

    public VanillaCraftListener(Kcraft plugin) {
        this.plugin = plugin;
    }

    public boolean registerUnknownCraftExtension() {
        return UnknownCraftEventBridge.register(plugin, this);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareItemCraft(PrepareItemCraftEvent event) {
        Player player = event.getView() != null && event.getView().getPlayer() instanceof Player
            ? (Player) event.getView().getPlayer() : null;
        plugin.getVanillaDebugManager().beginKnownPrepare(player);
        handlePrepare(event.getInventory(), event.getView(), "C-KNOWN PrepareItemCraftEvent");
    }

    void onPrepareUnknownCraft(InventoryEvent event) {
        if (!(event.getInventory() instanceof CraftingInventory)) return;
        Player player = event.getView() != null && event.getView().getPlayer() instanceof Player
            ? (Player) event.getView().getPlayer() : null;
        long traceId = readTraceId(event);
        plugin.getVanillaDebugManager().markUnknownRuntimeReceived(player, traceId);
        if (plugin.getVanillaDebugManager().isActive(player)) {
            plugin.getVanillaDebugManager().trace(player, "C-UNKNOWN-MAIN entrée classe="
                + event.getClass().getName() + " classloader-événement="
                + describeLoader(event.getClass().getClassLoader()) + " classloader-KCraft="
                + describeLoader(getClass().getClassLoader()) + " inventory="
                + event.getInventory().getClass().getName());
        }
        handlePrepare((CraftingInventory) event.getInventory(), event.getView(),
            "C-UNKNOWN-MAIN PrepareUnknownCraftEvent");
        if (plugin.getVanillaDebugManager().isActive(player)) {
            plugin.getVanillaDebugManager().trace(player, "HOOK résultat relu immédiatement="
                + plugin.getVanillaDebugManager().describeItem(
                    ((CraftingInventory) event.getInventory()).getResult()));
        }
    }

    private void handlePrepare(CraftingInventory inventory, InventoryView view, String eventName) {
        Player player = view != null && view.getPlayer() instanceof Player
            ? (Player) view.getPlayer() : null;
        ItemStack[] matrix = inventory == null ? null : inventory.getMatrix();
        if (plugin.getVanillaDebugManager().isActive(player)) {
            plugin.getVanillaDebugManager().trace(player, eventName + " entrée handler joueur="
                + player.getName() + " inventory="
                + (inventory == null ? "<null>" : inventory.getClass().getName())
                + " longueur-matrice=" + (matrix == null ? "<null>" : matrix.length)
                + " recette-Bukkit=" + (inventory == null ? "<null>"
                    : describeBukkitRecipe(inventory.getRecipe()))
                + " résultat-avant=" + (inventory == null ? "<indisponible>"
                    : plugin.getVanillaDebugManager().describeItem(inventory.getResult())));
        }
        if (inventory == null || matrix == null || matrix.length != 9) {
            if (plugin.getVanillaDebugManager().isActive(player)) {
                plugin.getVanillaDebugManager().trace(player, eventName
                    + " SORTIE taille invalide; attendu=9 actuel="
                    + (matrix == null ? "<null>" : matrix.length));
            }
            return;
        }
        ItemStack before = inventory.getResult() == null ? null : inventory.getResult().clone();
        boolean trace = player != null && plugin.getVanillaDebugManager().shouldTraceMatrix(
            player, matrix);
        if (trace) traceBefore(player, matrix, before, eventName);
        updatePreview(inventory, player);
        if (trace) {
            ItemStack after = inventory.getResult() == null ? null : inventory.getResult().clone();
            traceDecision(player, matrix, after);
            final UUID playerId = player.getUniqueId();
            final TraceObservation observation = new TraceObservation(inventory, after, eventName);
            pendingTraces.put(playerId, observation);
            plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
                @Override public void run() {
                    if (pendingTraces.get(playerId) == observation) pendingTraces.remove(playerId);
                }
            });
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void observePrepareAfterPlugins(PrepareItemCraftEvent event) {
        observePreparedResult(event.getInventory(), event.getView());
    }

    void observeUnknownCraft(InventoryEvent event) {
        if (!(event.getInventory() instanceof CraftingInventory)) return;
        observePreparedResult((CraftingInventory) event.getInventory(), event.getView());
    }

    private void observePreparedResult(CraftingInventory inventory, InventoryView view) {
        Player player = view != null && view.getPlayer() instanceof Player
            ? (Player) view.getPlayer() : null;
        if (player == null) return;
        final TraceObservation observation = pendingTraces.remove(player.getUniqueId());
        if (observation == null || observation.inventory != inventory) return;
        ItemStack monitor = inventory.getResult();
        plugin.getVanillaDebugManager().trace(player, observation.eventName + " MONITOR résultat="
            + plugin.getVanillaDebugManager().describeItem(monitor)
            + replacement(observation.kcraftResult, monitor));
        plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
            @Override public void run() {
                if (!plugin.getVanillaDebugManager().isActive(player)) return;
                ItemStack delayed = observation.inventory.getResult();
                plugin.getVanillaDebugManager().trace(player, "+1 tick résultat="
                    + plugin.getVanillaDebugManager().describeItem(delayed)
                    + replacement(observation.kcraftResult, delayed));
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onCraftingResultClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)
                || event.getRawSlot() != 0
                || event.getView() == null
                || !(event.getView().getTopInventory() instanceof CraftingInventory)) return;

        boolean cancelledByAnotherPlugin = event.isCancelled();
        Player player = (Player) event.getWhoClicked();
        CraftingInventory inventory = (CraftingInventory) event.getView().getTopInventory();
        ItemStack[] matrix = inventory.getMatrix();
        if (matrix == null || matrix.length != 9) return;

        VanillaRecipeResolver.Resolution resolution = resolve(matrix, player);
        traceResultClick(player, event, inventory, resolution);
        if (resolution.getKind() == VanillaRecipeResolver.Kind.VANILLA) {
            if (plugin.getVanillaDebugManager().isActive(player)) {
                plugin.getVanillaDebugManager().trace(player,
                    "C-CLICK décision=LAISSER_VANILLA motif=aucune recette KCraft correspondante "
                    + "et aucun item custom détecté");
            }
            previews.remove(player.getUniqueId());
            if (isBlockedVanillaResult(event.getCurrentItem())
                    || isBlockedVanillaResult(inventory.getResult())) {
                event.setCancelled(true);
                inventory.setResult(null);
                player.updateInventory();
            }
            return;
        }

        // Tout chemin custom ou ambigu est soustrait au moteur vanilla avant
        // la moindre consommation.
        if (plugin.getVanillaDebugManager().isActive(player)) {
            plugin.getVanillaDebugManager().trace(player,
                "C-CLICK appel event.setCancelled(true) résolution=" + resolution.getKind()
                + (resolution.getRecipe() == null ? ""
                    : " recette=" + resolution.getRecipe().getId()));
        }
        event.setCancelled(true);
        inventory.setResult(null);

        if (cancelledByAnotherPlugin
                || resolution.getKind() != VanillaRecipeResolver.Kind.CUSTOM) {
            updatePreview(inventory, player);
            player.updateInventory();
            return;
        }

        CraftRecipe recipe = resolution.getRecipe();
        if (!RecipeAccessPolicy.allowsVanilla(recipe) || !conditionsPass(player, recipe)
                || !recipe.shouldGiveResult()) {
            MessageUtil.sendError(player, "craft.no-permission");
            updatePreview(inventory, player);
            player.updateInventory();
            return;
        }

        PreparedPreview prepared = previews.get(player.getUniqueId());
        if (prepared == null || !recipe.getId().equals(prepared.recipeId)) {
            prepared = prepare(recipe);
        }
        if (prepared == null || prepared.output == null) {
            MessageUtil.sendError(player, "craft.failed");
            updatePreview(inventory, player);
            player.updateInventory();
            return;
        }

        CraftOutputTransfer.Destination destination = destination(event.getClick());
        if (destination == null) {
            updatePreview(inventory, player);
            player.updateInventory();
            return;
        }

        ItemStack[] workingMatrix = cloneItems(matrix);
        ItemStack[] workingStorage = cloneItems(player.getInventory().getContents());
        ItemStack workingCursor = player.getItemOnCursor() == null
            ? null : player.getItemOnCursor().clone();
        int limit = destination == CraftOutputTransfer.Destination.INVENTORY
            ? MAX_SHIFT_CRAFTS : 1;
        int crafted = 0;

        for (int iteration = 0; iteration < limit; iteration++) {
            CraftOutputTransfer.Plan plan = CraftOutputTransfer.planOnce(
                recipe, workingMatrix, prepared.output, destination,
                workingCursor, workingStorage, event.getHotbarButton());
            if (plan == null) break;

            KcraftPreCraftEvent preEvent = new KcraftPreCraftEvent(player, recipe, false);
            plugin.getServer().getPluginManager().callEvent(preEvent);
            if (preEvent.isCancelled()) break;

            boolean success = plugin.getCraftManager().executeCraft(
                player, recipe, workingMatrix, prepared.result, prepared.output);
            if (!success) {
                MessageUtil.sendError(player, "craft.failed");
                break;
            }

            workingMatrix = plan.getMatrix();
            workingStorage = plan.getStorage();
            workingCursor = plan.getCursor();
            crafted++;
        }

        if (crafted > 0) {
            // Commit unique apres validation de la destination et de chaque craft.
            inventory.setMatrix(workingMatrix);
            // En CraftBukkit 1.8, setContents(36 slots) efface les 4 slots
            // d'armure restants. Ecrire uniquement la zone de stockage 0..35.
            for (int slot = 0; slot < workingStorage.length; slot++) {
                player.getInventory().setItem(slot, workingStorage[slot]);
            }
            player.setItemOnCursor(workingCursor);
            MessageUtil.sendSuccess(player, "craft.success",
                MessageUtil.placeholders("item", recipe.getId()));
        }

        // Recalcul immediat, sans delai d'un tick et sans fallback vanilla.
        updatePreview(inventory, player);
        player.updateInventory();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void observeCraftingResultClick(final InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)
                || event.getRawSlot() != 0
                || event.getView() == null
                || !(event.getView().getTopInventory() instanceof CraftingInventory)) return;
        final Player player = (Player) event.getWhoClicked();
        if (!plugin.getVanillaDebugManager().isActive(player)) return;
        final CraftingInventory inventory =
            (CraftingInventory) event.getView().getTopInventory();
        plugin.getVanillaDebugManager().trace(player, "clic-résultat MONITOR annulé="
            + event.isCancelled() + " résultat="
            + plugin.getVanillaDebugManager().describeItem(inventory.getResult()));
        plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
            @Override public void run() {
                if (!plugin.getVanillaDebugManager().isActive(player)) return;
                plugin.getVanillaDebugManager().trace(player, "clic-résultat +1 tick résultat="
                    + plugin.getVanillaDebugManager().describeItem(inventory.getResult()));
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void watchUnknownHookAfterMatrixClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player) || event.getView() == null
                || !(event.getView().getTopInventory() instanceof CraftingInventory)
                || event.getRawSlot() < 1 || event.getRawSlot() > 9) return;
        scheduleUnknownHookCheck((Player) event.getWhoClicked(),
            (CraftingInventory) event.getView().getTopInventory());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void watchUnknownHookAfterMatrixDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player) || event.getView() == null
                || !(event.getView().getTopInventory() instanceof CraftingInventory)) return;
        boolean matrixChanged = false;
        for (Integer rawSlot : event.getRawSlots()) {
            if (rawSlot != null && rawSlot >= 1 && rawSlot <= 9) {
                matrixChanged = true;
                break;
            }
        }
        if (matrixChanged) scheduleUnknownHookCheck((Player) event.getWhoClicked(),
            (CraftingInventory) event.getView().getTopInventory());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        previews.remove(event.getPlayer().getUniqueId());
        pendingTraces.remove(event.getPlayer().getUniqueId());
        plugin.getVanillaDebugManager().stop(event.getPlayer());
    }

    public void shutdown() {
        previews.clear();
        pendingTraces.clear();
        loggedAmbiguities.clear();
    }

    private void updatePreview(CraftingInventory inventory, Player player) {
        ItemStack[] matrix = inventory.getMatrix();
        VanillaRecipeResolver.Resolution resolution = resolve(matrix, player);

        if (resolution.getKind() == VanillaRecipeResolver.Kind.CUSTOM) {
            CraftRecipe recipe = resolution.getRecipe();
            boolean policyAllows = RecipeAccessPolicy.allowsVanilla(recipe);
            boolean conditionsAllow = player != null && conditionsPass(player, recipe);
            boolean givesResult = recipe.shouldGiveResult();
            if (plugin.getVanillaDebugManager().isActive(player)) {
                plugin.getVanillaDebugManager().trace(player, "C-PREVIEW recette=" + recipe.getId()
                    + " allowsVanilla=" + policyAllows + " conditions=" + conditionsAllow
                    + " shouldGiveResult=" + givesResult);
            }
            if (player == null || !policyAllows || !conditionsAllow || !givesResult) {
                inventory.setResult(null);
                if (player != null) previews.remove(player.getUniqueId());
                return;
            }

            PreparedPreview prepared = prepare(recipe);
            if (prepared == null) {
                inventory.setResult(null);
                previews.remove(player.getUniqueId());
                return;
            }
            previews.put(player.getUniqueId(), prepared);
            if (plugin.getVanillaDebugManager().isActive(player)) {
                plugin.getVanillaDebugManager().trace(player, "C-PREVIEW objet construit recette="
                    + recipe.getId() + " valeur="
                    + plugin.getVanillaDebugManager().describeItem(prepared.output));
                plugin.getVanillaDebugManager().trace(player, "SET_RESULT recette=" + recipe.getId()
                    + " valeur=" + plugin.getVanillaDebugManager().describeItem(prepared.output));
            }
            inventory.setResult(prepared.output.clone());
            if (plugin.getVanillaDebugManager().isActive(player)) {
                plugin.getVanillaDebugManager().trace(player, "SET_RESULT relu="
                    + plugin.getVanillaDebugManager().describeItem(inventory.getResult()));
            }
            return;
        }

        if (player != null) previews.remove(player.getUniqueId());
        if (resolution.getKind() == VanillaRecipeResolver.Kind.BLOCKED_CUSTOM_INPUT
                || resolution.getKind() == VanillaRecipeResolver.Kind.AMBIGUOUS
                || isBlockedVanillaResult(inventory.getResult())) {
            inventory.setResult(null);
        }
    }

    private PreparedPreview prepare(CraftRecipe recipe) {
        CraftResult selected = recipe.getResult();
        ItemStack output = selected == null ? null : selected.createItem();
        return output == null ? null : new PreparedPreview(recipe.getId(), selected, output.clone());
    }

    private VanillaRecipeResolver.Resolution resolve(ItemStack[] matrix, Player player) {
        Collection<CraftRecipe> eligible = eligibleVanillaRecipes(player);
        VanillaRecipeResolver.Resolution resolution = VanillaRecipeResolver.resolve(
            eligible, matrix);
        if (resolution.getKind() == VanillaRecipeResolver.Kind.AMBIGUOUS) {
            String key = resolution.getAmbiguousRecipeIds().toString();
            if (loggedAmbiguities.add(key)) {
                plugin.getLogger().warning("Craft vanilla KCraft refuse car ambigu: " + key);
            }
        }
        return resolution;
    }

    private Collection<CraftRecipe> eligibleVanillaRecipes(Player player) {
        List<CraftRecipe> eligible = new ArrayList<CraftRecipe>();
        for (CraftRecipe recipe : plugin.getCraftManager().getAllRecipes().values()) {
            if (RecipeAccessPolicy.allowsVanilla(recipe)) eligible.add(recipe);
        }
        if (player != null && plugin.getVanillaDebugManager().isActive(player)) {
            eligible.add(plugin.getVanillaDebugManager().getInternalRecipe());
        }
        return eligible;
    }

    private void traceBefore(Player player, ItemStack[] matrix, ItemStack resultBefore,
                             String eventName) {
        plugin.getVanillaDebugManager().trace(player, eventName + " grille="
            + (matrix == null ? 0 : matrix.length) + " monde=" + player.getWorld().getName()
            + " table=WORKBENCH résultat-avant=" + plugin.getVanillaDebugManager().describeItem(resultBefore));
        if (matrix != null) {
            for (int slot = 0; slot < matrix.length; slot++) {
                plugin.getVanillaDebugManager().trace(player, "slot[" + slot + "]="
                    + plugin.getVanillaDebugManager().describeItem(matrix[slot]));
            }
        }
    }

    private void traceDecision(Player player, ItemStack[] matrix, ItemStack kcraftResult) {
        VanillaRecipeResolver.Resolution resolution = resolve(matrix, player);
        plugin.getVanillaDebugManager().trace(player, "résolution=" + resolution.getKind()
            + (resolution.getRecipe() == null ? "" : " recette=" + resolution.getRecipe().getId())
            + (resolution.getAmbiguousRecipeIds().isEmpty() ? "" : " ambiguës=" + resolution.getAmbiguousRecipeIds())
            + " résultat-KCraft=" + plugin.getVanillaDebugManager().describeItem(kcraftResult));
        int actualSlots = occupied(matrix);
        int reported = 0;
        for (CraftRecipe recipe : eligibleVanillaRecipes(player)) {
            if (expectedSlots(recipe) != actualSlots || !sharesMaterial(recipe, matrix)) continue;
            String source = VanillaDebugManager.INTERNAL_RECIPE_ID.equals(recipe.getId())
                ? "<interne-debug>" : plugin.getCraftManager().getRecipeSource(recipe.getId());
            String reason = CraftMatrixDiagnostics.describe(recipe, matrix);
            boolean policyAllows = RecipeAccessPolicy.allowsVanilla(recipe);
            boolean conditionsAllow = policyAllows && conditionsPass(player, recipe);
            if ("MATCH".equals(reason) && !conditionsAllow) {
                reason = "CONDITIONS_REFUSEES permission/monde/biome/plugin/faction";
            }
            plugin.getVanillaDebugManager().trace(player, "candidate=" + recipe.getId()
                + " source=" + source
                + " spécificité=" + VanillaRecipeResolver.specificity(recipe)
                + " allowsVanilla=" + policyAllows
                + " conditions=" + conditionsAllow
                + " shouldGiveResult=" + recipe.shouldGiveResult()
                + " => " + reason);
            if (++reported >= 30) break;
        }
        if (reported == 0) plugin.getVanillaDebugManager().trace(player,
            "candidate=<aucune avec le même nombre de slots et un matériau commun>");
    }

    private void traceResultClick(Player player, InventoryClickEvent event,
                                  CraftingInventory inventory,
                                  VanillaRecipeResolver.Resolution resolution) {
        if (!plugin.getVanillaDebugManager().isActive(player)) return;
        Recipe inventoryRecipe = inventory.getRecipe();
        Recipe eventRecipe = event instanceof CraftItemEvent
            ? ((CraftItemEvent) event).getRecipe() : null;
        plugin.getVanillaDebugManager().trace(player, "clic-résultat classe="
            + event.getClass().getName() + " rawSlot=" + event.getRawSlot()
            + " click=" + event.getClick() + " action=" + event.getAction()
            + " inventory.recipe=" + describeBukkitRecipe(inventoryRecipe)
            + " event.recipe=" + (event instanceof CraftItemEvent
                ? describeBukkitRecipe(eventRecipe) : "<non-CraftItemEvent>")
            + " résolution=" + resolution.getKind()
            + (resolution.getRecipe() == null ? ""
                : " recette-KCraft=" + resolution.getRecipe().getId())
            + " résultat-avant="
            + plugin.getVanillaDebugManager().describeItem(inventory.getResult()));
    }

    private static String describeBukkitRecipe(Recipe recipe) {
        if (recipe == null) return "<null>";
        ItemStack result = recipe.getResult();
        return recipe.getClass().getName() + "->"
            + (result == null ? "<null>" : result.getType() + ":" + result.getDurability());
    }

    private void scheduleUnknownHookCheck(final Player player,
                                          final CraftingInventory inventory) {
        if (!plugin.getVanillaDebugManager().isActive(player)) return;
        final long before = plugin.getVanillaDebugManager().getUnknownEventCount(player);
        plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
            @Override public void run() {
                if (!plugin.getVanillaDebugManager().isActive(player)) return;
                ItemStack[] matrix = inventory.getMatrix();
                if (matrix == null || matrix.length != 9) return;
                VanillaRecipeResolver.Resolution resolution = resolve(matrix, player);
                if (resolution.getKind() != VanillaRecipeResolver.Kind.CUSTOM
                        || inventory.getRecipe() != null
                        || plugin.getVanillaDebugManager().getUnknownEventCount(player) > before) return;
                if (plugin.getVanillaDebugManager().markMissingHookWarningIfFirst(player)) {
                    plugin.getVanillaDebugManager().trace(player,
                        "ERREUR: API présente mais hook serveur non déclenché pour une matrice "
                        + "KCraft inconnue; recette=" + resolution.getRecipe().getId());
                }
            }
        });
    }

    private static String describeLoader(ClassLoader loader) {
        return loader == null ? "<bootstrap>" : loader.getClass().getName()
            + "@" + Integer.toHexString(System.identityHashCode(loader));
    }

    private static long readTraceId(InventoryEvent event) {
        try {
            Object value = event.getClass().getMethod("getTraceId").invoke(event);
            return value instanceof Number ? ((Number) value).longValue() : 0L;
        } catch (Throwable unavailable) {
            return 0L;
        }
    }

    private static int occupied(ItemStack[] matrix) {
        int count = 0;
        if (matrix != null) for (ItemStack item : matrix) {
            if (ItemStackUtil.isPresent(item)) count++;
        }
        return count;
    }

    private static int expectedSlots(CraftRecipe recipe) {
        if (recipe.getType() == me.krunsh.kcraft.models.CraftType.SHAPELESS) {
            return recipe.getShapelessIngredients() == null ? 0 : recipe.getShapelessIngredients().size();
        }
        int count = 0;
        for (String row : recipe.getPattern()) if (row != null) {
            for (int i = 0; i < row.length(); i++) if (row.charAt(i) != ' ') count++;
        }
        return count;
    }

    private static boolean sharesMaterial(CraftRecipe recipe, ItemStack[] matrix) {
        if (matrix == null) return false;
        for (ItemStack item : matrix) {
            if (!ItemStackUtil.isPresent(item)) continue;
            if (recipe.getType() == me.krunsh.kcraft.models.CraftType.SHAPELESS) {
                for (CraftIngredient ingredient : recipe.getShapelessIngredients()) {
                    if (ingredient.getMaterial() == item.getType()) return true;
                }
            } else {
                for (CraftIngredient ingredient : recipe.getIngredients().values()) {
                    if (ingredient.getMaterial() == item.getType()) return true;
                }
            }
        }
        return false;
    }

    private static String replacement(ItemStack expected, ItemStack actual) {
        boolean same = expected == null ? actual == null : expected.equals(actual);
        return same ? "" : " [DIFFÉRENT DU RÉSULTAT KCRAFT: remplacement tardif probable]";
    }

    private boolean conditionsPass(Player player, CraftRecipe recipe) {
        return recipe.canCraft(player)
            && plugin.getHookManager().isPluginAvailable(recipe.getRequiredPlugin())
            && plugin.getHookManager().checkFactionLevel(player, recipe.getFactionLevelRequired());
    }

    private boolean isBlockedVanillaResult(ItemStack result) {
        return result != null && VanillaObtentionPolicy.isBlocked(result.getType(),
            plugin.getConfigManager().getBlockedVanillaCraftResults());
    }

    private static CraftOutputTransfer.Destination destination(ClickType click) {
        if (click == ClickType.SHIFT_LEFT || click == ClickType.SHIFT_RIGHT) {
            return CraftOutputTransfer.Destination.INVENTORY;
        }
        if (click == ClickType.NUMBER_KEY) return CraftOutputTransfer.Destination.HOTBAR;
        if (click == ClickType.LEFT || click == ClickType.RIGHT) {
            return CraftOutputTransfer.Destination.CURSOR;
        }
        return null;
    }

    private static ItemStack[] cloneItems(ItemStack[] source) {
        ItemStack[] clone = new ItemStack[source.length];
        for (int i = 0; i < source.length; i++) {
            clone[i] = source[i] == null ? null : source[i].clone();
        }
        return clone;
    }

    private static final class PreparedPreview {
        private final String recipeId;
        private final CraftResult result;
        private final ItemStack output;

        private PreparedPreview(String recipeId, CraftResult result, ItemStack output) {
            this.recipeId = recipeId;
            this.result = result;
            this.output = output;
        }
    }

    private static final class TraceObservation {
        private final CraftingInventory inventory;
        private final ItemStack kcraftResult;
        private final String eventName;
        private TraceObservation(CraftingInventory inventory, ItemStack kcraftResult,
                                 String eventName) {
            this.inventory = inventory;
            this.kcraftResult = kcraftResult;
            this.eventName = eventName;
        }
    }
}
