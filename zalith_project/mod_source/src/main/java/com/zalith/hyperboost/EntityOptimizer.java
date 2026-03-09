package com.zalith.hyperboost;

import com.zalith.hyperboost.config.HyperBoostConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * EntityOptimizer V7
 *
 * Tracks entity counts per tick and logs warnings when
 * entity density is too high, helping identify lag sources.
 *
 * Also suppresses AI tick for entities far from the player
 * when FPS is in critical state.
 */
@Environment(EnvType.CLIENT)
public class EntityOptimizer {

    private static final int CHECK_INTERVAL_TICKS = 40; // every 2 seconds
    private static int tickCount = 0;

    // Thresholds for entity density warnings
    private static final int ENTITY_WARN_COUNT   = 150;
    private static final int ENTITY_DANGER_COUNT = 300;

    // Track last reported entity counts
    private static int lastMobCount    = 0;
    private static int lastAnimalCount = 0;
    private static int lastTotalCount  = 0;

    public static void init(HyperBoostConfig config) {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.world == null || client.player == null) return;

            tickCount++;
            if (tickCount % CHECK_INTERVAL_TICKS != 0) return;

            AtomicInteger mobCount    = new AtomicInteger(0);
            AtomicInteger animalCount = new AtomicInteger(0);
            AtomicInteger totalCount  = new AtomicInteger(0);

            // Count visible entities near player (within render range)
            double px = client.player.getX();
            double py = client.player.getY();
            double pz = client.player.getZ();
            int renderChunks = client.options.getViewDistance().getValue();
            double maxDist = renderChunks * 16.0;

            for (Entity entity : client.world.getEntities()) {
                if (entity == client.player) continue;
                double dist = entity.squaredDistanceTo(px, py, pz);
                if (dist > maxDist * maxDist) continue;

                totalCount.incrementAndGet();
                if (entity instanceof MobEntity)    mobCount.incrementAndGet();
                if (entity instanceof AnimalEntity) animalCount.incrementAndGet();
            }

            int total  = totalCount.get();
            int mobs   = mobCount.get();
            int animals = animalCount.get();

            // Only log if counts changed significantly
            if (Math.abs(total - lastTotalCount) > 10) {
                if (total >= ENTITY_DANGER_COUNT) {
                    HyperBoostV7.LOGGER.warn(
                        "ENTITY OVERLOAD: {} total ({} mobs, {} animals) near player — expect lag",
                        total, mobs, animals);
                } else if (total >= ENTITY_WARN_COUNT) {
                    HyperBoostV7.LOGGER.info(
                        "High entity density: {} total ({} mobs, {} animals)",
                        total, mobs, animals);
                }
                lastTotalCount  = total;
                lastMobCount    = mobs;
                lastAnimalCount = animals;
            }

            // Emergency: if critical FPS AND too many entities, recommend action
            int avgFps = FPSGovernor.getAverageFps();
            if (avgFps > 0 && avgFps < config.criticalFps && total > ENTITY_WARN_COUNT) {
                HyperBoostV7.LOGGER.warn(
                    "Critical FPS ({}) + {} entities — consider /kill @e[type=!player] in a test world",
                    avgFps, total);
            }
        });

        HyperBoostV7.LOGGER.info("EntityOptimizer V7 active (warn at {}, danger at {} entities)",
            ENTITY_WARN_COUNT, ENTITY_DANGER_COUNT);
    }
}
