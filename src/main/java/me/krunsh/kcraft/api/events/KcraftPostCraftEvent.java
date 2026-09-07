package me.krunsh.kcraft.api.events;

import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

import me.krunsh.kcraft.api.execution.CraftExecutionContext;
import me.krunsh.kcraft.api.execution.CraftExecutionSource;
import me.krunsh.kcraft.models.CraftRecipe;

/**
 * Event legacy unitaire, enrichi en V2.6.
 *
 * Compatibilité binaire/source :
 * l'ancien constructeur 5 arguments est conservé.
 *
 * Les nouveaux consommateurs peuvent utiliser source/transactionId pour
 * distinguer les events unitaires d'un SHIFT_BATCH du nouvel event agrégé.
 */
public class KcraftPostCraftEvent extends Event {

    private static final HandlerList HANDLERS =
        new HandlerList();

    private final Player player;
    private final CraftRecipe recipe;
    private final ItemStack result;
    private final boolean success;
    private final boolean wasForced;

    private final CraftExecutionSource executionSource;
    private final UUID transactionId;
    private final int batchIndex;
    private final int plannedBatchSize;

    public KcraftPostCraftEvent(
            Player player,
            CraftRecipe recipe,
            ItemStack result,
            boolean success,
            boolean wasForced) {

        this(
            player,
            recipe,
            result,
            success,
            wasForced,
            CraftExecutionContext.snapshot()
        );
    }

    private KcraftPostCraftEvent(
            Player player,
            CraftRecipe recipe,
            ItemStack result,
            boolean success,
            boolean wasForced,
            CraftExecutionContext.Snapshot context) {

        this(
            player,
            recipe,
            result,
            success,
            wasForced,
            context.getSource(),
            context.getTransactionId(),
            context.getBatchIndex(),
            context.getPlannedBatchSize()
        );
    }

    public KcraftPostCraftEvent(
            Player player,
            CraftRecipe recipe,
            ItemStack result,
            boolean success,
            boolean wasForced,
            CraftExecutionSource executionSource,
            UUID transactionId,
            int batchIndex,
            int plannedBatchSize) {

        this.player = player;
        this.recipe = recipe;
        this.result = result;
        this.success = success;
        this.wasForced = wasForced;

        this.executionSource =
            executionSource == null
                ? CraftExecutionSource.UNKNOWN
                : executionSource;

        this.transactionId = transactionId;
        this.batchIndex = batchIndex;
        this.plannedBatchSize =
            Math.max(1, plannedBatchSize);
    }

    public Player getPlayer() {
        return player;
    }

    public CraftRecipe getRecipe() {
        return recipe;
    }

    public String getCraftId() {
        return recipe.getId();
    }

    public ItemStack getResult() {
        return result;
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean wasForced() {
        return wasForced;
    }

    public CraftExecutionSource getExecutionSource() {
        return executionSource;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    /**
     * Index 0-based dans le batch, -1 hors batch/contexte inconnu.
     */
    public int getBatchIndex() {
        return batchIndex;
    }

    public int getPlannedBatchSize() {
        return plannedBatchSize;
    }

    /**
     * true pour les events unitaires générés pendant un SHIFT_BATCH.
     *
     * Un consommateur migré V2 peut les ignorer et écouter
     * KcraftBatchCraftEvent à la place.
     */
    public boolean isLegacyBatchElement() {
        return executionSource
            == CraftExecutionSource.SHIFT_BATCH;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
