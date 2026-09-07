package me.krunsh.kcraft.managers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import me.krunsh.kcraft.Kcraft;
import me.krunsh.kcraft.logging.CraftLogRecord;
import me.krunsh.kcraft.logging.LoggingMetrics;

/**
 * Logging V2.7 : queue unique, worker unique, vraie persistance JSONL.
 *
 * Aucun accès Bukkit n'est effectué dans le worker async : toutes les données
 * du Player sont capturées dans CraftLogRecord sur le thread appelant.
 */
public final class LoggingManager {

    private static final int DEFAULT_MAX_QUEUE = 10000;
    private static final int DEFAULT_MAX_BATCH = 500;
    private static final long DEFAULT_FLUSH_TICKS = 40L;

    private final Kcraft plugin;
    private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();
    private final ConcurrentLinkedQueue<CraftLogRecord> queue =
        new ConcurrentLinkedQueue<CraftLogRecord>();
    private final Map<UUID, PlayerStats> playerStats =
        new ConcurrentHashMap<UUID, PlayerStats>();
    private final Map<String, AtomicLong> craftCounts =
        new ConcurrentHashMap<String, AtomicLong>();
    private final AtomicLong totalCrafts = new AtomicLong();
    private final AtomicLong failedCrafts = new AtomicLong();
    private final AtomicLong sequence = new AtomicLong();
    private final LoggingMetrics metrics = new LoggingMetrics();

    private volatile BukkitTask workerTask;

    public LoggingManager(Kcraft plugin) {
        this.plugin = plugin;
        ensureWorker();
    }

    public void logCraft(Player player, String craftId, String result, boolean success) {
        if (!plugin.getConfigManager().isLoggingEnabled() || player == null) return;

        updateStats(player, craftId, success);

        if (queue.size() >= DEFAULT_MAX_QUEUE) {
            metrics.recordDropped();
            return;
        }

        Location location = player.getLocation();
        String biome = location.getBlock().getBiome().name();

        CraftLogRecord record = new CraftLogRecord(
            sequence.incrementAndGet(),
            System.currentTimeMillis(),
            player.getName(),
            player.getUniqueId().toString(),
            craftId,
            result,
            success,
            location.getWorld() == null ? "" : location.getWorld().getName(),
            location.getBlockX(),
            location.getBlockY(),
            location.getBlockZ(),
            biome
        );

        queue.offer(record);
        metrics.recordQueued(queue.size());
    }

    public void ensureWorker() {
        if (workerTask != null || !plugin.getConfigManager().isLoggingEnabled()) return;

        workerTask = plugin.getServer().getScheduler().runTaskTimerAsynchronously(
            plugin,
            new Runnable() {
                @Override public void run() { flushQueue(DEFAULT_MAX_BATCH); }
            },
            DEFAULT_FLUSH_TICKS,
            DEFAULT_FLUSH_TICKS
        );
    }

    private synchronized boolean flushQueue(int maxBatch) {
        if (queue.isEmpty()) return true;

        List<CraftLogRecord> batch = new ArrayList<CraftLogRecord>(maxBatch);
        for (int i = 0; i < maxBatch; i++) {
            CraftLogRecord record = queue.poll();
            if (record == null) break;
            batch.add(record);
        }
        if (batch.isEmpty()) return true;

        long started = System.nanoTime();
        try {
            appendJsonLines(batch);
            metrics.recordBatch(batch.size(), System.nanoTime() - started);
            return true;
        } catch (IOException error) {
            metrics.recordFailure();
            // Retry en tête logique : ConcurrentLinkedQueue ne permet pas addFirst,
            // mais les records sont réinsérés et seront repris au prochain flush.
            for (CraftLogRecord record : batch) queue.offer(record);
            plugin.getLogger().warning("Erreur flush logs KCraft: " + error.getMessage());
            return false;
        }
    }

