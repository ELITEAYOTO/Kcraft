package me.krunsh.kcraft.api.events;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.UUID;

import org.junit.Test;

import me.krunsh.kcraft.api.execution.CraftExecutionContext;
import me.krunsh.kcraft.api.execution.CraftExecutionSource;
import me.krunsh.kcraft.models.CraftRecipe;

public class KcraftPostCraftEventV2Test {

    @Test
    public void legacyConstructorCapturesBatchContext() {
        CraftRecipe recipe =
            new CraftRecipe("test");

        UUID tx =
            UUID.randomUUID();

        try (CraftExecutionContext.Scope ignored =
                CraftExecutionContext.enter(
                    CraftExecutionSource.SHIFT_BATCH,
                    tx,
                    4,
                    10)) {

            KcraftPostCraftEvent event =
                new KcraftPostCraftEvent(
                    null,
                    recipe,
                    null,
                    true,
                    false
                );

            assertEquals(
                CraftExecutionSource.SHIFT_BATCH,
                event.getExecutionSource()
            );

            assertEquals(
                tx,
                event.getTransactionId()
            );

            assertEquals(
                4,
                event.getBatchIndex()
            );

            assertEquals(
                10,
                event.getPlannedBatchSize()
            );

            assertTrue(
                event.isLegacyBatchElement()
            );
        }
    }
}
