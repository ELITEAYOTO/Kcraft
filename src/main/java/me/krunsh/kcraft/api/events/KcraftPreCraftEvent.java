package me.krunsh.kcraft.api.events;

import me.krunsh.kcraft.models.CraftRecipe;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Event appelé AVANT qu'un craft soit exécuté
 * Peut être annulé pour empêcher le craft
 */
public class KcraftPreCraftEvent extends Event implements Cancellable {
    
    private static final HandlerList HANDLERS = new HandlerList();
    
    private final Player player;
    private final CraftRecipe recipe;
    private final boolean isForced;
    private boolean cancelled = false;
    
    public KcraftPreCraftEvent(Player player, CraftRecipe recipe, boolean isForced) {
        this.player = player;
        this.recipe = recipe;
        this.isForced = isForced;
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
    
    public boolean isForced() {
        return isForced;
    }
    
    @Override
    public boolean isCancelled() {
        return cancelled;
    }
    
    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
    
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
    
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}