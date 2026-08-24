package me.semmmetje.marsrankup;

import me.semmmetje.marsrankup.command.DynamicCommandManager;
import me.semmmetje.marsrankup.config.PlayerData;
import me.semmmetje.marsrankup.gui.ActionExecutor;
import me.semmmetje.marsrankup.gui.GuiManager;
import me.semmmetje.marsrankup.rank.RankManager;
import me.semmmetje.marsrankup.rank.RankPlaceholderExpansion;
import me.semmmetje.marsrankup.rank.RequirementEvaluator;
import me.semmmetje.marsrankup.rank.VaultHook;
import me.semmmetje.marsrankup.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;

public final class MarsRankupPlugin extends JavaPlugin {
    private PlayerData playerData;
    private VaultHook vault;
    private RequirementEvaluator requirements;
    private RankManager ranks;
    private ActionExecutor actions;
    private GuiManager guis;
    private DynamicCommandManager commands;
    private RankPlaceholderExpansion expansion;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResourceIfMissing("ranks.yml");
        saveResourceIfMissing("guis/ranks.yml");

        playerData = new PlayerData(this);
        vault = new VaultHook(this);
        requirements = new RequirementEvaluator(this);
        ranks = new RankManager(this);
        actions = new ActionExecutor(this);
        guis = new GuiManager(this);
        commands = new DynamicCommandManager(this);

        Bukkit.getPluginManager().registerEvents(guis, this);
        reloadEverything();

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            expansion = new RankPlaceholderExpansion(this);
            expansion.register();
        }

        getLogger().info("MarsRankup " + getPluginMeta().getVersion() + " enabled.");
    }

    @Override
    public void onDisable() {
        if (commands != null) commands.unregister();
        if (expansion != null) expansion.unregister();
    }

    public void reloadEverything() {
        reloadConfig();
        vault.hook();
        ranks.load();
        guis.load();
        commands.register();

        if (ranks.requiresVault() && !vault.available()) {
            getLogger().warning("At least one rank uses money-required, but no Vault economy provider is available.");
        }
    }

    private void saveResourceIfMissing(String path) {
        File target = new File(getDataFolder(), path);
        File parent = target.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();
        if (!target.exists()) saveResource(path, false);
    }

    public String rawMessage(String key) {
        return getConfig().getString("messages." + key, key);
    }

    public String message(String key) {
        return getConfig().getString("messages.prefix", "") + rawMessage(key);
    }

    public void send(CommandSender sender, String key) {
        sender.sendMessage(Text.color(message(key)));
    }

    public void debug(String text) {
        if (getConfig().getBoolean("settings.debug", false)) getLogger().info("[DEBUG] " + text);
    }

    public PlayerData playerData() { return playerData; }
    public VaultHook vault() { return vault; }
    public RequirementEvaluator requirements() { return requirements; }
    public RankManager ranks() { return ranks; }
    public ActionExecutor actions() { return actions; }
    public GuiManager guis() { return guis; }
}
