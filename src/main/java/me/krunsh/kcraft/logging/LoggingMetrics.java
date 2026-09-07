package me.krunsh.kcraft.logging;

import java.util.concurrent.atomic.AtomicLong;

/** Métriques logging V2.8. */
public final class LoggingMetrics {

    private final AtomicLong queued = new AtomicLong();
    private final AtomicLong written = new AtomicLong();
    private final AtomicLong batches = new AtomicLong();
    private final AtomicLong failures = new AtomicLong();
    private final AtomicLong dropped = new AtomicLong();
    private final AtomicLong maxQueue = new AtomicLong();
    private final AtomicLong totalWriteNanos = new AtomicLong();
    private final AtomicLong maxWriteNanos = new AtomicLong();

    public void recordQueued(long queueSize) {
        queued.incrementAndGet();
        updateMax(maxQueue, queueSize);
    }

    public void recordBatch(int count, long nanos) {
        batches.incrementAndGet();
        written.addAndGet(Math.max(0, count));
        totalWriteNanos.addAndGet(Math.max(0L, nanos));
        updateMax(maxWriteNanos, Math.max(0L, nanos));
    }

    public void recordFailure() { failures.incrementAndGet(); }
    public void recordDropped() { dropped.incrementAndGet(); }

    public long getQueued() { return queued.get(); }
    public long getWritten() { return written.get(); }
    public long getBatches() { return batches.get(); }
    public long getFailures() { return failures.get(); }
    public long getDropped() { return dropped.get(); }
    public long getMaxQueue() { return maxQueue.get(); }

    public double getAverageWriteMicros() {
        long count = batches.get();
        return count == 0L ? 0D : (totalWriteNanos.get() / 1000D) / count;
    }

    public double getMaxWriteMicros() { return maxWriteNanos.get() / 1000D; }

    public void reset() {
        queued.set(0L);
        written.set(0L);
        batches.set(0L);
        failures.set(0L);
        dropped.set(0L);
        maxQueue.set(0L);
        totalWriteNanos.set(0L);
        maxWriteNanos.set(0L);
    }

    private static void updateMax(AtomicLong target, long value) {
        while (true) {
            long old = target.get();
            if (value <= old) return;
            if (target.compareAndSet(old, value)) return;
        }
    }
}
