package net.runelite.client.plugins.microbot.crafthelper;

import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.*;

public class CraftHelperOverlay extends OverlayPanel {

    private final CraftHelperPlugin plugin;

    @Inject
    public CraftHelperOverlay(CraftHelperPlugin plugin) {
        super(plugin);
        this.plugin = plugin;
        setPosition(OverlayPosition.TOP_LEFT);
        setLayer(OverlayLayer.ABOVE_SCENE);
        setNaughty();
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        try {
            panelComponent.setPreferredSize(new Dimension(280, 0));
            panelComponent.getChildren().clear();

            panelComponent.getChildren().add(TitleComponent.builder()
                    .text("CraftHelper v" + CraftHelperPlugin.version)
                    .color(Color.GREEN)
                    .build());

            if (plugin.getCurrentState() != null) {
                panelComponent.getChildren().add(LineComponent.builder()
                        .left("Current State:")
                        .right(plugin.getCurrentState().toString())
                        .build());
            }
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Location:")
                    .right(String.valueOf(Rs2Player.getWorldLocation()))
                    .build());
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Inventory:")
                    .right(Rs2Inventory.all().size() + " items")
                    .build());
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        return super.render(graphics);
    }
}
