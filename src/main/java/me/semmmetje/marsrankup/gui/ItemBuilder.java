package me.semmmetje.marsrankup.gui;

import me.semmmetje.marsrankup.MarsRankupPlugin;
import me.semmmetje.marsrankup.util.Text;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ItemBuilder {
    private final MarsRankupPlugin plugin;
    public ItemBuilder(MarsRankupPlugin plugin) { this.plugin = plugin; }

    public ItemStack build(Player player, ConfigurationSection base, ConfigurationSection state) {
        String materialName = getString(state, base, "material", "STONE");
        Material material = Material.matchMaterial(materialName);
        if (material == null) material = Material.STONE;

        int amount = Math.max(1, Math.min(99, getInt(state, base, "amount", 1)));
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        String name = getString(state, base, "name", getString(state, base, "display_name", null));
        if (name != null) meta.setDisplayName(Text.color(plugin.requirements().resolve(player, name)));

        List<String> lore = getStringList(state, base, "lore");
        if (!lore.isEmpty()) {
            List<String> rendered = new ArrayList<>(lore.size());
            for (String line : lore) rendered.add(Text.color(plugin.requirements().resolve(player, line)));
            meta.setLore(rendered);
        }

        int modelData = getInt(state, base, "custom-model-data", getInt(state, base, "custom_model_data", 0));
        if (modelData > 0) meta.setCustomModelData(modelData);

        meta.setUnbreakable(getBoolean(state, base, "unbreakable", false));

        ConfigurationSection enchants = state != null && state.isConfigurationSection("enchantments")
                ? state.getConfigurationSection("enchantments")
                : base == null ? null : base.getConfigurationSection("enchantments");

        if (enchants != null) {
            for (String key : enchants.getKeys(false)) {
                Enchantment enchantment = Enchantment.getByName(key.toUpperCase(Locale.ROOT));
                if (enchantment != null) meta.addEnchant(enchantment, enchants.getInt(key, 1), true);
            }
        }

        if (getBoolean(state, base, "glow", false)) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);

        for (String flagName : getStringList(state, base, "item-flags")) {
            try { meta.addItemFlags(ItemFlag.valueOf(flagName.toUpperCase(Locale.ROOT))); }
            catch (IllegalArgumentException ignored) { plugin.debug("Unknown item flag: " + flagName); }
        }

        item.setItemMeta(meta);
        return item;
    }

    public List<String> actions(ConfigurationSection base, ConfigurationSection state) {
        return getStringList(state, base, "actions");
    }

    private static String getString(ConfigurationSection state, ConfigurationSection base, String key, String fallback) {
        if (state != null && state.contains(key)) return state.getString(key, fallback);
        return base == null ? fallback : base.getString(key, fallback);
    }

    private static int getInt(ConfigurationSection state, ConfigurationSection base, String key, int fallback) {
        if (state != null && state.contains(key)) return state.getInt(key, fallback);
        return base == null ? fallback : base.getInt(key, fallback);
    }

    private static boolean getBoolean(ConfigurationSection state, ConfigurationSection base, String key, boolean fallback) {
        if (state != null && state.contains(key)) return state.getBoolean(key, fallback);
        return base == null ? fallback : base.getBoolean(key, fallback);
    }

    private static List<String> getStringList(ConfigurationSection state, ConfigurationSection base, String key) {
        if (state != null && state.contains(key)) return state.getStringList(key);
        return base == null ? List.of() : base.getStringList(key);
    }
}
