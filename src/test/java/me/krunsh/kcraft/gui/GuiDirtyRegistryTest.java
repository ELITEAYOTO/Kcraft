package me.krunsh.kcraft.gui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.Test;

public class GuiDirtyRegistryTest {

    @Test
    public void duplicateMarksAreCoalesced() {
        GuiDirtyRegistry registry =
            new GuiDirtyRegistry();

        UUID uuid =
            UUID.randomUUID();

        assertTrue(
            registry.mark(uuid)
        );

        assertFalse(
            registry.mark(uuid)
        );

        assertFalse(
            registry.mark(uuid)
        );

        assertEquals(
            1,
            registry.size()
        );

        List<UUID> drained =
            registry.drain();

        assertEquals(
            1,
            drained.size()
        );

        assertEquals(
            uuid,
            drained.get(0)
        );

        assertTrue(
            registry.isEmpty()
        );
    }

    @Test
    public void removePreventsStaleRefresh() {
        GuiDirtyRegistry registry =
            new GuiDirtyRegistry();

        UUID uuid =
            UUID.randomUUID();

        registry.mark(uuid);

        assertTrue(
            registry.remove(uuid)
        );

        assertTrue(
            registry.drain()
                .isEmpty()
        );
    }

    @Test
    public void drainAllowsRemarkForNextTick() {
        GuiDirtyRegistry registry =
            new GuiDirtyRegistry();

        UUID uuid =
            UUID.randomUUID();

        registry.mark(uuid);
        registry.drain();

        assertTrue(
            registry.mark(uuid)
        );

        assertEquals(
            1,
            registry.size()
        );
    }
}
