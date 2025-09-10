package net.runelite.client.plugins.microbot.interactionpauser;

import com.google.inject.Provides;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.input.MouseListener;
import net.runelite.client.input.MouseManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.PluginConstants;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.event.MouseEvent;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@PluginDescriptor(
        name = PluginConstants.FIRE + "Interaction Pauser",
        description = "Pauses scripts on mouse movement or clicks in the game window.",
        tags = {"microbot", "utility"},
        authors = { "Fire" },
        version = InteractionPauserPlugin.version,
        minClientVersion = "2.0.0",
        enabledByDefault = PluginConstants.DEFAULT_ENABLED,
        isExternal = PluginConstants.IS_EXTERNAL,
        cardUrl = "https://chsami.github.io/Microbot-Hub/CraftHelperPlugin/assets/card.png"
)
@Slf4j
public class InteractionPauserPlugin extends Plugin implements MouseListener {

    static final String version = "0.1";

    @Inject
    Client client;

    @Inject
    InteractionPauserConfig config;

    @Inject
    OverlayManager overlayManager;

    @Inject
    InteractionPauserOverlay overlay;

    @Inject
    MouseManager mouseManager;

    @Inject
    ScheduledExecutorService scheduledExecutorService;

    @Getter
    private boolean paused;

    @Getter
    private long remainingPauseTime;

    private ScheduledFuture<?> pauseFuture;

    @Provides
    InteractionPauserConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(InteractionPauserConfig.class);
    }

    @Override
    protected void startUp() throws Exception {
        overlayManager.add(overlay);
        mouseManager.registerMouseListener(this);
    }

    @Override
    protected void shutDown() throws Exception {
        overlayManager.remove(overlay);
        mouseManager.unregisterMouseListener(this);
        if (pauseFuture != null && !pauseFuture.isDone()) {
            pauseFuture.cancel(true);
        }
        if (paused) {
            Microbot.pauseAllScripts.set(false);
        }
    }

    private void handleInteraction() {
        if (client.getMouseCanvasPosition() == null) {
            return; // Mouse is outside the canvas
        }

        if (pauseFuture != null && !pauseFuture.isDone()) {
            pauseFuture.cancel(true);
        }

        if (!paused) {
            paused = true;
            Microbot.pauseAllScripts.set(true);
        }

        remainingPauseTime = config.pauseDuration();
        pauseFuture = scheduledExecutorService.scheduleAtFixedRate(() -> {
            remainingPauseTime--;
            if (remainingPauseTime <= 0) {
                resumeScripts();
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    private void resumeScripts() {
        if (pauseFuture != null) {
            pauseFuture.cancel(true);
            pauseFuture = null;
        }
        paused = false;
        Microbot.pauseAllScripts.set(false);
    }

    @Override
    public MouseEvent mouseClicked(MouseEvent event) {
        handleInteraction();
        return event;
    }

    @Override
    public MouseEvent mousePressed(MouseEvent event) {
        return event;
    }

    @Override
    public MouseEvent mouseReleased(MouseEvent event) {
        return event;
    }

    @Override
    public MouseEvent mouseEntered(MouseEvent event) {
        return event;
    }

    @Override
    public MouseEvent mouseExited(MouseEvent event) {
        return event;
    }

    @Override
    public MouseEvent mouseDragged(MouseEvent event) {
        handleInteraction();
        return event;
    }

    @Override
    public MouseEvent mouseMoved(MouseEvent event) {
        handleInteraction();
        return event;
    }
}
