package me.krunsh.kcraft.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import me.krunsh.kcraft.Kcraft;
import me.krunsh.kcraft.config.DefaultCraftBootstrap;
import me.krunsh.kcraft.models.CraftRecipe;
import me.krunsh.kcraft.models.CraftResult;
import me.krunsh.kcraft.models.CraftTable;
import me.krunsh.kcraft.models.VanillaRecipeResolver;
import me.krunsh.kcraft.utils.MessageUtil;

/**
 * Commande principale /kcraft
 */
public class KcraftCommand implements CommandExecutor, TabCompleter {
    
    private final Kcraft plugin;
    
    public KcraftCommand(Kcraft plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "reload":
                return handleReload(sender);
                
            case "give":
                return handleGive(sender, args);
                
            case "list":
                return handleList(sender, args);
                
            case "info":
                return handleInfo(sender);
                
            case "stats":
                return handleStats(sender, args);
                
            case "givetable":
                return handleGiveTable(sender, args);

            case "defaults":
                return handleDefaults(sender, args);

            case "debug":
                return handleDebug(sender, args);

            case "debugrecipe":
                return handleDebugRecipe(sender, args);

            case "debughand":
                return handleDebugHand(sender);
                
            default:
                MessageUtil.sendError(sender, "commands.invalid-command");
                return true;
        }
    }
    
    /**
     * Gère la commande reload
     */
    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("kcraft.admin")) {
            MessageUtil.sendError(sender, "commands.no-permission-cmd");
            return true;
        }
        
        try {
            plugin.reload();
            MessageUtil.sendSuccess(sender, "commands.reload");
        } catch (Exception e) {
            sender.sendMessage("§cErreur lors du reload: " + e.getMessage());
            plugin.getLogger().warning("Erreur reload par " + sender.getName() + ": " + e.getMessage());
        }
        
        return true;
    }

    private boolean handleDefaults(CommandSender sender, String[] args) {
        if (!sender.hasPermission("kcraft.admin")) {
            MessageUtil.sendError(sender, "commands.no-permission-cmd");
            return true;
        }
        if (args.length < 2 || "list".equalsIgnoreCase(args[1])) {
            sender.sendMessage("§6Exemples KCraft embarqués (export manuel):");
            for (String file : plugin.getConfigManager().getDefaultCraftFiles()) {
                boolean exists = new java.io.File(plugin.getConfigManager().getCraftsFolder(), file).isFile();
                sender.sendMessage("§7- §e" + file + (exists ? " §8[déjà présent]" : " §a[disponible]"));
            }
            return true;
        }
        if ("export".equalsIgnoreCase(args[1])) {
            if (args.length < 3) {
                sender.sendMessage("§cUsage: /kcraft defaults export <fichier>");
                return true;
            }
            exportDefault(sender, args[2]);
            return true;
        }
        if ("export-all".equalsIgnoreCase(args[1])) {
            int exported = 0;
            int existing = 0;
            for (String file : plugin.getConfigManager().getDefaultCraftFiles()) {
                DefaultCraftBootstrap.ExportStatus status = exportDefault(sender, file, false);
                if (status == DefaultCraftBootstrap.ExportStatus.EXPORTED) exported++;
                if (status == DefaultCraftBootstrap.ExportStatus.ALREADY_EXISTS) existing++;
            }
            sender.sendMessage("§aExport terminé: §e" + exported + " §afichier(s), §7" + existing
                + " déjà présent(s). Aucun fichier écrasé. Faites /kcraft reload pour les charger.");
            return true;
        }
        sender.sendMessage("§cUsage: /kcraft defaults <list|export <fichier>|export-all>");
        return true;
    }

    private boolean handleDebug(CommandSender sender, String[] args) {
        if (!sender.hasPermission("kcraft.admin")) {
            MessageUtil.sendError(sender, "commands.no-permission-cmd");
            return true;
        }
        if (args.length < 2 || "status".equalsIgnoreCase(args[1])) {
            List<String> sessions = plugin.getVanillaDebugManager().statusLines();
            sender.sendMessage("§6Debug table vanilla: §e" + sessions.size() + " session(s)");
            sender.sendMessage("§6Santé du hook: §e" + plugin.getVanillaDebugManager().healthSummary());
            for (String session : sessions) sender.sendMessage("§7- " + session);
            return true;
        }
        if (("vanilla".equalsIgnoreCase(args[1]) || "off".equalsIgnoreCase(args[1])) && args.length < 3) {
            sender.sendMessage("§cUsage: /kcraft debug " + args[1] + " <joueur> [secondes]");
            return true;
        }
        if ("vanilla".equalsIgnoreCase(args[1])) {
            Player target = plugin.getServer().getPlayer(args[2]);
            if (target == null) { sender.sendMessage("§cJoueur introuvable ou hors ligne: §e" + args[2]); return true; }
            int seconds = 60;
            if (args.length >= 4) {
                try { seconds = Integer.parseInt(args[3]); }
                catch (NumberFormatException invalid) { sender.sendMessage("§cDurée invalide: " + args[3]); return true; }
            }
            seconds = plugin.getVanillaDebugManager().start(target, seconds);
            sender.sendMessage("§aDebug vanilla activé pour §e" + target.getName() + "§a pendant " + seconds
                + "s. La console recevra une trace à chaque modification de matrice.");
            return true;
        }
        if ("off".equalsIgnoreCase(args[1])) {
            Player target = plugin.getServer().getPlayer(args[2]);
            if (target == null) { sender.sendMessage("§cJoueur introuvable ou hors ligne: §e" + args[2]); return true; }
            boolean stopped = plugin.getVanillaDebugManager().stop(target);
            sender.sendMessage(stopped ? "§aDebug arrêté pour §e" + target.getName()
                : "§7Aucune session active pour §e" + target.getName());
            return true;
        }
        sender.sendMessage("§cUsage: /kcraft debug <status|vanilla <joueur> [secondes]|off <joueur>>");
        return true;
    }

    private boolean handleDebugRecipe(CommandSender sender, String[] args) {
        if (!sender.hasPermission("kcraft.admin")) {
            MessageUtil.sendError(sender, "commands.no-permission-cmd");
            return true;
        }
        if (args.length < 2) { sender.sendMessage("§cUsage: /kcraft debugrecipe <id>"); return true; }
        String id = args[1];
        if (plugin.getCraftManager().isDuplicateRecipe(id)) {
            sender.sendMessage("§cRECETTE REFUSÉE: identifiant dupliqué dans §e"
                + plugin.getCraftManager().getDuplicateRecipeSources(id));
            return true;
        }
        CraftRecipe recipe;
        String source;
        if (me.krunsh.kcraft.managers.VanillaDebugManager.INTERNAL_RECIPE_ID.equals(id)) {
            recipe = plugin.getVanillaDebugManager().getInternalRecipe();
            source = "<interne-debug; inactive hors session ciblée>";
        } else {
            recipe = plugin.getCraftManager().getRecipe(id);
            source = plugin.getCraftManager().getRecipeSource(id);
        }
        if (recipe == null) { sender.sendMessage("§cRecette non chargée/absente: §e" + id); return true; }
        sender.sendMessage("§6=== Diagnostic recette " + id + " ===");
        for (String line : plugin.getVanillaDebugManager().describeRecipe(recipe, source)) {
            sender.sendMessage("§7" + line);
        }
        List<String> ambiguities = new ArrayList<String>();
        for (CraftRecipe other : plugin.getCraftManager().getAllRecipes().values()) {
            if (other == recipe) continue;
            if (VanillaRecipeResolver.specificity(recipe) == VanillaRecipeResolver.specificity(other)
                    && VanillaRecipeResolver.signature(recipe).equals(VanillaRecipeResolver.signature(other))) {
                ambiguities.add(other.getId() + "@" + plugin.getCraftManager().getRecipeSource(other.getId()));
            }
        }
        sender.sendMessage(ambiguities.isEmpty() ? "§aambiguïté=aucune"
            : "§cambiguïtés=" + ambiguities);
        return true;
    }

    @SuppressWarnings("deprecation")
    private boolean handleDebugHand(CommandSender sender) {
        if (!sender.hasPermission("kcraft.admin")) {
            MessageUtil.sendError(sender, "commands.no-permission-cmd");
            return true;
        }
        if (!(sender instanceof Player)) { sender.sendMessage("§cCommande joueur uniquement."); return true; }
        Player player = (Player) sender;
        sender.sendMessage("§6Main: §7" + plugin.getVanillaDebugManager().describeItem(player.getItemInHand()));
        return true;
    }

    private void exportDefault(CommandSender sender, String file) {
        DefaultCraftBootstrap.ExportStatus status = exportDefault(sender, file, true);
        if (status == DefaultCraftBootstrap.ExportStatus.EXPORTED) {
            sender.sendMessage("§aExemple exporté: §e" + file + "§a. Faites /kcraft reload pour le charger.");
        }
    }

    private DefaultCraftBootstrap.ExportStatus exportDefault(CommandSender sender, String file, boolean report) {
        try {
            DefaultCraftBootstrap.ExportStatus status = plugin.getConfigManager().exportDefaultCraft(file);
            if (report && status == DefaultCraftBootstrap.ExportStatus.ALREADY_EXISTS) {
                sender.sendMessage("§cRefus: ce fichier existe déjà et ne sera pas écrasé: §e" + file);
            } else if (report && status == DefaultCraftBootstrap.ExportStatus.UNKNOWN_DEFAULT) {
                sender.sendMessage("§cExemple inconnu ou nom de fichier invalide: §e" + file);
            }
            return status;
        } catch (Exception e) {
            sender.sendMessage("§cÉchec de l'export de §e" + file + "§c: " + e.getMessage());
            plugin.getLogger().warning("Export d'exemple KCraft impossible pour " + file + ": " + e.getMessage());
            return DefaultCraftBootstrap.ExportStatus.UNKNOWN_DEFAULT;
        }
    }
    
    /**
     * Gère la commande give
     */
    private boolean handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("kcraft.admin")) {
            MessageUtil.sendError(sender, "commands.no-permission-cmd");
            return true;
        }
        
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /kcraft give <joueur> <craft_id> [quantité]");
            return true;
        }
        
        String playerName = args[1];
        String craftId = args[2];
        int amount = 1;

        if ("craft_table_tier1".equalsIgnoreCase(craftId)) {
            craftId = "craft_table_antique";
        }
        
        plugin.getLogger().info("[GIVE] Tentative: joueur=" + playerName + ", craft=" + craftId);
        
        // Quantité optionnelle
        if (args.length >= 4) {
            try {
                amount = Integer.parseInt(args[3]);
                if (amount <= 0 || amount > 64) {
                    sender.sendMessage("§cLa quantité doit être entre 1 et 64!");
                    return true;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage("§cQuantité invalide: " + args[3]);
                return true;
            }
        }
        
        Player target = plugin.getServer().getPlayer(playerName);
        if (target == null) {
            MessageUtil.sendError(sender, "commands.player-not-found");
            return true;
        }
        
        CraftRecipe recipe = plugin.getCraftManager().getRecipe(craftId);
        if (recipe == null && !craftId.startsWith("craft_")) {
            // Alias utile: /kcraft give ... table_antique -> craft_table_antique
            recipe = plugin.getCraftManager().getRecipe("craft_" + craftId);
            if (recipe != null) {
                craftId = recipe.getId();
            }
        }
        if (recipe == null) {
            sender.sendMessage("§c❌ Craft introuvable: §e" + craftId);
            sender.sendMessage("§7Utilisez §f/kcraft list §7pour voir tous les crafts disponibles");
            return true;
        }

        CraftResult recipeResult = recipe.getResult();
        if (recipeResult == null) {
            sender.sendMessage("§c❌ Le craft §e" + craftId + " §cn'a pas de résultat valide.");
            sender.sendMessage("§7Vérifiez le material/result dans les fichiers de craft.");
            plugin.getLogger().warning("[GIVE] Craft sans résultat valide: " + craftId);
            return true;
        }
        
        try {
            // Créer et donner les items selon la quantité
            for (int i = 0; i < amount; i++) {
                ItemStack result = recipeResult.createItem();
                
                HashMap<Integer, ItemStack> leftover = target.getInventory().addItem(result);
                if (!leftover.isEmpty()) {
                    // Si l'inventaire est plein, drop les items au sol
                    for (ItemStack item : leftover.values()) {
                        target.getWorld().dropItemNaturally(target.getLocation(), item);
                    }
                }
            }
            
            // Messages selon la quantité
            if (amount > 1) {
                sender.sendMessage("§a✓ " + amount + "x §e" + craftId + " §adonné à §e" + target.getName());
                target.sendMessage("§aVous avez reçu: §e" + amount + "x " + craftId);
            } else {
                MessageUtil.sendSuccess(sender, "commands.give-success", 
                    MessageUtil.placeholders("item", craftId, "player", target.getName()));
                target.sendMessage("§aVous avez reçu: §e" + craftId);
            }
            
            // Log succès
            plugin.getLogger().info("[GIVE] Succès: " + sender.getName() + " a donné " + amount + "x " + craftId + " à " + target.getName());
            
            // Message si inventaire plein
            if (amount > 1) {
                target.sendMessage("§7(Items surplus droppés au sol si inventaire plein)");
            }
            
        } catch (Exception e) {
            MessageUtil.sendError(sender, "commands.give-error");
            plugin.getLogger().warning("Erreur give: " + e.getMessage());
        }
        
        return true;
    }
    
    /**
     * Gère la commande list
     */
    private boolean handleList(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cCette commande est réservée aux joueurs.");
            return true;
        }
        
        Player player = (Player) sender;
        
        // Page (optionnelle)
        int page = 1;
        if (args.length > 1) {
            try {
                page = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                page = 1;
            }
        }
        
        // Obtenir les crafts disponibles pour ce joueur
        List<CraftRecipe> availableCrafts = new ArrayList<>();
        for (CraftRecipe recipe : plugin.getCraftManager().getAllRecipes().values()) {
            if (recipe.canCraft(player) && plugin.getHookManager().isPluginAvailable(recipe.getRequiredPlugin())) {
                availableCrafts.add(recipe);
            }
        }
        
        if (availableCrafts.isEmpty()) {
            MessageUtil.sendError(player, "commands.no-crafts");
            return true;
        }
        
        // Pagination
        int itemsPerPage = 10;
        int totalPages = (int) Math.ceil((double) availableCrafts.size() / itemsPerPage);
        page = Math.max(1, Math.min(page, totalPages));
        
        int startIndex = (page - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, availableCrafts.size());
        
        // Header
        MessageUtil.sendInfo(player, "commands.list-header", 
            MessageUtil.placeholders("page", String.valueOf(page), "max", String.valueOf(totalPages)));
        
        // Crafts
        for (int i = startIndex; i < endIndex; i++) {
            CraftRecipe recipe = availableCrafts.get(i);
            MessageUtil.sendMessage(player, "commands.list-entry", 
                MessageUtil.placeholders("craft", recipe.getId(), "table", recipe.getRequiredTable()));
        }
        
        return true;
    }
    
    /**
     * Gère la commande info
     */
    private boolean handleInfo(CommandSender sender) {
        sender.sendMessage("§6=== Kcraft Info ===");
        sender.sendMessage("§7Version: §e1.0.0");
        sender.sendMessage("§7Crafts chargés: §e" + plugin.getCraftManager().getCraftCount());
        sender.sendMessage("§7Tables disponibles: §e" + plugin.getTableManager().getAllTables().size());
        
        // Stats cache si activé
        if (plugin.getConfigManager().isCacheEnabled()) {
            sender.sendMessage("§7Cache activé: §a✓");
        } else {
            sender.sendMessage("§7Cache activé: §c✗");
        }
        
        // Plugins détectés
        sender.sendMessage("§7Plugins intégrés:");
        if (plugin.getHookManager().isKfactionEnabled()) {
            sender.sendMessage("  §a✓ Kfaction");
        }
        if (plugin.getHookManager().isKharvestEnabled()) {
            sender.sendMessage("  §a✓ Kharvester");
        }
        if (plugin.getHookManager().isOutilsEvolutifEnabled()) {
            sender.sendMessage("  §a✓ OutilsEvolutif");
        }
        
        return true;
    }
    
    /**
     * Gère la commande stats
     */
    private boolean handleStats(CommandSender sender, String[] args) {
        if (!sender.hasPermission("kcraft.admin")) {
            MessageUtil.sendError(sender, "commands.no-permission-cmd");
            return true;
        }
        
        // Stats globales
        if (args.length == 1) {
            sender.sendMessage("§6=== Stats Globales Kcraft ===");
            
            // TODO: Afficher les vraies stats depuis LoggingManager
            sender.sendMessage("§7Total crafts: §e" + "N/A");
            sender.sendMessage("§7Joueurs actifs: §e" + "N/A");
            sender.sendMessage("§7Craft le plus populaire: §e" + "N/A");
            
            return true;
        }
        
        // Stats d'un joueur spécifique
        if (args.length == 2) {
            String playerName = args[1];
            // TODO: Afficher stats du joueur
            sender.sendMessage("§7Stats de §e" + playerName + "§7: En développement");
            return true;
        }
        
        return true;
    }
    
    /**
     * Gère la commande givetable
     */
    private boolean handleGiveTable(CommandSender sender, String[] args) {
        if (!sender.hasPermission("kcraft.admin")) {
            MessageUtil.sendError(sender, "commands.no-permission-cmd");
            return true;
        }
        
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /kcraft givetable <joueur> <table_id>");
            return true;
        }
        
        String playerName = args[1];
        String tableId = args[2];

        if ("table_antique".equalsIgnoreCase(tableId) || "craft_table_antique".equalsIgnoreCase(tableId)) {
            tableId = "tier1";
        }
        
        Player target = plugin.getServer().getPlayer(playerName);
        if (target == null) {
            MessageUtil.sendError(sender, "commands.player-not-found");
            return true;
        }
        
        // Vérifier si la table existe
        CraftTable table = plugin.getTableManager().getTable(tableId);
        if (table == null) {
            sender.sendMessage("§cTable introuvable: " + tableId);
            return true;
        }
        
        try {
            // Créer la table custom avec NBT et le bon type de bloc
            org.bukkit.Material blockType = (table.getBlockType() != null) 
                ? table.getBlockType() : org.bukkit.Material.WORKBENCH;
            byte blockDataValue = table.getBlockDataValue();
            
            ItemStack customTable = me.krunsh.kcraft.utils.NBTUtil.createCustomCraftTable(tableId, blockType, blockDataValue);
            
            // Personnaliser l'apparence
            org.bukkit.inventory.meta.ItemMeta meta = customTable.getItemMeta();
            meta.setDisplayName("§6Table de Craft " + table.getName());
            java.util.List<String> lore = new java.util.ArrayList<>();
            lore.add("§7Table de craft custom du plugin Kcraft");
            lore.add("§7Type: §e" + tableId);
            lore.add("§7Taille: §e" + table.getSize());
            lore.add("§7Bloc: §e" + blockType.name() + (blockDataValue > 0 ? ":" + blockDataValue : ""));
            lore.add("§8ID: " + tableId);
            meta.setLore(lore);
            customTable.setItemMeta(meta);
            
            // Donner au joueur
            HashMap<Integer, ItemStack> leftover = target.getInventory().addItem(customTable);
            
            if (!leftover.isEmpty()) {
                for (ItemStack item : leftover.values()) {
                    target.getWorld().dropItemNaturally(target.getLocation(), item);
                }
                target.sendMessage("§cInventaire plein! Table droppée au sol.");
            }
            
            MessageUtil.sendSuccess(sender, "commands.givetable-success", 
                MessageUtil.placeholders("table", tableId, "player", target.getName()));
                
            target.sendMessage("§aVous avez reçu une table de craft custom: §e" + table.getName());
            
        } catch (Exception e) {
            MessageUtil.sendError(sender, "commands.givetable-error");
            plugin.getLogger().warning("Erreur givetable: " + e.getMessage());
        }
        
        return true;
    }
    
    /**
     * Affiche l'aide
     */
    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6=== Commandes Kcraft ===");
        sender.sendMessage("§e/kcraft reload §7- Recharge la configuration");
        sender.sendMessage("§e/kcraft list [page] §7- Liste les crafts disponibles");
        sender.sendMessage("§e/kcraft info §7- Informations sur le plugin");
        
        if (sender.hasPermission("kcraft.admin")) {
            sender.sendMessage("§c=== Admin ===");
            sender.sendMessage("§e/kcraft give <joueur> <craft_id> [quantité] §7- Donne un item crafté");
            sender.sendMessage("§e/kcraft givetable <joueur> <table> §7- Donne une table custom");
            sender.sendMessage("§e/kcraft stats [joueur] §7- Statistiques");
            sender.sendMessage("§e/kcraft defaults <list|export|export-all> §7- Gère les exemples sans écrasement");
            sender.sendMessage("§e/kcraft debug ... | debugrecipe <id> | debughand §7- Diagnostic ciblé vanilla");
        }
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // Sous-commandes principales
            completions.addAll(Arrays.asList("reload", "list", "info", "give", "givetable", "stats",
                "defaults", "debug", "debugrecipe", "debughand"));
            
            // Filtrer selon les permissions
            if (!sender.hasPermission("kcraft.admin")) {
                completions.removeAll(Arrays.asList("reload", "give", "givetable", "stats", "defaults",
                    "debug", "debugrecipe", "debughand"));
            }
        } else if (args.length == 2) {
            if ("give".equals(args[0]) && sender.hasPermission("kcraft.admin")) {
                // Noms des joueurs en ligne
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    completions.add(player.getName());
                }
            } else if ("givetable".equals(args[0]) && sender.hasPermission("kcraft.admin")) {
                // Noms des joueurs en ligne
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    completions.add(player.getName());
                }
            } else if ("stats".equals(args[0]) && sender.hasPermission("kcraft.admin")) {
                // Noms des joueurs pour stats
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    completions.add(player.getName());
                }
            } else if ("defaults".equals(args[0]) && sender.hasPermission("kcraft.admin")) {
                completions.addAll(Arrays.asList("list", "export", "export-all"));
            } else if ("debug".equals(args[0]) && sender.hasPermission("kcraft.admin")) {
                completions.addAll(Arrays.asList("status", "vanilla", "off"));
            } else if ("debugrecipe".equals(args[0]) && sender.hasPermission("kcraft.admin")) {
                completions.addAll(plugin.getCraftManager().getAllRecipes().keySet());
                completions.add(me.krunsh.kcraft.managers.VanillaDebugManager.INTERNAL_RECIPE_ID);
            }
        } else if (args.length == 3) {
            if ("give".equals(args[0]) && sender.hasPermission("kcraft.admin")) {
                // IDs des crafts
                for (String craftId : plugin.getCraftManager().getAllRecipes().keySet()) {
                    completions.add(craftId);
                }
            } else if ("givetable".equals(args[0]) && sender.hasPermission("kcraft.admin")) {
                // IDs des tables
                for (String tableId : plugin.getTableManager().getAllTables().keySet()) {
                    completions.add(tableId);
                }
            } else if ("defaults".equals(args[0]) && "export".equals(args[1])
                    && sender.hasPermission("kcraft.admin")) {
                completions.addAll(plugin.getConfigManager().getDefaultCraftFiles());
            } else if ("debug".equals(args[0])
                    && ("vanilla".equals(args[1]) || "off".equals(args[1]))
                    && sender.hasPermission("kcraft.admin")) {
                for (Player player : plugin.getServer().getOnlinePlayers()) completions.add(player.getName());
            }
        }
        
        // Filtrer selon ce que tape l'utilisateur
        String current = args[args.length - 1].toLowerCase();
        completions.removeIf(s -> !s.toLowerCase().startsWith(current));
        
        return completions;
    }
}
