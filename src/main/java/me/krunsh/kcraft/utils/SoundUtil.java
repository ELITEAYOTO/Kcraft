package me.krunsh.kcraft.utils;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

/**
 * Helper pour jouer un son sans planter si le nom est invalide (compat 1.8.8).
 */
public final class SoundUtil {

    private SoundUtil() {}

    /**
     * Joue un son a la position du joueur. Si soundName est null/vide/invalide,
     * la methode ne fait rien (et logge en debug niveau 2 si invalide).
     */
    public static void playSafe(Player player, String soundName) {
        playSafe(player, soundName, 1.0f, 1.0f);
    }

    public static void playSafe(Player player, String soundName, float volume, float pitch) {
        if (player == null || soundName == null || soundName.isEmpty()) {
            return;
        }
        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase());
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (IllegalArgumentException e) {
            MessageUtil.debug("Son invalide '" + soundName + "' (1.8.8): ignore", 2);
        }
    }
}