    private void appendJsonLines(List<CraftLogRecord> batch) throws IOException {
        File file = new File(plugin.getConfigManager().getLogsFolder(), normalizeLogFile());
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(file, true), StandardCharsets.UTF_8))) {
            for (CraftLogRecord record : batch) {
                writer.write(gson.toJson(record.toMap()));
                writer.newLine();
            }
        }
    }

    private String normalizeLogFile() {
        String configured = plugin.getConfigManager().getLogFile();
        if (configured == null || configured.trim().isEmpty()) return "crafts-history.jsonl";
        String trimmed = configured.trim();
        if (trimmed.endsWith(".jsonl")) return trimmed;
        if (trimmed.endsWith(".json")) return trimmed.substring(0, trimmed.length() - 5) + ".jsonl";
        return trimmed + ".jsonl";
    }

    private void updateStats(Player player, String craftId, boolean success) {
        totalCrafts.incrementAndGet();
        if (!success) failedCrafts.incrementAndGet();

        AtomicLong count = craftCounts.get(craftId);
        if (count == null) {
            AtomicLong created = new AtomicLong();
            AtomicLong existing = craftCounts.putIfAbsent(craftId, created);
            count = existing == null ? created : existing;
        }
        count.incrementAndGet();

        UUID id = player.getUniqueId();
        PlayerStats stats = playerStats.get(id);
        if (stats == null) {
            PlayerStats created = new PlayerStats(player.getName());
            PlayerStats existing = playerStats.putIfAbsent(id, created);
            stats = existing == null ? created : existing;
        }
        stats.record(player.getName(), craftId, success, System.currentTimeMillis());
    }

    public synchronized void saveAll() {
        BukkitTask task = workerTask;
        if (task != null) {
            task.cancel();
            workerTask = null;
        }

        // Une panne disque ne doit pas bloquer indéfiniment l'arrêt du serveur.
        while (!queue.isEmpty()) {
            if (!flushQueue(DEFAULT_MAX_BATCH)) {
                plugin.getLogger().severe("Arrêt du flush KCraft après erreur disque; "
                    + queue.size() + " événement(s) non écrits, conservés en mémoire uniquement.");
                break;
            }
        }

        if (!plugin.getConfigManager().isLoggingEnabled()) return;
        try {
            writeJsonAtomic(new File(plugin.getConfigManager().getLogsFolder(), "stats-global.json"), getGlobalStats());
            if (plugin.getConfigManager().isPerPlayerLogging()) {
                Map<String, Object> all = new LinkedHashMap<String, Object>();
                for (Map.Entry<UUID, PlayerStats> entry : playerStats.entrySet()) {
                    all.put(entry.getKey().toString(), entry.getValue().snapshot());
                }
                writeJsonAtomic(new File(plugin.getConfigManager().getLogsFolder(), "stats-players.json"), all);
            }
        } catch (IOException error) {
            plugin.getLogger().warning("Erreur sauvegarde stats KCraft: " + error.getMessage());
        }
    }

    private void writeJsonAtomic(File target, Object value) throws IOException {
        File parent = target.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();
        File tmp = new File(target.getParentFile(), target.getName() + ".tmp");
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(tmp, false), StandardCharsets.UTF_8))) {
            gson.toJson(value, writer);
        }
        try {
            Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
            Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public Map<String, Object> getPlayerStats(UUID playerId) {
        PlayerStats stats = playerStats.get(playerId);
        return stats == null ? null : stats.snapshot();
    }

    public Map<String, Object> getGlobalStats() {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("total_crafts", Long.valueOf(totalCrafts.get()));
        out.put("failed_crafts", Long.valueOf(failedCrafts.get()));
        out.put("unique_players", Integer.valueOf(playerStats.size()));

        String most = "";
        long mostCount = 0L;
        Map<String, Long> counts = new LinkedHashMap<String, Long>();
        for (Map.Entry<String, AtomicLong> entry : craftCounts.entrySet()) {
            long value = entry.getValue().get();
            counts.put(entry.getKey(), Long.valueOf(value));
            if (value > mostCount) {
                mostCount = value;
                most = entry.getKey();
            }
        }
        out.put("most_crafted", most);
        out.put("craft_counts", counts);
        out.put("pending_logs", Integer.valueOf(queue.size()));
        return out;
    }

    public int getPendingLogCount() { return queue.size(); }
    public LoggingMetrics getMetrics() { return metrics; }

    private static final class PlayerStats {
        private String name;
        private long total;
        private long success;
        private long lastCraft;
        private final Map<String, Long> crafts = new HashMap<String, Long>();

        private PlayerStats(String name) { this.name = name; }

        private synchronized void record(String currentName, String craftId, boolean ok, long timestamp) {
            name = currentName;
            total++;
            if (ok) success++;
            lastCraft = timestamp;
            Long count = crafts.get(craftId);
            crafts.put(craftId, Long.valueOf(count == null ? 1L : count.longValue() + 1L));
        }

        private synchronized Map<String, Object> snapshot() {
            Map<String, Object> out = new LinkedHashMap<String, Object>();
            out.put("name", name);
            out.put("total_crafts", Long.valueOf(total));
            out.put("success_count", Long.valueOf(success));
            out.put("success_rate", Double.valueOf(total == 0L ? 0D : success * 100D / total));
            out.put("last_craft", Long.valueOf(lastCraft));
            out.put("crafts", new LinkedHashMap<String, Long>(crafts));
            return out;
        }
    }
}
