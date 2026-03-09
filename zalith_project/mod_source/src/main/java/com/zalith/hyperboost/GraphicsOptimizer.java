package com.zalith.hyperboost;

import com.zalith.hyperboost.config.HyperBoostConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GraphicsMode;

/**
 * GraphicsOptimizer V7
 *
 * Automatically steps graphics quality down when FPS is low:
 *   FABULOUS → FANCY → FAST
 *
 * Also disables smooth lighting in critical states.
 * Restores original settings when FPS recovers.
 *
 * NOTE: We intentionally do NOT call worldRenderer.reload() — it is
 * extremely expensive (~100ms freeze) and would cause visible stutter.
 * The option change takes effect on the next natural reload.
 */
@Environment(EnvType.CLIENT)
public class GraphicsOptimizer {

    private static GraphicsMode originalMode = null;
    private static Boolean originalSmoothLighting = null;
    private static int cooldown = 0;

    public static void init(HyperBoostConfig config) {
        if (!config.autoGraphics) {
            HyperBoostV7.LOGGER.info("GraphicsOptimizer V7 disabled by config");
            return;
        }

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.world == null) return;
            if (cooldown > 0) { cooldown--; return; }

            int avgFps = FPSGovernor.getAverageFps();
            if (avgFps == 0) return;

            handleGraphicsMode(client, config, avgFps);
            handleSmoothLighting(client, config, avgFps);
        });

        HyperBoostV7.LOGGER.info("GraphicsOptimizer V7 active");
    }

    private static void handleGraphicsMode(MinecraftClient client, HyperBoostConfig config, int avgFps) {
        GraphicsMode current = client.options.getGraphicsMode().getValue();

        if (avgFps < config.criticalFps) {
            // Step down one level if not already at FAST
            if (current == GraphicsMode.FABULOUS) {
                if (originalMode == null) originalMode = current;
                client.options.getGraphicsMode().setValue(GraphicsMode.FANCY);
                HyperBoostV7.LOGGER.warn("CRITICAL FPS ({}) — graphics FABULOUS → FANCY", avgFps);
                cooldown = config.adjustCooldownTicks;
            } else if (current == GraphicsMode.FANCY) {
                if (originalMode == null) originalMode = current;
                client.options.getGraphicsMode().setValue(GraphicsMode.FAST);
                HyperBoostV7.LOGGER.warn("CRITICAL FPS ({}) — graphics FANCY → FAST", avgFps);
                cooldown = config.adjustCooldownTicks;
            }

        } else if (avgFps > config.highFps && originalMode != null && current != originalMode) {
            // FPS recovered — restore one step at a time
            client.options.getGraphicsMode().setValue(originalMode);
            HyperBoostV7.LOGGER.info("FPS recovered ({}) — graphics restored to {}", avgFps, originalMode);
            cooldown = config.adjustCooldownTicks;
            originalMode = null;
        }
    }

    private static void handleSmoothLighting(MinecraftClient client, HyperBoostConfig config, int avgFps) {
        boolean current = client.options.getSmoothLighting().getValue();

        if (avgFps < config.criticalFps && current) {
            originalSmoothLighting = true;
            client.options.getSmoothLighting().setValue(false);
            HyperBoostV7.LOGGER.warn("CRITICAL FPS ({}) — smooth lighting OFF", avgFps);
            cooldown = Math.max(cooldown, config.adjustCooldownTicks);

        } else if (avgFps > config.highFps && Boolean.TRUE.equals(originalSmoothLighting) && !current) {
            client.options.getSmoothLighting().setValue(true);
            HyperBoostV7.LOGGER.info("FPS recovered ({}) — smooth lighting ON", avgFps);
            originalSmoothLighting = null;
        }
    }
}
