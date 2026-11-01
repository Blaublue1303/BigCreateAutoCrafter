package com.blaublue1303.bigcreateautocrafter.client;

import static com.blaublue1303.bigcreateautocrafter.client.ClientSelection.KEY;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import com.blaublue1303.bigcreateautocrafter.profile.CrafterProfile;
import com.blaublue1303.bigcreateautocrafter.profile.CrafterRegion;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestScreen;
import com.simibubi.create.foundation.gui.AllGuiTextures;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * Button next to the Stock Keeper window that chooses the crafter setup for the JEI "+". A click opens a drop-down list
 * with every option in full plus "Manage profiles…". Drawn and clicked through screen events, so Create's own widget
 * handling stays untouched.
 */
public final class ProfileSelector {
    private static final int MAX_WIDTH = 120;
    private static final int MIN_WIDTH = 64;
    private static final int HEIGHT = 16;
    private static final int GAP = 3;
    private static final int ROW_HEIGHT = 12;
    private static final int PADDING = 4;
    /** Create 6.0.10 draws the Stock Keeper window from {@code guiLeft - 15} with the width of its header texture. */
    private static final int WINDOW_OFFSET_X = -15;
    private static final String ARROW = " ▾";
    private static final String ELLIPSIS = "…";

    @Nullable
    private static Button button;
    /** Inputs the button's label and tooltip were last built from; rebuilt only when these change. */
    @Nullable
    private static List<CrafterProfile> shownProfiles;
    @Nullable
    private static ClientSelection.Option shownOption;

    /** Open drop-down: its rows (options, then "manage") and position; null when closed. */
    @Nullable
    private static Dropdown dropdown;

    private record Dropdown(List<ClientSelection.Option> options, int x, int y, int width) {
        int rows() {
            return options.size() + 1;
        }

        /** Height including padding and the separator line before "manage". */
        int height() {
            return rows() * ROW_HEIGHT + 2 * PADDING + 3;
        }

        int rowTop(int row) {
            return y + PADDING + row * ROW_HEIGHT + (row == options.size() ? 3 : 0);
        }

