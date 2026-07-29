package me.krunsh.kcraft.listeners;

import java.util.Set;

import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkPopulateEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import me.krunsh.kcraft.Kcraft;
import me.krunsh.kcraft.models.VanillaObtentionPolicy;

/** Retire uniquement le loot interdit lors de la population initiale d'un chunk. */
public final class GeneratedLootRestrictionListener implements Listener {

    private final Kcraft plugin;

    public GeneratedLootRestrictionListener(Kcraft plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkPopulate(ChunkPopulateEvent event) {
        if (!plugin.getConfigManager().shouldCleanGeneratedLoot()) return;
        Set<Material> blocked = plugin.getConfigManager().getBlockedGeneratedLoot();
        if (blocked.isEmpty()) return;

        int removed = 0;
        for (BlockState state : event.getChunk().getTileEntities()) {
            if (!(state instanceof InventoryHolder)) continue;
            Inventory inventory = ((InventoryHolder) state).getInventory();
            if (inventory == null) continue;
            for (int slot = 0; slot < inventory.getSize(); slot++) {
                ItemStack item = inventory.getItem(slot);
                if (item == null || !VanillaObtentionPolicy.isBlocked(item.getType(), blocked)) continue;
                removed += Math.max(1, item.getAmount());
                inventory.setItem(slot, null);
            }
        }
        if (removed > 0) {
            plugin.getLogger().info("Loot vanilla bloque: " + removed + " item(s) dans chunk "
                    + event.getChunk().getX() + "," + event.getChunk().getZ()
                    + " (" + event.getWorld().getName() + ")");
        }
    }
}
