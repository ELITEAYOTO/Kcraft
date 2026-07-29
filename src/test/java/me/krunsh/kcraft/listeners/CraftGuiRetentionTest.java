package me.krunsh.kcraft.listeners;

import java.lang.reflect.Field;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.junit.Test;

import me.krunsh.kcraft.gui.CraftTableGUI;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

public class CraftGuiRetentionTest {

    @Test
    public void guiAndListenerDoNotStorePlayerInstances() {
        assertNoPlayerField(CraftTableGUI.class);
        assertNoPlayerField(CraftGUIListener.class);
    }

    @Test
    public void cleanupIsIdempotentForOnePlayerSession() {
        PlayerGuiRegistry<Object> registry = new PlayerGuiRegistry<Object>();
        UUID playerId = UUID.randomUUID();
        Object gui = new Object();
        final int[] cleanupCalls = {0};

        registry.put(playerId, gui);
        registry.cleanup(playerId, new PlayerGuiRegistry.Cleanup<Object>() {
            @Override public void run(Object value) {
                cleanupCalls[0]++;
            }
        });
        registry.cleanup(playerId, new PlayerGuiRegistry.Cleanup<Object>() {
            @Override public void run(Object value) {
                cleanupCalls[0]++;
            }
        });

        assertEquals(1, cleanupCalls[0]);
        assertEquals(0, registry.size());
    }

    @Test
    public void failedRestitutionStillReleasesSession() {
        PlayerGuiRegistry<Object> registry = new PlayerGuiRegistry<Object>();
        UUID playerId = UUID.randomUUID();
        final Object gui = new Object();
        registry.put(playerId, gui);

        try {
            registry.cleanup(playerId, new PlayerGuiRegistry.Cleanup<Object>() {
                @Override public void run(Object value) {
                    assertSame(gui, value);
                    throw new IllegalStateException("simulated inventory failure");
                }
            });
            fail("The simulated failure must propagate");
        } catch (IllegalStateException expected) {
            assertEquals("simulated inventory failure", expected.getMessage());
        }

        assertEquals(0, registry.size());
    }

    private static void assertNoPlayerField(Class<?> type) {
        for (Field field : type.getDeclaredFields()) {
            assertFalse(type.getSimpleName() + "." + field.getName()
                + " must not retain Player", Player.class.isAssignableFrom(field.getType()));
        }
    }
}
