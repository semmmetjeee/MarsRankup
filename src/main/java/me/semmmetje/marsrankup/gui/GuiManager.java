package me.semmmetje.marsrankup.gui;

import me.semmmetje.marsrankup.MarsRankupPlugin;
import me.semmmetje.marsrankup.rank.RankDefinition;
import me.semmmetje.marsrankup.rank.RankManager;
import me.semmmetje.marsrankup.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import java.io.File;
import java.util.*;

public final class GuiManager implements Listener {
    private final MarsRankupPlugin plugin;
    private final ItemBuilder itemBuilder;
    private final Map<String, YamlConfiguration> guis = new HashMap<>();

    public GuiManager(MarsRankupPlugin plugin) {
        this.plugin = plugin;
        this.itemBuilder = new ItemBuilder(plugin);
    }

    public void load() {
        guis.clear();
        File directory = new File(plugin.getDataFolder(), "guis");
        if (!directory.exists()) directory.mkdirs();

        File[] files = directory.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (files == null) return;
        Arrays.sort(files, Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));

        for (File file : files) {
            String id = file.getName().substring(0, file.getName().length() - 4).toLowerCase(Locale.ROOT);
            guis.put(id, YamlConfiguration.loadConfiguration(file));
        }
        plugin.getLogger().info("Loaded " + guis.size() + " GUI(s).");
    }

    public List<String> ids() { return guis.keySet().stream().sorted().toList(); }

    public void open(Player player, String rawId) {
        String id = rawId == null ? "" : rawId.trim().toLowerCase(Locale.ROOT);
        YamlConfiguration gui = guis.get(id);

        if (gui == null) {
            player.sendMessage(Text.color(plugin.message("unknown-gui").replace("%gui%", rawId == null ? "" : rawId)));
            return;
        }

        int size = normalizeSize(gui.getInt("size", 45));
        RankGuiHolder holder = new RankGuiHolder(id);
        String title = plugin.requirements().resolve(player, gui.getString("title", "Ranks"));
        Inventory inventory = Bukkit.createInventory(holder, size, Text.color(title));
        holder.inventory = inventory;

        renderBorder(player, gui.getConfigurationSection("border"), inventory, holder);
        renderSection(player, gui.getConfigurationSection("panes"), inventory, holder, false);
        renderSection(player, gui.getConfigurationSection("decorations"), inventory, holder, false);
        renderRanks(player, inventory, holder);
        renderSection(player, gui.getConfigurationSection("items"), inventory, holder, true);

        player.openInventory(inventory);
    }

    private void renderRanks(Player player, Inventory inventory, RankGuiHolder holder) {
        for (RankDefinition rank : plugin.ranks().all()) {
            ConfigurationSection base = rank.item();
            if (base == null) continue;

            String stateName = state(player, rank);
            ConfigurationSection state = base.getConfigurationSection(stateName);
            List<Integer> slots = parseSlots(base, inventory.getSize());
            if (slots.isEmpty()) continue;

            ItemStack stack = itemBuilder.build(player, base, state);
            List<String> actions = itemBuilder.actions(base, state);

            for (int slot : slots) {
                inventory.setItem(slot, stack);
                holder.rankSlots.put(slot, new RankSlot(rank.id(), stateName, List.copyOf(actions)));
                holder.actions.remove(slot);
            }
        }
    }

    private String state(Player player, RankDefinition rank) {
        if (plugin.ranks().isClaimed(player, rank.id())) return "claimed";
        return plugin.ranks().canClaim(player, rank) ? "claimable" : "in-progress";
    }

    private void renderBorder(Player player, ConfigurationSection section, Inventory inventory, RankGuiHolder holder) {
        if (section == null) return;
        List<Integer> slots = parseSlots(section, inventory.getSize());

        if (slots.isEmpty()) {
            for (int slot = 0; slot < inventory.getSize(); slot++) {
                if (slot < 9 || slot >= inventory.getSize()-9 || slot%9==0 || slot%9==8) slots.add(slot);
            }
        }
        renderOne(player, section, inventory, holder, false, slots);
    }

    private void renderSection(Player player, ConfigurationSection section, Inventory inventory, RankGuiHolder holder, boolean overwrite) {
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            ConfigurationSection item = section.getConfigurationSection(key);
            if (item == null) continue;

            String permission = item.getString("permission", "");
            if (!permission.isBlank() && !permission.equalsIgnoreCase("none") && !player.hasPermission(permission)) continue;

            renderOne(player, item, inventory, holder, overwrite, parseSlots(item, inventory.getSize()));
        }
    }

    private void renderOne(Player player, ConfigurationSection item, Inventory inventory, RankGuiHolder holder, boolean overwrite, List<Integer> slots) {
        if (slots.isEmpty()) return;

        ItemStack stack = itemBuilder.build(player, item, null);
        List<String> actions = new ArrayList<>(item.getStringList("actions"));
        if (item.getBoolean("close", false)) actions.add(0, "[CLOSE]");

        for (int slot : slots) {
            if (!overwrite && inventory.getItem(slot) != null) continue;
            inventory.setItem(slot, stack);
            if (!actions.isEmpty()) holder.actions.put(slot, List.copyOf(actions));
            else holder.actions.remove(slot);
            if (overwrite) holder.rankSlots.remove(slot);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof RankGuiHolder holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getView().getTopInventory().getSize()) return;

        RankSlot rankSlot = holder.rankSlots.get(slot);
        if (rankSlot != null) {
            handleRankClick(player, holder.guiId, rankSlot);
            return;
        }

        List<String> actions = holder.actions.get(slot);
        if (actions != null) plugin.actions().execute(player, actions);
    }

    private void handleRankClick(Player player, String guiId, RankSlot clicked) {
        RankDefinition rank = plugin.ranks().get(clicked.rankId());
        if (rank == null) return;

        if (clicked.state().equals("claimed")) {
            plugin.actions().execute(player, clicked.actions());
            player.sendMessage(Text.color(plugin.message("already-claimed").replace("%rank%", String.valueOf(rank.id()))));
            return;
        }

        if (clicked.state().equals("in-progress")) {
            plugin.actions().execute(player, clicked.actions());

            Integer next = plugin.ranks().next(player);
            boolean wrongOrder = !plugin.getConfig().getBoolean("settings.allow-skipping-ranks", false)
                    && (next == null || next != rank.id());

            if (wrongOrder) {
                player.sendMessage(Text.color(plugin.message("wrong-rank-order").replace("%next_rank%", next == null ? "MAX" : String.valueOf(next))));
            } else {
                player.sendMessage(Text.color(plugin.message("requirements-not-met").replace("%rank%", String.valueOf(rank.id()))));
            }
            return;
        }

        RankManager.ClaimResult result = plugin.ranks().claim(player, rank);
        switch (result) {
            case SUCCESS -> {
                plugin.actions().execute(player, clicked.actions());
                player.sendMessage(Text.color(plugin.message("rank-claimed").replace("%rank%", String.valueOf(rank.id()))));

                if (plugin.getConfig().getBoolean("settings.close-gui-after-claim", false)) {
                    player.closeInventory();
                } else if (plugin.getConfig().getBoolean("settings.refresh-gui-after-claim", true)) {
                    Bukkit.getScheduler().runTask(plugin, () -> { if (player.isOnline()) open(player, guiId); });
                }
            }
            case ALREADY_CLAIMED -> player.sendMessage(Text.color(plugin.message("already-claimed").replace("%rank%", String.valueOf(rank.id()))));
            case WRONG_ORDER -> {
                Integer next = plugin.ranks().next(player);
                player.sendMessage(Text.color(plugin.message("wrong-rank-order").replace("%next_rank%", next == null ? "MAX" : String.valueOf(next))));
            }
            case REQUIREMENTS_NOT_MET, ECONOMY_FAILED -> player.sendMessage(Text.color(plugin.message("requirements-not-met").replace("%rank%", String.valueOf(rank.id()))));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof RankGuiHolder) event.setCancelled(true);
    }

    private static int normalizeSize(int size) {
        int bounded = Math.max(9, Math.min(54, size));
        return bounded % 9 == 0 ? bounded : Math.min(54, ((bounded / 9) + 1) * 9);
    }

    private static List<Integer> parseSlots(ConfigurationSection section, int size) {
        LinkedHashSet<Integer> result = new LinkedHashSet<>();
        if (section.contains("slot")) result.add(section.getInt("slot"));

        List<?> rawSlots = section.getList("slots");
        if (rawSlots != null) {
            for (Object raw : rawSlots) parseSlotToken(String.valueOf(raw), result, size);
        }

        result.removeIf(slot -> slot < 0 || slot >= size);
        return new ArrayList<>(result);
    }

    private static void parseSlotToken(String raw, LinkedHashSet<Integer> result, int size) {
        String token = raw.replace("[", "").replace("]", "").trim();

        if (token.contains("-")) {
            String[] parts = token.split("-", 2);
            try {
                int start = Integer.parseInt(parts[0].trim());
                int end = Integer.parseInt(parts[1].trim());
                for (int slot = Math.min(start, end); slot <= Math.max(start, end); slot++) if (slot >= 0 && slot < size) result.add(slot);
            } catch (NumberFormatException ignored) {}
        } else {
            try {
                int slot = Integer.parseInt(token);
                if (slot >= 0 && slot < size) result.add(slot);
            } catch (NumberFormatException ignored) {}
        }
    }

    private record RankSlot(int rankId, String state, List<String> actions) {}

    private static final class RankGuiHolder implements InventoryHolder {
        private final String guiId;
        private final Map<Integer, RankSlot> rankSlots = new HashMap<>();
        private final Map<Integer, List<String>> actions = new HashMap<>();
        private Inventory inventory;

        private RankGuiHolder(String guiId) { this.guiId = guiId; }
        @Override public Inventory getInventory() { return inventory; }
    }
}
