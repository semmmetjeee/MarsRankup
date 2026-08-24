package me.semmmetje.marsrankup.rank;

import me.semmmetje.marsrankup.MarsRankupPlugin;
import me.semmmetje.marsrankup.util.NumberParser;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.NavigableMap;
import java.util.TreeMap;

public final class RankManager {
    public enum ClaimResult { SUCCESS, ALREADY_CLAIMED, WRONG_ORDER, REQUIREMENTS_NOT_MET, ECONOMY_FAILED }

    private final MarsRankupPlugin plugin;
    private final NavigableMap<Integer, RankDefinition> ranks = new TreeMap<>();

    public RankManager(MarsRankupPlugin plugin) { this.plugin = plugin; }

    public void load() {
        ranks.clear();
        File file = new File(plugin.getDataFolder(), "ranks.yml");
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("ranks");

        if (root == null) {
            plugin.getLogger().warning("ranks.yml has no 'ranks' section.");
            return;
        }

        for (String key : root.getKeys(false)) {
            int id;
            try { id = Integer.parseInt(key); }
            catch (NumberFormatException ex) {
                plugin.getLogger().warning("Ignoring non-numeric rank id: " + key);
                continue;
            }

            ConfigurationSection section = root.getConfigurationSection(key);
            if (section == null) continue;

            ranks.put(id, new RankDefinition(
                    id,
                    section.getString("display-name", "Rank " + id),
                    section.getString("money-required", "0"),
                    section.getString("playtime-required", "0s"),
                    java.util.List.copyOf(section.getStringList("permissions-required")),
                    java.util.List.copyOf(section.getStringList("custom-requirements")),
                    section.getConfigurationSection("item"),
                    java.util.List.copyOf(section.getStringList("rewards"))
            ));
        }

        plugin.getLogger().info("Loaded " + ranks.size() + " rank(s).");
    }

    public Collection<RankDefinition> all() { return Collections.unmodifiableCollection(ranks.values()); }
    public RankDefinition get(int id) { return ranks.get(id); }

    public int current(Player player) {
        return plugin.playerData().getRank(player.getUniqueId(), plugin.getConfig().getInt("settings.start-rank", 0));
    }

    public String currentName(Player player) {
        RankDefinition rank = ranks.get(current(player));
        return rank == null ? String.valueOf(current(player)) : rank.displayName();
    }

    public Integer next(Player player) { return ranks.higherKey(current(player)); }
    public boolean isClaimed(Player player, int rankId) { return current(player) >= rankId; }

    public boolean canClaim(Player player, RankDefinition rank) {
        if (rank == null || isClaimed(player, rank.id())) return false;
        if (!plugin.getConfig().getBoolean("settings.allow-skipping-ranks", false)) {
            Integer next = next(player);
            if (next == null || next != rank.id()) return false;
        }
        return plugin.requirements().meetsAll(player, rank);
    }

    public ClaimResult claim(Player player, RankDefinition rank) {
        if (rank == null) return ClaimResult.REQUIREMENTS_NOT_MET;
        if (isClaimed(player, rank.id())) return ClaimResult.ALREADY_CLAIMED;

        if (!plugin.getConfig().getBoolean("settings.allow-skipping-ranks", false)) {
            Integer next = next(player);
            if (next == null || next != rank.id()) return ClaimResult.WRONG_ORDER;
        }

        if (!plugin.requirements().meetsAll(player, rank)) return ClaimResult.REQUIREMENTS_NOT_MET;

        double money;
        try { money = NumberParser.parse(rank.moneyRequired()); }
        catch (RuntimeException ex) { return ClaimResult.REQUIREMENTS_NOT_MET; }

        if (money > 0D && plugin.getConfig().getBoolean("settings.withdraw-money-on-claim", true)) {
            if (!plugin.vault().withdraw(player, money)) return ClaimResult.ECONOMY_FAILED;
        }

        plugin.playerData().setRank(player.getUniqueId(), rank.id());
        plugin.actions().executeRewards(player, rank.rewards());
        return ClaimResult.SUCCESS;
    }

    public boolean requiresVault() {
        return ranks.values().stream().anyMatch(rank -> {
            try { return NumberParser.parse(rank.moneyRequired()) > 0D; }
            catch (RuntimeException ignored) { return false; }
        });
    }
}
