package me.krunsh.kcraft.matching;

import java.util.concurrent.atomic.AtomicLong;

/** Compteurs O(1) V2.8 pour /kcraft perf. */
public final class MatchingMetrics {

    private final AtomicLong attempts = new AtomicLong();
    private final AtomicLong candidates = new AtomicLong();
    private final AtomicLong exactChecks = new AtomicLong();
    private final AtomicLong matches = new AtomicLong();
    private final AtomicLong nbtSnapshots = new AtomicLong();
    private final AtomicLong totalNanos = new AtomicLong();
    private final AtomicLong maxNanos = new AtomicLong();

    public void record(int candidateCount, int checkedCount, boolean matched,
                       int nbtSnapshotCount, long nanos) {
        attempts.incrementAndGet();
        candidates.addAndGet(Math.max(0, candidateCount));
        exactChecks.addAndGet(Math.max(0, checkedCount));
        if (matched) matches.incrementAndGet();
        nbtSnapshots.addAndGet(Math.max(0, nbtSnapshotCount));
        totalNanos.addAndGet(Math.max(0L, nanos));
        updateMax(Math.max(0L, nanos));
    }

    public long getAttempts() { return attempts.get(); }
    public long getCandidates() { return candidates.get(); }
    public long getExactChecks() { return exactChecks.get(); }
    public long getMatches() { return matches.get(); }
    public long getNbtSnapshots() { return nbtSnapshots.get(); }

    public double getAverageCandidates() {
        long count = attempts.get();
        return count <= 0L ? 0D : candidates.get() / (double) count;
    }

    public double getAverageExactChecks() {
        long count = attempts.get();
        return count <= 0L ? 0D : exactChecks.get() / (double) count;
    }

    public double getAverageMicros() {
        long count = attempts.get();
        return count <= 0L ? 0D : (totalNanos.get() / 1000D) / count;
    }

    public double getMaxMicros() { return maxNanos.get() / 1000D; }

    public void reset() {
        attempts.set(0L);
        candidates.set(0L);
        exactChecks.set(0L);
        matches.set(0L);
        nbtSnapshots.set(0L);
        totalNanos.set(0L);
        maxNanos.set(0L);
    }

    private void updateMax(long nanos) {
        while (true) {
            long previous = maxNanos.get();
            if (nanos <= previous) return;
            if (maxNanos.compareAndSet(previous, nanos)) return;
        }
    }
}
