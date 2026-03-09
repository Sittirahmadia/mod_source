package com.zalith.hyperboost.hud;

import com.zalith.hyperboost.FPSGovernor;
import com.zalith.hyperboost.FpsSpikeDetector;
import com.zalith.hyperboost.HyperBoostV7;
import com.zalith.hyperboost.WorldJoinHandler;
import com.zalith.hyperboost.config.HyperBoostConfig;
import com.zalith.hyperboost.profile.ProfileManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/**
 * HudOverlay V7
 *
 * Compact on-screen panel. Toggle with F8. Auto-hides on F3/menus.
 *
 * Layout:
 *  [HyperBoost V7] ■ BALANCED  [WARMUP]
 *  FPS: 62 (avg 60)    Frame: 16ms
 *  Render: 10ch  Sim: 8ch
 *  Mem: 52%  (1024/2048MB)
 *  Particles: decreased  Spikes: 2
 */
@Environment(EnvType.CLIENT)
public class HudOverlay {

    private static boolean visible = true;
    private static HyperBoostConfig config;

    private static final int BG_COLOR    = 0x99000000;
    private static final int COLOR_VALUE = 0xFFFFFFFF;

    private static final int PANEL_X     = 4;
    private static final int PANEL_Y     = 4;
    private static final int LINE_HEIGHT = 10;
    private static final int PADDING     = 4;
    private static final int PANEL_W     = 220;

    public static void init(HyperBoostConfig cfg) {
        config = cfg;
        visible = cfg.hudEnabled;
        HudRenderCallback.EVENT.register(HudOverlay::render);
        HyperBoostV7.LOGGER.info("HUD Overlay V7 registered (F8 to toggle)");
    }

    private static void render(DrawContext ctx, float tickDelta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!visible) return;
        if (mc.options.debugEnabled) return;
        if (mc.currentScreen != null) return;

        TextRenderer font = mc.textRenderer;

        int avgFps    = FPSGovernor.getAverageFps();
        int curFps    = mc.getCurrentFps();
        int render    = mc.options.getViewDistance().getValue();
        int sim       = mc.options.getSimulationDistance().getValue();
        String parts  = mc.options.getParticles().getValue().toString().toLowerCase();
        String prof   = ProfileManager.getCurrent().name();
        boolean warmup = WorldJoinHandler.isInWarmup();
        int spikes    = FpsSpikeDetector.getSpikeCount();

        // Frame time in ms (inverse of FPS)
        String frameMs = curFps > 0 ? String.format("%.1fms", 1000.0f / curFps) : "—";

        Runtime rt    = Runtime.getRuntime();
        long usedMB   = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
        long maxMB    = rt.maxMemory() / (1024 * 1024);
        int  memPct   = (int)(usedMB * 100 / maxMB);

        String warmupLabel = warmup ? " §c[WARMUP]§r" : "";

        String[] lines = {
            "§b[HyperBoost V7] §e" + prof + warmupLabel,
            "§7FPS: " + fpsColor(curFps) + curFps + " §7(avg §f" + avgFps + "§7)  §7Frame: §f" + frameMs,
            "§7Render: §f" + render + "§7ch  Sim: §f" + sim + "§7ch",
            "§7Mem: " + memColor(memPct) + memPct + "% §7(" + usedMB + "/" + maxMB + "MB)"
                + (spikes > 0 ? "  §7Spikes: §c" + spikes : ""),
            "§7Particles: §f" + parts
        };

        int panelH = PADDING * 2 + lines.length * LINE_HEIGHT;

        ctx.fill(PANEL_X, PANEL_Y, PANEL_X + PANEL_W, PANEL_Y + panelH, BG_COLOR);

        for (int i = 0; i < lines.length; i++) {
            ctx.drawTextWithShadow(font, lines[i], PANEL_X + PADDING,
                PANEL_Y + PADDING + i * LINE_HEIGHT, COLOR_VALUE);
        }
    }

    private static String fpsColor(int fps) {
        if (fps < config.criticalFps) return "§c";
        if (fps < config.lowFps)      return "§6";
        return "§a";
    }

    private static String memColor(int pct) {
        if (pct >= 90) return "§c";
        if (pct >= 75) return "§6";
        return "§a";
    }

    public static void toggleVisible() {
        visible = !visible;
        if (config != null) {
            config.hudEnabled = visible;
            config.save();
        }
    }

    public static boolean isVisible() { return visible; }
}
