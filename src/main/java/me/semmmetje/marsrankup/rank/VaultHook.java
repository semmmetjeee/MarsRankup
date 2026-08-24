package me.semmmetje.marsrankup.rank;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class VaultHook {
    private final JavaPlugin plugin;
    private Economy economy;

    public VaultHook(JavaPlugin plugin) { this.plugin = plugin; }

    public void hook() {
        economy = null;
        if (!Bukkit.getPluginManager().isPluginEnabled("Vault")) return;

        RegisteredServiceProvider<Economy> registration = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (registration != null) {
            economy = registration.getProvider();
            plugin.getLogger().info("Hooked Vault economy: " + economy.getName());
        }
    }

    public boolean available() { return economy != null; }
    public double balance(Player player) { return economy == null ? 0D : economy.getBalance(player); }
    public boolean withdraw(Player player, double amount) {
        if (amount <= 0D) return true;
        return economy != null && economy.withdrawPlayer(player, amount).transactionSuccess();
    }
}
