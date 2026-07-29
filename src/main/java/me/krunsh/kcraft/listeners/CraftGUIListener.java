package me.krunsh.kcraft.listeners;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

import me.krunsh.kcraft.Kcraft;
import me.krunsh.kcraft.api.events.KcraftPreCraftEvent;
import me.krunsh.kcraft.gui.CraftTableGUI;
import me.krunsh.kcraft.models.CraftIngredient;
import me.krunsh.kcraft.models.CraftMatrixTransaction;
import me.krunsh.kcraft.models.CraftRecipe;
import me.krunsh.kcraft.models.CraftResult;
import me.krunsh.kcraft.models.CraftType;
import me.krunsh.kcraft.utils.MessageUtil;

/**
 * Listener pour les interactions avec les GUI de craft
 */
public class CraftGUIListener implements Listener {
    
    private final Kcraft plugin;
    private final PlayerGuiRegistry<CraftTableGUI> activeGUIs;
    
    public CraftGUIListener(Kcraft plugin) {
        this.plugin = plugin;
        this.activeGUIs = new PlayerGuiRegistry<CraftTableGUI>();
    }
    
    /**
     * Enregistre une GUI active pour un joueur
     */
    public void registerGUI(Player player, CraftTableGUI gui) {
        UUID playerId = player.getUniqueId();
        cleanupPlayer(playerId, player, true);
        activeGUIs.put(playerId, gui);
    }
    
    /**
     * Supprime une GUI active pour un joueur
     */
    public void unregisterGUI(Player player) {
        cleanupPlayer(player.getUniqueId(), player, false);
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        CraftTableGUI gui = activeGUIs.get(player.getUniqueId());
        
        if (gui == null) {
            return;
        }
        
        int slot = event.getRawSlot();
        
        // Si c'est dans la GUI Kcraft
        if (slot >= 0 && slot < gui.getInventory().getSize()) {
            
            // Bouton de craft - annuler et gérer manuellement
            if (gui.isCraftButton(slot)) {
                event.setCancelled(true);
                if (event.isLeftClick()) {
                    if (event.isShiftClick()) {
                        handleMassCraft(player, gui);
                    } else {
                        handleCraftClick(player, gui);
                    }
                }
                return;
            }
            
            // Slot de résultat - lecture seule
            if (gui.isResultSlot(slot)) {
                event.setCancelled(true);
                return;
            }
            
            // Slots de craft - laisser Bukkit gérer normalement
            if (gui.isModifiableSlot(slot)) {
                // NE PAS ANNULER - laisser Bukkit gérer les quantités
                // Juste programmer une mise à jour après
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    updateCraftResult(gui);
                }, 1L);
                return;
            }
            
