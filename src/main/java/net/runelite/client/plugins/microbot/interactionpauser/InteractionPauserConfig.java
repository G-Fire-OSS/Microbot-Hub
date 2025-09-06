package net.runelite.client.plugins.microbot.interactionpauser;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("interactionPauser")
public interface InteractionPauserConfig extends Config {
    @ConfigItem(
            keyName = "pauseDuration",
            name = "Pause Duration (seconds)",
            description = "How long to pause scripts for after mouse movement or clicks.",
            position = 0
    )
    default int pauseDuration() {
        return 5;
    }
}
