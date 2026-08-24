package me.semmmetje.marsrankup.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TimeParser {
    private static final Pattern PART = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*([smhdw])", Pattern.CASE_INSENSITIVE);
    private TimeParser() {}

    public static long parseToTicks(String raw) {
        if (raw == null || raw.isBlank()) return 0L;

        String compact = raw.replace(" ", "");
        Matcher matcher = PART.matcher(compact);
        double seconds = 0D;
        int cursor = 0;
        boolean found = false;

        while (matcher.find()) {
            if (matcher.start() != cursor) throw new IllegalArgumentException("Invalid duration: " + raw);
            found = true;
            cursor = matcher.end();
            double value = Double.parseDouble(matcher.group(1));
            seconds += switch (matcher.group(2).toLowerCase()) {
                case "s" -> value;
                case "m" -> value * 60D;
                case "h" -> value * 3_600D;
                case "d" -> value * 86_400D;
                case "w" -> value * 604_800D;
                default -> 0D;
            };
        }

        if (!found || cursor != compact.length()) throw new IllegalArgumentException("Invalid duration: " + raw);
        return Math.round(seconds * 20D);
    }
}
