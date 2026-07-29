package me.krunsh.kcraft.api.events;

import me.krunsh.kcraft.models.CraftRecipe;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

/**
 * Event appelé APRÈS qu'un craft soit exécuté
 * Event informatif seulement (non annulable)
 */
public class KcraftPostCraftEvent extends Event {
    
    private static final HandlerList HANDLERS = new HandlerList();
    
    private final Player player;
    private final CraftRecipe recipe;
    private final ItemStack result;
    private final boolean success;
    private final boolean wasForced;
    
    public KcraftPostCraftEvent(Player player, CraftRecipe recipe, ItemStack result, boolean success, boolean wasForced) {
        this.player = player;
        this.recipe = recipe;
        this.result = result;
        this.success = success;
        this.wasForced = wasForced;
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
    
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
    
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}