package com.blaublue1303.bigcreateautocrafter.client;

import com.blaublue1303.bigcreateautocrafter.compat.create.CreateCompat;
import com.blaublue1303.bigcreateautocrafter.net.CrafterClickPayload;
import com.blaublue1303.bigcreateautocrafter.net.ModifierStatePayload;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Profile-modifier (default Left Ctrl) + right-click on a Mechanical Crafter: create a profile / add or remove a
 * region. The decision itself is made on the server ({@code ProfileService}).
 */
public final class CrafterClickHandler {
    private static boolean reportedHeld;

    private CrafterClickHandler() {
    }

    /** Checks the physical key, so it also works when the key is shared with another binding (Ctrl = sprint). */
    public static boolean isModifierHeld() {
        Minecraft mc = Minecraft.getInstance();
        KeyMapping mapping = BcacKeyMappings.PROFILE_MODIFIER;
        if (mc.screen != null || mapping.isUnbound()) {
            return false;
        }
        InputConstants.Key key = mapping.getKey();
        if (key.getType() == InputConstants.Type.KEYSYM) {
            return InputConstants.isKeyDown(mc.getWindow().getWindow(), key.getValue());
        }
        return mapping.isDown();
    }

    public static void onClientTick(ClientTickEvent.Pre event) {
        if (Minecraft.getInstance().getConnection() == null) {
            reportedHeld = false;
            return;
        }
        report(isModifierHeld());
    }

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getLevel().isClientSide() || !isModifierHeld()
                || !CreateCompat.isMechanicalCrafter(event.getLevel().getBlockState(event.getPos()))) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getHand() == InteractionHand.MAIN_HAND) {
            report(true);
            PacketDistributor.sendToServer(new CrafterClickPayload(event.getPos(), Screen.hasShiftDown()));
        }
    }

    private static void report(boolean held) {
        if (held != reportedHeld) {
            reportedHeld = held;
            PacketDistributor.sendToServer(new ModifierStatePayload(held));
        }
    }
}
