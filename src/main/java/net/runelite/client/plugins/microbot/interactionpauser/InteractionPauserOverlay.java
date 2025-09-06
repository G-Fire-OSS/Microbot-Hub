package net.runelite.client.plugins.microbot.interactionpauser;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.*;

public class InteractionPauserOverlay extends OverlayPanel {
    private final InteractionPauserPlugin plugin;

    @Inject
    InteractionPauserOverlay(InteractionPauserPlugin plugin) {
        this.plugin = plugin;
        setPosition(OverlayPosition.TOP_LEFT);
        setNaughty();
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (plugin.isPaused()) {
            panelComponent.setPreferredSize(new Dimension(200, 0));
            panelComponent.getChildren().add(TitleComponent.builder()
                    .text("Scripts Paused")
                    .color(Color.YELLOW)
                    .build());

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Time remaining:")
                    .right(String.valueOf(plugin.getRemainingPauseTime()))
                    .build());
        }
        return super.render(graphics);
    }
}
