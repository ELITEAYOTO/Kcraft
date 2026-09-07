package me.krunsh.kcraft.api.execution;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.UUID;

import org.junit.Test;

public class CraftExecutionContextTest {

    @Test
    public void defaultsToUnknown() {
        CraftExecutionContext.Snapshot snapshot =
            CraftExecutionContext.snapshot();

        assertEquals(
            CraftExecutionSource.UNKNOWN,
            snapshot.getSource()
        );

        assertNull(
            snapshot.getTransactionId()
        );

        assertEquals(
            -1,
            snapshot.getBatchIndex()
        );
    }

    @Test
    public void scopeRestoresPreviousContext() {
        UUID outerId =
            UUID.randomUUID();

        UUID innerId =
            UUID.randomUUID();

        try (CraftExecutionContext.Scope outer =
                CraftExecutionContext.enter(
                    CraftExecutionSource.CUSTOM_GUI_SINGLE,
                    outerId,
                    -1,
                    1)) {

            assertEquals(
                outerId,
                CraftExecutionContext.snapshot()
                    .getTransactionId()
            );

            try (CraftExecutionContext.Scope inner =
                    CraftExecutionContext.enter(
                        CraftExecutionSource.SHIFT_BATCH,
                        innerId,
                        3,
                        12)) {

                CraftExecutionContext.Snapshot current =
                    CraftExecutionContext.snapshot();

                assertEquals(
                    CraftExecutionSource.SHIFT_BATCH,
                    current.getSource()
                );

                assertEquals(
                    innerId,
                    current.getTransactionId()
                );

                assertEquals(
                    3,
                    current.getBatchIndex()
                );

                assertEquals(
                    12,
                    current.getPlannedBatchSize()
                );
            }

            assertEquals(
                outerId,
                CraftExecutionContext.snapshot()
                    .getTransactionId()
            );
        }

        assertEquals(
            CraftExecutionSource.UNKNOWN,
            CraftExecutionContext.snapshot()
                .getSource()
        );
    }
}
