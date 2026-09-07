package me.krunsh.kcraft.models;

import org.bukkit.Material;

/** Modèle de table KCraft V2. Les seules tailles supportées sont 3x3, 4x4 et 5x5. */
public class CraftTable {

    private String id;
    private String name;
    private Material blockType;
    private byte blockDataValue;
    private String size;
    private String permission;
    private boolean isPhysical;
    private String command;
    private boolean consoleOnly;
    private String particle;
    private String soundOpen;
    private String soundClose;

    public CraftTable(String id) {
        if (id == null || id.trim().isEmpty()) throw new IllegalArgumentException("table id");
        this.id = id;
        this.name = id;
        this.size = "3x3";
        this.isPhysical = true;
        this.consoleOnly = false;
        this.blockDataValue = 0;
    }

    public int[] getDimensions() {
        switch (size.toLowerCase()) {
            case "3x3": return new int[]{3, 3};
            case "4x4": return new int[]{4, 4};
            case "5x5": return new int[]{5, 5};
            default:
                throw new IllegalStateException("Taille KCraft V2 invalide: " + size);
        }
    }

    /** Toutes les GUI V2 utilisent actuellement un conteneur 54 slots. */
    public int getGuiSize() {
        return 54;
    }

    public boolean canUse(org.bukkit.entity.Player player) {
        if (player == null) return false;
        if (permission != null && !permission.isEmpty()) {
            return player.isOp() || player.hasPermission(permission);
        }
        return true;
    }

    public static boolean isSupportedSize(String size) {
        if (size == null) return false;
        String normalized = size.trim().toLowerCase();
        return "3x3".equals(normalized) || "4x4".equals(normalized) || "5x5".equals(normalized);
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Material getBlockType() { return blockType; }
    public void setBlockType(Material blockType) { this.blockType = blockType; }
    public byte getBlockDataValue() { return blockDataValue; }
    public void setBlockDataValue(byte blockDataValue) { this.blockDataValue = blockDataValue; }
    public String getSize() { return size; }
    public void setSize(String size) {
        if (!isSupportedSize(size)) {
            throw new IllegalArgumentException("Taille de table non supportee: " + size
                + " (supportees: 3x3, 4x4, 5x5)");
        }
        this.size = size.trim().toLowerCase();
    }
    public String getPermission() { return permission; }
    public void setPermission(String permission) { this.permission = permission; }
    public boolean isPhysical() { return isPhysical; }
    public void setPhysical(boolean physical) { isPhysical = physical; }
    public String getCommand() { return command; }
    public void setCommand(String command) { this.command = command; }
    public boolean isConsoleOnly() { return consoleOnly; }
    public void setConsoleOnly(boolean consoleOnly) { this.consoleOnly = consoleOnly; }
    public String getParticle() { return particle; }
    public void setParticle(String particle) { this.particle = particle; }
    public String getSoundOpen() { return soundOpen; }
    public void setSoundOpen(String soundOpen) { this.soundOpen = soundOpen; }
    public String getSoundClose() { return soundClose; }
    public void setSoundClose(String soundClose) { this.soundClose = soundClose; }

    @Override public String toString() {
        return "CraftTable{" + "id='" + id + '\'' + ", name='" + name + '\''
            + ", size='" + size + '\'' + ", physical=" + isPhysical + '}';
    }
}
