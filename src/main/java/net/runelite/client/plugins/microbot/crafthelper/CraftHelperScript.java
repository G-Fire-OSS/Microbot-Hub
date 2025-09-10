package net.runelite.client.plugins.microbot.crafthelper;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.GameObject;
import net.runelite.api.NPC;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

@Slf4j
public class CraftHelperScript extends Script {

    private CraftHelperPlugin plugin;
    private final CraftHelperConfig config;

    private long lastAnimationTime = 0;

    // --- Item IDs ---
    private static final int CLAY_ID = 434;
    private static final int SOFT_CLAY_ID = 1761;
    private static final int BUCKET_ID = 1925;
    private static final int BUCKET_OF_WATER_ID = 1929;
    private static final int NEEDLE_ID = 1733;
    private static final int THREAD_ID = 1734;
    private static final int COWHIDE_ID = 1739;
    private static final int SOFT_LEATHER_ID = 1741;
    private static final int HARD_LEATHER_ID = 1743;
    private static final int COINS_ID = 995;

    // --- Object IDs ---
    private static final int POTTERY_WHEEL_ID = 14887;
    private static final int POTTERY_OVEN_ID = 11601;

    // --- NPC IDs ---
    private static final int ELLIS_NPC_ID = 545;

    // --- Locations ---
    private static final WorldPoint BARBARIAN_VILLAGE_POTTERY_WHEEL = new WorldPoint(3086, 3409, 0);
    private static final WorldPoint BARBARIAN_VILLAGE_POTTERY_OVEN = new WorldPoint(3085, 3406, 0);
    private static final WorldPoint AL_KHARID_BANK = new WorldPoint(3268, 3167, 0);
    private static final WorldPoint ELLIS_LOCATION = new WorldPoint(3273, 3191, 0);

    @Inject
    public CraftHelperScript(CraftHelperConfig config) {
        this.config = config;
    }

    // --- Main Loop ---
    public boolean run(CraftHelperPlugin plugin) {
        this.plugin = plugin;
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!super.run() || !config.startBot()) return;

                if (Rs2Player.isAnimating()) {
                    lastAnimationTime = System.currentTimeMillis();
                    return;
                }

                if (System.currentTimeMillis() - lastAnimationTime < 2000) {
                    return;
                }

                if (Rs2Player.isMoving() && this.plugin.getPotteryState() != CraftHelperPlugin.PotteryState.WALKING && this.plugin.getLeatherState() != CraftHelperPlugin.LeatherState.WALKING_TO_TANNER && this.plugin.getLeatherState() != CraftHelperPlugin.LeatherState.WALKING_TO_BANK) {
                    return;
                }

