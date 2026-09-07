package me.krunsh.kcraft.batch;

import java.util.concurrent.atomic.AtomicLong;

/** Compteurs O(1) V2.8 pour /kcraft perf. */
public final class CraftBatchMetrics {

    private final AtomicLong plans = new AtomicLong();
    private final AtomicLong plannedCrafts = new AtomicLong();
    private final AtomicLong executedCrafts = new AtomicLong();
    private final AtomicLong cancelledCrafts = new AtomicLong();
    private final AtomicLong failedCrafts = new AtomicLong();
    private final AtomicLong totalPlanningNanos = new AtomicLong();
    private final AtomicLong maxPlanningNanos = new AtomicLong();

    public void recordPlan(int crafts, long nanos) {
        plans.incrementAndGet();
        plannedCrafts.addAndGet(Math.max(0, crafts));
        long safe = Math.max(0L, nanos);
        totalPlanningNanos.addAndGet(safe);
        updateMax(safe);
    }

    public void recordExecuted() { executedCrafts.incrementAndGet(); }
    public void recordCancelled() { cancelledCrafts.incrementAndGet(); }
    public void recordFailed() { failedCrafts.incrementAndGet(); }

    public long getPlans() { return plans.get(); }
    public long getPlannedCrafts() { return plannedCrafts.get(); }
    public long getExecutedCrafts() { return executedCrafts.get(); }
    public long getCancelledCrafts() { return cancelledCrafts.get(); }
    public long getFailedCrafts() { return failedCrafts.get(); }

    public double getAveragePlanningMicros() {
        long count = plans.get();
        return count <= 0L ? 0D : (totalPlanningNanos.get() / 1000D) / count;
    }

    public double getMaxPlanningMicros() { return maxPlanningNanos.get() / 1000D; }

    public void reset() {
        plans.set(0L);
        plannedCrafts.set(0L);
        executedCrafts.set(0L);
        cancelledCrafts.set(0L);
        failedCrafts.set(0L);
        totalPlanningNanos.set(0L);
        maxPlanningNanos.set(0L);
    }

    private void updateMax(long nanos) {
        while (true) {
            long previous = maxPlanningNanos.get();
            if (nanos <= previous) return;
            if (maxPlanningNanos.compareAndSet(previous, nanos)) return;
        }
    }
}
