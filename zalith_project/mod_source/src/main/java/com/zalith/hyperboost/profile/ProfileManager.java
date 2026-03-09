package com.zalith.hyperboost.profile;

import com.zalith.hyperboost.HyperBoostV7;
import com.zalith.hyperboost.config.HyperBoostConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;

/**
 * ProfileManager V7
 *
 * Three built-in performance presets that override config thresholds at runtime.
 * The player can cycle through them with a keybind (handled in KeybindManager).
 *
 *  PERFORMANCE  — aggressive reduction, prioritise FPS above all
 *  BALANCED     — default V6 behaviour
 *  QUALITY      — only reduces settings in emergencies, keeps visuals high
 */
@Environment(EnvType.CLIENT)
public class ProfileManager {

    public enum Profile {
        PERFORMANCE, BALANCED, QUALITY;

        public Profile next() {
            Profile[] vals = values();
            return vals[(ordinal() + 1) % vals.length];
        }
    }

    private static Profile current = Profile.BALANCED;
    private static HyperBoostConfig config;

    public static void init(HyperBoostConfig cfg) {
        config = cfg;
        try {
            current = Profile.valueOf(cfg.activeProfile.toUpperCase());
        } catch (IllegalArgumentException e) {
            current = Profile.BALANCED;
        }
        apply(current);
        HyperBoostV7.LOGGER.info("ProfileManager V7 ready. Active: {}", current);
    }

    /** Cycle to the next profile and apply it. */
    public static void cycleProfile() {
        current = current.next();
        apply(current);
        config.activeProfile = current.name();
        config.save();

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            mc.player.sendMessage(
                net.minecraft.text.Text.literal("§b[HyperBoost V7]§r Profile: §e" + current.name()),
                true // action bar, not chat
            );
        }
        HyperBoostV7.LOGGER.info("Profile switched → {}", current);
    }

    /** Apply preset values to the live config object. */
    private static void apply(Profile p) {
        switch (p) {
            case PERFORMANCE -> {
                config.targetFps           = 60;
                config.criticalFps         = 30;
                config.lowFps              = 45;
                config.highFps             = 70;
                config.minRenderDistance   = 2;
                config.maxRenderDistance   = 8;
                config.minSimDistance      = 2;
                config.maxSimDistance      = 8;
                config.autoParticles       = true;
                config.memoryWarningPercent = 70;
            }
            case BALANCED -> {
                config.targetFps           = 60;
                config.criticalFps         = 25;
                config.lowFps              = 40;
                config.highFps             = 80;
                config.minRenderDistance   = 4;
                config.maxRenderDistance   = 12;
                config.minSimDistance      = 4;
                config.maxSimDistance      = 12;
                config.autoParticles       = true;
                config.memoryWarningPercent = 80;
            }
            case QUALITY -> {
                config.targetFps           = 60;
                config.criticalFps         = 15;
                config.lowFps              = 25;
                config.highFps             = 60;
                config.minRenderDistance   = 8;
                config.maxRenderDistance   = 20;
                config.minSimDistance      = 8;
                config.maxSimDistance      = 20;
                config.autoParticles       = false;
                config.memoryWarningPercent = 90;
            }
        }
    }

    public static Profile getCurrent() { return current; }
}
