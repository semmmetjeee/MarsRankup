package me.semmmetje.marsrankup.update;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.semmmetje.marsrankup.MarsRankupPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class UpdateChecker implements Listener {
    private static final String PRODUCT_SLUG = "mars-rankup";
    private static final String PRODUCTS_ENDPOINT = "https://mars-license-api.vanderlandsem8.workers.dev/api/products";
    private static final String PRODUCT_URL = "https://mars.semmmetje.nl/resources/mars-rankup";

    private final MarsRankupPlugin plugin;
    private final HttpClient client;
    private volatile UpdateInfo updateInfo;

    public UpdateChecker(MarsRankupPlugin plugin) {
        this.plugin = plugin;
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public void start() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        checkNow();

        // Refresh every 30 minutes so servers do not need a restart to notice a release.
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::checkForUpdates, 20L * 60L * 30L, 20L * 60L * 30L);
    }

    public void checkNow() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::checkForUpdates);
    }

    @EventHandler
    public void onAdminJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("marsrankup.admin")) return;

        UpdateInfo info = updateInfo;
        if (info == null) return;

        Bukkit.getScheduler().runTaskLater(plugin, () -> sendUpdateMessage(player, info), 20L);
    }

    private void checkForUpdates() {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(PRODUCTS_ENDPOINT))
                    .timeout(Duration.ofSeconds(8))
                    .header("Accept", "application/json")
                    .header("User-Agent", "MarsRankup/" + plugin.getPluginMeta().getVersion())
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                plugin.debug("Update check returned HTTP " + response.statusCode() + ".");
                return;
            }

            JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
            JsonArray products = root.has("products") && root.get("products").isJsonArray()
                    ? root.getAsJsonArray("products")
                    : new JsonArray();

            JsonObject product = null;
            for (JsonElement element : products) {
                if (!element.isJsonObject()) continue;
                JsonObject candidate = element.getAsJsonObject();
                String slug = string(candidate, "slug");
                if (PRODUCT_SLUG.equalsIgnoreCase(slug)) {
                    product = candidate;
                    break;
                }
            }

            if (product == null) {
                plugin.debug("Update check could not find product slug '" + PRODUCT_SLUG + "'.");
                return;
            }

            String latest = string(product, "version").trim();
            if (latest.isEmpty()) {
                plugin.debug("Update check found the product but it has no version.");
                return;
            }

            String current = plugin.getPluginMeta().getVersion();
            if (compareVersions(latest, current) > 0) {
                updateInfo = new UpdateInfo(current, latest);
                plugin.getLogger().warning("A new MarsRankup version is available: " + latest + " (installed: " + current + ")");
                plugin.getLogger().warning("Download: " + PRODUCT_URL);
            } else {
                updateInfo = null;
                plugin.debug("MarsRankup is up to date (" + current + ").");
            }
        } catch (Exception ex) {
            plugin.debug("Update check failed: " + ex.getMessage());
        }
    }

    private void sendUpdateMessage(Player player, UpdateInfo info) {
        Component prefix = Component.text("MARS", NamedTextColor.GOLD, TextDecoration.BOLD)
                .append(Component.text(" » ", NamedTextColor.DARK_GRAY));

        player.sendMessage(Component.empty());
        player.sendMessage(prefix.append(
                Component.text("A new MarsRankup version is available!", NamedTextColor.YELLOW, TextDecoration.BOLD)
        ));
        player.sendMessage(Component.text("Installed: ", NamedTextColor.GRAY)
                .append(Component.text(info.currentVersion(), NamedTextColor.RED))
                .append(Component.text("  |  Latest: ", NamedTextColor.GRAY))
                .append(Component.text(info.latestVersion(), NamedTextColor.GREEN)));
        player.sendMessage(Component.text("Click here to open the newest version", NamedTextColor.GOLD, TextDecoration.UNDERLINED)
                .clickEvent(ClickEvent.openUrl(PRODUCT_URL))
                .hoverEvent(Component.text(PRODUCT_URL, NamedTextColor.GRAY)));
        player.sendMessage(Component.empty());
    }

    private static String string(JsonObject object, String key) {
        if (!object.has(key) || object.get(key).isJsonNull()) return "";
        try {
            return object.get(key).getAsString();
        } catch (Exception ignored) {
            return "";
        }
    }

    static int compareVersions(String left, String right) {
        List<Integer> a = numericParts(left);
        List<Integer> b = numericParts(right);
        int max = Math.max(a.size(), b.size());
        for (int i = 0; i < max; i++) {
            int av = i < a.size() ? a.get(i) : 0;
            int bv = i < b.size() ? b.get(i) : 0;
            if (av != bv) return Integer.compare(av, bv);
        }

        // Same numeric version: treat a stable release as newer than a pre-release.
        boolean aPre = isPreRelease(left);
        boolean bPre = isPreRelease(right);
        if (aPre != bPre) return aPre ? -1 : 1;
        return 0;
    }

    private static List<Integer> numericParts(String version) {
        String normalized = version.toLowerCase(Locale.ROOT).replaceFirst("^v", "");
        String core = normalized.split("[-+]", 2)[0];
        String[] split = core.split("\\.");
        List<Integer> values = new ArrayList<>();
        for (String part : split) {
            String digits = part.replaceAll("[^0-9]", "");
            if (digits.isEmpty()) values.add(0);
            else {
                try {
                    values.add(Integer.parseInt(digits));
                } catch (NumberFormatException ignored) {
                    values.add(0);
                }
            }
        }
        return values;
    }

    private static boolean isPreRelease(String version) {
        String v = version.toLowerCase(Locale.ROOT);
        return v.contains("-") || v.contains("snapshot") || v.contains("alpha") || v.contains("beta") || v.contains("rc");
    }

    private record UpdateInfo(String currentVersion, String latestVersion) {}
}
