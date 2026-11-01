package com.blaublue1303.bigcreateautocrafter.client;

import org.lwjgl.glfw.GLFW;

import com.blaublue1303.bigcreateautocrafter.BigCreateAutoCrafter;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;

/**
 * Key held while right-clicking a Mechanical Crafter to create a profile / add a region.
 * Default Left Ctrl as requested; rebindable in Controls (Left Ctrl is also vanilla's sprint key, and
 * Ctrl + click is a right click on macOS, see risk R13).
 */
public final class BcacKeyMappings {
    public static final String CATEGORY = "key.categories." + BigCreateAutoCrafter.MODID;

    public static final KeyMapping PROFILE_MODIFIER = new KeyMapping(
            "key." + BigCreateAutoCrafter.MODID + ".profile_modifier",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_CONTROL,
            CATEGORY);

    /** Opens the "Crafter profiles" menu from anywhere in game; unbound by default. */
    public static final KeyMapping OPEN_MANAGER = new KeyMapping(
            "key." + BigCreateAutoCrafter.MODID + ".open_manager",
            KeyConflictContext.IN_GAME,
            InputConstants.UNKNOWN,
            CATEGORY);

    private BcacKeyMappings() {
    }

    static void register(RegisterKeyMappingsEvent event) {
        event.register(PROFILE_MODIFIER);
        event.register(OPEN_MANAGER);
    }

    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        while (OPEN_MANAGER.consumeClick()) {
            if (mc.screen == null && mc.player != null) {
                mc.setScreen(new ProfileManagerScreen(null));
            }
        }
    }
}
