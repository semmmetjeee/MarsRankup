package me.semmmetje.marsrankup.command;

import me.semmmetje.marsrankup.MarsRankupPlugin;
import me.semmmetje.marsrankup.rank.RankDefinition;
import me.semmmetje.marsrankup.rank.RankManager;
import me.semmmetje.marsrankup.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class RankCommand extends BukkitCommand {
    private final MarsRankupPlugin plugin;

    public RankCommand(MarsRankupPlugin plugin, String name, List<String> aliases, String permission) {
        super(name, "Open and manage MarsRankup", "/" + name, aliases);
        this.plugin = plugin;
        if (permission != null && !permission.isBlank()) setPermission(permission);
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
        if (!testPermission(sender)) return true;

        if (args.length == 0) {
            Player player = requirePlayer(sender);
            if (player == null) return true;
            plugin.guis().open(player, plugin.getConfig().getString("settings.default-gui", "ranks"));
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        switch (sub) {
            case "reload" -> {
                if (!admin(sender)) return true;
                plugin.reloadEverything();
                sender.sendMessage(Text.color(plugin.message("reload")));
            }
            case "open" -> {
                Player player = requirePlayer(sender);
                if (player == null) return true;
                String gui = args.length >= 2 ? args[1] : plugin.getConfig().getString("settings.default-gui", "ranks");
                plugin.guis().open(player, gui);
            }
            case "claim" -> {
                Player player = requirePlayer(sender);
                if (player == null) return true;

                Integer rankId = args.length >= 2 ? parseRank(args[1]) : plugin.ranks().next(player);
                if (rankId == null) {
                    player.sendMessage(Text.color(plugin.message("max-rank")));
                    return true;
                }

                RankDefinition rank = plugin.ranks().get(rankId);
                if (rank == null) {
                    player.sendMessage(Text.color(plugin.message("invalid-rank").replace("%rank%", String.valueOf(rankId))));
                    return true;
                }

                sendClaimResult(player, rank, plugin.ranks().claim(player, rank));
            }
            case "info" -> {
                Player player = requirePlayer(sender);
                if (player == null) return true;
                Integer next = plugin.ranks().next(player);
                sender.sendMessage(Text.color("&#FF8A1F&lMarsRankup &8» &#8F98A3Current: &#BFC5CC" + plugin.ranks().current(player) + " &8| &#8F98A3Next: &#BFC5CC" + (next == null ? "MAX" : next)));
            }
            case "set" -> {
                if (!admin(sender)) return true;
                if (args.length < 3) {
                    sender.sendMessage(Text.color("&#BFC5CCUsage: &#FF8A1F/" + label + " set <player> <rank>"));
                    return true;
                }

                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(Text.color(plugin.message("player-not-found")));
                    return true;
                }

                Integer rank = parseRank(args[2]);
                if (rank == null) {
                    sender.sendMessage(Text.color(plugin.message("invalid-number")));
                    return true;
                }

                plugin.playerData().setRank(target.getUniqueId(), rank);
                sender.sendMessage(Text.color(plugin.message("rank-set").replace("%player%", target.getName()).replace("%rank%", String.valueOf(rank))));
            }
            case "reset" -> {
                if (!admin(sender)) return true;
                if (args.length < 2) {
                    sender.sendMessage(Text.color("&#BFC5CCUsage: &#FF8A1F/" + label + " reset <player>"));
                    return true;
                }

                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(Text.color(plugin.message("player-not-found")));
                    return true;
                }

                int startRank = plugin.getConfig().getInt("settings.start-rank", 0);
                plugin.playerData().setRank(target.getUniqueId(), startRank);
                sender.sendMessage(Text.color(plugin.message("rank-reset").replace("%player%", target.getName()).replace("%rank%", String.valueOf(startRank))));
            }
            default -> {
                Player player = requirePlayer(sender);
                if (player != null) plugin.guis().open(player, plugin.getConfig().getString("settings.default-gui", "ranks"));
            }
        }
        return true;
    }

    private void sendClaimResult(Player player, RankDefinition rank, RankManager.ClaimResult result) {
        switch (result) {
            case SUCCESS -> player.sendMessage(Text.color(plugin.message("rank-claimed").replace("%rank%", String.valueOf(rank.id()))));
            case ALREADY_CLAIMED -> player.sendMessage(Text.color(plugin.message("already-claimed").replace("%rank%", String.valueOf(rank.id()))));
            case WRONG_ORDER -> {
                Integer next = plugin.ranks().next(player);
                player.sendMessage(Text.color(plugin.message("wrong-rank-order").replace("%next_rank%", next == null ? "MAX" : String.valueOf(next))));
            }
            case REQUIREMENTS_NOT_MET, ECONOMY_FAILED -> player.sendMessage(Text.color(plugin.message("requirements-not-met").replace("%rank%", String.valueOf(rank.id()))));
        }
    }

    private Player requirePlayer(CommandSender sender) {
        if (sender instanceof Player player) return player;
        sender.sendMessage(Text.color(plugin.message("player-only")));
        return null;
    }

    private boolean admin(CommandSender sender) {
        if (sender.hasPermission("marsrankup.admin")) return true;
        sender.sendMessage(Text.color(plugin.message("no-permission")));
        return false;
    }

    private static Integer parseRank(String raw) {
        try { return Integer.parseInt(raw); }
        catch (NumberFormatException ignored) { return null; }
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>(List.of("claim", "open", "info"));
            if (sender.hasPermission("marsrankup.admin")) {
                options.add("reload"); options.add("set"); options.add("reset");
            }
            String search = args[0].toLowerCase(Locale.ROOT);
            return options.stream().filter(option -> option.startsWith(search)).toList();
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("open")) {
            String search = args[1].toLowerCase(Locale.ROOT);
            return plugin.guis().ids().stream().filter(gui -> gui.startsWith(search)).toList();
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("reset")) && sender.hasPermission("marsrankup.admin")) {
            String search = args[1].toLowerCase(Locale.ROOT);
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(name -> name.toLowerCase(Locale.ROOT).startsWith(search)).toList();
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("claim")) {
            String search = args[1];
            return plugin.ranks().all().stream().map(rank -> String.valueOf(rank.id())).filter(id -> id.startsWith(search)).toList();
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("set") && sender.hasPermission("marsrankup.admin")) {
            String search = args[2];
            return plugin.ranks().all().stream().map(rank -> String.valueOf(rank.id())).filter(id -> id.startsWith(search)).toList();
        }

        return List.of();
    }
}
