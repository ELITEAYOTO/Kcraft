package me.krunsh.kcraft.utils;

import java.util.Collections;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ItemStackPresenceTest {

    @Test
    public void nullIsEmptyAndNeverReachesNbtApi() {
        assertFalse(ItemStackUtil.isPresent(null));
        assertFalse(NBTUtil.hasCustomNBT(null));
        assertFalse(NBTUtil.hasNBTKey(null, "sparrowmc-item"));
        assertNull(NBTUtil.getNBTValue(null, "sparrowmc-item"));
    }

    @Test
    public void airIsEmptyAndNeverReachesNbtApi() {
        ItemStack air = new ItemStack(Material.AIR, 1);
        assertFalse(ItemStackUtil.isPresent(air));
        assertFalse(NBTUtil.hasCustomNBT(air));
        assertFalse(NBTUtil.hasNBTData(air,
            Collections.<String, Object>singletonMap("sparrowmc-item", "azurite")));
        assertTrue(NBTUtil.getNBTKeys(air).isEmpty());
    }

    @Test
    public void zeroAmountIsEmptyAndNeverReachesNbtApi() {
        ItemStack zero = new ItemStack(Material.DIAMOND, 0);
        assertFalse(ItemStackUtil.isPresent(zero));
        assertFalse(NBTUtil.hasCustomNBT(zero));
        assertFalse(NBTUtil.hasMatchingNBT(zero,
            Collections.<String, Object>singletonMap("sparrowmc-item", "azurite")));
    }

    @Test
    public void positiveNonAirItemIsPresent() {
        assertTrue(ItemStackUtil.isPresent(new ItemStack(Material.DIAMOND, 1)));
    }
}
