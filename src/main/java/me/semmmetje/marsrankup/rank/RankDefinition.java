package me.semmmetje.marsrankup.rank;

import org.bukkit.configuration.ConfigurationSection;
import java.util.List;

public record RankDefinition(
        int id,
        String displayName,
        String moneyRequired,
        String playtimeRequired,
        List<String> permissionsRequired,
        List<String> customRequirements,
        ConfigurationSection item,
        List<String> rewards
) {}
