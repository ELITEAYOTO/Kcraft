package me.krunsh.kcraft.managers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitTask;

import me.krunsh.kcraft.Kcraft;
import me.krunsh.kcraft.models.CraftIngredient;
import me.krunsh.kcraft.models.CraftRecipe;
import me.krunsh.kcraft.models.CraftResult;
import me.krunsh.kcraft.utils.NBTUtil;
import me.krunsh.kcraft.utils.ItemStackUtil;

/** Sessions de debug temporaires, ciblees par joueur et dedupliquees par matrice. */
public final class VanillaDebugManager {
    private static final int DEFAULT_SECONDS = 60;
    private static final int MAX_SECONDS = 300;
    public static final String INTERNAL_RECIPE_ID = "__debug_vanilla_listener";
    public static final String DEBUG_METADATA = "kcraft-vanilla-debug";

    private final Kcraft plugin;
    private final Map<UUID, Session> sessions = new HashMap<UUID, Session>();
    private final Map<UUID, BukkitTask> expiryTasks = new HashMap<UUID, BukkitTask>();
    private final CraftRecipe internalRecipe;
    private boolean unknownApiDetected;
    private boolean unknownListenerRegistered;
    private boolean unknownRuntimeReceived;

    public VanillaDebugManager(Kcraft plugin) {
        this.plugin = plugin;
        this.internalRecipe = createInternalRecipe();
    }

