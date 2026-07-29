package me.krunsh.kcraft.models;

/**
 * Données d'une table custom placée dans le monde
 */
public class PlacedTableData {
    
    private String world;
    private int x;
    private int y;
    private int z;
    private String tableId;
    private String placedBy;
    private long placedAt;
    private String brokenBy;
    private Long brokenAt;
    
    public PlacedTableData() {
    }
    
    public PlacedTableData(String world, int x, int y, int z, String tableId, String placedBy) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.tableId = tableId;
        this.placedBy = placedBy;
        this.placedAt = System.currentTimeMillis();
    }
    
    public String getLocationKey() {
        return world + ":" + x + ":" + y + ":" + z;
    }
    
    // === GETTERS & SETTERS ===
    
    public String getWorld() {
        return world;
    }
    
    public void setWorld(String world) {
        this.world = world;
    }
    
    public int getX() {
        return x;
    }
    
    public void setX(int x) {
        this.x = x;
    }
    
    public int getY() {
        return y;
    }
    
    public void setY(int y) {
        this.y = y;
    }
    
    public int getZ() {
        return z;
    }
    
    public void setZ(int z) {
        this.z = z;
    }
    
    public String getTableId() {
        return tableId;
    }
    
    public void setTableId(String tableId) {
        this.tableId = tableId;
    }
    
    public String getPlacedBy() {
        return placedBy;
    }
    
    public void setPlacedBy(String placedBy) {
        this.placedBy = placedBy;
    }
    
    public long getPlacedAt() {
        return placedAt;
    }
    
    public void setPlacedAt(long placedAt) {
        this.placedAt = placedAt;
    }
    
    public String getBrokenBy() {
        return brokenBy;
    }
    
    public void setBrokenBy(String brokenBy) {
        this.brokenBy = brokenBy;
    }
    
    public Long getBrokenAt() {
        return brokenAt;
    }
    
    public void setBrokenAt(Long brokenAt) {
        this.brokenAt = brokenAt;
    }
}
