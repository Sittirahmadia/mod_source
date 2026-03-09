package com.zalith.hyperboost;

import com.zalith.hyperboost.config.HyperBoostConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

/**
 * MemoryManager V7
 *
 * Monitors JVM heap usage and hints the GC when usage
 * exceeds the configured threshold. This helps avoid
 * GC pause spikes that cause sudden FPS drops.
 */
@Environment(EnvType.CLIENT)
public class MemoryManager {

    private static int tickCount = 0;

    public static void init(HyperBoostConfig config) {
        if (!config.memoryWatchdog) return;

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            tickCount++;
            if (tickCount % config.memoryCheckIntervalTicks != 0) return;

            Runtime rt = Runtime.getRuntime();
            long totalMem = rt.totalMemory();
            long freeMem  = rt.freeMemory();
            long usedMem  = totalMem - freeMem;
            long maxMem   = rt.maxMemory();

            int usedPercent = (int) ((usedMem * 100L) / maxMem);
            long usedMB  = usedMem  / (1024 * 1024);
            long maxMB   = maxMem   / (1024 * 1024);

            if (usedPercent >= config.memoryWarningPercent) {
                HyperBoostV7.LOGGER.warn(
                    "Memory pressure: {}% used ({}/{}MB) — requesting GC",
                    usedPercent, usedMB, maxMB);
                System.gc(); // hint only; JVM may ignore
            } else {
                HyperBoostV7.LOGGER.debug(
                    "Memory OK: {}% used ({}/{}MB)",
                    usedPercent, usedMB, maxMB);
            }
        });

        HyperBoostV7.LOGGER.info("MemoryManager V7 active (warn at {}% heap)", config.memoryWarningPercent);
    }
}
