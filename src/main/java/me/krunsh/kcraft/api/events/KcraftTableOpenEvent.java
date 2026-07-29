package me.krunsh.kcraft.api.events;

import me.krunsh.kcraft.models.CraftTable;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Event appelé quand un joueur ouvre une table de craft
 * Peut être annulé pour empêcher l'ouverture
 */
public class KcraftTableOpenEvent extends Event implements Cancellable {
    
    private static final HandlerList HANDLERS = new HandlerList();
    
    private final Player player;
    private final CraftTable table;
    private boolean cancelled = false;
    
    public KcraftTableOpenEvent(Player player, CraftTable table) {
        this.player = player;
        this.table = table;
    }
    
    public Player getPlayer() {
        return player;
    }
    
    public CraftTable getTable() {
        return table;
    }
    
    public String getTableId() {
        return table.getId();
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