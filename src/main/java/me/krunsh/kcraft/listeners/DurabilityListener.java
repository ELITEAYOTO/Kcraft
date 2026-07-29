package me.krunsh.kcraft.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;

import me.krunsh.kcraft.Kcraft;
import me.krunsh.kcraft.utils.MessageUtil;
import me.krunsh.kcraft.utils.NBTUtil;

/**
 * Listener pour le système de durabilité custom Kcraft
 * 
 * Intercepte les dégâts de durabilité et applique le modificateur custom.
 * Compatible avec Unbreaking (s'applique APRÈS ce listener).
 * 
 * Fonctionnement:
 * - durability_modifier: 1.0 = normal (perd 1 dura/coup)
 * - durability_modifier: 2.0 = 2x plus durable (50% chance de perdre 1 dura)
 * - durability_modifier: 3.0 = 3x plus durable (33% chance de perdre 1 dura)
 * 
 * Si le joueur ajoute Unbreaking III, l'effet s'additionne !
 */
public class DurabilityListener implements Listener {
    
    private final Kcraft plugin;
    
    public DurabilityListener(Kcraft plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Intercepte les dégâts de durabilité sur les items
     * Note: Cet event est appelé AVANT l'enchantement Unbreaking
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemDamage(PlayerItemDamageEvent event) {
        ItemStack item = event.getItem();
        
        // Vérifier si l'item a un modificateur de durabilité custom
        if (!NBTUtil.hasDurabilityModifier(item)) {
            return;
        }
        
        double modifier = NBTUtil.getDurabilityModifier(item);
        
        // Si modifier = 1.0, comportement normal
        if (modifier == 1.0) {
            return;
        }
        
        Player player = event.getPlayer();
        int originalDamage = event.getDamage();
        
        // Calculer si les dégâts doivent être appliqués
        // modifier = 2.0 → 50% chance d'annuler (1/2)
        // modifier = 3.0 → 66% chance d'annuler (2/3)
        // modifier = 0.5 → 0% chance d'annuler + dégâts x2
        
        if (modifier > 1.0) {
            // Plus durable: chance d'annuler les dégâts
            double chanceToCancel = 1.0 - (1.0 / modifier);
            
            if (Math.random() < chanceToCancel) {
                // Annuler les dégâts de durabilité
                event.setCancelled(true);
                MessageUtil.debug("Durabilité préservée pour " + player.getName() + 
                                 " (modifier: " + modifier + "x, chance: " + (chanceToCancel * 100) + "%)", 3);
                return;
            }
        } else if (modifier < 1.0) {
            // Moins durable: augmenter les dégâts
            // modifier = 0.5 → dégâts x2
            int multipliedDamage = (int) Math.ceil(originalDamage / modifier);
            event.setDamage(multipliedDamage);
            MessageUtil.debug("Durabilité réduite pour " + player.getName() + 
                             " (modifier: " + modifier + "x, dégâts: " + originalDamage + " → " + multipliedDamage + ")", 3);
        }
    }
}
