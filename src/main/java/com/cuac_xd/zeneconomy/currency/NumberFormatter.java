package com.cuac_xd.zeneconomy.currency;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.NavigableMap;
import java.util.TreeMap;

public final class NumberFormatter {

    private static final NavigableMap<Double, String> SUFFIXES = new TreeMap<>();

    static {
        SUFFIXES.put(1_000.0, "k");
        SUFFIXES.put(1_000_000.0, "M");
        SUFFIXES.put(1_000_000_000.0, "B");
        SUFFIXES.put(1_000_000_000_000.0, "T");
        SUFFIXES.put(1_000_000_000_000_000.0, "Q");
    }

    private NumberFormatter() {}

    public static String format(double value, int decimals, boolean abbreviated) {
        if (abbreviated && Math.abs(value) >= 1_000.0) {
            return formatAbbreviated(value, decimals);
        }
        return formatStandard(value, decimals);
    }

    public static String formatStandard(double value, int decimals) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator(',');
        symbols.setDecimalSeparator('.');

        StringBuilder pattern = new StringBuilder("#,##0");
        if (decimals > 0) {
            pattern.append(".");
            pattern.append("0".repeat(decimals));
        }

        DecimalFormat df = new DecimalFormat(pattern.toString(), symbols);
        return df.format(value);
    }

    public static String formatAbbreviated(double value, int decimals) {
        if (value < 0) {
            return "-" + formatAbbreviated(-value, decimals);
        }
        if (value < 1_000.0) {
            // Si el valor es entero, evitar mostrar decimales innecesarios en formato abreviado
            if (value == Math.floor(value)) {
                return formatStandard(value, 0);
            }
            return formatStandard(value, decimals);
        }

        var entry = SUFFIXES.floorEntry(value);
        if (entry == null) {
            return formatStandard(value, decimals);
        }

        Double divideBy = entry.getKey();
        String suffix = entry.getValue();

        double truncated = value / divideBy;
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setDecimalSeparator('.');
        // Keep at most 2 decimal places for compact view
        DecimalFormat df = new DecimalFormat("0.##", symbols);
        return df.format(truncated) + suffix;
    }
}
