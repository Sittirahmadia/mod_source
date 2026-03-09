package com.zalith.hyperboost;

import com.zalith.hyperboost.command.StatusCommand;
import com.zalith.hyperboost.config.HyperBoostConfig;
import com.zalith.hyperboost.hud.HudOverlay;
import com.zalith.hyperboost.profile.ProfileManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public class HyperBoostV7 implements ClientModInitializer {

    public static final String MOD_ID = "hyperboostv7";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        LOGGER.info("╔══════════════════════════════════════╗");
        LOGGER.info("║   HyperBoost V7 — Zalith             ║");
        LOGGER.info("║   Profiles · HUD · Smart Boost       ║");
        LOGGER.info("║   Spike Detect · World Warmup · /hb  ║");
        LOGGER.info("╚══════════════════════════════════════╝");

        HyperBoostConfig config = HyperBoostConfig.load();

        // Core optimizer modules
        FPSGovernor.init(config);
        EntityOptimizer.init(config);
        ParticleReducer.init(config);
        MemoryManager.init(config);
        GraphicsOptimizer.init(config);

        // V7 new modules
        FpsSpikeDetector.init(config);
        WorldJoinHandler.init(config);

        // V7 UI & control
        ProfileManager.init(config);
        KeybindManager.init(config);
        HudOverlay.init(config);
        StatusCommand.register(config);

        LOGGER.info("HyperBoost V7 fully initialized — type /hb for status");
    }
}
