package net.runelite.client.plugins.microbot.crafthelper;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.GameObject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
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

    // --- Object IDs ---
    private static final int POTTERY_WHEEL_ID = 14887;
    private static final int POTTERY_OVEN_ID = 11601;

    // --- Locations ---
    private static final WorldPoint BARBARIAN_VILLAGE_POTTERY_WHEEL = new WorldPoint(3086, 3409, 0);
    private static final WorldPoint BARBARIAN_VILLAGE_POTTERY_OVEN = new WorldPoint(3085, 3406, 0);

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

                if (Rs2Player.isMoving() && this.plugin.getCurrentState() != CraftHelperPlugin.State.WALKING) {
                    return;
                }

                // Main logic switch
                switch (config.craftingMode()) {
                    case CLAY:
                        handleClay();
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
    private void handleClay() {
        switch (config.clayMode()) {
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
        determineLeatherState();

        switch (plugin.getCurrentState()) {
            case BANKING:
                handleLeatherBanking();
                break;
            case CRAFTING:
                handleLeatherCrafting();
                break;
        }
    }

    private void determineLeatherState() {
        int leatherId = config.leatherItem().getLeatherId();
        boolean hasLeather = Rs2Inventory.hasItem(leatherId);

        if (hasLeather) {
            plugin.setCurrentState(CraftHelperPlugin.State.CRAFTING);
        } else {
            plugin.setCurrentState(CraftHelperPlugin.State.BANKING);
        }
    }

    private void handleLeatherBanking() {
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

        switch (plugin.getCurrentState()) {
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
                plugin.setCurrentState(CraftHelperPlugin.State.GETTING_WATER);
            } else {
                plugin.setCurrentState(CraftHelperPlugin.State.BANKING);
            }
            return;
        }

        if (hasClay && hasWaterBuckets) {
            plugin.setCurrentState(CraftHelperPlugin.State.CRAFTING);
            return;
        }

        if (hasClay && hasEmptyBuckets) {
            plugin.setCurrentState(CraftHelperPlugin.State.GETTING_WATER);
            return;
        }

        plugin.setCurrentState(CraftHelperPlugin.State.BANKING);
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
                plugin.setCurrentState(CraftHelperPlugin.State.WALKING);
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

        switch (plugin.getCurrentState()) {
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
            plugin.setCurrentState(CraftHelperPlugin.State.USING_POTTERY_WHEEL);
        } else if (hasUnfiredPottery) {
            plugin.setCurrentState(CraftHelperPlugin.State.FIRING_POTTERY);
        } else if (hasFiredPottery) {
            plugin.setCurrentState(CraftHelperPlugin.State.BANKING);
        } else {
            plugin.setCurrentState(CraftHelperPlugin.State.BANKING);
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
            plugin.setCurrentState(CraftHelperPlugin.State.WALKING);
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
            plugin.setCurrentState(CraftHelperPlugin.State.WALKING);
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
