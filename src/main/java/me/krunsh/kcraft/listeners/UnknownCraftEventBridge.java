package me.krunsh.kcraft.listeners;

import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.plugin.EventExecutor;

import me.krunsh.kcraft.Kcraft;

/** Runtime adapter for the optional KHopeSpigot unknown-craft extension. */
final class UnknownCraftEventBridge {

    static final String EVENT_CLASS =
        "com.hpfxd.pandaspigot.event.inventory.PrepareUnknownCraftEvent";

    private UnknownCraftEventBridge() {
    }

    @SuppressWarnings("unchecked")
    static boolean register(final Kcraft plugin, final VanillaCraftListener listener) {
        try {
            ClassLoader serverLoader = plugin.getServer().getClass().getClassLoader();
            Class<?> rawClass = Class.forName(EVENT_CLASS, false, serverLoader);
            if (!Event.class.isAssignableFrom(rawClass)
                    || !InventoryEvent.class.isAssignableFrom(rawClass)) {
                plugin.getLogger().severe("Extension de craft KHopeSpigot incompatible: "
                    + EVENT_CLASS + " n'est pas un InventoryEvent.");
                return false;
            }
            plugin.getVanillaDebugManager().markUnknownApiDetected();

            Class<? extends Event> eventClass = (Class<? extends Event>) rawClass;
            plugin.getServer().getPluginManager().registerEvent(eventClass, listener,
                EventPriority.HIGHEST, new EventExecutor() {
                    @Override
                    public void execute(Listener ignored, Event event) throws EventException {
                        listener.onPrepareUnknownCraft((InventoryEvent) event);
                    }
                }, plugin, false);
            plugin.getServer().getPluginManager().registerEvent(eventClass, listener,
                EventPriority.MONITOR, new EventExecutor() {
                    @Override
                    public void execute(Listener ignored, Event event) throws EventException {
                        listener.observeUnknownCraft((InventoryEvent) event);
                    }
                }, plugin, false);
            plugin.getVanillaDebugManager().markUnknownListenerRegistered();
            return true;
        } catch (ClassNotFoundException unavailable) {
            plugin.getLogger().warning("Extension de craft KHopeSpigot absente: les recettes "
                + "KCraft sans recette vanilla correspondante ne peuvent pas afficher de resultat.");
            return false;
        } catch (Throwable failure) {
            plugin.getLogger().severe("Impossible d'enregistrer l'extension de craft KHopeSpigot: "
                + failure.getClass().getSimpleName() + ": " + failure.getMessage());
            return false;
        }
    }
}
