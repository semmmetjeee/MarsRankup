package me.semmmetje.marsrankup.rank;

import me.clip.placeholderapi.PlaceholderAPI;
import me.semmmetje.marsrankup.MarsRankupPlugin;
import me.semmmetje.marsrankup.util.NumberParser;
import me.semmmetje.marsrankup.util.TimeParser;
import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RequirementEvaluator {
    private static final Pattern EXPRESSION = Pattern.compile("^(.+?)\\s*(>=|<=|==|!=|>|<)\\s*(.+)$");
    private final MarsRankupPlugin plugin;

    public RequirementEvaluator(MarsRankupPlugin plugin) { this.plugin = plugin; }

    public boolean meetsAll(Player player, RankDefinition rank) {
        return meetsMoney(player, rank) && meetsPlaytime(player, rank) && meetsPermissions(player, rank) && meetsCustom(player, rank);
    }

    public boolean meetsMoney(Player player, RankDefinition rank) {
        double required;
        try { required = NumberParser.parse(rank.moneyRequired()); }
        catch (RuntimeException ex) {
            plugin.getLogger().warning("Invalid money-required on rank " + rank.id() + ": " + rank.moneyRequired());
            return false;
        }
        return required <= 0D || (plugin.vault().available() && plugin.vault().balance(player) >= required);
    }

    public boolean meetsPlaytime(Player player, RankDefinition rank) {
        if (rank.playtimeRequired() == null || rank.playtimeRequired().isBlank()) return true;
        try {
            long requiredTicks = TimeParser.parseToTicks(rank.playtimeRequired());
            long playedTicks = player.getStatistic(Statistic.PLAY_ONE_MINUTE);
            return playedTicks >= requiredTicks;
        } catch (RuntimeException ex) {
            plugin.getLogger().warning("Invalid playtime-required on rank " + rank.id() + ": " + rank.playtimeRequired());
            return false;
        }
    }

    public boolean meetsPermissions(Player player, RankDefinition rank) {
        return rank.permissionsRequired().stream().filter(p -> p != null && !p.isBlank()).allMatch(player::hasPermission);
    }

    public boolean meetsCustom(Player player, RankDefinition rank) {
        return rank.customRequirements().stream().allMatch(req -> evaluate(player, req));
    }

    public boolean evaluate(Player player, String expression) {
        String resolved = resolve(player, expression).trim();
        Matcher matcher = EXPRESSION.matcher(resolved);
        if (!matcher.matches()) {
            plugin.debug("Could not parse requirement: " + expression + " -> " + resolved);
            return false;
        }

        String left = clean(matcher.group(1));
        String operator = matcher.group(2);
        String right = clean(matcher.group(3));

        Double leftNumber = tryNumber(left);
        Double rightNumber = tryNumber(right);

        if (leftNumber != null && rightNumber != null) {
            return switch (operator) {
                case ">=" -> leftNumber >= rightNumber;
                case "<=" -> leftNumber <= rightNumber;
                case ">" -> leftNumber > rightNumber;
                case "<" -> leftNumber < rightNumber;
                case "==" -> Double.compare(leftNumber, rightNumber) == 0;
                case "!=" -> Double.compare(leftNumber, rightNumber) != 0;
                default -> false;
            };
        }

        int comparison = left.compareToIgnoreCase(right);
        return switch (operator) {
            case "==" -> comparison == 0;
            case "!=" -> comparison != 0;
            default -> false;
        };
    }

    public String resolve(Player player, String text) {
        if (text == null) return "";
        String value = text.replace("%player%", player.getName()).replace("%player_name%", player.getName());
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) value = PlaceholderAPI.setPlaceholders(player, value);
        return value;
    }

    private static String clean(String input) {
        String value = input.trim();
        if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
            value = value.substring(1, value.length() - 1);
        }
        return value.replace("§", "").replace(",", "").trim();
    }

    private static Double tryNumber(String raw) {
        try { return NumberParser.parse(raw.toLowerCase(Locale.ROOT)); }
        catch (RuntimeException ignored) { return null; }
    }
}
