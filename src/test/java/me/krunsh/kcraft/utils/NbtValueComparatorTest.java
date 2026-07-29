package me.krunsh.kcraft.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class NbtValueComparatorTest {

    @Test
    public void integralNbtTypesCompareByValue() {
        assertTrue(NbtValueComparator.equivalent(Integer.valueOf(3), Byte.valueOf((byte) 3)));
        assertFalse(NbtValueComparator.equivalent(Integer.valueOf(3), Long.valueOf(4L)));
    }

    @Test
    public void citIdentifiersAreExactAndCaseSensitive() {
        assertTrue(NbtValueComparator.equivalent("azurite_chestplate", "azurite_chestplate"));
        assertFalse(NbtValueComparator.equivalent("azurite_chestplate", "Azurite_Chestplate"));
    }

    @Test
    public void invalidBooleanTextNeverMatchesFalse() {
        assertFalse(NbtValueComparator.equivalent(Boolean.FALSE, "not-a-boolean"));
        assertTrue(NbtValueComparator.equivalent(Boolean.TRUE, Integer.valueOf(1)));
    }

    @Test
    public void arraysUseContentRatherThanObjectIdentity() {
        assertTrue(NbtValueComparator.equivalent(new int[] {1, 2}, new int[] {1, 2}));
        assertFalse(NbtValueComparator.equivalent(new int[] {1, 2}, new int[] {2, 1}));
    }
}
