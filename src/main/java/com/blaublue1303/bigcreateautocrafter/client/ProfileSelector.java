package com.blaublue1303.bigcreateautocrafter.client;

import static com.blaublue1303.bigcreateautocrafter.client.ClientSelection.KEY;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.blaublue1303.bigcreateautocrafter.profile.CrafterProfile;
import com.blaublue1303.bigcreateautocrafter.profile.CrafterRegion;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestScreen;
import com.simibubi.create.foundation.gui.AllGuiTextures;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * Button next to the Stock Keeper window that chooses the crafter setup for the JEI "+".
 * Drawn and clicked through screen events, so Create's own widget handling stays untouched.
 */
public final class ProfileSelector {
    private static final int MAX_WIDTH = 120;
    private static final int MIN_WIDTH = 64;
    private static final int HEIGHT = 16;
    private static final int GAP = 3;
    /** Create 6.0.10 draws the Stock Keeper window from {@code guiLeft - 15} with the width of its header texture. */
    private static final int WINDOW_OFFSET_X = -15;

    @Nullable
    private static Button button;
    /** Inputs the button's label and tooltip were last built from; rebuilt only when these change. */
    @Nullable
    private static List<CrafterProfile> shownProfiles;
    @Nullable
    private static ClientSelection.Option shownOption;

    private ProfileSelector() {
    }

    public static void onInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof StockKeeperRequestScreen screen)) {
            return;
        }
        // Prefer the left side (JEI's ingredient list is on the right); never overlap Create's window.
        int windowLeft = screen.getGuiLeft() + WINDOW_OFFSET_X;
        int windowRight = windowLeft + AllGuiTextures.STOCK_KEEPER_REQUEST_HEADER.getWidth();
        int leftSpace = windowLeft - 2 * GAP;
        int rightSpace = screen.width - windowRight - 2 * GAP;
        int x;
        int width;
        if (leftSpace >= MIN_WIDTH) {
            width = Math.min(MAX_WIDTH, leftSpace);
            x = windowLeft - GAP - width;
        } else if (rightSpace >= MIN_WIDTH) {
            width = Math.min(MAX_WIDTH, rightSpace);
            x = windowRight + GAP;
        } else {
            width = MIN_WIDTH;
            x = GAP;
        }
        int y = screen.getGuiTop() + 4;
        button = Button.builder(Component.empty(), b -> {}).bounds(x, y, width, HEIGHT).build();
        shownOption = null;
        // Create rebuilds this list in every init and hands it to JEI as area to keep free.
        screen.getExtraAreas().add(new Rect2i(x, y, width, HEIGHT));
    }

    public static void onRender(ScreenEvent.Render.Post event) {
        Button current = button;
        Minecraft mc = Minecraft.getInstance();
        if (current == null || mc.level == null || !(event.getScreen() instanceof StockKeeperRequestScreen)) {
            return;
        }
        List<CrafterProfile> profiles = ClientProfileCache.all();
        ClientSelection.Option option = ClientSelection.current(mc.level.dimension());
        if (profiles != shownProfiles || !option.equals(shownOption)) {
            current.setMessage(Component.literal(mc.font.plainSubstrByWidth(option.label().getString(), current.getWidth() - 8)));
            // vanilla widget tooltip: shown in the screen's next tooltip pass while hovered
            current.setTooltip(Tooltip.create(CommonComponents.joinLines(tooltip(option, profiles.isEmpty()))));
            shownProfiles = profiles;
            shownOption = option;
        }
        current.render(event.getGuiGraphics(), event.getMouseX(), event.getMouseY(), event.getPartialTick());
    }

    public static void onMouseClicked(ScreenEvent.MouseButtonPressed.Pre event) {
        Button current = button;
        if (current == null || !(event.getScreen() instanceof StockKeeperRequestScreen) || !current.isMouseOver(event.getMouseX(), event.getMouseY())) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && (event.getButton() == 0 || event.getButton() == 1)) {
            ClientSelection.cycle(mc.level.dimension(), event.getButton() == 0 ? 1 : -1);
            current.playDownSound(mc.getSoundManager());
        }
        event.setCanceled(true);
    }

    private static List<Component> tooltip(ClientSelection.Option option, boolean noProfiles) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(KEY + "title").withStyle(ChatFormatting.GOLD));
        lines.add(option.label().copy().withStyle(ChatFormatting.WHITE));
        if (option.isStandard()) {
            lines.add(Component.translatable(KEY + "standard_hint").withStyle(ChatFormatting.GRAY));
        } else if (option.regionIndex() < 0) {
            lines.add(Component.translatable(KEY + "auto_hint").withStyle(ChatFormatting.GRAY));
            for (CrafterRegion region : option.profile().regions()) {
                lines.add(regionLine(region));
            }
        } else {
            lines.add(regionLine(option.profile().regions().get(option.regionIndex())));
        }
        lines.add(Component.translatable(KEY + "controls").withStyle(ChatFormatting.DARK_GRAY));
        if (noProfiles) {
            lines.add(Component.translatable(KEY + "no_profiles").withStyle(ChatFormatting.DARK_GRAY));
        }
        return lines;
    }

    private static Component regionLine(CrafterRegion region) {
        return Component.literal(" • " + region.name() + "  → " + region.displayAddress()).withStyle(ChatFormatting.GRAY);
    }
}
