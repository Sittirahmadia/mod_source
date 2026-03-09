package com.zalith.hyperboost;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class HyperBoostV5 implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        System.out.println("HyperBoost V5 Engine Loaded");
        FPSGovernor.init();
        EntityOptimizer.init();
    }
}
