package me.semmmetje.marsrankup.command;

import me.semmmetje.marsrankup.MarsRankupPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import java.util.List;
import java.util.Locale;

public final class DynamicCommandManager {
    private final MarsRankupPlugin plugin;
    private RankCommand registered;

    public DynamicCommandManager(MarsRankupPlugin plugin) { this.plugin = plugin; }

    public void register() {
        unregister();

        String name = sanitize(plugin.getConfig().getString("command.name", "rankup"), "rankup");
        List<String> aliases = plugin.getConfig().getStringList("command.aliases").stream()
                .map(alias -> sanitize(alias, ""))
                .filter(alias -> !alias.isBlank())
                .filter(alias -> !alias.equals(name))
                .distinct()
                .toList();

        String permission = plugin.getConfig().getString("command.permission", "marsrankup.use");
        registered = new RankCommand(plugin, name, aliases, permission);

        CommandMap commandMap = Bukkit.getServer().getCommandMap();
        commandMap.register(plugin.getName().toLowerCase(Locale.ROOT), registered);

        Bukkit.getScheduler().runTask(plugin, () -> Bukkit.getOnlinePlayers().forEach(org.bukkit.entity.Player::updateCommands));
        plugin.getLogger().info("Registered /" + name + " with aliases " + aliases + ".");
    }

    public void unregister() {
        if (registered == null) return;
        try { registered.unregister(Bukkit.getServer().getCommandMap()); }
        catch (Exception ex) { plugin.debug("Could not fully unregister dynamic command: " + ex.getMessage()); }
        registered = null;
    }

    private static String sanitize(String value, String fallback) {
        if (value == null) return fallback;
        String cleaned = value.trim().toLowerCase(Locale.ROOT).replaceFirst("^/+", "").replaceAll("[^a-z0-9_:-]", "");
        return cleaned.isBlank() ? fallback : cleaned;
    }
}
