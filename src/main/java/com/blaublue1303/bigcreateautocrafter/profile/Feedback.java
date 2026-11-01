package com.blaublue1303.bigcreateautocrafter.profile;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/** Result of a profile edit, shown to the player (chat for commands, action bar for the profile menu). */
public record Feedback(boolean success, Component message) {
    static Feedback ok(Component message) {
        return new Feedback(true, message.copy().withStyle(ChatFormatting.GREEN));
    }

    static Feedback fail(Component message) {
        return new Feedback(false, message.copy().withStyle(ChatFormatting.RED));
    }
}
