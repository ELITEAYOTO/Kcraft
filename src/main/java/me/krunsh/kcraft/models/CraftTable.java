package me.krunsh.kcraft.models;

import org.bukkit.Material;

/**
 * Modèle pour une table de craft physique ou virtuelle
 */
public class CraftTable {
    
    private String id;
    private String name;
    private Material blockType;
    private byte blockDataValue; // Data value pour 1.8.8 (ex: STONE:2 = Polished Granite)
    private String size; // "3x3", "5x5", "7x7", "9x9"
    private String permission;
    private boolean isPhysical;
    private String command;
    private boolean consoleOnly;
    
    // Effets
    private String particle;
    private String soundOpen;
    private String soundClose;
    
    public CraftTable(String id) {
        this.id = id;
        this.name = id;
        this.size = "3x3";
        this.isPhysical = true;
        this.consoleOnly = false;
        this.blockDataValue = 0;
    }
    
    /**
     * Obtient les dimensions de la table
     */
    public int[] getDimensions() {
        switch (size.toLowerCase()) {
            case "3x3":
                return new int[]{3, 3};
            case "5x5":
                return new int[]{5, 5};
            case "7x7":
                return new int[]{7, 7};
            case "9x9":
                return new int[]{9, 9};
            default:
                return new int[]{3, 3};
        }
    }
    
    /**
     * Obtient le nombre de slots de la GUI
     */
    public int getGuiSize() {
        int[] dims = getDimensions();
        int craftSlots = dims[0] * dims[1];
        
        // Ajouter des slots pour bordures et boutons
        if (craftSlots <= 9) return 27; // 3 lignes
        if (craftSlots <= 25) return 45; // 5 lignes
        if (craftSlots <= 49) return 54; // 6 lignes
        return 54; // Maximum
    }
    
    /**
     * Vérifie si un joueur peut utiliser cette table
     */
    public boolean canUse(org.bukkit.entity.Player player) {
        if (permission != null && !permission.isEmpty()) {
            return player.isOp() || player.hasPermission(permission);
        }
        return true;
    }
    
    // === GETTERS & SETTERS ===
    
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public Material getBlockType() {
        return blockType;
    }
    
    public void setBlockType(Material blockType) {
        this.blockType = blockType;
    }
    
    public byte getBlockDataValue() {
        return blockDataValue;
    }
    
    public void setBlockDataValue(byte blockDataValue) {
        this.blockDataValue = blockDataValue;
    }
    
    public String getSize() {
        return size;
    }
    
    public void setSize(String size) {
        this.size = size;
    }
    
    public String getPermission() {
        return permission;
    }
    
    public void setPermission(String permission) {
        this.permission = permission;
    }
    
    public boolean isPhysical() {
        return isPhysical;
    }
    
    public void setPhysical(boolean physical) {
        isPhysical = physical;
    }
    
    public String getCommand() {
        return command;
    }
    
    public void setCommand(String command) {
        this.command = command;
    }
    
    public boolean isConsoleOnly() {
        return consoleOnly;
    }
    
    public void setConsoleOnly(boolean consoleOnly) {
        this.consoleOnly = consoleOnly;
    }
    
    public String getParticle() {
        return particle;
    }
    
    public void setParticle(String particle) {
        this.particle = particle;
    }
    
    public String getSoundOpen() {
        return soundOpen;
    }
    
    public void setSoundOpen(String soundOpen) {
        this.soundOpen = soundOpen;
    }
    
    public String getSoundClose() {
        return soundClose;
    }
    
    public void setSoundClose(String soundClose) {
        this.soundClose = soundClose;
    }
    
    @Override
    public String toString() {
        return "CraftTable{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", size='" + size + '\'' +
                ", physical=" + isPhysical +
                '}';
    }
}