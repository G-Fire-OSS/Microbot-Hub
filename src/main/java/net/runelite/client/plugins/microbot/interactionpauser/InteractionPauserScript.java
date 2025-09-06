package net.runelite.client.plugins.microbot.interactionpauser;

import net.runelite.client.plugins.microbot.Script;

import java.util.concurrent.TimeUnit;

public class InteractionPauserScript extends Script {

    public boolean run(InteractionPauserConfig config) {
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                // Nothing to do in the main loop, the logic is handled by event listeners
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }, 0, 1000, TimeUnit.MILLISECONDS);
        return true;
    }

    public void shutdown() {
        super.shutdown();
    }
}
