package me.krunsh.kcraft.utils;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.krunsh.kcraft.config.ConfigManager;

/**
 * Utilitaire pour gérer les messages avec placeholders et couleurs
 */
public class MessageUtil {
    
    private static ConfigManager configManager;
    private static String prefix;
    
    public static void init(ConfigManager config) {
        configManager = config;
        prefix = colorize(config.getPrefix());
    }
    
    /**
     * Colorise un message avec les codes couleurs &
     */
    public static String colorize(String message) {
        if (message == null) return "";
        return ChatColor.translateAlternateColorCodes('&', message);
    }
    
    /**
     * Colorise une liste de messages
     */
    public static java.util.List<String> colorizeList(java.util.List<String> messages) {
        if (messages == null) return new java.util.ArrayList<>();
        
        java.util.List<String> colorized = new java.util.ArrayList<>();
        for (String message : messages) {
            colorized.add(colorize(message));
        }
        return colorized;
    }
    
    /**
     * Envoie un message à un CommandSender avec placeholders
     */
    public static void sendMessage(CommandSender sender, String messageKey, Map<String, String> placeholders) {
        String message = getMessage(messageKey, placeholders);
        if (!message.isEmpty()) {
            sender.sendMessage(message);
        }
    }
    
    /**
     * Envoie un message simple à un CommandSender
     */
    public static void sendMessage(CommandSender sender, String messageKey) {
        sendMessage(sender, messageKey, new HashMap<>());
    }
    
    /**
     * Envoie un message à un joueur avec placeholders
     */
    public static void sendMessage(Player player, String messageKey, Map<String, String> placeholders) {
        sendMessage((CommandSender) player, messageKey, placeholders);
    }
    
    /**
     * Envoie un message simple à un joueur
     */
    public static void sendMessage(Player player, String messageKey) {
        sendMessage(player, messageKey, new HashMap<>());
    }
    
    /**
     * Obtient un message avec placeholders remplacés
     */
    public static String getMessage(String messageKey, Map<String, String> placeholders) {
        String message = configManager.getMessage(messageKey);
        
        // Remplacer le prefix
        message = message.replace("%prefix%", prefix);
        
        // Remplacer les placeholders
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                message = message.replace("%" + entry.getKey() + "%", entry.getValue());
            }
        }
        
        return colorize(message);
    }
    
    /**
     * Obtient un message simple
     */
    public static String getMessage(String messageKey) {
        return getMessage(messageKey, new HashMap<>());
    }
    
    /**
     * Crée une map de placeholders facilement
     */
    public static Map<String, String> placeholders(String... keyValues) {
        Map<String, String> placeholders = new HashMap<>();
        
        for (int i = 0; i < keyValues.length; i += 2) {
            if (i + 1 < keyValues.length) {
                placeholders.put(keyValues[i], keyValues[i + 1]);
            }
        }
        
        return placeholders;
    }
    
    /**
     * Messages de succès
     */
    public static void sendSuccess(CommandSender sender, String messageKey, Map<String, String> placeholders) {
        sendMessage(sender, messageKey, placeholders);
    }
    
    public static void sendSuccess(CommandSender sender, String messageKey) {
        sendMessage(sender, messageKey);
    }
    
    /**
     * Messages d'erreur
     */
    public static void sendError(CommandSender sender, String messageKey, Map<String, String> placeholders) {
        sendMessage(sender, messageKey, placeholders);
    }
    
    public static void sendError(CommandSender sender, String messageKey) {
        sendMessage(sender, messageKey);
    }
    
    /**
     * Messages d'info
     */
    public static void sendInfo(CommandSender sender, String messageKey, Map<String, String> placeholders) {
        sendMessage(sender, messageKey, placeholders);
    }
    
    public static void sendInfo(CommandSender sender, String messageKey) {
        sendMessage(sender, messageKey);
    }

    /**
     * Envoie un message brut (non key-based) avec couleurs.
     */
    public static void sendRaw(CommandSender sender, String rawMessage) {
        if (rawMessage == null || rawMessage.trim().isEmpty()) {
            return;
        }
        sender.sendMessage(colorize(rawMessage));
    }
    
    /**
     * Obtient le prefix coloré
     */
    public static String getPrefix() {
        return prefix;
    }
    
    /**
     * Log debug si niveau suffisant
     */
    public static void debug(String message, int level) {
        if (configManager != null && configManager.getDebugLevel() >= level) {
            System.out.println("[Kcraft DEBUG " + level + "] " + message);
        }
    }
    
    /**
     * Log avec prefix
     */
    public static void log(String message) {
        System.out.println("[Kcraft] " + message);
    }
}