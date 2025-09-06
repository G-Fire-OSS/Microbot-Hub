package net.runelite.client.plugins.microbot.playertracker;

import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

import javax.inject.Inject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;

public class PlayerTrackerOverlay extends Overlay {

    private final Client client;
    private final PlayerTrackerPlugin plugin;
    private final PlayerTrackerConfig config;

    @Inject
    private PlayerTrackerOverlay(Client client, PlayerTrackerPlugin plugin, PlayerTrackerConfig config) {
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (config.playerName().trim().isEmpty()) {
            return null;
        }

        long itemAppearTimeMs = config.itemAppearTime() * 1000L;

        plugin.getTrackedTiles().forEach((worldPoint, time) -> {
            if (worldPoint.getPlane() != client.getPlane()) {
                return;
            }

            long timeElapsed = System.currentTimeMillis() - time;
            if (timeElapsed < itemAppearTimeMs) {
                drawTile(graphics, worldPoint, timeElapsed, itemAppearTimeMs);
            }
        });

        return null;
    }

    private void drawTile(Graphics2D graphics, WorldPoint worldPoint, long timeElapsed, long itemAppearTimeMs) {
        LocalPoint lp = LocalPoint.fromWorld(client, worldPoint);
        if (lp == null) return;

        Polygon poly = Perspective.getCanvasTilePoly(client, lp);
        if (poly == null) return;

        OverlayUtil.renderPolygon(graphics, poly, config.tileColor());

        long timeLeftSeconds = (itemAppearTimeMs - timeElapsed) / 1000;
        String text = String.valueOf(timeLeftSeconds);
        Point textLocation = Perspective.getCanvasTextLocation(client, graphics, lp, text, 0);

        if (textLocation != null) {
            OverlayUtil.renderTextLocation(graphics, textLocation, text, Color.WHITE);
        }
    }
}