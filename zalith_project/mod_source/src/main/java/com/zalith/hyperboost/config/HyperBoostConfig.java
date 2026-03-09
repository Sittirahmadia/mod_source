package com.zalith.hyperboost.config;

import com.zalith.hyperboost.HyperBoostV7;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;

/**
 * HyperBoost V7 Configuration
 * Loads from .minecraft/config/hyperboost_v7.properties
 */
public class HyperBoostConfig {

    // Active profile name (PERFORMANCE / BALANCED / QUALITY)
    public String activeProfile = "BALANCED";

    // FPS thresholds (overridden by profile at runtime)
    public int targetFps         = 60;
    public int criticalFps       = 25;
    public int lowFps            = 40;
    public int highFps           = 80;

    // Render & simulation distance (chunks)
    public int minRenderDistance  = 4;
    public int maxRenderDistance  = 12;
    public int minSimDistance     = 4;
    public int maxSimDistance     = 12;

    // Feature toggles
    public boolean autoRenderDistance    = true;
    public boolean autoParticles         = true;
    public boolean autoSimDistance       = true;
    public boolean memoryWatchdog        = true;
    public boolean autoGraphics          = true;
    public boolean hudEnabled            = true;

    // Tuning
    public int memoryWarningPercent      = 80;
    public int memoryCheckIntervalTicks  = 200;
    public int adjustCooldownTicks       = 60;

    private static final Path CONFIG_PATH = Paths.get("config", "hyperboost_v7.properties");

    public static HyperBoostConfig load() {
        HyperBoostConfig cfg = new HyperBoostConfig();
        Properties p = new Properties();

        if (Files.exists(CONFIG_PATH)) {
            try (InputStream in = Files.newInputStream(CONFIG_PATH)) {
                p.load(in);
                cfg.activeProfile            = p.getProperty("active_profile",            cfg.activeProfile);
                cfg.targetFps                = intOf(p, "target_fps",                     cfg.targetFps);
                cfg.criticalFps              = intOf(p, "critical_fps",                   cfg.criticalFps);
                cfg.lowFps                   = intOf(p, "low_fps",                        cfg.lowFps);
                cfg.highFps                  = intOf(p, "high_fps",                       cfg.highFps);
                cfg.minRenderDistance        = intOf(p, "min_render_distance",            cfg.minRenderDistance);
                cfg.maxRenderDistance        = intOf(p, "max_render_distance",            cfg.maxRenderDistance);
                cfg.minSimDistance           = intOf(p, "min_sim_distance",               cfg.minSimDistance);
                cfg.maxSimDistance           = intOf(p, "max_sim_distance",               cfg.maxSimDistance);
                cfg.autoRenderDistance       = boolOf(p, "auto_render_distance",          cfg.autoRenderDistance);
                cfg.autoParticles            = boolOf(p, "auto_particles",                cfg.autoParticles);
                cfg.autoSimDistance          = boolOf(p, "auto_sim_distance",             cfg.autoSimDistance);
                cfg.memoryWatchdog           = boolOf(p, "memory_watchdog",               cfg.memoryWatchdog);
                cfg.autoGraphics             = boolOf(p, "auto_graphics",                 cfg.autoGraphics);
                cfg.hudEnabled               = boolOf(p, "hud_enabled",                   cfg.hudEnabled);
                cfg.memoryWarningPercent     = intOf(p, "memory_warning_percent",         cfg.memoryWarningPercent);
                cfg.memoryCheckIntervalTicks = intOf(p, "memory_check_interval_ticks",    cfg.memoryCheckIntervalTicks);
                cfg.adjustCooldownTicks      = intOf(p, "adjust_cooldown_ticks",          cfg.adjustCooldownTicks);
                HyperBoostV7.LOGGER.info("Config loaded from {}", CONFIG_PATH);
            } catch (IOException e) {
                HyperBoostV7.LOGGER.warn("Failed to load config, using defaults: {}", e.getMessage());
            }
        } else {
            cfg.save();
        }
        return cfg;
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Properties p = new Properties();
            p.setProperty("active_profile",             activeProfile);
            p.setProperty("target_fps",                 String.valueOf(targetFps));
            p.setProperty("critical_fps",               String.valueOf(criticalFps));
            p.setProperty("low_fps",                    String.valueOf(lowFps));
            p.setProperty("high_fps",                   String.valueOf(highFps));
            p.setProperty("min_render_distance",        String.valueOf(minRenderDistance));
            p.setProperty("max_render_distance",        String.valueOf(maxRenderDistance));
            p.setProperty("min_sim_distance",           String.valueOf(minSimDistance));
            p.setProperty("max_sim_distance",           String.valueOf(maxSimDistance));
            p.setProperty("auto_render_distance",       String.valueOf(autoRenderDistance));
            p.setProperty("auto_particles",             String.valueOf(autoParticles));
            p.setProperty("auto_sim_distance",          String.valueOf(autoSimDistance));
            p.setProperty("memory_watchdog",            String.valueOf(memoryWatchdog));
            p.setProperty("auto_graphics",              String.valueOf(autoGraphics));
            p.setProperty("hud_enabled",                String.valueOf(hudEnabled));
            p.setProperty("memory_warning_percent",     String.valueOf(memoryWarningPercent));
            p.setProperty("memory_check_interval_ticks",String.valueOf(memoryCheckIntervalTicks));
            p.setProperty("adjust_cooldown_ticks",      String.valueOf(adjustCooldownTicks));
            try (OutputStream out = Files.newOutputStream(CONFIG_PATH)) {
                p.store(out, "HyperBoost V7 Config — F8: Toggle HUD | F9: Cycle Profile");
            }
        } catch (IOException e) {
            HyperBoostV7.LOGGER.warn("Could not save config: {}", e.getMessage());
        }
    }

    private static int  intOf(Properties p, String k, int def) {
        try { return Integer.parseInt(p.getProperty(k, String.valueOf(def)).trim()); }
        catch (NumberFormatException e) { return def; }
    }
    private static boolean boolOf(Properties p, String k, boolean def) {
        return Boolean.parseBoolean(p.getProperty(k, String.valueOf(def)).trim());
    }
}
