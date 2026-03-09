package com.zalith.hyperboost;

import com.zalith.hyperboost.config.HyperBoostConfig;
import com.zalith.hyperboost.hud.HudOverlay;
import com.zalith.hyperboost.profile.ProfileManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/**
 * KeybindManager V7
 *
 * Registers two keybinds in the "HyperBoost" category:
 *
 *  F8  — toggle HUD overlay on/off
 *  F9  — cycle profile: PERFORMANCE → BALANCED → QUALITY → …
 */
@Environment(EnvType.CLIENT)
public class KeybindManager {

    private static KeyBinding toggleHud;
    private static KeyBinding cycleProfile;

    public static void init(HyperBoostConfig config) {
        toggleHud = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.hyperboostv7.toggle_hud",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_F8,
            "category.hyperboostv7"
        ));

        cycleProfile = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.hyperboostv7.cycle_profile",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_F9,
            "category.hyperboostv7"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleHud.wasPressed()) {
                HudOverlay.toggleVisible();
                HyperBoostV7.LOGGER.info("HUD visibility toggled: {}", HudOverlay.isVisible());
            }
            while (cycleProfile.wasPressed()) {
                ProfileManager.cycleProfile();
            }
        });

        HyperBoostV7.LOGGER.info("Keybinds registered — F8: Toggle HUD | F9: Cycle Profile");
    }
}
