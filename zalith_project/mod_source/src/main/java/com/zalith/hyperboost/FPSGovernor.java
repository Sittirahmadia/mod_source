package com.zalith.hyperboost;

import com.zalith.hyperboost.config.HyperBoostConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.SimpleOption;

/**
 * FPSGovernor V7
 *
 * Monitors FPS every tick and dynamically adjusts:
 *  - Render distance (view distance)
 *  - Simulation distance
 *
 * Uses a rolling average + cooldown to prevent oscillation.
 */
@Environment(EnvType.CLIENT)
public class FPSGovernor {

    private static int cooldown = 0;
    private static int tickCount = 0;

    // Rolling average over last 20 ticks (~1 second)
    private static final int[] fpsHistory = new int[20];
    private static int historyIndex = 0;

    public static void init(HyperBoostConfig config) {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.world == null) return;

            // Record FPS sample
            fpsHistory[historyIndex % fpsHistory.length] = client.getCurrentFps();
            historyIndex++;
            tickCount++;

            // Only evaluate every 20 ticks (1 second)
            if (tickCount % 20 != 0) return;
            if (cooldown > 0) { cooldown--; return; }

            int avgFps = average(fpsHistory);

            if (config.autoRenderDistance) adjustRenderDistance(client, config, avgFps);
            if (config.autoSimDistance)    adjustSimDistance(client, config, avgFps);
        });

        HyperBoostV7.LOGGER.info("FPSGovernor V7 active (target: {}fps)", config.targetFps);
    }

    private static void adjustRenderDistance(MinecraftClient client, HyperBoostConfig config, int avgFps) {
        SimpleOption<Integer> viewOpt = client.options.getViewDistance();
        int current = viewOpt.getValue();

        if (avgFps < config.criticalFps && current > config.minRenderDistance) {
            int next = Math.max(config.minRenderDistance, current - 2);
            viewOpt.setValue(next);
            HyperBoostV7.LOGGER.warn("CRITICAL FPS ({}) — render {}→{} chunks", avgFps, current, next);
            cooldown = config.adjustCooldownTicks;

        } else if (avgFps < config.lowFps && current > config.minRenderDistance) {
            int next = current - 1;
            viewOpt.setValue(next);
            HyperBoostV7.LOGGER.info("Low FPS ({}) — render {}→{} chunks", avgFps, current, next);
            cooldown = config.adjustCooldownTicks;

        } else if (avgFps > config.highFps && current < config.maxRenderDistance) {
            int next = current + 1;
            viewOpt.setValue(next);
            HyperBoostV7.LOGGER.info("Good FPS ({}) — render {}→{} chunks", avgFps, current, next);
            cooldown = config.adjustCooldownTicks;
        }
    }

    private static void adjustSimDistance(MinecraftClient client, HyperBoostConfig config, int avgFps) {
        SimpleOption<Integer> simOpt = client.options.getSimulationDistance();
        int current = simOpt.getValue();

        if (avgFps < config.criticalFps && current > config.minSimDistance) {
            int next = Math.max(config.minSimDistance, current - 2);
            simOpt.setValue(next);
            HyperBoostV7.LOGGER.warn("CRITICAL FPS ({}) — sim distance {}→{}", avgFps, current, next);

        } else if (avgFps > config.highFps && current < config.maxSimDistance) {
            int next = current + 1;
            simOpt.setValue(next);
            HyperBoostV7.LOGGER.info("Good FPS ({}) — sim distance {}→{}", avgFps, current, next);
        }
    }

    private static int average(int[] arr) {
        int sum = 0;
        for (int v : arr) sum += v;
        return sum / arr.length;
    }

    public static int getAverageFps() {
        return average(fpsHistory);
    }
}
