package me.krunsh.kcraft.api.events;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import me.krunsh.kcraft.api.execution.CraftBatchResult;
import me.krunsh.kcraft.api.execution.CraftExecutionSource;
import me.krunsh.kcraft.models.CraftRecipe;

/**
 * Event agrégé V2.6 émis UNE FOIS à la fin d'un batch.
 *
 * Pendant la transition, les KcraftPostCraftEvent unitaires sont encore émis
 * pour les plugins legacy (dont Kjobs actuel).
 */
public final class KcraftBatchCraftEvent
        extends Event {

    private static final HandlerList HANDLERS =
        new HandlerList();

    private final Player player;
    private final CraftRecipe recipe;
    private final UUID transactionId;
    private final CraftExecutionSource executionSource;

    private final int plannedCrafts;
    private final int attemptedCrafts;
    private final int successfulCrafts;
    private final int failedCrafts;
    private final int totalResultItems;

    private final boolean cancelled;
    private final List<CraftBatchResult> results;

    public KcraftBatchCraftEvent(
            Player player,
            CraftRecipe recipe,
            UUID transactionId,
            CraftExecutionSource executionSource,
            int plannedCrafts,
            int attemptedCrafts,
            int successfulCrafts,
            int failedCrafts,
            int totalResultItems,
            boolean cancelled,
            List<CraftBatchResult> results) {

        this.player = player;
        this.recipe = recipe;
        this.transactionId = transactionId;

        this.executionSource =
            executionSource == null
                ? CraftExecutionSource.UNKNOWN
                : executionSource;

        this.plannedCrafts =
            Math.max(0, plannedCrafts);

        this.attemptedCrafts =
            Math.max(0, attemptedCrafts);

        this.successfulCrafts =
            Math.max(0, successfulCrafts);

        this.failedCrafts =
            Math.max(0, failedCrafts);

        this.totalResultItems =
            Math.max(0, totalResultItems);

        this.cancelled = cancelled;

        this.results =
            results == null
                ? Collections.<CraftBatchResult>emptyList()
                : Collections.unmodifiableList(
                    new ArrayList<CraftBatchResult>(
                        results
                    )
                );
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

    public UUID getTransactionId() {
        return transactionId;
    }

    public CraftExecutionSource getExecutionSource() {
        return executionSource;
    }

    public int getPlannedCrafts() {
        return plannedCrafts;
    }

    public int getAttemptedCrafts() {
        return attemptedCrafts;
    }

    public int getSuccessfulCrafts() {
        return successfulCrafts;
    }

    public int getFailedCrafts() {
        return failedCrafts;
    }

    public int getTotalResultItems() {
        return totalResultItems;
    }

    public boolean wasCancelled() {
        return cancelled;
    }

    public boolean isFullySuccessful() {
        return !cancelled
            && failedCrafts == 0
            && successfulCrafts == plannedCrafts;
    }

    public List<CraftBatchResult> getResults() {
        return results;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
