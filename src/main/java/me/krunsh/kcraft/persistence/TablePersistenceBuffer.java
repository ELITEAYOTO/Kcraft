package me.krunsh.kcraft.persistence;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.bukkit.scheduler.BukkitTask;

import com.google.gson.Gson;

import me.krunsh.kcraft.Kcraft;
import me.krunsh.kcraft.models.PlacedTableData;

/** Debounce + écriture async atomique de placed_tables.json. */
public final class TablePersistenceBuffer {

    public interface SnapshotProvider {
        List<PlacedTableData> snapshot();
    }

    private final Kcraft plugin;
    private final File target;
    private final Gson gson;
    private final SnapshotProvider provider;
    private final long debounceTicks;

    private final AtomicLong writes = new AtomicLong();
    private final AtomicLong coalesced = new AtomicLong();
    private final AtomicLong failures = new AtomicLong();

    private volatile long generation;
    private volatile BukkitTask scheduled;

    public TablePersistenceBuffer(Kcraft plugin, File target, Gson gson,
                                  SnapshotProvider provider, long debounceTicks) {
        this.plugin = plugin;
        this.target = target;
        this.gson = gson;
        this.provider = provider;
        this.debounceTicks = Math.max(1L, debounceTicks);
    }

    public synchronized void markDirty() {
        generation++;
        if (scheduled != null) {
            coalesced.incrementAndGet();
            return;
        }

        scheduled = plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
            @Override public void run() { captureAndFlush(); }
        }, debounceTicks);
    }

    private void captureAndFlush() {
        final long capturedGeneration;
        final List<PlacedTableData> snapshot;
        synchronized (this) {
            scheduled = null;
            capturedGeneration = generation;
            snapshot = provider.snapshot();
        }

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override public void run() {
                try {
                    writeAtomic(snapshot);
                    writes.incrementAndGet();
                } catch (IOException error) {
                    failures.incrementAndGet();
                    plugin.getLogger().warning("Erreur persistence tables: " + error.getMessage());
                } finally {
                    plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
                        @Override public void run() {
                            synchronized (TablePersistenceBuffer.this) {
                                if (generation != capturedGeneration && scheduled == null) {
                                    markDirty();
                                }
                            }
                        }
                    });
                }
            }
        });
    }

    private void writeAtomic(List<PlacedTableData> snapshot) throws IOException {
        File parent = target.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();
        File tmp = new File(parent, target.getName() + ".tmp");
        try (OutputStreamWriter writer = new OutputStreamWriter(
                new FileOutputStream(tmp, false), StandardCharsets.UTF_8)) {
            gson.toJson(snapshot, writer);
        }
        try {
            Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
            Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public void flushNow(List<PlacedTableData> snapshot) {
        BukkitTask task = scheduled;
        if (task != null) {
            task.cancel();
            scheduled = null;
        }
        try {
            writeAtomic(snapshot);
            writes.incrementAndGet();
        } catch (IOException error) {
            failures.incrementAndGet();
            plugin.getLogger().warning("Erreur flush final tables: " + error.getMessage());
        }
    }

    public long getWrites() { return writes.get(); }
    public long getCoalesced() { return coalesced.get(); }
    public long getFailures() { return failures.get(); }

    public void resetMetrics() {
        writes.set(0L);
        coalesced.set(0L);
        failures.set(0L);
    }
}
