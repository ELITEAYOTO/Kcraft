package me.krunsh.kcraft.diagnostics;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import me.krunsh.kcraft.batch.CraftBatchMetrics;
import me.krunsh.kcraft.gui.GuiRefreshMetrics;
import me.krunsh.kcraft.logging.LoggingMetrics;
import me.krunsh.kcraft.matching.MatchingMetrics;

public class MetricsResetTest {

    @Test
    public void allMetricTypesCanReset() {
        MatchingMetrics matching = new MatchingMetrics();
        matching.record(4, 2, true, 1, 1000L);
        matching.reset();
        assertEquals(0L, matching.getAttempts());

        CraftBatchMetrics batch = new CraftBatchMetrics();
        batch.recordPlan(12, 1000L);
        batch.recordExecuted();
        batch.reset();
        assertEquals(0L, batch.getPlans());
        assertEquals(0L, batch.getExecutedCrafts());

        GuiRefreshMetrics gui = new GuiRefreshMetrics();
        gui.recordMark(false);
        gui.recordRefreshed();
        gui.reset();
        assertEquals(0L, gui.getMarks());
        assertEquals(0L, gui.getRefreshed());

        LoggingMetrics logging = new LoggingMetrics();
        logging.recordQueued(5L);
        logging.recordBatch(4, 1000L);
        logging.reset();
        assertEquals(0L, logging.getQueued());
        assertEquals(0L, logging.getWritten());
    }
}
