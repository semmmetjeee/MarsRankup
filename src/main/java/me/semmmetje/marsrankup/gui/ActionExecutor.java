package me.semmmetje.marsrankup.gui;

import me.semmmetje.marsrankup.MarsRankupPlugin;
import me.semmmetje.marsrankup.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import java.util.List;
import java.util.Locale;

public final class ActionExecutor {
    private final MarsRankupPlugin plugin;
    public ActionExecutor(MarsRankupPlugin plugin) { this.plugin = plugin; }

    public void execute(Player player, List<String> actions) {
        if (actions == null) return;
        for (String action : actions) execute(player, action, false);
    }

    public void executeRewards(Player player, List<String> rewards) {
        if (rewards == null) return;
        for (String reward : rewards) execute(player, reward, true);
    }

    private void execute(Player player, String raw, boolean reward) {
        if (raw == null || raw.isBlank()) return;

        String resolved = plugin.requirements().resolve(player, raw)
                .replace("%player%", player.getName())
                .replace("%player_name%", player.getName())
                .replace("%rank%", String.valueOf(plugin.ranks().current(player)));
        String upper = resolved.toUpperCase(Locale.ROOT);

        try {
            if (upper.startsWith("[CONSOLE]")) console(strip(resolved, "[CONSOLE]"));
            else if (upper.startsWith("[PLAYER]")) player.performCommand(strip(resolved, "[PLAYER]"));
            else if (upper.startsWith("[COMMAND]")) player.performCommand(strip(resolved, "[COMMAND]"));
            else if (upper.startsWith("[CMD]")) player.performCommand(strip(resolved, "[CMD]"));
            else if (upper.startsWith("[MESSAGE]")) player.sendMessage(Text.color(strip(resolved, "[MESSAGE]")));
            else if (upper.startsWith("[BROADCAST]")) Bukkit.broadcastMessage(Text.color(strip(resolved, "[BROADCAST]")));
            else if (upper.startsWith("[SOUND]")) playSound(player, strip(resolved, "[SOUND]"));
            else if (upper.startsWith("[CLOSE]")) player.closeInventory();
            else if (upper.startsWith("[GUI]")) plugin.guis().open(player, strip(resolved, "[GUI]"));
            else if (upper.startsWith("[MENU]")) plugin.guis().open(player, strip(resolved, "[MENU]"));
            else if (upper.startsWith("[OPEN_MENU]")) plugin.guis().open(player, strip(resolved, "[OPEN_MENU]"));
            else if (reward && plugin.getConfig().getBoolean("settings.untagged-rewards-as-console", true)) console(resolved.replaceFirst("^/", ""));
            else plugin.getLogger().warning("Unknown action: " + raw);
        } catch (Exception ex) {
            plugin.getLogger().warning("Failed action '" + raw + "': " + ex.getMessage());
        }
    }

    private static void console(String command) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replaceFirst("^/", ""));
    }

    private static void playSound(Player player, String input) {
        String[] parts = input.trim().split("\\s+");
        if (parts.length == 0 || parts[0].isBlank()) return;
        Sound sound = Sound.valueOf(parts[0].toUpperCase(Locale.ROOT));
        float volume = parts.length > 1 ? Float.parseFloat(parts[1]) : 1F;
        float pitch = parts.length > 2 ? Float.parseFloat(parts[2]) : 1F;
        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    private static String strip(String input, String prefix) {
        return input.substring(prefix.length()).trim();
    }
}
