package com.zalith.hyperboost.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.zalith.hyperboost.FPSGovernor;
import com.zalith.hyperboost.FpsSpikeDetector;
import com.zalith.hyperboost.HyperBoostV7;
import com.zalith.hyperboost.WorldJoinHandler;
import com.zalith.hyperboost.config.HyperBoostConfig;
import com.zalith.hyperboost.profile.ProfileManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;

/**
 * StatusCommand V7
 *
 * Registers the /hyperboost (alias /hb) client-side command.
 *
 * Subcommands:
 *   /hb status   — print live performance snapshot in chat
 *   /hb profile  — cycle profile (same as F9)
 *   /hb reset    — reset all settings to default config values
 */
public class StatusCommand {

    public static void register(HyperBoostConfig config) {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            registerCommands(dispatcher, config);
        });
        HyperBoostV7.LOGGER.info("StatusCommand V7 registered (/hb, /hyperboost)");
    }

    private static void registerCommands(
            CommandDispatcher<FabricClientCommandSource> dispatcher,
            HyperBoostConfig config) {

        var root = ClientCommandManager.literal("hyperboost")
            .then(ClientCommandManager.literal("status")
                .executes(ctx -> cmdStatus(ctx, config)))
            .then(ClientCommandManager.literal("profile")
                .executes(ctx -> cmdProfile(ctx, config)))
            .then(ClientCommandManager.literal("reset")
                .executes(ctx -> cmdReset(ctx, config)))
            .executes(ctx -> cmdStatus(ctx, config)) // /hyperboost alone → status
            .build();

        var alias = ClientCommandManager.literal("hb")
            .then(ClientCommandManager.literal("status")
                .executes(ctx -> cmdStatus(ctx, config)))
            .then(ClientCommandManager.literal("profile")
                .executes(ctx -> cmdProfile(ctx, config)))
            .then(ClientCommandManager.literal("reset")
                .executes(ctx -> cmdReset(ctx, config)))
            .executes(ctx -> cmdStatus(ctx, config))
            .build();

        dispatcher.getRoot().addChild(root);
        dispatcher.getRoot().addChild(alias);
    }

    private static int cmdStatus(CommandContext<FabricClientCommandSource> ctx, HyperBoostConfig config) {
        var client = ctx.getSource().getClient();

        int avgFps   = FPSGovernor.getAverageFps();
        int curFps   = client.getCurrentFps();
        int render   = client.options.getViewDistance().getValue();
        int sim      = client.options.getSimulationDistance().getValue();
        int spikes   = FpsSpikeDetector.getSpikeCount();
        String profile = ProfileManager.getCurrent().name();
        boolean warmup = WorldJoinHandler.isInWarmup();

        Runtime rt     = Runtime.getRuntime();
        long usedMB    = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
        long maxMB     = rt.maxMemory() / (1024 * 1024);
        int  memPct    = (int)(usedMB * 100 / maxMB);

        String fpsColor  = curFps < config.criticalFps ? "§c" : curFps < config.lowFps ? "§6" : "§a";
        String memColor  = memPct  > 90 ? "§c" : memPct > 75 ? "§6" : "§a";

        send(ctx, "§b╔═══════════════════════════════╗");
        send(ctx, "§b║  §fHyperBoost V7 Status         §b║");
        send(ctx, "§b╚═══════════════════════════════╝");
        send(ctx, " §7Profile:   §e" + profile + (warmup ? " §c[WARMUP]" : ""));
        send(ctx, " §7FPS:       " + fpsColor + curFps + " §7(avg §f" + avgFps + "§7)");
        send(ctx, " §7Render:    §f" + render + " §7chunks  Sim: §f" + sim + " §7chunks");
        send(ctx, " §7Memory:    " + memColor + memPct + "% §7(" + usedMB + "/" + maxMB + " MB)");
        send(ctx, " §7Spikes:    §f" + spikes + " §7detected this session");
        send(ctx, " §7Particles: §f" + client.options.getParticles().getValue().toString().toLowerCase());
        send(ctx, " §7Graphics:  §f" + client.options.getGraphicsMode().getValue().toString().toLowerCase());
        send(ctx, "§7Use §f/hb profile §7to cycle · §f/hb reset §7to reset");

        return 1;
    }

    private static int cmdProfile(CommandContext<FabricClientCommandSource> ctx, HyperBoostConfig config) {
        ProfileManager.cycleProfile();
        return 1;
    }

    private static int cmdReset(CommandContext<FabricClientCommandSource> ctx, HyperBoostConfig config) {
        // Reset to a fresh BALANCED config
        HyperBoostConfig fresh = new HyperBoostConfig();
        config.activeProfile            = fresh.activeProfile;
        config.targetFps                = fresh.targetFps;
        config.criticalFps              = fresh.criticalFps;
        config.lowFps                   = fresh.lowFps;
        config.highFps                  = fresh.highFps;
        config.minRenderDistance        = fresh.minRenderDistance;
        config.maxRenderDistance        = fresh.maxRenderDistance;
        config.autoParticles            = fresh.autoParticles;
        config.memoryWarningPercent     = fresh.memoryWarningPercent;
        config.save();
        send(ctx, "§b[HyperBoost V7]§r Config reset to defaults (BALANCED).");
        HyperBoostV7.LOGGER.info("Config reset to defaults via /hb reset");
        return 1;
    }

    private static void send(CommandContext<FabricClientCommandSource> ctx, String msg) {
        ctx.getSource().sendFeedback(Text.literal(msg));
    }
}