            // Slots de bordure/décoration - protégés
            event.setCancelled(true);
            
        } else {
            // Clic dans l'inventaire du joueur - autoriser et mettre à jour
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                updateCraftResult(gui);
            }, 1L);
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        CraftTableGUI gui = activeGUIs.get(player.getUniqueId());
        
        if (gui == null) {
            return;
        }
        
        // Vérifier si le drag affecte des slots protégés
        boolean hasProtectedSlot = false;
        for (int slot : event.getRawSlots()) {
            if (slot >= 0 && slot < gui.getInventory().getSize()) {
                if (!gui.isModifiableSlot(slot)) {
                    hasProtectedSlot = true;
                    break;
                }
            }
        }
        
        if (hasProtectedSlot) {
            event.setCancelled(true);
            return;
        }
        
        // Si autorisé, programmer une mise à jour
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            updateCraftResult(gui);
        }, 1L);
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getPlayer();
        CraftTableGUI gui = activeGUIs.get(player.getUniqueId());
        if (gui == null || event.getInventory() != gui.getInventory()) return;

        cleanupPlayer(player.getUniqueId(), player, true);
        MessageUtil.debug("GUI fermée pour " + player.getName(), 2);
    }

    @EventHandler
    public void onPlayerKick(PlayerKickEvent event) {
        cleanupPlayer(event.getPlayer().getUniqueId(), event.getPlayer(), true);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        cleanupPlayer(event.getPlayer().getUniqueId(), event.getPlayer(), true);
    }

    /** Nettoie toutes les sessions encore actives avant la désactivation. */
    public void shutdown() {
        for (UUID playerId : activeGUIs.playerIds()) {
            Player player = plugin.getServer().getPlayer(playerId);
            cleanupPlayer(playerId, player, player != null);
            if (player != null) {
                try {
                    player.closeInventory();
                } catch (RuntimeException error) {
                    plugin.getLogger().warning("Impossible de fermer la GUI KCraft de "
                        + player.getName() + ": " + error.getMessage());
                }
            }
        }
        activeGUIs.clear();
    }

    private void cleanupPlayer(final UUID playerId, final Player player,
                               final boolean returnIngredients) {
        try {
            activeGUIs.cleanup(playerId, new PlayerGuiRegistry.Cleanup<CraftTableGUI>() {
                @Override
                public void run(CraftTableGUI gui) {
                    if (returnIngredients && player != null) returnIngredients(player, gui);
                }
            });
        } catch (RuntimeException error) {
            String name = player == null ? playerId.toString() : player.getName();
            plugin.getLogger().warning("Erreur pendant le nettoyage de la GUI KCraft de "
                + name + ": " + error.getMessage());
        }
    }

    private void returnIngredients(Player player, CraftTableGUI gui) {
        ItemStack[] matrix = gui.getCraftMatrix();
        int[] craftSlots = gui.getCraftSlots();
        for (int index = 0; index < matrix.length; index++) {
            ItemStack item = matrix[index];
            if (item == null || item.getType() == org.bukkit.Material.AIR || item.getAmount() <= 0) continue;

            boolean returned = false;
            try {
                HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item.clone());
                for (ItemStack drop : leftover.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), drop);
                }
                returned = true;
            } catch (RuntimeException error) {
                plugin.getLogger().warning("Impossible de restituer un ingrédient KCraft à "
                    + player.getName() + ": " + error.getMessage());
            } finally {
                if (returned && index < craftSlots.length) {
                    gui.getInventory().setItem(craftSlots[index], null);
                }
            }
        }
    }
    
    /**
     * Gère le clic sur le bouton de craft
     */
    private void handleCraftClick(Player player, CraftTableGUI gui) {
        try {
            ItemStack[] matrix = gui.getCraftMatrix();
            
            // Trouver une recette correspondante
            CraftRecipe recipe = plugin.getCraftManager().findMatchingRecipe(matrix, gui.getTable());
            
            if (recipe == null) {
                MessageUtil.sendError(player, "craft.no-recipe");
                return;
            }
            
            // Vérifier les permissions et conditions
            if (!recipe.canCraft(player)) {
                MessageUtil.sendError(player, "craft.no-permission");
                return;
            }
            
            // Vérifier les hooks nécessaires
            if (!plugin.getHookManager().isPluginAvailable(recipe.getRequiredPlugin())) {
                MessageUtil.sendError(player, "craft.plugin-required");
                return;
            }

            // Vérifier le niveau de faction requis
            if (!plugin.getHookManager().checkFactionLevel(player, recipe.getFactionLevelRequired())) {
                MessageUtil.sendError(player, "craft.faction-level-low",
                    MessageUtil.placeholders("level", String.valueOf(recipe.getFactionLevelRequired())));
                return;
            }
            
            ItemStack[] consumedMatrix = CraftMatrixTransaction.consumeOnce(recipe, matrix);
            if (consumedMatrix == null) {
                MessageUtil.sendError(player, "craft.failed");
                updateCraftResult(gui);
                return;
            }

            CraftResult selectedResult = recipe.shouldGiveResult() ? recipe.getResult() : null;
            if (recipe.shouldGiveResult() && selectedResult == null) {
                MessageUtil.sendError(player, "craft.failed");
                updateCraftResult(gui);
                return;
            }

            KcraftPreCraftEvent preEvent = new KcraftPreCraftEvent(player, recipe, false);
            plugin.getServer().getPluginManager().callEvent(preEvent);
            if (preEvent.isCancelled()) {
                updateCraftResult(gui);
                return;
            }

            boolean success = plugin.getCraftManager()
                    .executeCraft(player, recipe, matrix, selectedResult);
            
            if (success) {
                applyMatrix(gui, consumedMatrix);
                
                // Donner le résultat (sauf si consume-result: false)
                if (selectedResult != null) giveResult(player, selectedResult.createItem());
                
                // Messages et sons
                MessageUtil.sendSuccess(player, "craft.success", 
                    MessageUtil.placeholders("item", recipe.getId()));
                
                // Son de succes (fallback sur soundOpen si pas de son dedie a la recette)
                if (plugin.getConfigManager().areSoundsEnabled() && gui.getTable().getSoundOpen() != null) {
                    me.krunsh.kcraft.utils.SoundUtil.playSafe(player, gui.getTable().getSoundOpen(), 1.0f, 1.4f);
                }
                
                // Mettre à jour l'affichage
                updateCraftResult(gui);
                
            } else {
                // Si la config indique que les items sont perdus en cas d'échec,
                // on consomme les ingrédients même lorsque le craft rate.
                if (!recipe.isReturnItemsOnFail()) {
                    applyMatrix(gui, consumedMatrix);
                    updateCraftResult(gui);
                }
                MessageUtil.sendError(player, "craft.failed");
            }
            
        } catch (Exception e) {
            plugin.getLogger().warning("Erreur lors du craft par " + player.getName() + ": " + e.getMessage());
            MessageUtil.sendError(player, "craft.error");
        }
    }
    
    /**
     * Gère le craft en masse (shift+clic)
     */
    private void handleMassCraft(Player player, CraftTableGUI gui) {
        try {
            ItemStack[] matrix = gui.getCraftMatrix();
            
            // Trouver une recette correspondante
            CraftRecipe recipe = plugin.getCraftManager().findMatchingRecipe(matrix, gui.getTable());
            
            if (recipe == null) {
                MessageUtil.sendError(player, "craft.no-recipe");
                return;
            }
            
            // Vérifier les permissions et conditions
            if (!recipe.canCraft(player)) {
                MessageUtil.sendError(player, "craft.no-permission");
                return;
            }

            if (!plugin.getHookManager().isPluginAvailable(recipe.getRequiredPlugin())) {
                MessageUtil.sendError(player, "craft.plugin-required");
                return;
            }
            
            // Vérifier le niveau de faction requis
            if (!plugin.getHookManager().checkFactionLevel(player, recipe.getFactionLevelRequired())) {
                MessageUtil.sendError(player, "craft.faction-level-low",
                    MessageUtil.placeholders("level", String.valueOf(recipe.getFactionLevelRequired())));
                return;
            }

            // Limiter selon la config
            int maxCrafts = plugin.getConfigManager().getConfig().getInt("Kcraft.optimizations.batch-craft.max-batch", 64);
            if (maxCrafts <= 0) {
                maxCrafts = 64;
            }
            
            int successfulCrafts = 0;
            
            // Boucle de craft
            for (int i = 0; i < maxCrafts; i++) {
                // Revalider la recette à chaque itération pour éviter tout exploit
                // quand la grille change après consommation.
                ItemStack[] currentMatrix = gui.getCraftMatrix();
                CraftRecipe currentRecipe = plugin.getCraftManager().findMatchingRecipe(currentMatrix, gui.getTable());
                if (currentRecipe == null || !currentRecipe.getId().equals(recipe.getId())) {
                    break;
                }

                if (!plugin.getHookManager().isPluginAvailable(currentRecipe.getRequiredPlugin())) {
                    break;
                }

                if (!plugin.getHookManager().checkFactionLevel(player, currentRecipe.getFactionLevelRequired())) {
                    break;
                }

                if (!currentRecipe.canCraft(player)) break;

                ItemStack[] consumedMatrix = CraftMatrixTransaction.consumeOnce(currentRecipe, currentMatrix);
                if (consumedMatrix == null) break;
                CraftResult selectedResult = currentRecipe.shouldGiveResult()
                        ? currentRecipe.getResult() : null;
                if (currentRecipe.shouldGiveResult() && selectedResult == null) break;

                KcraftPreCraftEvent preEvent = new KcraftPreCraftEvent(player, currentRecipe, false);
                plugin.getServer().getPluginManager().callEvent(preEvent);
                if (preEvent.isCancelled()) break;
                
                // Exécuter un craft
                boolean success = plugin.getCraftManager()
                        .executeCraft(player, currentRecipe, currentMatrix, selectedResult);
                
                if (success) {
                    applyMatrix(gui, consumedMatrix);
                    
                    // Donner le résultat (sauf si consume-result: false)
                    if (selectedResult != null) giveResult(player, selectedResult.createItem());
                    
                    successfulCrafts++;
                } else {
                    if (!currentRecipe.isReturnItemsOnFail()) {
                        applyMatrix(gui, consumedMatrix);
                    }
                    break; // Arrêter sur premier échec
                }
            }
            
            // Message de résultat
            if (successfulCrafts > 0) {
                MessageUtil.sendSuccess(player, "craft.mass-success", 
                    MessageUtil.placeholders("amount", String.valueOf(successfulCrafts), "item", recipe.getId()));
                    
                // Mettre à jour l'affichage
                updateCraftResult(gui);
            } else {
                MessageUtil.sendError(player, "craft.mass-failed");
            }
            
        } catch (Exception e) {
            plugin.getLogger().warning("Erreur lors du craft en masse par " + player.getName() + ": " + e.getMessage());
            MessageUtil.sendError(player, "craft.error");
        }
    }

    private void applyMatrix(CraftTableGUI gui, ItemStack[] matrix) {
        int[] slots = gui.getCraftSlots();
        for (int i = 0; i < slots.length && i < matrix.length; i++) {
            gui.getInventory().setItem(slots[i], matrix[i]);
        }
    }

    private void giveResult(Player player, ItemStack result) {
        if (result == null || result.getType() == org.bukkit.Material.AIR) return;
        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(result);
        for (ItemStack item : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), item);
        }
    }
    
    /**
     * Consomme les ingrédients après un craft réussi
     */
    private void consumeIngredients(CraftTableGUI gui, CraftRecipe recipe) {
        if (recipe.getType() == CraftType.SHAPELESS) {
            consumeShapeless(gui, recipe);
            return;
        }

        consumeShaped(gui, recipe);
    }

    private void consumeShaped(CraftTableGUI gui, CraftRecipe recipe) {
        ItemStack[] matrix = gui.getCraftMatrix();
        List<String> pattern = recipe.getPattern();

        if (pattern == null || pattern.isEmpty()) {
            return;
        }

        int tableSize = (int) Math.sqrt(matrix.length);
        int patternRows = pattern.size();
        int patternCols = pattern.get(0).length();

        int[] origin = findPatternOrigin(matrix, pattern, recipe.getIngredients(), tableSize, patternRows, patternCols);
        if (origin == null) {
            return;
        }

        int startRow = origin[0];
        int startCol = origin[1];

        for (int row = 0; row < patternRows; row++) {
            String patternRow = pattern.get(row);
            for (int col = 0; col < patternCols; col++) {
                char symbol = patternRow.charAt(col);
                if (symbol == ' ') {
                    continue;
                }

                CraftIngredient ingredient = recipe.getIngredients().get(symbol);
                if (ingredient == null) {
                    continue;
                }

                int matrixIndex = (startRow + row) * tableSize + (startCol + col);
                if (matrixIndex < 0 || matrixIndex >= matrix.length) {
                    continue;
                }

                ItemStack item = matrix[matrixIndex];
                if (item == null) {
                    continue;
                }

                int newAmount = item.getAmount() - ingredient.getAmount();
                if (newAmount <= 0) {
                    gui.getInventory().setItem(gui.getCraftSlots()[matrixIndex], null);
                } else {
                    item.setAmount(newAmount);
                    gui.getInventory().setItem(gui.getCraftSlots()[matrixIndex], item);
                }
            }
        }
    }

    private void consumeShapeless(CraftTableGUI gui, CraftRecipe recipe) {
        ItemStack[] matrix = gui.getCraftMatrix();
        List<CraftIngredient> remaining = new ArrayList<>(recipe.getShapelessIngredients());

        for (int i = 0; i < matrix.length && !remaining.isEmpty(); i++) {
            ItemStack item = matrix[i];
            if (item == null || item.getType() == org.bukkit.Material.AIR) {
                continue;
            }

            for (int j = 0; j < remaining.size(); j++) {
                CraftIngredient ingredient = remaining.get(j);
                if (ingredient.matches(item)) {
                    int newAmount = item.getAmount() - ingredient.getAmount();
                    if (newAmount <= 0) {
                        gui.getInventory().setItem(gui.getCraftSlots()[i], null);
                    } else {
                        item.setAmount(newAmount);
                        gui.getInventory().setItem(gui.getCraftSlots()[i], item);
                    }
                    remaining.remove(j);
                    break;
                }
            }
        }
    }

    private int[] findPatternOrigin(ItemStack[] matrix, List<String> pattern, Map<Character, CraftIngredient> ingredients,
                                    int tableSize, int patternRows, int patternCols) {
        for (int startRow = 0; startRow <= tableSize - patternRows; startRow++) {
            for (int startCol = 0; startCol <= tableSize - patternCols; startCol++) {
                if (matchesPatternAtOrigin(matrix, pattern, ingredients, startRow, startCol, tableSize) &&
                    areOtherSlotsEmpty(matrix, pattern, startRow, startCol, tableSize)) {
                    return new int[]{startRow, startCol};
                }
            }
        }
        return null;
    }

    private boolean matchesPatternAtOrigin(ItemStack[] matrix, List<String> pattern, Map<Character, CraftIngredient> ingredients,
                                           int startRow, int startCol, int tableSize) {
        for (int row = 0; row < pattern.size(); row++) {
            String patternRow = pattern.get(row);
            for (int col = 0; col < patternRow.length(); col++) {
                char symbol = patternRow.charAt(col);
                int matrixIndex = (startRow + row) * tableSize + (startCol + col);
                if (matrixIndex >= matrix.length) {
                    return false;
                }

                ItemStack item = matrix[matrixIndex];
                if (symbol == ' ') {
                    if (item != null && item.getType() != org.bukkit.Material.AIR) {
                        return false;
                    }
                    continue;
                }

                CraftIngredient ingredient = ingredients.get(symbol);
                if (ingredient == null || item == null || !ingredient.matches(item)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean areOtherSlotsEmpty(ItemStack[] matrix, List<String> pattern,
                                       int startRow, int startCol, int tableSize) {
        for (int row = 0; row < tableSize; row++) {
            for (int col = 0; col < tableSize; col++) {
                int matrixIndex = row * tableSize + col;

                boolean isInPattern = row >= startRow && row < startRow + pattern.size() &&
                                      col >= startCol && col < startCol + pattern.get(0).length();

                if (!isInPattern) {
                    ItemStack item = matrix[matrixIndex];
                    if (item != null && item.getType() != org.bukkit.Material.AIR) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
    
    /**
     * Met à jour le résultat affiché
     */
    private void updateCraftResult(CraftTableGUI gui) {
        try {
            ItemStack[] matrix = gui.getCraftMatrix();
            
            // D'abord chercher dans les crafts custom
            CraftRecipe customRecipe = plugin.getCraftManager().findMatchingRecipe(matrix, gui.getTable());
            
            if (customRecipe != null) {
                Player player = gui.getPlayer();

                if (player == null || !player.isOnline()) {
                    gui.updateResult(null);
                    return;
                }

                if (!customRecipe.canCraft(player) ||
                    !plugin.getHookManager().isPluginAvailable(customRecipe.getRequiredPlugin()) ||
                    !plugin.getHookManager().checkFactionLevel(player, customRecipe.getFactionLevelRequired())) {
                    gui.updateResult(null);
                    return;
                }

                CraftResult previewResult = customRecipe.shouldGiveResult()
                        ? customRecipe.getResult() : null;
                gui.updateResult(previewResult == null ? null : previewResult.createItem());
                return;
            }
            
            // Si pas de craft custom, chercher dans les crafts vanilla
            ItemStack vanillaResult = findVanillaCraftResult(matrix);
            if (vanillaResult != null) {
                gui.updateResult(vanillaResult);
                return;
            }
            
            // Aucun résultat
            gui.updateResult(null);
            
        } catch (Exception e) {
            plugin.getLogger().warning("Erreur lors de la mise à jour du résultat: " + e.getMessage());
        }
    }
    
    /**
     * Trouve le résultat d'un craft vanilla (simulation)
     * Note: En 1.8.8, on ne peut pas accéder facilement aux recettes vanilla
     * Cette méthode peut être étendue pour supporter les crafts les plus courants
     */
    private ItemStack findVanillaCraftResult(ItemStack[] matrix) {
        // Pour l'instant, retourner null
        // TODO: Implémenter détection des crafts vanilla communs si nécessaire
        // Par exemple: planches, sticks, torches, etc.
        
        return null;
    }
}

/** Registre des GUI actives indexé exclusivement par UUID. */
final class PlayerGuiRegistry<T> {
    interface Cleanup<T> {
        void run(T value);
    }

    private final Map<UUID, T> values = new HashMap<UUID, T>();

    T put(UUID playerId, T value) {
        if (playerId == null || value == null) throw new IllegalArgumentException("playerId/value");
        return values.put(playerId, value);
    }

    T get(UUID playerId) {
        return values.get(playerId);
    }

    void cleanup(UUID playerId, Cleanup<T> cleanup) {
        T value = values.remove(playerId);
        if (value == null) return;
        try {
            cleanup.run(value);
        } finally {
            // Ne supprime jamais une nouvelle session créée pendant le nettoyage.
            if (values.get(playerId) == value) values.remove(playerId);
        }
    }

    List<UUID> playerIds() {
        return new ArrayList<UUID>(values.keySet());
    }

    int size() {
        return values.size();
    }

    void clear() {
        values.clear();
    }
}
