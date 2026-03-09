package com.zalith.hyperboost;

import com.zalith.hyperboost.config.HyperBoostConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.text.Text;

/**
 * FpsSpikeDetector V7
 *
 * Distinguishes between two different FPS problems:
 *
 *  SPIKE  — sudden single-tick drop of >= SPIKE_THRESHOLD fps.
 *            Usually caused by chunk generation, GC pauses, or
 *            entity pathfinding. Does NOT trigger graphics changes.
 *            Shows an on-screen action-bar message to inform the player.
 *
 *  SUSTAINED LOW — average FPS below threshold for SUSTAINED_TICKS
 *            consecutive seconds. Triggers actual optimization steps
 *            (already handled by FPSGovernor).
 *
 * This prevents FPSGovernor from over-reacting to a single bad tick.
 */
@Environment(EnvType.CLIENT)
public class FpsSpikeDetector {

    private static final int SPIKE_THRESHOLD    = 25;  // drop >= 25fps in one tick = spike
    private static final int SPIKE_COOLDOWN     = 100; // ticks before next spike alert

    private static int lastFps      = -1;
    private static int spikeCooldown = 0;
    private static int spikeCount   = 0;
    private static int tickCount    = 0;

    // Sustained low tracking
    private static int sustainedLowTicks  = 0;
    private static final int SUSTAINED_TICKS = 60; // 3 seconds

    public static void init(HyperBoostConfig config) {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.world == null || client.player == null) return;

            tickCount++;
            int currentFps = client.getCurrentFps();
            if (spikeCooldown > 0) spikeCooldown--;

            // Spike detection: compare to last known FPS
            if (lastFps > 0 && spikeCooldown == 0) {
                int drop = lastFps - currentFps;
                if (drop >= SPIKE_THRESHOLD && currentFps < config.lowFps) {
                    spikeCount++;
                    spikeCooldown = SPIKE_COOLDOWN;
                    HyperBoostV7.LOGGER.warn(
                        "FPS spike detected: {}→{} fps (drop: {}, total spikes: {})",
                        lastFps, currentFps, drop, spikeCount);

                    client.player.sendMessage(
                        Text.literal("§6[HyperBoost]§r FPS spike: §c" + currentFps
                            + "§r fps (was §e" + lastFps + "§r)"),
                        true);
                }
            }

            // Sustained low tracking
            if (currentFps > 0 && currentFps < config.lowFps) {
                sustainedLowTicks++;
                if (sustainedLowTicks == SUSTAINED_TICKS) {
                    HyperBoostV7.LOGGER.warn(
                        "Sustained low FPS: {}fps for {}s — optimization modules active",
                        currentFps, SUSTAINED_TICKS / 20);
                }
            } else {
                sustainedLowTicks = 0;
            }

            // Update last FPS (use a light smoothing: 70% new, 30% old)
            if (lastFps < 0) {
                lastFps = currentFps;
            } else {
                lastFps = (currentFps * 7 + lastFps * 3) / 10;
            }
        });

        HyperBoostV7.LOGGER.info("FpsSpikeDetector V7 active (spike threshold: {} fps drop)", SPIKE_THRESHOLD);
    }

    public static int getSpikeCount()       { return spikeCount; }
    public static int getSustainedLowTicks(){ return sustainedLowTicks; }
    public static boolean isSustainedLow(HyperBoostConfig config) {
        return sustainedLowTicks >= SUSTAINED_TICKS;
    }
}
