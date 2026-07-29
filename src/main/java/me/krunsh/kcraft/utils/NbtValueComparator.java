package me.krunsh.kcraft.utils;

import java.util.Arrays;

/** Comparaison typee, pure et testable des valeurs NBT configurees. */
public final class NbtValueComparator {
    private NbtValueComparator() {}

    public static boolean equivalent(Object required, Object actual) {
        if (required == actual) return true;
        if (required == null || actual == null) return false;
        if (required instanceof Number && actual instanceof Number) {
            Number left = (Number) required;
            Number right = (Number) actual;
            if (isDecimal(required) || isDecimal(actual)) {
                return Double.compare(left.doubleValue(), right.doubleValue()) == 0;
            }
            return left.longValue() == right.longValue();
        }
        if (required instanceof Boolean || actual instanceof Boolean) {
            Boolean left = parseBoolean(required);
            Boolean right = parseBoolean(actual);
            return left != null && right != null && left.equals(right);
        }
        if (required instanceof byte[] && actual instanceof byte[]) {
            return Arrays.equals((byte[]) required, (byte[]) actual);
        }
        if (required instanceof int[] && actual instanceof int[]) {
            return Arrays.equals((int[]) required, (int[]) actual);
        }
        if (required instanceof long[] && actual instanceof long[]) {
            return Arrays.equals((long[]) required, (long[]) actual);
        }
        // Les identifiants CIT textuels restent sensibles a la casse.
        return required.equals(actual);
    }

    private static boolean isDecimal(Object value) {
        return value instanceof Float || value instanceof Double;
    }

    private static Boolean parseBoolean(Object value) {
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof Number) {
            int number = ((Number) value).intValue();
            if (number == 0) return Boolean.FALSE;
            if (number == 1) return Boolean.TRUE;
            return null;
        }
        if (value instanceof String) {
            String text = ((String) value).trim();
            if ("true".equalsIgnoreCase(text) || "1".equals(text)) return Boolean.TRUE;
            if ("false".equalsIgnoreCase(text) || "0".equals(text)) return Boolean.FALSE;
        }
        return null;
    }
}