                // Main logic switch
                switch (plugin.getCurrentMode()) {
                    case POTTERY:
                        handlePottery();
                        break;
                    case LEATHER:
                        handleLeather();
                        break;
                }

            } catch (Exception ex) {
                log.error("CraftHelper Script Error: ", ex);
            }
        }, 0, 600, TimeUnit.MILLISECONDS);
        return true;
    }

    // --- Clay Mode Handler ---
    private void handlePottery() {
        switch (config.potterySubMode()) {
            case MAKE_SOFT_CLAY:
                handleMakeSoftClay();
                break;
            case MAKE_POTTERY:
                handleMakePottery();
                break;
        }
    }

    // --- Leather Mode Handler ---
    private void handleLeather() {
        switch (config.leatherMode()) {
            case TAN_HIDES:
                handleLeatherTanning();
                break;
            case CRAFT_ITEMS:
                handleLeatherCraftingItems();
                break;
        }
    }

    private void handleLeatherTanning() {
        determineLeatherTanningState();

        switch (plugin.getLeatherState()) {
            case BANKING:
                handleLeatherTanningBanking();
                break;
            case WALKING_TO_TANNER:
                handleWalkingToTanner();
                break;
            case TANNING:
                handleTanning();
                break;
            case WALKING_TO_BANK:
                handleWalkingToBank();
                break;
        }
    }

    private void determineLeatherTanningState() {
        boolean hasCowhides = Rs2Inventory.hasItem(COWHIDE_ID);
        boolean hasSoftLeather = Rs2Inventory.hasItem(SOFT_LEATHER_ID);
        boolean hasHardLeather = Rs2Inventory.hasItem(HARD_LEATHER_ID);

        if (hasCowhides) {
            plugin.setLeatherState(CraftHelperPlugin.LeatherState.WALKING_TO_TANNER);
        } else if (hasSoftLeather || hasHardLeather) {
            plugin.setLeatherState(CraftHelperPlugin.LeatherState.WALKING_TO_BANK);
        } else {
            plugin.setLeatherState(CraftHelperPlugin.LeatherState.BANKING);
        }
    }

    private void handleLeatherTanningBanking() {
        if (!Rs2Bank.isOpen()) {
            Rs2Bank.walkToBankAndUseBank();
            return;
        }

        if (Rs2Inventory.hasItem(SOFT_LEATHER_ID)) {
            Rs2Bank.depositAll(SOFT_LEATHER_ID);
            sleepUntil(() -> !Rs2Inventory.hasItem(SOFT_LEATHER_ID));
        }
        if (Rs2Inventory.hasItem(HARD_LEATHER_ID)) {
            Rs2Bank.depositAll(HARD_LEATHER_ID);
            sleepUntil(() -> !Rs2Inventory.hasItem(HARD_LEATHER_ID));
        }

        if (!Rs2Inventory.hasItem(COWHIDE_ID)) {
            if (!Rs2Bank.hasItem(COWHIDE_ID)) {
                Microbot.showMessage("You don't have any more cowhides.");
                shutdown();
                return;
            }
            Rs2Bank.withdrawAll(COWHIDE_ID);
            sleepUntil(() -> Rs2Inventory.hasItem(COWHIDE_ID));
        }

        // Withdraw coins for tanning
        int costPerHide = config.tanType() == CraftHelperConfig.TanType.SOFT_LEATHER ? 1 : 3;
        int hidesToTan = Rs2Inventory.count(COWHIDE_ID);
        int totalCost = hidesToTan * costPerHide;

        if (!Rs2Inventory.hasItem(COINS_ID) || Rs2Inventory.count(COINS_ID) < totalCost) {
            if (!Rs2Bank.hasItem(COINS_ID) || Rs2Bank.count(COINS_ID) < totalCost) {
                Microbot.showMessage("Not enough coins to tan hides.");
                shutdown();
                return;
            }
            Rs2Bank.withdrawX(COINS_ID, totalCost);
            sleepUntil(() -> Rs2Inventory.hasItem(COINS_ID) && Rs2Inventory.count(COINS_ID) >= totalCost);
        }

        Rs2Bank.closeBank();
        sleepUntil(() -> !Rs2Bank.isOpen());
    }

    private void handleWalkingToTanner() {
        if (Rs2Player.getWorldLocation().distanceTo(ELLIS_LOCATION) > 5) {
            Rs2Walker.walkTo(ELLIS_LOCATION);
        } else {
            plugin.setLeatherState(CraftHelperPlugin.LeatherState.TANNING);
        }
    }

    private void handleTanning() {
        NPC ellis = Rs2Npc.getNpc(ELLIS_NPC_ID);
        if (ellis == null) {
            log.info("Ellis not found.");
            return;
        }

        if (Rs2Widget.getWidget(219, 1) == null) { // Check if trade interface is open
            Rs2Npc.interact(ellis, "Trade");
            sleepUntil(() -> Rs2Widget.getWidget(219, 1) != null, 5000);
        }

        if (Rs2Widget.getWidget(219, 1) != null) {
            if (config.tanType() == CraftHelperConfig.TanType.SOFT_LEATHER) {
                Rs2Widget.clickWidget(219, 1); // First option for soft leather
            } else {
                Rs2Widget.clickWidget(219, 2); // Second option for hard leather
            }
            sleepUntil(() -> !Rs2Inventory.hasItem(COWHIDE_ID), getCraftingTimeout());
        }
    }

    private void handleWalkingToBank() {
        if (Rs2Player.getWorldLocation().distanceTo(AL_KHARID_BANK) > 5) {
            Rs2Walker.walkTo(AL_KHARID_BANK);
        } else {
            plugin.setLeatherState(CraftHelperPlugin.LeatherState.BANKING);
        }
    }

    // --- Leather Crafting Items Handler ---
    private void handleLeatherCraftingItems() {
        determineLeatherCraftingState();

        switch (plugin.getLeatherState()) {
            case BANKING:
                handleLeatherCraftingBanking();
                break;
            case CRAFTING:
                handleLeatherCrafting();
                break;
        }
    }

    private void determineLeatherCraftingState() {
        int leatherId = config.leatherItem().getLeatherId();
        boolean hasLeather = Rs2Inventory.hasItem(leatherId);

        if (hasLeather) {
            plugin.setLeatherState(CraftHelperPlugin.LeatherState.CRAFTING);
        } else {
            plugin.setLeatherState(CraftHelperPlugin.LeatherState.BANKING);
        }
    }

    private void handleLeatherCraftingBanking() {
        if (!Rs2Bank.isOpen()) {
            Rs2Bank.walkToBankAndUseBank();
            return;
        }

        int leatherId = config.leatherItem().getLeatherId();
        int craftedItemId = config.leatherItem().getItemId();

        if (Rs2Inventory.hasItem(craftedItemId)) {
            Rs2Bank.depositAll(craftedItemId);
            sleepUntil(() -> !Rs2Inventory.hasItem(craftedItemId));
        }

        if (!Rs2Inventory.hasItem(NEEDLE_ID)) {
            if (!Rs2Bank.hasItem(NEEDLE_ID)) {
                Microbot.showMessage("You need a needle to craft leather.");
                shutdown();
                return;
            }
            Rs2Bank.withdrawX(NEEDLE_ID, 1);
            sleepUntil(() -> Rs2Inventory.hasItem(NEEDLE_ID));
        }

        if (!Rs2Inventory.hasItem(THREAD_ID)) {
            if (!Rs2Bank.hasItem(THREAD_ID)) {
                Microbot.showMessage("You need thread to craft leather.");
                shutdown();
                return;
            }
            Rs2Bank.withdrawAll(THREAD_ID);
            sleepUntil(() -> Rs2Inventory.hasItem(THREAD_ID));
        }

        if (!Rs2Inventory.hasItem(leatherId)) {
            if (!Rs2Bank.hasItem(leatherId)) {
                Microbot.showMessage("You don't have any more leather.");
                shutdown();
                return;
            }
            Rs2Bank.withdrawAll(leatherId);
            sleepUntil(() -> Rs2Inventory.hasItem(leatherId));
        }

        Rs2Bank.closeBank();
        sleepUntil(() -> !Rs2Bank.isOpen());
    }

    private void handleLeatherCrafting() {
        int leatherId = config.leatherItem().getLeatherId();

        if (Rs2Widget.getWidget(270, 0) == null) {
            Rs2Inventory.use(NEEDLE_ID);
            sleep(200);
            Rs2Inventory.use(leatherId);
            sleepUntil(() -> Rs2Widget.getWidget(270, 0) != null, 5000);
        }

        if (Rs2Widget.getWidget(270, 0) != null) {
            Rs2Widget.clickWidget(config.leatherItem().toString());
            sleepUntil(() -> !Rs2Inventory.hasItem(leatherId), getCraftingTimeout());
        }
    }


    // --- Soft Clay Logic ---
    private void handleMakeSoftClay() {
        if (Rs2Widget.getWidget(270, 0) != null) {
            Rs2Widget.clickWidget("Soft clay");
            sleepUntil(() -> !Rs2Inventory.hasItem(CLAY_ID), getCraftingTimeout());
            return;
        }

        determineSoftClayState();

        switch (plugin.getPotteryState()) {
            case BANKING:
                handleSoftClayBanking();
                break;
            case GETTING_WATER:
                handleGetWater();
                break;
            case CRAFTING:
                handleSoftClayCrafting();
                break;
            case WALKING:
                // Handled by the isMoving check
                break;
        }
    }

    private boolean isNearBank() {
        GameObject nearestBank = Rs2GameObject.getGameObject("Bank booth");
        if (nearestBank == null) {
            nearestBank = Rs2GameObject.getGameObject("Bank chest");
        }
        return nearestBank != null && nearestBank.getWorldLocation().distanceTo(Rs2Player.getWorldLocation()) < 10;
    }

    private void determineSoftClayState() {
        boolean hasClay = Rs2Inventory.hasItem(CLAY_ID);
        boolean hasSoftClay = Rs2Inventory.hasItem(SOFT_CLAY_ID);
        boolean hasWaterBuckets = Rs2Inventory.hasItem(BUCKET_OF_WATER_ID);
        boolean hasEmptyBuckets = Rs2Inventory.hasItem(BUCKET_ID);

        if (!hasClay && hasSoftClay) {
            if (hasEmptyBuckets && !isNearBank()) {
                plugin.setPotteryState(CraftHelperPlugin.PotteryState.GETTING_WATER);
            } else {
                plugin.setPotteryState(CraftHelperPlugin.PotteryState.BANKING);
            }
            return;
        }

        if (hasClay && hasWaterBuckets) {
            plugin.setPotteryState(CraftHelperPlugin.PotteryState.CRAFTING);
            return;
        }

        if (hasClay && hasEmptyBuckets) {
            plugin.setPotteryState(CraftHelperPlugin.PotteryState.GETTING_WATER);
            return;
        }

        plugin.setPotteryState(CraftHelperPlugin.PotteryState.BANKING);
    }


    private void handleSoftClayBanking() {
        if (!Rs2Bank.isOpen()) {
            Rs2Bank.walkToBankAndUseBank();
            return;
        }

        if (Rs2Inventory.hasItem(SOFT_CLAY_ID)) {
            Rs2Bank.depositAll(SOFT_CLAY_ID);
            sleepUntil(() -> !Rs2Inventory.hasItem(SOFT_CLAY_ID));
        }

        if (!Rs2Inventory.hasItem(CLAY_ID)) {
            if (!Rs2Bank.hasItem(CLAY_ID)) {
                Microbot.showMessage("Not enough clay in the bank.");
                shutdown();
                return;
            }
            Rs2Bank.withdrawX(CLAY_ID, 14);
            sleepUntil(() -> Rs2Inventory.hasItem(CLAY_ID));
        }

        if (!Rs2Inventory.hasItem(BUCKET_ID) && !Rs2Inventory.hasItem(BUCKET_OF_WATER_ID)) {
            if (!Rs2Bank.hasItem(BUCKET_ID)) {
                Microbot.showMessage("Not enough buckets in the bank.");
                shutdown();
                return;
            }
            Rs2Bank.withdrawX(BUCKET_ID, 14);
            sleepUntil(() -> Rs2Inventory.hasItem(BUCKET_ID));
        }

        if (Rs2Inventory.hasItem(CLAY_ID) && (Rs2Inventory.hasItem(BUCKET_ID) || Rs2Inventory.hasItem(BUCKET_OF_WATER_ID))) {
            Rs2Bank.closeBank();
            sleepUntil(() -> !Rs2Bank.isOpen());
        }
    }

    private void handleGetWater() {
        if (Rs2Inventory.count(BUCKET_OF_WATER_ID) == 14) {
            return;
        }

        GameObject waterSource = Rs2GameObject.getGameObject("Well");
        if (waterSource == null) waterSource = Rs2GameObject.getGameObject("Sink");
        if (waterSource == null) waterSource = Rs2GameObject.getGameObject("Fountain");

        if (waterSource != null) {
            if (waterSource.getWorldLocation().distanceTo(Rs2Player.getWorldLocation()) > 8) {
                plugin.setPotteryState(CraftHelperPlugin.PotteryState.WALKING);
                Rs2Walker.walkTo(waterSource.getWorldLocation());
                return;
            }
            Rs2Inventory.use(BUCKET_ID);
            sleep(200);
            Rs2GameObject.interact(waterSource);
            sleepUntil(() -> !Rs2Inventory.hasItem(BUCKET_ID), getCraftingTimeout());
        } else {
            log.info("Could not find a water source.");
        }
    }

    private void handleSoftClayCrafting() {
        if (!Rs2Inventory.hasItem(BUCKET_OF_WATER_ID) || !Rs2Inventory.hasItem(CLAY_ID)) {
            determineSoftClayState();
            return;
        }

        Rs2Inventory.use(CLAY_ID);
        sleep(200);
        Rs2Inventory.use(BUCKET_OF_WATER_ID);
        sleepUntil(() -> Rs2Widget.getWidget(270,0) != null, 5000);
    }


    // --- Pottery Logic ---
    private void handleMakePottery() {
        determinePotteryState();

        switch (plugin.getPotteryState()) {
            case BANKING:
                handlePotteryBanking();
                break;
            case WALKING:
                break;
            case USING_POTTERY_WHEEL:
                handleUsingPotteryWheel();
                break;
            case FIRING_POTTERY:
                handleFiringPottery();
                break;
        }
    }

    private void determinePotteryState() {
        int unfiredId = config.potteryItem().getUnfiredId();
        int firedId = config.potteryItem().getFiredId();

        boolean hasFiredPottery = Rs2Inventory.hasItem(firedId);
        boolean hasUnfiredPottery = Rs2Inventory.hasItem(unfiredId);
        boolean hasSoftClay = Rs2Inventory.hasItem(SOFT_CLAY_ID);

        if (hasSoftClay) {
            plugin.setPotteryState(CraftHelperPlugin.PotteryState.USING_POTTERY_WHEEL);
        } else if (hasUnfiredPottery) {
            plugin.setPotteryState(CraftHelperPlugin.PotteryState.FIRING_POTTERY);
        } else if (hasFiredPottery) {
            plugin.setPotteryState(CraftHelperPlugin.PotteryState.BANKING);
        } else {
            plugin.setPotteryState(CraftHelperPlugin.PotteryState.BANKING);
        }
    }

    private void handlePotteryBanking() {
        if (!Rs2Bank.isOpen()) {
            Rs2Bank.walkToBankAndUseBank();
            return;
        }

        if (!Rs2Inventory.isEmpty()) {
            Rs2Bank.depositAll();
            sleepUntil(Rs2Inventory::isEmpty);
        }

        if (!Rs2Inventory.hasItem(SOFT_CLAY_ID)) {
            if (!Rs2Bank.hasItem(SOFT_CLAY_ID)) {
                Microbot.showMessage("Not enough soft clay in the bank.");
                shutdown();
                return;
            }
            Rs2Bank.withdrawAll(SOFT_CLAY_ID);
            sleepUntil(() -> Rs2Inventory.hasItem(SOFT_CLAY_ID));
        }

        Rs2Bank.closeBank();
        sleepUntil(() -> !Rs2Bank.isOpen());
    }

    private void handleUsingPotteryWheel() {
        if (Rs2Player.getWorldLocation().distanceTo(BARBARIAN_VILLAGE_POTTERY_WHEEL) > 10) {
            plugin.setPotteryState(CraftHelperPlugin.PotteryState.WALKING);
            Rs2Walker.walkTo(BARBARIAN_VILLAGE_POTTERY_WHEEL);
            return;
        }

        if (Rs2Widget.getWidget(270, 0) == null) {
            GameObject potteryWheel = Rs2GameObject.getGameObject(POTTERY_WHEEL_ID);
            if (potteryWheel == null) {
                log.info("Could not find Pottery Wheel.");
                return;
            }
            Rs2Inventory.use(SOFT_CLAY_ID);
            sleep(200);
            Rs2GameObject.interact(potteryWheel);
            sleepUntil(() -> Rs2Widget.getWidget(270, 0) != null, 5000);
        }

        if (Rs2Widget.getWidget(270, 0) != null) {
            Rs2Widget.clickWidget(config.potteryItem().toString());
            sleepUntil(() -> !Rs2Inventory.hasItem(SOFT_CLAY_ID), getCraftingTimeout());
        }
    }

    private void handleFiringPottery() {
        if (Rs2Player.getWorldLocation().distanceTo(BARBARIAN_VILLAGE_POTTERY_OVEN) > 10) {
            plugin.setPotteryState(CraftHelperPlugin.PotteryState.WALKING);
            Rs2Walker.walkTo(BARBARIAN_VILLAGE_POTTERY_OVEN);
            return;
        }

        GameObject potteryOven = Rs2GameObject.getGameObject(POTTERY_OVEN_ID);
        int unfiredId = config.potteryItem().getUnfiredId();

        if (potteryOven != null) {
            if (Rs2Widget.getWidget(270, 0) == null) {
                Rs2Inventory.use(unfiredId);
                sleep(200);
                Rs2GameObject.interact(potteryOven);
                sleepUntil(() -> Rs2Widget.getWidget(270, 0) != null, 5000);
            }

            if (Rs2Widget.getWidget(270, 0) != null) {
                Rs2Widget.clickWidget(config.potteryItem().toString());
                sleepUntil(() -> !Rs2Inventory.hasItem(unfiredId), getCraftingTimeout());
            }
        } else {
            log.info("Could not find Pottery Oven.");
        }
    }

    private int getCraftingTimeout() {
        if (config.randomizeCraftTime()) {
            return (int) (config.minCraftTime() + (Math.random() * (config.maxCraftTime() - config.minCraftTime())));
        } else {
            return config.minCraftTime();
        }
    }

    @Override
    public void shutdown() {
        super.shutdown();
        Rs2Walker.setTarget(null);
    }
}
