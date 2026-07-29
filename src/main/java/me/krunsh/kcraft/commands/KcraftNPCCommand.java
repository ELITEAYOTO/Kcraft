package me.krunsh.kcraft.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.krunsh.kcraft.Kcraft;
import me.krunsh.kcraft.models.CraftTable;
import me.krunsh.kcraft.utils.MessageUtil;

/**
 * Commande spéciale pour NPCs /kcraft-npc
 * Sécurisée pour console/NPCs uniquement
 */
public class KcraftNPCCommand implements CommandExecutor {
    
    private final Kcraft plugin;
    
    public KcraftNPCCommand(Kcraft plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        
        // Vérifier que c'est bien un NPC/Console (pas un joueur direct)
        if (sender instanceof Player) {
            Player player = (Player) sender;
            
            // Vérifier permission spéciale ou si c'est via Citizens/autre
            if (!player.hasPermission("kcraft.npc") && !isFromNPC(player)) {
                MessageUtil.sendError(sender, "commands.no-permission-cmd");
                return true;
            }
        }
        
        if (args.length == 0) {
            sender.sendMessage("§cUsage: /kcraft-npc <table_id> [player]");
            return true;
        }
        
        String tableId = args[0];
        
        // Déterminer le joueur cible
        Player target;
        if (args.length > 1) {
            // Joueur spécifié (pour console)
            target = plugin.getServer().getPlayer(args[1]);
            if (target == null) {
                MessageUtil.sendError(sender, "commands.player-not-found");
                return true;
            }
        } else {
            // Sender doit être un joueur
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cVous devez spécifier un joueur depuis la console.");
                return true;
            }
            target = (Player) sender;
        }
        
        // Vérifier que la table existe
        CraftTable table = plugin.getTableManager().getTable(tableId);
        if (table == null) {
            sender.sendMessage("§cTable introuvable: " + tableId);
            return true;
        }
        
        // Vérifier que c'est une table virtuelle autorisée
        if (table.isPhysical()) {
            sender.sendMessage("§cCette table doit être utilisée physiquement dans le monde.");
            return true;
        }
        
        if (table.isConsoleOnly() && sender instanceof Player) {
            Player senderPlayer = (Player) sender;
            if (!senderPlayer.hasPermission("kcraft.npc.bypass")) {
                sender.sendMessage("§cCette table est réservée aux NPCs/Console.");
                return true;
            }
        }
        
        // Vérifications pour le joueur cible
        if (!table.canUse(target)) {
            MessageUtil.sendError(sender, "table.no-access");
            return true;
        }
        
        if (!isWorldEnabled(target)) {
            MessageUtil.sendError(sender, "craft.wrong-world");
            return true;
        }
        
        // Ouvrir la GUI
        try {
            me.krunsh.kcraft.gui.CraftTableGUI gui = new me.krunsh.kcraft.gui.CraftTableGUI(plugin, target, table);
            gui.open();

            target.sendMessage("§7[NPC] Table de craft ouverte: §e" + table.getName());
            
            if (sender != target) {
                sender.sendMessage("§aGUI ouverte pour " + target.getName() + " - Table: " + table.getName());
            }
            
            // Log l'action
            MessageUtil.debug("GUI NPC ouverte - Sender: " + sender.getName() + 
                            ", Target: " + target.getName() + 
                            ", Table: " + tableId, 2);
            
        } catch (Exception e) {
            sender.sendMessage("§cErreur lors de l'ouverture: " + e.getMessage());
            plugin.getLogger().warning("Erreur NPC command: " + e.getMessage());
        }
        
        return true;
    }
    
    /**
     * Vérifie si la commande vient d'un NPC (Citizens, etc.)
     */
    private boolean isFromNPC(Player player) {
        // TODO: Implémenter détection Citizens/autres systèmes NPC
        // Pour Citizens: return CitizensAPI.getNPCRegistry().isNPC(player);
        
        // Vérification basique par nom (NPCs ont souvent des noms spéciaux)
        String name = player.getName();
        return name.startsWith("NPC_") || 
               name.startsWith("Bot_") || 
               name.length() > 16; // Noms NPCs souvent plus longs
    }
    
    /**
     * Vérifie si le monde est activé
     */
    private boolean isWorldEnabled(Player player) {
        String worldName = player.getWorld().getName();
        return plugin.getConfigManager().getEnabledWorlds().isEmpty() || 
               plugin.getConfigManager().getEnabledWorlds().contains(worldName);
    }
}