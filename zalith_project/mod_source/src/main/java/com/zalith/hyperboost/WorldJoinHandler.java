package com.zalith.hyperboost;

import com.zalith.hyperboost.config.HyperBoostConfig;
import com.zalith.hyperboost.profile.ProfileManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.text.Text;

/**
 * WorldJoinHandler V7
 *
 * When the player joins/loads a world:
 *  1. Temporarily locks render distance to minRenderDistance
 *     so initial chunk generation is fast and smooth.
 *  2. After WARMUP_TICKS, gradually restores render distance
 *     one chunk per RESTORE_INTERVAL_TICKS until reaching the
 *     target set by the active profile / FPSGovernor.
 *
 * This prevents the notorious "FPS spike on world join" caused
 * by all chunks loading at full distance simultaneously.
 */
@Environment(EnvType.CLIENT)
public class WorldJoinHandler {

    private static final int WARMUP_TICKS           = 60;  // 3 seconds before restoring
    private static final int RESTORE_INTERVAL_TICKS = 40;  // restore 1 chunk every 2 seconds

    private static boolean inWarmup     = false;
    private static int     warmupTimer  = 0;
    private static int     restoreTimer = 0;
    private static int     targetRender = -1;

    public static void init(HyperBoostConfig config) {
        // Trigger on world join
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            client.execute(() -> {
                inWarmup    = true;
                warmupTimer = WARMUP_TICKS;
                targetRender = config.maxRenderDistance;

                // Lock to minimum for fast initial load
                SimpleOption<Integer> view = client.options.getViewDistance();
                int clamped = Math.min(view.getValue(), config.minRenderDistance + 2);
                view.setValue(clamped);

                HyperBoostV7.LOGGER.info(
                    "World joined — clamping render to {} chunks for {}s warmup",
                    clamped, WARMUP_TICKS / 20);

                if (client.player != null) {
                    client.player.sendMessage(
                        Text.literal("§b[HyperBoost V7]§r Loading world — render locked at §e"
                            + clamped + " chunks§r for smooth start"),
                        true);
                }
            });
        });

        // Disconnect: reset state
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            inWarmup     = false;
            warmupTimer  = 0;
            restoreTimer = 0;
        });

        // Tick handler: warmup countdown then gradual restore
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!inWarmup || client.world == null || client.player == null) return;

            if (warmupTimer > 0) {
                warmupTimer--;
                return;
            }

            // Warmup over — begin gradual restore
            restoreTimer++;
            if (restoreTimer % RESTORE_INTERVAL_TICKS != 0) return;

            SimpleOption<Integer> view = client.options.getViewDistance();
            int current = view.getValue();
            int target  = Math.min(targetRender, FPSGovernor.getAverageFps() > config.lowFps
                                                 ? config.maxRenderDistance
                                                 : config.minRenderDistance + 2);

            if (current < target) {
                view.setValue(current + 1);
                HyperBoostV7.LOGGER.info("Warmup restore: render {} → {} chunks", current, current + 1);
            } else {
                // Reached target — warmup complete
                inWarmup = false;
                HyperBoostV7.LOGGER.info("World warmup complete. Render distance: {} chunks", current);
                client.player.sendMessage(
                    Text.literal("§b[HyperBoost V7]§r World loaded — render restored to §e"
                        + current + " chunks"),
                    true);
            }
        });

        HyperBoostV7.LOGGER.info("WorldJoinHandler V7 active ({}s warmup, gradual restore)", WARMUP_TICKS / 20);
    }

    public static boolean isInWarmup() { return inWarmup; }
}
