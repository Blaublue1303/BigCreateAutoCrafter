package com.blaublue1303.bigcreateautocrafter.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.simibubi.create.AllSoundEvents;

import net.createmod.catnip.gui.widget.AbstractSimiWidget;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;

/**
 * Drawing helpers for our screens in the look of Create's Stock Keeper: paper cards and the wooden panel of its category
 * editor, its text colours and its tooltip style. The textures are read from Create's jar (Create is a required
 * dependency), not copied into this mod.
 */
final class CreateUi {
    /** Create 6.0.10: {@code AllGuiTextures.STOCK_KEEPER_CATEGORY_*} live on this sheet (256 × 256). */
    private static final ResourceLocation CATEGORIES = ResourceLocation.fromNamespaceAndPath("create", "textures/gui/stock_keeper_categories.png");

    /** Text on a paper card (Create's category names). */
    static final int TEXT_CARD = 0x656565;
    /** Dark brown of the Stock Keeper title and address text. */
    static final int TEXT_DARK = 0x714A40;
    /** Placeholder text on paper. */
    static final int TEXT_MUTED = 0xB59370;
    /** Title on the blue header strip of the category editor. */
    static final int TEXT_HEADER = 0x3D3C48;
    /** Light text on wood with its dark drop shadow (Stock Keeper category headings). */
    static final int TEXT_LIGHT = 0xF8F8EC;
    static final int TEXT_LIGHT_SHADOW = 0x4A2D31;
    /** The brown of the "×" and "+" marks on Create's cards. */
    static final int MARK = 0x8C5D4B;
    static final int WARNING = 0xB4302A;

    static final int CARD_HEIGHT = 18;
    /** Card body starts after the ring and notch; Create puts the item icon here. */
    static final int CARD_ICON_X = 12;
    static final int CARD_TEXT_X = 32;
    /** Child cards (regions) sit this far right of their parent card, tied to it by a thread. */
    static final int CHILD_INDENT = 10;
    /** Width of the card end that carries the "×". */
    static final int CARD_CROSS_WIDTH = 17;

    static final int PANEL_HEADER = 18;
    static final int PANEL_BOTTOM = 2;
    static final int PANEL_ROW = 20;

    private CreateUi() {
    }

    // ---- cards ----

    /** A paper card like Create's category entries, any width ≥ 32, optionally with the "×" at its right end. */
    static void card(GuiGraphics graphics, int x, int y, int width, boolean cross) {
        int rightWidth = cross ? CARD_CROSS_WIDTH : 3;
        graphics.blit(CATEGORIES, x, y, 38, 159, 12, CARD_HEIGHT);
        graphics.blit(CATEGORIES, x + 12, y, width - 12 - rightWidth, CARD_HEIGHT, 100, 159, 1, CARD_HEIGHT, 256, 256);
        graphics.blit(CATEGORIES, x + width - rightWidth, y, cross ? 192 : 206, 159, rightWidth, CARD_HEIGHT);
    }