    public int start(Player player, int requestedSeconds) {
        int seconds = requestedSeconds <= 0 ? DEFAULT_SECONDS : Math.min(MAX_SECONDS, requestedSeconds);
        final UUID playerId = player.getUniqueId();
        final long expiresAt = System.currentTimeMillis() + seconds * 1000L;
        BukkitTask previousTask = expiryTasks.remove(playerId);
        if (previousTask != null) previousTask.cancel();
        sessions.put(playerId, new Session(player.getName(), expiresAt));
        player.setMetadata(DEBUG_METADATA, new FixedMetadataValue(plugin, expiresAt));
        trace(player, "SESSION activée pour " + seconds + "s; recette interne " + INTERNAL_RECIPE_ID + " active.");
        trace(player, "SANTÉ hook inconnu: " + healthSummary());
        BukkitTask expiryTask = plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
            @Override public void run() {
                Session current = sessions.get(playerId);
                if (current == null || current.expiresAt != expiresAt
                        || current.expiresAt > System.currentTimeMillis()) return;
                sessions.remove(playerId);
                expiryTasks.remove(playerId);
                Player online = plugin.getServer().getPlayer(playerId);
                if (online != null) online.removeMetadata(DEBUG_METADATA, plugin);
            }
        }, seconds * 20L + 2L);
        expiryTasks.put(playerId, expiryTask);
        return seconds;
    }

    public boolean stop(Player player) {
        boolean removed = sessions.remove(player.getUniqueId()) != null;
        BukkitTask task = expiryTasks.remove(player.getUniqueId());
        if (task != null) task.cancel();
        player.removeMetadata(DEBUG_METADATA, plugin);
        return removed;
    }

    public boolean isActive(Player player) {
        if (player == null) return false;
        Session session = sessions.get(player.getUniqueId());
        if (session == null) return false;
        if (session.expiresAt <= System.currentTimeMillis()) {
            sessions.remove(player.getUniqueId());
            BukkitTask task = expiryTasks.remove(player.getUniqueId());
            if (task != null) task.cancel();
            player.removeMetadata(DEBUG_METADATA, plugin);
            return false;
        }
        return true;
    }

    public boolean shouldTraceMatrix(Player player, ItemStack[] matrix) {
        if (!isActive(player)) return false;
        Session session = sessions.get(player.getUniqueId());
        String fingerprint = fingerprint(matrix);
        if (fingerprint.equals(session.lastFingerprint)) return false;
        session.lastFingerprint = fingerprint;
        return true;
    }

    public List<String> statusLines() {
        long now = System.currentTimeMillis();
        List<String> lines = new ArrayList<String>();
        List<UUID> expired = new ArrayList<UUID>();
        for (Map.Entry<UUID, Session> entry : sessions.entrySet()) {
            Session session = entry.getValue();
            if (session.expiresAt <= now) { expired.add(entry.getKey()); continue; }
            lines.add(session.playerName + " (" + Math.max(1L, (session.expiresAt - now + 999L) / 1000L) + "s)");
        }
        for (UUID id : expired) {
            sessions.remove(id);
            BukkitTask task = expiryTasks.remove(id);
            if (task != null) task.cancel();
            Player online = plugin.getServer().getPlayer(id);
            if (online != null) online.removeMetadata(DEBUG_METADATA, plugin);
        }
        Collections.sort(lines);
        return lines;
    }

    public void shutdown() {
        for (BukkitTask task : expiryTasks.values()) task.cancel();
        expiryTasks.clear();
        for (UUID playerId : new ArrayList<UUID>(sessions.keySet())) {
            Player online = plugin.getServer().getPlayer(playerId);
            if (online != null) online.removeMetadata(DEBUG_METADATA, plugin);
        }
        sessions.clear();
    }

    public CraftRecipe getInternalRecipe() { return internalRecipe; }

    public void markUnknownApiDetected() {
        this.unknownApiDetected = true;
    }

    public void markUnknownListenerRegistered() {
        this.unknownListenerRegistered = true;
    }

    public void markUnknownRuntimeReceived(Player player, long traceId) {
        this.unknownRuntimeReceived = true;
        if (player == null) return;
        Session session = sessions.get(player.getUniqueId());
        if (session != null && session.expiresAt > System.currentTimeMillis()) {
            session.unknownEvents++;
            session.lastForkTraceId = traceId;
        }
    }

    /** Evite d'attribuer au chemin NMS connu l'identifiant du hook inconnu precedent. */
    public void beginKnownPrepare(Player player) {
        Session session = player == null ? null : sessions.get(player.getUniqueId());
        if (session != null && session.expiresAt > System.currentTimeMillis()) {
            session.lastForkTraceId = 0L;
        }
    }

    public long getUnknownEventCount(Player player) {
        Session session = player == null ? null : sessions.get(player.getUniqueId());
        return session == null ? 0L : session.unknownEvents;
    }

    public boolean markMissingHookWarningIfFirst(Player player) {
        Session session = player == null ? null : sessions.get(player.getUniqueId());
        if (session == null || session.missingHookWarningSent) return false;
        session.missingHookWarningSent = true;
        return true;
    }

    public String healthSummary() {
        return "classe API=" + state(unknownApiDetected)
            + ", listener KCraft=" + state(unknownListenerRegistered)
            + ", événement runtime=" + state(unknownRuntimeReceived);
    }

    public void trace(Player player, String message) {
        Session session = player == null ? null : sessions.get(player.getUniqueId());
        String correlation = session == null || session.lastForkTraceId <= 0L
            ? "" : "[#" + session.lastForkTraceId + "]";
        plugin.getLogger().info("[KCraft-VanillaDebug]["
            + (player == null ? "?" : player.getName()) + "]" + correlation + " " + message);
    }

    public String describeItem(ItemStack item) {
        if (!ItemStackUtil.isPresent(item)) return "VIDE";
        StringBuilder out = new StringBuilder(item.getType().name())
            .append(':').append(item.getDurability()).append(" x").append(item.getAmount());
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) out.append(" nom='").append(meta.getDisplayName()).append('\'');
        try {
            Set<String> keys = NBTUtil.getNBTKeys(item);
            if (!keys.isEmpty()) {
                Map<String, Object> values = new LinkedHashMap<String, Object>();
                for (String key : keys) values.put(key, NBTUtil.getNBTValue(item, key));
                out.append(" nbt=").append(values);
                Object cit = values.containsKey("cit-model") ? values.get("cit-model")
                    : values.containsKey("sparrowmc-item") ? values.get("sparrowmc-item") : null;
                if (cit != null) out.append(" CIT=").append(cit);
            }
        } catch (Throwable error) {
            out.append(" nbt=<lecture impossible: ").append(error.getClass().getSimpleName()).append('>');
        }
        return out.toString();
    }

    public List<String> describeRecipe(CraftRecipe recipe, String source) {
        if (recipe == null) return Collections.singletonList("RECETTE ABSENTE");
        List<String> lines = new ArrayList<String>();
        lines.add("id=" + recipe.getId() + " source=" + source + " type=" + recipe.getType());
        lines.add("table=" + recipe.getRequiredTable() + " allow-vanilla=" + recipe.isAllowVanillaWorkbench()
            + " tags=" + recipe.getTags() + " mirror=" + recipe.isAllowMirror()
            + " mode=" + (recipe.isVanillaLooseMatch() ? "MATERIAL/LOOSE" : "EXACT"));
        lines.add("permission=" + recipe.getPermission() + " faction=" + recipe.getFactionLevelRequired()
            + " worlds=" + recipe.getEnabledWorlds() + " biomes=" + recipe.getEnabledBiomes()
            + " plugin=" + recipe.getRequiredPlugin());
        lines.add("pattern=" + recipe.getPattern());
        for (Map.Entry<Character, CraftIngredient> entry : recipe.getIngredients().entrySet()) {
            CraftIngredient ingredient = entry.getValue();
            lines.add("ingredient " + entry.getKey() + " material=" + ingredient.getMaterial()
                + " data=" + ingredient.getDataValue() + " amount=" + ingredient.getAmount()
                + " nom=" + ingredient.getName() + " lore=" + ingredient.getLore()
                + " NBT/CIT=" + ingredient.getRequiredNBT());
        }
        CraftResult result = recipe.getResult();
        lines.add("result=" + (result == null ? "ABSENT" : result.getMaterial() + ":" + result.getDataValue()
            + " x" + result.getAmount() + " NBT/CIT=" + result.getNbtData()));
        return lines;
    }

    private String fingerprint(ItemStack[] matrix) {
        if (matrix == null) return "null";
        StringBuilder out = new StringBuilder();
        for (ItemStack item : matrix) out.append('[').append(describeItem(item)).append(']');
        return out.toString();
    }

    private static CraftRecipe createInternalRecipe() {
        CraftResult result = new CraftResult(Material.PAPER);
        result.setName("&aKCraft vanilla debug OK");
        result.setLore(Arrays.asList("&7Résultat interne temporaire", "&8Actif uniquement pendant le debug ciblé"));
        Map<String, Object> nbt = new LinkedHashMap<String, Object>();
        nbt.put("kcraft-debug-test", 1);
        result.setNbtData(nbt);
        return CraftRecipe.builder(INTERNAL_RECIPE_ID)
            .table("vanilla-debug")
            .pattern("DDD", "DED", "DDD")
            .ingredient('D', new CraftIngredient(Material.DIAMOND))
            .ingredient('E', new CraftIngredient(Material.EMERALD))
            .result(result)
            .build();
    }

    private static final class Session {
        private final String playerName;
        private final long expiresAt;
        private String lastFingerprint;
        private long unknownEvents;
        private long lastForkTraceId;
        private boolean missingHookWarningSent;
        private Session(String playerName, long expiresAt) {
            this.playerName = playerName;
            this.expiresAt = expiresAt;
        }
    }

    private static String state(boolean value) {
        return value ? "OK" : "NON";
    }
}
