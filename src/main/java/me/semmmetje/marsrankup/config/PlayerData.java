package me.semmmetje.marsrankup.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.io.IOException;
import java.util.UUID;

public final class PlayerData {
    private final JavaPlugin plugin;
    private final File file;
    private YamlConfiguration yaml;

    public PlayerData(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "playerdata.yml");
        reload();
    }

    public void reload() {
        yaml = YamlConfiguration.loadConfiguration(file);
    }

    public int getRank(UUID uuid, int fallback) {
        return yaml.getInt("players." + uuid + ".rank", fallback);
    }

    public void setRank(UUID uuid, int rank) {
        yaml.set("players." + uuid + ".rank", rank);
        save();
    }

    private void save() {
        try {
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().severe("Could not save playerdata.yml: " + ex.getMessage());
        }
    }
}
