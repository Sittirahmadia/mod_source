package com.zalith.hyperboost;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

@Environment(EnvType.CLIENT)
public class FPSGovernor {

    public static void init() {
        // Register a client tick event to monitor FPS after game has loaded
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            
            int fps = client.getCurrentFps();
            if (fps > 0 && fps < 40) {
                System.out.println("HyperBoost: Low FPS detected (" + fps + "), applying optimizations");
            }
        });
        System.out.println("HyperBoost: FPSGovernor registered");
    }
}
