package me.krunsh.kcraft.listeners;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.InventoryView;
import org.junit.Test;
import me.krunsh.kcraft.Kcraft;

public class VanillaReloadPreviewTest {
    @Test public void invalidatesOldPreviewAndClosesOnlyItsCraftingView() throws Exception {
        Kcraft plugin = mock(Kcraft.class);
        Server server = mock(Server.class);
        when(plugin.getServer()).thenReturn(server);
        VanillaCraftListener listener = new VanillaCraftListener(plugin);
        Map<UUID, Object> previews = previews(listener);
        UUID id = UUID.randomUUID();
        previews.put(id, null); // Only the ownership key is read during invalidation.
        Player player = mock(Player.class);
        InventoryView view = mock(InventoryView.class);
        CraftingInventory inventory = mock(CraftingInventory.class);
        when(server.getPlayer(id)).thenReturn(player);
        when(player.getOpenInventory()).thenReturn(view);
        when(view.getTopInventory()).thenReturn(inventory);
        listener.refreshAfterReload();
        assertTrue(previews.isEmpty());
        verify(inventory).setResult(null);
        verify(player).closeInventory();
        listener.refreshAfterReload();
        verify(player, times(1)).closeInventory();
    }

    @Test public void onePlayerFailureDoesNotSkipTheNextPlayer() throws Exception {
        Kcraft plugin = mock(Kcraft.class);
        Server server = mock(Server.class);
        Logger logger = Logger.getAnonymousLogger();
        logger.setLevel(Level.OFF);
        when(plugin.getServer()).thenReturn(server);
        when(plugin.getLogger()).thenReturn(logger);
        VanillaCraftListener listener = new VanillaCraftListener(plugin);
        Map<UUID, Object> previews = previews(listener);
        previews.put(UUID.randomUUID(), null);
        previews.put(UUID.randomUUID(), null);
        java.util.Iterator<UUID> ids = previews.keySet().iterator();
        UUID first = ids.next();
        UUID second = ids.next();
        when(server.getPlayer(first)).thenThrow(new IllegalStateException("player failure"));
        Player player = mock(Player.class);
        InventoryView view = mock(InventoryView.class);
        CraftingInventory inventory = mock(CraftingInventory.class);
        when(server.getPlayer(second)).thenReturn(player);
        when(player.getOpenInventory()).thenReturn(view);
        when(view.getTopInventory()).thenReturn(inventory);
        listener.refreshAfterReload();
        assertTrue(previews.isEmpty());
        verify(player).closeInventory();
    }

    @SuppressWarnings("unchecked")
    private Map<UUID, Object> previews(VanillaCraftListener listener) throws Exception {
        Field field = VanillaCraftListener.class.getDeclaredField("previews");
        field.setAccessible(true);
        return (Map<UUID, Object>) field.get(listener);
    }
}