        /** Row under the mouse, or -1. */
        int rowAt(double mouseX, double mouseY) {
            if (mouseX < x || mouseX >= x + width) {
                return -1;
            }
            for (int row = 0; row < rows(); row++) {
                if (mouseY >= rowTop(row) && mouseY < rowTop(row) + ROW_HEIGHT) {
                    return row;
                }
            }
            return -1;
        }
    }

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
        dropdown = null;
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
            current.setMessage(Component.literal(fit(mc.font, buttonText(option), current.getWidth() - 8 - mc.font.width(ARROW)) + ARROW));
            // vanilla widget tooltip: shown in the screen's next tooltip pass while hovered
            current.setTooltip(Tooltip.create(CommonComponents.joinLines(tooltip(option, profiles.isEmpty()))));
            shownProfiles = profiles;
            shownOption = option;
        }
        current.render(event.getGuiGraphics(), event.getMouseX(), event.getMouseY(), event.getPartialTick());
        if (dropdown != null) {
            renderDropdown(event.getGuiGraphics(), mc.font, dropdown, option, event.getMouseX(), event.getMouseY());
        }
    }

    public static void onMouseClicked(ScreenEvent.MouseButtonPressed.Pre event) {
        Button current = button;
        if (current == null || !(event.getScreen() instanceof StockKeeperRequestScreen screen)) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (dropdown != null) {
            // An open drop-down takes every click: choose a row, or close it.
            int row = dropdown.rowAt(event.getMouseX(), event.getMouseY());
            Dropdown open = dropdown;
            dropdown = null;
            if (row >= 0 && row < open.options().size()) {
                ClientSelection.select(open.options().get(row));
                current.playDownSound(mc.getSoundManager());
            } else if (row == open.options().size()) {
                current.playDownSound(mc.getSoundManager());
                openManager(screen);
            }
            event.setCanceled(true);
        } else if (current.isMouseOver(event.getMouseX(), event.getMouseY()) && mc.level != null) {
            dropdown = createDropdown(screen, current, mc.font, ClientSelection.options(mc.level.dimension()));
            current.playDownSound(mc.getSoundManager());
            event.setCanceled(true);
        }
    }

    public static void onKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        if (dropdown != null && event.getKeyCode() == GLFW.GLFW_KEY_ESCAPE && event.getScreen() instanceof StockKeeperRequestScreen) {
            dropdown = null;
            event.setCanceled(true);
        }
    }

    /** Opens the profile menu over the Stock Keeper; closing it returns to the Stock Keeper. */
    public static void openManager(Screen parent) {
        dropdown = null;
        Minecraft.getInstance().setScreen(new ProfileManagerScreen(parent));
    }

    /** Opens the drop-down (used by the client smoke test). */
    public static void openDropdown(StockKeeperRequestScreen screen) {
        Minecraft mc = Minecraft.getInstance();
        if (button != null && mc.level != null) {
            dropdown = createDropdown(screen, button, mc.font, ClientSelection.options(mc.level.dimension()));
        }
    }

    private static Dropdown createDropdown(Screen screen, Button anchor, Font font, List<ClientSelection.Option> options) {
        int width = anchor.getWidth();
        for (ClientSelection.Option option : options) {
            width = Math.max(width, font.width(rowText(option)) + 2 * PADDING + 10);
        }
        width = Math.max(width, font.width(Component.translatable(KEY + "manage")) + 2 * PADDING + 10);
        width = Math.min(width, screen.width - 2 * GAP);
        int x = Math.min(anchor.getX(), screen.width - GAP - width);
        Dropdown below = new Dropdown(options, x, anchor.getY() + anchor.getHeight() + 2, width);
        if (below.y() + below.height() <= screen.height - GAP) {
            return below;
        }
        return new Dropdown(options, x, Math.max(GAP, anchor.getY() - 2 - below.height()), width);
    }

    private static void renderDropdown(GuiGraphics graphics, Font font, Dropdown dropdown, ClientSelection.Option selected,
                                       int mouseX, int mouseY) {
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 500);
        TooltipRenderUtil.renderTooltipBackground(graphics, dropdown.x(), dropdown.y(), dropdown.width(), dropdown.height(), 0);
        int hovered = dropdown.rowAt(mouseX, mouseY);
        for (int row = 0; row < dropdown.rows(); row++) {
            int top = dropdown.rowTop(row);
            if (row == hovered) {
                graphics.fill(dropdown.x(), top, dropdown.x() + dropdown.width(), top + ROW_HEIGHT, 0x40FFFFFF);
            }
            if (row < dropdown.options().size()) {
                ClientSelection.Option option = dropdown.options().get(row);
                boolean isSelected = option.equals(selected);
                graphics.drawString(font, isSelected ? "●" : "○", dropdown.x() + PADDING, top + 2, isSelected ? 0xFFFFAA00 : 0xFF808080);
                graphics.drawString(font, rowText(option), dropdown.x() + PADDING + 10, top + 2, isSelected ? 0xFFFFFFFF : 0xFFC0C0C0);
            } else {
                graphics.fill(dropdown.x() + PADDING, top - 2, dropdown.x() + dropdown.width() - PADDING, top - 1, 0x60FFFFFF);
                graphics.drawString(font, Component.translatable(KEY + "manage"), dropdown.x() + PADDING + 10, top + 2, 0xFF7FD7FF);
            }
        }
        graphics.pose().popPose();
    }

    /** Short text for the button: "Create 3×3", the profile name (Auto) or the region name; details are in the list and tooltip. */
    private static String buttonText(ClientSelection.Option option) {
        if (option.isStandard()) {
            return Component.translatable(KEY + "button_standard").getString();
        }
        if (option.regionIndex() < 0) {
            return option.profile().name();
        }
        return option.profile().regions().get(option.regionIndex()).name();
    }

    /** Text of a drop-down row; regions are indented under their profile's "Auto" row. */
    private static String rowText(ClientSelection.Option option) {
        if (option.regionIndex() >= 0) {
            return "  ↳ " + option.profile().regions().get(option.regionIndex()).name();
        }
        return option.label().getString();
    }

    /** {@code text}, cut with "…" if wider than {@code width}. */
    private static String fit(Font font, String text, int width) {
        if (font.width(text) <= width) {
            return text;
        }
        return font.plainSubstrByWidth(text, Math.max(0, width - font.width(ELLIPSIS))) + ELLIPSIS;
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
