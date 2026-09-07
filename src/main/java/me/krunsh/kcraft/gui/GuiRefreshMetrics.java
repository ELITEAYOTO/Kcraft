package me.krunsh.kcraft.gui;

import java.util.concurrent.atomic.AtomicLong;

/** Compteurs GUI V2.8. */
public final class GuiRefreshMetrics {

    private final AtomicLong marks = new AtomicLong();
    private final AtomicLong coalesced = new AtomicLong();
    private final AtomicLong drains = new AtomicLong();
    private final AtomicLong refreshed = new AtomicLong();
    private final AtomicLong staleSkipped = new AtomicLong();
    private final AtomicLong previewCreated = new AtomicLong();
    private final AtomicLong previewReused = new AtomicLong();
    private final AtomicLong previewConsumed = new AtomicLong();

    public void recordMark(boolean first) {
        marks.incrementAndGet();
        if (!first) coalesced.incrementAndGet();
    }

    public void recordDrain() { drains.incrementAndGet(); }
    public void recordRefreshed() { refreshed.incrementAndGet(); }
    public void recordStaleSkipped() { staleSkipped.incrementAndGet(); }
    public void recordPreviewCreated() { previewCreated.incrementAndGet(); }
    public void recordPreviewReused() { previewReused.incrementAndGet(); }
    public void recordPreviewConsumed() { previewConsumed.incrementAndGet(); }

    public long getMarks() { return marks.get(); }
    public long getCoalesced() { return coalesced.get(); }
    public long getDrains() { return drains.get(); }
    public long getRefreshed() { return refreshed.get(); }
    public long getStaleSkipped() { return staleSkipped.get(); }
    public long getPreviewCreated() { return previewCreated.get(); }
    public long getPreviewReused() { return previewReused.get(); }
    public long getPreviewConsumed() { return previewConsumed.get(); }

    public double getCoalesceRate() {
        long total = marks.get();
        return total <= 0L ? 0D : coalesced.get() * 100D / total;
    }

    public void reset() {
        marks.set(0L);
        coalesced.set(0L);
        drains.set(0L);
        refreshed.set(0L);
        staleSkipped.set(0L);
        previewCreated.set(0L);
        previewReused.set(0L);
        previewConsumed.set(0L);
    }
}
