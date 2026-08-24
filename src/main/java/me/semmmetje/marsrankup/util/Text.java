package me.semmmetje.marsrankup.util;

import org.bukkit.ChatColor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Text {
    private static final Pattern GRADIENT = Pattern.compile("(?i)<#([0-9a-f]{6}):#([0-9a-f]{6})>([^<&]*)");
    private static final Pattern HEX = Pattern.compile("(?i)&#([0-9a-f]{6})");
    private Text() {}

    public static String color(String input) {
        if (input == null) return "";
        String text = applyGradients(input);

        Matcher matcher = HEX.matcher(text);
        StringBuffer output = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(output, Matcher.quoteReplacement(legacyHex(matcher.group(1))));
        }
        matcher.appendTail(output);
        return ChatColor.translateAlternateColorCodes('&', output.toString());
    }

    private static String applyGradients(String input) {
        String current = input;
        for (int pass = 0; pass < 32; pass++) {
            Matcher matcher = GRADIENT.matcher(current);
            if (!matcher.find()) return current;
            String replacement = gradient(matcher.group(3), Integer.parseInt(matcher.group(1), 16), Integer.parseInt(matcher.group(2), 16));
            current = matcher.replaceFirst(Matcher.quoteReplacement(replacement));
        }
        return current;
    }

    private static String gradient(String text, int start, int end) {
        int visible = text.codePointCount(0, text.length());
        if (visible == 0) return text;

        int sr=(start>>16)&255, sg=(start>>8)&255, sb=start&255;
        int er=(end>>16)&255, eg=(end>>8)&255, eb=end&255;

        StringBuilder out = new StringBuilder();
        int index = 0;
        for (int offset = 0; offset < text.length();) {
            int cp = text.codePointAt(offset);
            double p = visible <= 1 ? 0D : (double) index / (visible - 1);
            int r=(int)Math.round(sr+(er-sr)*p), g=(int)Math.round(sg+(eg-sg)*p), b=(int)Math.round(sb+(eb-sb)*p);
            out.append(String.format("&#%02x%02x%02x", r, g, b)).appendCodePoint(cp);
            offset += Character.charCount(cp);
            index++;
        }
        return out.toString();
    }

    private static String legacyHex(String hex) {
        StringBuilder out = new StringBuilder("§x");
        for (char c : hex.toCharArray()) out.append('§').append(c);
        return out.toString();
    }
}
