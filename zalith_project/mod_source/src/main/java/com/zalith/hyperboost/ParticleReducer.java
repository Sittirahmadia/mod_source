package com.zalith.hyperboost;

import com.zalith.hyperboost.config.HyperBoostConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.option.ParticlesMode;
import net.minecraft.client.option.SimpleOption;

/**
 * ParticleReducer V7
 *
 * Automatically steps particle quality down when FPS is low
 * and restores it when FPS recovers.
 *
 * Levels: ALL → DECREASED → MINIMAL
 */
@Environment(EnvType.CLIENT)
public class ParticleReducer {

    private static int cooldown = 0;
    private static ParticlesMode originalMode = null;

    public static void init(HyperBoostConfig config) {
        if (!config.autoParticles) return;

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.world == null) return;
            if (cooldown > 0) { cooldown--; return; }

            int avgFps = FPSGovernor.getAverageFps();
            if (avgFps == 0) return; // Not enough data yet

            SimpleOption<ParticlesMode> particleOpt = client.options.getParticles();
            ParticlesMode current = particleOpt.getValue();

            if (avgFps < config.criticalFps) {
                // Critical: force MINIMAL
                if (current != ParticlesMode.MINIMAL) {
                    if (originalMode == null) originalMode = current;
                    particleOpt.setValue(ParticlesMode.MINIMAL);
                    HyperBoostV7.LOGGER.warn("CRITICAL FPS ({}) — particles set to MINIMAL", avgFps);
                    cooldown = config.adjustCooldownTicks;
                }

            } else if (avgFps < config.lowFps) {
                // Low: step down one level
                if (current == ParticlesMode.ALL) {
                    if (originalMode == null) originalMode = current;
                    particleOpt.setValue(ParticlesMode.DECREASED);
                    HyperBoostV7.LOGGER.info("Low FPS ({}) — particles ALL → DECREASED", avgFps);
                    cooldown = config.adjustCooldownTicks;
                }

            } else if (avgFps > config.highFps && originalMode != null) {
                // FPS recovered: restore original setting
                if (current != originalMode) {
                    particleOpt.setValue(originalMode);
                    HyperBoostV7.LOGGER.info("FPS recovered ({}) — particles restored to {}", avgFps, originalMode);
                    cooldown = config.adjustCooldownTicks;
                    originalMode = null;
                }
            }
        });

        HyperBoostV7.LOGGER.info("ParticleReducer V7 active");
    }
}
