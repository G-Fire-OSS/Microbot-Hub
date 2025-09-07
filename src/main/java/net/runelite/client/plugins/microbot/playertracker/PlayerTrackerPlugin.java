package net.runelite.client.plugins.microbot.playertracker;

import com.google.inject.Provides;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.PluginConstants;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@PluginDescriptor(
        name = PluginConstants.FIRE + "Player Tracker",
        description = "Tracks a player's movement and shows item drop visibility timers on tiles.",
        tags = {"microbot", "utility", "tracker", "pvp"},
        authors = { "Gemini" },
        version = "1.0.1",
        enabledByDefault = false,
        isExternal = PluginConstants.IS_EXTERNAL,
        minClientVersion = "1.10.12"
)
public class PlayerTrackerPlugin extends Plugin {

    private static final int CLEANUP_INTERVAL_MS = 1000;

    @Inject
    private Client client;

    @Inject
    private PlayerTrackerConfig config;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private PlayerTrackerOverlay overlay;

    @Getter
    private final Map<WorldPoint, Long> trackedTiles = new ConcurrentHashMap<>();

    private WorldPoint lastPlayerLocation;
    private long lastCleanupTime = 0;

    @Provides
    PlayerTrackerConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(PlayerTrackerConfig.class);
    }

    @Override
    protected void startUp() {
        overlayManager.add(overlay);
    }

    @Override
    protected void shutDown() {
        overlayManager.remove(overlay);
        trackedTiles.clear();
        lastPlayerLocation = null;
    }

    @Subscribe
    @SuppressWarnings("unused")
    public void onGameTick(GameTick gameTick) {
        final String playerName = config.playerName().trim();
        if (playerName.isEmpty()) {
            trackedTiles.clear();
            return;
        }

        Player targetPlayer = findTrackedPlayer(playerName);
        if (targetPlayer == null) {
            // Clear tiles if the player is not in render distance
            trackedTiles.clear();
            lastPlayerLocation = null;
            return;
        }

        WorldPoint currentPlayerLocation = targetPlayer.getWorldLocation();
        if (!currentPlayerLocation.equals(lastPlayerLocation)) {
            trackedTiles.put(currentPlayerLocation, System.currentTimeMillis());
            lastPlayerLocation = currentPlayerLocation;
        }

        cleanupExpiredTiles();
    }

    private Player findTrackedPlayer(String playerName) {
        return client.getPlayers().stream()
                .filter(Objects::nonNull)
                .filter(p -> p.getName() != null && p.getName().equalsIgnoreCase(playerName))
                .findFirst()
                .orElse(null);
    }

    private void cleanupExpiredTiles() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCleanupTime > CLEANUP_INTERVAL_MS) {
            trackedTiles.entrySet().removeIf(entry ->
                    (currentTime - entry.getValue()) > (config.itemAppearTime() * 1000L));
            lastCleanupTime = currentTime;
        }
    }
}
