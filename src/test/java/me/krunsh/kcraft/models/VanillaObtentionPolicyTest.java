package me.krunsh.kcraft.models;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.EnumSet;

import org.bukkit.Material;
import org.junit.Test;

public class VanillaObtentionPolicyTest {

    @Test
    public void trappedChestIsBlockedWhenConfigured() {
        assertTrue(VanillaObtentionPolicy.isBlocked(Material.TRAPPED_CHEST,
                EnumSet.of(Material.TRAPPED_CHEST)));
    }

    @Test
    public void ordinaryVanillaResultIsPreserved() {
        assertFalse(VanillaObtentionPolicy.isBlocked(Material.WORKBENCH,
                EnumSet.of(Material.TRAPPED_CHEST)));
        assertFalse(VanillaObtentionPolicy.isBlocked(Material.TRAPPED_CHEST,
                Collections.<Material>emptySet()));
    }
}
