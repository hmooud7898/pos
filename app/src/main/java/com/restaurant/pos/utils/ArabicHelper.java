package com.restaurant.pos.utils;

public class ArabicHelper {
    // Simple helper - Android handles RTL natively
    // This just ensures text direction is correct
    public static String reshape(String text) {
        if (text == null) return "";
        return text;
    }

    public static String formatLL(long value) {
        return String.format(java.util.Locale.getDefault(), "%,d", value);
    }

    public static String formatUSD(double value) {
        return String.format(java.util.Locale.getDefault(), "%.2f", value);
    }
}
