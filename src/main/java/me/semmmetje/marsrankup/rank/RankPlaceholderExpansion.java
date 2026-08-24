package me.semmmetje.marsrankup.rank;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.semmmetje.marsrankup.MarsRankupPlugin;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Locale;

public final class RankPlaceholderExpansion extends PlaceholderExpansion {
    private final MarsRankupPlugin plugin;
    public RankPlaceholderExpansion(MarsRankupPlugin plugin) { this.plugin = plugin; }

    @Override public @NotNull String getIdentifier() { return "marsrankup"; }
    @Override public @NotNull String getAuthor() { return "Semmmetje"; }
    @Override public @NotNull String getVersion() { return plugin.getPluginMeta().getVersion(); }
    @Override public boolean persist() { return true; }

    @Override
    public @Nullable String onRequest(OfflinePlayer offlinePlayer, @NotNull String params) {
        if (offlinePlayer == null || !offlinePlayer.isOnline()) return "";

        Player player = offlinePlayer.getPlayer();
        if (player == null) return "";

        String key = params.toLowerCase(Locale.ROOT);
        int current = plugin.ranks().current(player);
        Integer nextId = plugin.ranks().next(player);
        RankDefinition next = nextId == null ? null : plugin.ranks().get(nextId);

        return switch (key) {
            case "rank" -> String.valueOf(current);
            case "rank_name" -> plugin.ranks().currentName(player);
            case "next_rank" -> nextId == null ? "MAX" : String.valueOf(nextId);
            case "next_rank_name" -> next == null ? "MAX" : next.displayName();
            case "can_claim_next" -> next != null && plugin.ranks().canClaim(player, next) ? "true" : "false";
            default -> resolveDynamic(player, key);
        };
    }

    private String resolveDynamic(Player player, String key) {
        if (key.startsWith("can_claim_")) {
            RankDefinition rank = rankFromSuffix(key, "can_claim_");
            return rank != null && plugin.ranks().canClaim(player, rank) ? "true" : "false";
        }
        if (key.startsWith("requirement_money_")) {
            RankDefinition rank = rankFromSuffix(key, "requirement_money_");
            return rank == null ? "" : status(plugin.requirements().meetsMoney(player, rank), rank.moneyRequired());
        }
        if (key.startsWith("requirement_playtime_")) {
            RankDefinition rank = rankFromSuffix(key, "requirement_playtime_");
            return rank == null ? "" : status(plugin.requirements().meetsPlaytime(player, rank), rank.playtimeRequired());
        }
        if (key.startsWith("requirement_permissions_")) {
            RankDefinition rank = rankFromSuffix(key, "requirement_permissions_");
            return rank == null ? "" : status(plugin.requirements().meetsPermissions(player, rank), rank.permissionsRequired().isEmpty() ? "none" : String.valueOf(rank.permissionsRequired().size()));
        }
        if (key.startsWith("requirement_custom_")) {
            RankDefinition rank = rankFromSuffix(key, "requirement_custom_");
            return rank == null ? "" : status(plugin.requirements().meetsCustom(player, rank), rank.customRequirements().isEmpty() ? "none" : String.valueOf(rank.customRequirements().size()));
        }
        return null;
    }

    private RankDefinition rankFromSuffix(String value, String prefix) {
        try { return plugin.ranks().get(Integer.parseInt(value.substring(prefix.length()))); }
        catch (NumberFormatException ignored) { return null; }
    }

    private static String status(boolean success, String requirement) {
        return (success ? "&a✔ " : "&c✘ ") + requirement;
    }
}
