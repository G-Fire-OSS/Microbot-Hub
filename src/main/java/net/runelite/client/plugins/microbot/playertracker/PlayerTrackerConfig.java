package net.runelite.client.plugins.microbot.playertracker;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

import java.awt.Color;

@ConfigGroup("playertracker")
public interface PlayerTrackerConfig extends Config {

    @ConfigItem(
            keyName = "playerName",
            name = "Player Name",
            description = "The in-game name of the player you want to track.",
            position = 1
    )
    default String playerName() {
        return "";
    }

    @ConfigItem(
            keyName = "tileColor",
            name = "Tile Color",
            description = "The color of the tiles the tracked player walks on.",
            position = 2
    )
    default Color tileColor() {
        return new Color(255, 0, 0, 50);
    }

    @ConfigItem(
            keyName = "itemAppearTime",
            name = "Item Appear Time (s)",
            description = "Time in seconds for a dropped item to appear to other players.",
            position = 3
    )
    default int itemAppearTime() {
        return 60;
    }
}