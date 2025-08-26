package net.runelite.client.plugins.microbot.crafthelper;

import lombok.Getter;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigInformation;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup("crafthelper")
@ConfigInformation("Free to Play Crafting Helper<br/>" +
        "Tested at Barbarian Village and Al Kharid<br/>" +
        "1. For Soft Clay: Have 14+ empty buckets and clay in your bank.<br/>" +
        "2. For Leather: Have a needle, thread, and leather in your bank.")
public interface CraftHelperConfig extends Config {

    // --- Sections ---
    @ConfigSection(
            name = "General",
            description = "General settings",
            position = 0
    )
    String generalSection = "general";

    @ConfigSection(
            name = "Crafting",
            description = "Select what to craft",
            position = 1
    )
    String craftingSection = "crafting";

    @ConfigSection(
            name = "Clay Options",
            description = "Options for Clay Crafting",
            position = 2
    )
    String claySection = "clay";

    @ConfigSection(
            name = "Leather Options",
            description = "Options for Leather Crafting",
            position = 3
    )
    String leatherSection = "leather";

    @ConfigSection(
            name = "UI Settings",
            description = "Settings for the UI",
            position = 4
    )
    String uiSection = "ui";

    @ConfigSection(
            name = "Anti-Ban",
            description = "Anti-Ban settings",
            position = 5
    )
    String antiBanSection = "antiBan";


    // --- General Settings ---
    @ConfigItem(
            keyName = "startBot",
            name = "Start/Stop",
            description = "Toggles the bot on and off",
            position = 0,
            section = generalSection
    )
    default boolean startBot() {
        return false;
    }

    // --- Crafting Mode ---
    enum CraftingMode {
        CLAY,
        LEATHER
    }

    @ConfigItem(
            keyName = "craftingMode",
            name = "Crafting Mode",
            description = "Select the main crafting category",
            position = 1,
            section = craftingSection
    )
    default CraftingMode craftingMode() {
        return CraftingMode.CLAY;
    }


    // --- Clay Settings ---
    enum ClayMode {
        MAKE_SOFT_CLAY,
        MAKE_POTTERY
    }

    @ConfigItem(
            keyName = "clayMode",
            name = "Clay Mode",
            description = "Select what to do with clay. For Soft Clay Start with 14+ buckets in inv or bank.",
            position = 2,
            section = claySection
    )
    default ClayMode clayMode() {
        return ClayMode.MAKE_SOFT_CLAY;
    }

    @Getter
    enum PotteryItem {
        POT(1787, 1931),
        PIE_DISH(1789, 2313),
        BOWL(1791, 1923);

        private final int unfiredId;
        private final int firedId;

        PotteryItem(int unfiredId, int firedId) {
            this.unfiredId = unfiredId;
            this.firedId = firedId;
        }

        @Override
        public String toString() {
            String name = name().toLowerCase().replace('_', ' ');
            return Character.toUpperCase(name.charAt(0)) + name.substring(1);
        }
    }

    @ConfigItem(
            keyName = "potteryItem",
            name = "Pottery Item",
            description = "Item to make on the pottery wheel. Only works if Clay Mode is MAKE_POTTERY.",
            position = 3,
            section = claySection
    )
    default PotteryItem potteryItem() {
        return PotteryItem.POT;
    }


    // --- Leather Settings ---
    @Getter
    enum LeatherItem {
        LEATHER_GLOVES(1059, 1741),
        LEATHER_BOOTS(1061, 1741),
        LEATHER_COWL(1167, 1741),
        LEATHER_VAMBRACES(1063, 1741),
        LEATHER_BODY(1129, 1741),
        HARDLEATHER_BODY(1131, 1743);

        private final int itemId;
        private final int leatherId;

        LeatherItem(int itemId, int leatherId) {
            this.itemId = itemId;
            this.leatherId = leatherId;
        }

        @Override
        public String toString() {
            String name = name().toLowerCase().replace('_', ' ');
            if (this == HARDLEATHER_BODY) {
                return "Hardleather body";
            }
            return Character.toUpperCase(name.charAt(0)) + name.substring(1);
        }
    }

    @ConfigItem(
            keyName = "leatherItem",
            name = "Item to Craft",
            description = "Item to craft from leather. Only works if Crafting Mode is LEATHER.",
            position = 4,
            section = leatherSection
    )
    default LeatherItem leatherItem() {
        return LeatherItem.LEATHER_GLOVES;
    }

    // --- UI Settings ---
    @ConfigItem(
            keyName = "devUI",
            name = "Dev UI",
            description = "Shows extra information for debugging",
            position = 0,
            section = uiSection
    )
    default boolean devUI() {
        return false;
    }

    // --- Anti-Ban Settings ---
    @ConfigItem(
            keyName = "useAntiban",
            name = "Use Anti-Ban",
            description = "Enable to use anti-ban features",
            position = 0,
            section = antiBanSection
    )
    default boolean useAntiban() {
        return true;
    }

    @ConfigItem(
            keyName = "naturalMouse",
            name = "Natural Mouse",
            description = "Adds a natural random delay to mouse movements",
            position = 1,
            section = antiBanSection
    )
    default boolean naturalMouse() {
        return true;
    }

    @Range
    @ConfigItem(
            keyName = "minCraftTime",
            name = "Min Craft Time (ms)",
            description = "Minimum time to wait for crafting an inventory.",
            position = 2,
            section = antiBanSection
    )
    default int minCraftTime() {
        return 10000;
    }

    @Range
    @ConfigItem(
            keyName = "maxCraftTime",
            name = "Max Craft Time (ms)",
            description = "Maximum time to wait for crafting an inventory.",
            position = 3,
            section = antiBanSection
    )
    default int maxCraftTime() {
        return 15000;
    }

    @ConfigItem(
            keyName = "randomizeCraftTime",
            name = "Randomize Craft Time",
            description = "Randomize the time to wait for crafting an inventory between min and max.",
            position = 4,
            section = antiBanSection
    )
    default boolean randomizeCraftTime() {
        return true;
    }
}
