package me.krunsh.kcraft.logging;

import java.util.LinkedHashMap;
import java.util.Map;

/** Snapshot pur Java d'un craft, sans référence Bukkit/Player. */
public final class CraftLogRecord {

    private final long sequence;
    private final long timestamp;
    private final String playerName;
    private final String playerUuid;
    private final String craftId;
    private final String result;
    private final boolean success;
    private final String world;
    private final int x;
    private final int y;
    private final int z;
    private final String biome;

    public CraftLogRecord(
            long sequence,
            long timestamp,
            String playerName,
            String playerUuid,
            String craftId,
            String result,
            boolean success,
            String world,
            int x,
            int y,
            int z,
            String biome) {

        this.sequence = sequence;
        this.timestamp = timestamp;
        this.playerName = playerName;
        this.playerUuid = playerUuid;
        this.craftId = craftId;
        this.result = result;
        this.success = success;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.biome = biome;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("id", Long.valueOf(sequence));
        out.put("timestamp", Long.valueOf(timestamp));

        Map<String, Object> player = new LinkedHashMap<String, Object>();
        player.put("name", playerName);
        player.put("uuid", playerUuid);
        out.put("player", player);

        Map<String, Object> craft = new LinkedHashMap<String, Object>();
        craft.put("recipe_id", craftId);
        craft.put("result", result);
        craft.put("success", Boolean.valueOf(success));
        out.put("craft", craft);

        Map<String, Object> location = new LinkedHashMap<String, Object>();
        location.put("world", world);
        location.put("x", Integer.valueOf(x));
        location.put("y", Integer.valueOf(y));
        location.put("z", Integer.valueOf(z));
        location.put("biome", biome);
        out.put("location", location);
        return out;
    }
}
