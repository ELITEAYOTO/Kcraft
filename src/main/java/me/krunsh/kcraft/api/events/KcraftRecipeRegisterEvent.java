package me.krunsh.kcraft.api.events;

import me.krunsh.kcraft.models.CraftRecipe;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Event appelé quand une nouvelle recette est enregistrée
 * Event informatif pour les add-ons
 */
public class KcraftRecipeRegisterEvent extends Event {
    
    private static final HandlerList HANDLERS = new HandlerList();
    
    private final CraftRecipe recipe;
    
    public KcraftRecipeRegisterEvent(CraftRecipe recipe) {
        this.recipe = recipe;
    }
    
    public CraftRecipe getRecipe() {
        return recipe;
    }
    
    public String getCraftId() {
        return recipe.getId();
    }
    
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
    
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}