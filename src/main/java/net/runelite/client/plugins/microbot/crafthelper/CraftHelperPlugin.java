package net.runelite.client.plugins.microbot.crafthelper;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.PluginConstants;
import net.runelite.client.plugins.microbot.util.antiban.Rs2AntibanSettings;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.AWTException;

@PluginDescriptor(
        name = PluginConstants.FIRE + "[Fire] CraftHelper",
        description = "Will attempt to help with Crafting Pottery and Leather",
        tags = {"crafting", "leather", "clay", "pottery"},
        authors = { "Fire" },
        version = CraftHelperPlugin.version,
        minClientVersion = "1.9.8",
        disable = true,
        enabledByDefault = PluginConstants.DEFAULT_ENABLED,
        isExternal = PluginConstants.IS_EXTERNAL
)
@Slf4j
public class CraftHelperPlugin extends Plugin {

    static final String version = "0.1.2";

    @Getter
    private final CraftHelperConfig config;
    private final OverlayManager overlayManager;
    private final CraftHelperScript craftHelperScript;
    private final CraftHelperOverlay craftHelperOverlay;


    public enum State {
        BANKING,
        GETTING_WATER,
        CRAFTING,
        WALKING,
        USING_POTTERY_WHEEL,
        FIRING_POTTERY
    }

    @Getter
    @Setter
    private State currentState;

    @Inject
    private CraftHelperPlugin(CraftHelperConfig config, OverlayManager overlayManager, CraftHelperScript craftHelperScript, CraftHelperOverlay craftHelperOverlay) {
        this.config = config;
        this.overlayManager = overlayManager;
        this.craftHelperScript = craftHelperScript;
        this.craftHelperOverlay = craftHelperOverlay;
    }

    @Override
    protected void startUp() throws AWTException {
        log.info("CraftHelper Plugin started!");
        currentState = State.BANKING;
        updateAntibanSettings();

        if (overlayManager != null) {
            if (config.devUI()) {
                overlayManager.add(craftHelperOverlay);
            }
        }

        craftHelperScript.run(this);
    }

    @Override
    protected void shutDown() {
        log.info("CraftHelper Plugin stopped!");
        craftHelperScript.shutdown();
        overlayManager.remove(craftHelperOverlay);
        Rs2AntibanSettings.reset(); // Reset to default when plugin stops
    }

    @Subscribe
    public void onConfigChanged(final ConfigChanged event) {
        if (!event.getGroup().equals("micro-crafthelper")) {
            return;
        }

        updateAntibanSettings();

        if (event.getKey().equals("devUI")) {
            if (config.devUI()) {
                overlayManager.add(craftHelperOverlay);
            } else {
                overlayManager.remove(craftHelperOverlay);
            }
        }
    }

    private void updateAntibanSettings() {
        Rs2AntibanSettings.antibanEnabled = config.useAntiban();
        Rs2AntibanSettings.naturalMouse = config.naturalMouse();
    }
}
