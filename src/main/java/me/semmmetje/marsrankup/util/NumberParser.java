package me.semmmetje.marsrankup.util;

import java.math.BigDecimal;
import java.util.Locale;

public final class NumberParser {
    private NumberParser() {}

    public static double parse(String raw) {
        if (raw == null || raw.isBlank()) return 0D;

        String value = raw.trim().toLowerCase(Locale.ROOT)
                .replace("_", "").replace(",", "").replace("€", "").replace("$", "");

        BigDecimal multiplier = BigDecimal.ONE;
        if (value.endsWith("k")) { multiplier = BigDecimal.valueOf(1_000L); value = value.substring(0, value.length() - 1); }
        else if (value.endsWith("m")) { multiplier = BigDecimal.valueOf(1_000_000L); value = value.substring(0, value.length() - 1); }
        else if (value.endsWith("b")) { multiplier = BigDecimal.valueOf(1_000_000_000L); value = value.substring(0, value.length() - 1); }
        else if (value.endsWith("t")) { multiplier = BigDecimal.valueOf(1_000_000_000_000L); value = value.substring(0, value.length() - 1); }

        return new BigDecimal(value.trim()).multiply(multiplier).doubleValue();
    }
}
