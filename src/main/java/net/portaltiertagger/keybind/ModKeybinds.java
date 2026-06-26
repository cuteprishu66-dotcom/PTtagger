package net.portaltiertagger.keybind;

import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.portaltiertagger.PortalTierTagger;
import net.portaltiertagger.config.ModConfig;
import org.lwjgl.glfw.GLFW;

public class ModKeybinds {

    private static KeyBinding toggleKey;
    private static KeyBinding refreshKey;
    private static KeyBinding openGuiKey; // NEW

    public static void register() {
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.portaltiertagger.toggle",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_P,
            "category.portaltiertagger"
        ));

        refreshKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.portaltiertagger.refresh",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            "category.portaltiertagger"
        ));

        // NEW: Right Shift opens config
        openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.portaltiertagger.opengui",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_RIGHT_SHIFT,
            "category.portaltiertagger"
        ));
    }

    public static void tick() {
        while (toggleKey.wasPressed()) {
            ModConfig config = PortalTierTagger.getConfig();
            if (config != null) {
                config.enabled = !config.enabled;
                if (PortalTierTagger.getConfigManager() != null) PortalTierTagger.getConfigManager().save();
                if (PortalTierTagger.getClient() != null && PortalTierTagger.getClient().player != null) {
                    PortalTierTagger.getClient().player.sendMessage(
                        Text.literal("Tier Tagger: ").append(
                            Text.literal(config.enabled ? "ON" : "OFF").formatted(config.enabled ? Formatting.GREEN : Formatting.RED)
                        ), true
                    );
                }
            }
        }

        while (refreshKey.wasPressed()) {
            PortalTierTagger.triggerRefresh();
            if (PortalTierTagger.getClient() != null && PortalTierTagger.getClient().player != null) {
                PortalTierTagger.getClient().player.sendMessage(
                    Text.literal("Refreshing rankings...").formatted(Formatting.YELLOW), true
                );
            }
        }

        // NEW: Open Config Menu
        while (openGuiKey.wasPressed()) {
            MinecraftClient client = PortalTierTagger.getClient();
            if (client != null && client.currentScreen == null) { // Only open if no other menu is open
                Screen screen = AutoConfig.getConfigScreen(ModConfig.class, client.currentScreen).get();
                client.setScreen(screen);
            }
        }
    }
}