    /** Card tinted: light blue for the selected entry, slightly darker while hovered. */
    static void card(GuiGraphics graphics, int x, int y, int width, boolean cross, boolean selected, boolean hovered) {
        float shade = hovered ? 0.9f : 1f;
        if (selected) {
            RenderSystem.setShaderColor(0.80f * shade, 0.88f * shade, 1f * shade, 1f);
        } else {
            RenderSystem.setShaderColor(shade, shade, shade, 1f);
        }
        card(graphics, x, y, width, cross);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    /** Whether the mouse is over the "×" of a card drawn at {@code x, y} with {@code width}. */
    static boolean overCross(int x, int y, int width, double mouseX, double mouseY) {
        int crossX = x + width - CARD_CROSS_WIDTH + 2;
        return mouseX >= crossX && mouseX < crossX + 12 && mouseY >= y + 2 && mouseY < y + CARD_HEIGHT - 2;
    }

    /** Covers the card's "×" with a red one (armed delete). */
    static void armedCross(GuiGraphics graphics, int x, int y, int width) {
        int cx = x + width - CARD_CROSS_WIDTH + 5;
        int cy = y + 5;
        int[][] pixels = {{0, 1, 5, 6}, {0, 1, 2, 4, 5, 6}, {1, 2, 3, 4, 5}, {2, 3, 4}, {1, 2, 3, 4, 5}, {0, 1, 2, 4, 5, 6}, {0, 1, 5, 6}};
        for (int row = 0; row < pixels.length; row++) {
            for (int column : pixels[row]) {
                graphics.fill(cx + column, cy + row, cx + column + 1, cy + row + 1, 0xFF000000 | WARNING);
            }
        }
    }

    /** Small pixel arrow pointing down, in the brown of Create's card marks. */
    static void arrowDown(GuiGraphics graphics, int x, int y) {
        int color = 0xFF000000 | MARK;
        graphics.fill(x, y, x + 5, y + 1, color);
        graphics.fill(x + 1, y + 1, x + 4, y + 2, color);
        graphics.fill(x + 2, y + 2, x + 3, y + 3, color);
    }

    /** The string that ties a child card's ring to its parent card's ring (a card's ring: x+0…3, y+7…11). */
    static void thread(GuiGraphics graphics, int parentX, int parentY, int childX, int childY) {
        int x = parentX + 1;
        int top = parentY + 12;
        int y = childY + 8;
        graphics.fill(x, top, x + 2, y + 2, 0xFF000000 | TEXT_DARK);
        graphics.fill(x, top, x + 1, y + 1, 0xFF000000 | MARK);
        graphics.fill(x, y, childX, y + 2, 0xFF000000 | TEXT_DARK);
        graphics.fill(x, y, childX, y + 1, 0xFF000000 | MARK);
    }

    // ---- wooden panel (category editor) ----

    /** Blue title strip with the start of the wood below it ({@value #PANEL_HEADER} px high). */
    static void panelHeader(GuiGraphics graphics, Font font, int x, int y, int width, Component title) {
        stretchRow(graphics, x, y, width, 0, PANEL_HEADER);
        graphics.drawString(font, title, x + (width - font.width(title)) / 2, y + 4, TEXT_HEADER, false);
    }

    /** Wood between header and bottom, any height. */
    static void panelBody(GuiGraphics graphics, int x, int y, int width, int height) {
        for (int offset = 0; offset < height; offset += PANEL_ROW) {
            stretchRow(graphics, x, y + offset, width, 32, Math.min(PANEL_ROW, height - offset));
        }
    }

    /** Lower edge of the wood ({@value #PANEL_BOTTOM} px). */
    static void panelBottom(GuiGraphics graphics, int x, int y, int width) {
        stretchRow(graphics, x, y, width, 79, PANEL_BOTTOM);
    }

    /** One horizontal band of the 192 px wide category window, stretched to {@code width} (4 px edges kept). */
    private static void stretchRow(GuiGraphics graphics, int x, int y, int width, int v, int height) {
        graphics.blit(CATEGORIES, x, y, 32, v, 4, height);
        graphics.blit(CATEGORIES, x + 4, y, width - 8, height, 100, v, 1, height, 256, 256);
        graphics.blit(CATEGORIES, x + width - 4, y, 220, v, 4, height);
    }

    // ---- text ----

    /** {@code text}, cut with "…" if wider than {@code width}. */
    static String fit(Font font, String text, int width) {
        if (font.width(text) <= width) {
            return text;
        }
        return font.plainSubstrByWidth(text, Math.max(0, width - font.width("…"))) + "…";
    }

    /** Light text with a dark shadow one pixel down-right, as Create draws headings on wood. */
    static void lightText(GuiGraphics graphics, Font font, Component text, int x, int y) {
        graphics.drawString(font, text, x + 1, y + 1, TEXT_LIGHT_SHADOW, false);
        graphics.drawString(font, text, x, y, TEXT_LIGHT, false);
    }

    /** Tooltip heading in Create's blue. */
    static MutableComponent header(Component text) {
        return text.copy().withStyle(s -> s.withColor(AbstractSimiWidget.HEADER_RGB.getRGB()));
    }

    /** Tooltip hint in Create's light blue. */
    static MutableComponent hint(Component text) {
        return text.copy().withStyle(s -> s.withColor(AbstractSimiWidget.HINT_RGB.getRGB()));
    }

    /** Control hint in Create's style (dark grey, italic). */
    static MutableComponent controls(Component text) {
        return text.copy().withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC);
    }

    // ---- sounds ----

    static void click() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1f));
    }

    /** Create's scroll-input tick; pitch rises with {@code position} (0…1) like in Create. */
    static void scrollTick(float position) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(AllSoundEvents.SCROLL_VALUE.getMainEvent(), 1.5f + 0.1f * position));
    }
}
