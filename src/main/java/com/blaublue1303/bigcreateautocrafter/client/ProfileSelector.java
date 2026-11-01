package com.blaublue1303.bigcreateautocrafter.client;

import static com.blaublue1303.bigcreateautocrafter.client.ClientSelection.KEY;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import com.blaublue1303.bigcreateautocrafter.profile.CrafterRegion;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestScreen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * Chooses the crafter setup for the JEI "+" in the Stock Keeper. A paper tag in the style of Create's Stock Keeper
 * cards sits on the left frame of the window, level with the search bar, and shows the current choice. Clicking it opens
 * a wooden panel like Create's category editor with one card per option and "Manage profiles…"; scrolling over the tag
 * steps through the options like a Create scroll input. Drawn and clicked through screen events, so Create's own widget
 * handling stays untouched.
 */
public final class ProfileSelector {
    /** Create 6.0.10 draws the Stock Keeper window from {@code guiLeft - 15}; its frame is opaque from texture x 24. */
    private static final int FRAME_LEFT = 9;
    /** The search bar strip of the Stock Keeper header (y offset and height). */
    private static final int SEARCH_ROW_Y = 17;
    private static final int MIN_TAG_WIDTH = 44;
    private static final int MAX_TAG_WIDTH = 150;
    private static final int MARGIN = 2;
    private static final int PANEL_PADDING = 6;
    private static final int MANAGE_GAP = 4;
    private static final float PANEL_Z = 350;

    private static final ItemStack CRAFTER = AllBlocks.MECHANICAL_CRAFTER.asStack();
    private static final ItemStack CRAFTING_TABLE = new ItemStack(Items.CRAFTING_TABLE);
    private static final ItemStack WRENCH = AllItems.WRENCH.asStack();

    /** The tag's bounds; also handed to JEI as an area to keep free, so it is resized in place. */
    @Nullable
    private static Rect2i tag;
    @Nullable
    private static Panel panel;
    @Nullable
    private static List<Rect2i> jeiAreas;

    /** One card in the open panel: an option or (option == null) "Manage profiles…". */
    private record Row(@Nullable ClientSelection.Option option, int x, int y, int width) {
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + CreateUi.CARD_HEIGHT;
        }
    }

    private record Panel(Rect2i area, List<Row> rows) {
        @Nullable
        Row rowAt(double mouseX, double mouseY) {
            return rows.stream().filter(row -> row.contains(mouseX, mouseY)).findFirst().orElse(null);
        }

        boolean contains(double mouseX, double mouseY) {
            return ProfileSelector.contains(area, mouseX, mouseY);
        }
    }

    private ProfileSelector() {
    }

    public static void onInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof StockKeeperRequestScreen screen)) {
            return;
        }
        panel = null;
        tag = new Rect2i(0, screen.getGuiTop() + SEARCH_ROW_Y, MIN_TAG_WIDTH, CreateUi.CARD_HEIGHT);
        layoutTag(screen, MIN_TAG_WIDTH);
        // Create rebuilds this list in every init and hands it to JEI as the area to keep free.
        jeiAreas = screen.getExtraAreas();
        jeiAreas.add(tag);
    }

    public static void onRender(ScreenEvent.Render.Post event) {
        Rect2i bounds = tag;
        Minecraft mc = Minecraft.getInstance();
        if (bounds == null || mc.level == null || !(event.getScreen() instanceof StockKeeperRequestScreen screen)) {
            return;
        }
        GuiGraphics graphics = event.getGuiGraphics();
        Font font = mc.font;
        int mouseX = event.getMouseX();
        int mouseY = event.getMouseY();
        ClientSelection.Option current = ClientSelection.current(mc.level.dimension());

        String text = tagText(current);
        layoutTag(screen, CreateUi.CARD_TEXT_X + font.width(text) + 14);
        boolean hovered = panel == null && contains(bounds, mouseX, mouseY);
        renderTag(graphics, font, bounds, current, text, hovered);

        if (panel != null) {
            renderPanel(graphics, font, panel, current, mouseX, mouseY);
        } else if (hovered) {
            graphics.renderComponentTooltip(font, tooltip(current), mouseX, mouseY);
        }
    }

    public static void onMouseClicked(ScreenEvent.MouseButtonPressed.Pre event) {
        Rect2i bounds = tag;
        Minecraft mc = Minecraft.getInstance();
        if (bounds == null || mc.level == null || !(event.getScreen() instanceof StockKeeperRequestScreen screen)) {
            return;
        }
        if (panel != null) {
            // An open panel takes every click: choose a card, or close it.
            Row row = panel.rowAt(event.getMouseX(), event.getMouseY());
            closePanel();
            if (row != null && row.option() != null) {
                ClientSelection.select(row.option());
                CreateUi.click();
            } else if (row != null) {
                CreateUi.click();
                openManager(screen);
            }
            event.setCanceled(true);
        } else if (contains(bounds, event.getMouseX(), event.getMouseY())) {
            openPanel(screen);
            CreateUi.click();
            event.setCanceled(true);
        }
    }

    /** Scrolling over the tag steps through the options (down = next), like Create's selection inputs. */
    public static void onMouseScrolled(ScreenEvent.MouseScrolled.Pre event) {
        Rect2i bounds = tag;
        Minecraft mc = Minecraft.getInstance();
        if (bounds == null || mc.level == null || !(event.getScreen() instanceof StockKeeperRequestScreen)) {
            return;
        }
        if (panel != null) {
            if (panel.contains(event.getMouseX(), event.getMouseY())) {
                event.setCanceled(true);
            }
            return;
        }
        if (!contains(bounds, event.getMouseX(), event.getMouseY()) || event.getScrollDeltaY() == 0) {
            return;
        }
        List<ClientSelection.Option> options = ClientSelection.options(mc.level.dimension());
        int index = Math.max(0, options.indexOf(ClientSelection.current(mc.level.dimension())));
        int next = Math.clamp(index + (event.getScrollDeltaY() < 0 ? 1 : -1), 0, options.size() - 1);
        if (next != index) {
            ClientSelection.select(options.get(next));
            CreateUi.scrollTick(options.size() > 1 ? next / (float) (options.size() - 1) : 0);
        }
        event.setCanceled(true);
    }

    /** Create still sees the mouse over its items under the open panel; their tooltips would cover the cards. */
    public static void onRenderTooltip(RenderTooltipEvent.Pre event) {
        if (panel != null && Minecraft.getInstance().screen instanceof StockKeeperRequestScreen && panel.contains(event.getX(), event.getY())) {
            event.setCanceled(true);
        }
    }

    public static void onKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        if (panel != null && event.getKeyCode() == GLFW.GLFW_KEY_ESCAPE && event.getScreen() instanceof StockKeeperRequestScreen) {
            closePanel();
            event.setCanceled(true);
        }
    }

    /** Opens the profile menu over the Stock Keeper; closing it returns to the Stock Keeper. */
    public static void openManager(Screen parent) {
        closePanel();
        Minecraft.getInstance().setScreen(new ProfileManagerScreen(parent));
    }

    /** Opens the option panel (also used by the client smoke test). */
    public static void openPanel(StockKeeperRequestScreen screen) {
        Minecraft mc = Minecraft.getInstance();
        if (tag == null || mc.level == null) {
            return;
        }
        closePanel();
        panel = createPanel(screen, mc.font, tag, ClientSelection.options(mc.level.dimension()));
        if (jeiAreas != null) {
            jeiAreas.add(panel.area());
        }
    }

    /** Where the tag is on the open Stock Keeper (used by the client smoke test). */
    public static Optional<Rect2i> tagArea() {
        return Optional.ofNullable(tag);
    }

    private static void closePanel() {
        if (panel != null && jeiAreas != null) {
            jeiAreas.remove(panel.area());
        }
        panel = null;
    }

    // ---- tag ----

    /** Places the tag left of the window frame, its right edge on the frame's outline; {@code desired} is its natural width. */
    private static void layoutTag(StockKeeperRequestScreen screen, int desired) {
        Rect2i bounds = tag;
        if (bounds == null) {
            return;
        }
        int right = screen.getGuiLeft() + FRAME_LEFT + 1;
        int space = right - MARGIN;
        int width = Math.clamp(desired, MIN_TAG_WIDTH, Math.max(MIN_TAG_WIDTH, Math.min(MAX_TAG_WIDTH, space)));
        bounds.setX(space >= MIN_TAG_WIDTH ? right - width : MARGIN);
        bounds.setWidth(width);
    }

    private static void renderTag(GuiGraphics graphics, Font font, Rect2i bounds, ClientSelection.Option current, String text,
                                  boolean hovered) {
        int x = bounds.getX();
        int y = bounds.getY();
        int width = bounds.getWidth();
        CreateUi.card(graphics, x, y, width, false, false, hovered);
        graphics.renderItem(current.isStandard() ? CRAFTING_TABLE : CRAFTER, x + CreateUi.CARD_ICON_X, y + 1);
        int textWidth = width - CreateUi.CARD_TEXT_X - 13;
        if (textWidth >= 12) {
            graphics.drawString(font, CreateUi.fit(font, text, textWidth), x + CreateUi.CARD_TEXT_X, y + 5, CreateUi.TEXT_DARK, false);
        }
        CreateUi.arrowDown(graphics, x + width - 10, y + 8);
    }

    /** Short text on the tag; the full label is in its tooltip and the panel. */
    private static String tagText(ClientSelection.Option option) {
        if (option.isStandard()) {
            return Component.translatable(KEY + "button_standard").getString();
        }
        if (option.regionIndex() < 0) {
            return option.profile().name();
        }
        return option.profile().regions().get(option.regionIndex()).name();
    }

    private static List<Component> tooltip(ClientSelection.Option option) {
        List<Component> lines = new ArrayList<>();
        lines.add(CreateUi.header(Component.translatable(KEY + "title")));
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
        if (ClientProfileCache.all().isEmpty()) {
            lines.add(CreateUi.hint(Component.translatable(KEY + "no_profiles")));
        }
        lines.add(CreateUi.controls(Component.translatable(KEY + "controls")));
        lines.add(CreateUi.controls(Component.translatable(KEY + "scroll")));
        return lines;
    }

    private static Component regionLine(CrafterRegion region) {
        String address = region.address().isEmpty() ? "" : "  → " + region.address();
        return Component.literal(" " + region.name() + address).withStyle(ChatFormatting.GRAY);
    }

    // ---- panel ----

    private static Panel createPanel(Screen screen, Font font, Rect2i anchor, List<ClientSelection.Option> options) {
        Component title = Component.translatable(KEY + "panel_title");
        int contentWidth = Math.max(anchor.getWidth(), font.width(title) + 16) - 2 * PANEL_PADDING;
        for (ClientSelection.Option option : options) {
            contentWidth = Math.max(contentWidth, cardIndent(option) + cardWidth(option, font));
        }
        contentWidth = Math.max(contentWidth, CreateUi.CARD_TEXT_X + font.width(Component.translatable(KEY + "manage")) + 8);
        int width = Math.min(contentWidth + 2 * PANEL_PADDING, screen.width - 2 * MARGIN);
        int height = CreateUi.PANEL_HEADER + 3 + (options.size() + 1) * CreateUi.PANEL_ROW + MANAGE_GAP + CreateUi.PANEL_BOTTOM;

        int x = Math.max(MARGIN, Math.min(anchor.getX(), screen.width - MARGIN - width));
        int y = anchor.getY() + anchor.getHeight() + 2;
        if (y + height > screen.height - MARGIN) {
            y = Math.max(MARGIN, anchor.getY() - 2 - height);
        }

        List<Row> rows = new ArrayList<>();
        int cardY = y + CreateUi.PANEL_HEADER + 3;
        for (ClientSelection.Option option : options) {
            int indent = cardIndent(option);
            rows.add(new Row(option, x + PANEL_PADDING + indent, cardY, width - 2 * PANEL_PADDING - indent));
            cardY += CreateUi.PANEL_ROW;
        }
        rows.add(new Row(null, x + PANEL_PADDING, cardY + MANAGE_GAP, width - 2 * PANEL_PADDING));
        return new Panel(new Rect2i(x, y, width, height), rows);
    }

    private static void renderPanel(GuiGraphics graphics, Font font, Panel panel, ClientSelection.Option current, int mouseX, int mouseY) {
        Rect2i area = panel.area();
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, PANEL_Z);
        int bodyHeight = area.getHeight() - CreateUi.PANEL_HEADER - CreateUi.PANEL_BOTTOM;
        CreateUi.panelHeader(graphics, font, area.getX(), area.getY(), area.getWidth(), Component.translatable(KEY + "panel_title"));
        CreateUi.panelBody(graphics, area.getX(), area.getY() + CreateUi.PANEL_HEADER, area.getWidth(), bodyHeight);
        CreateUi.panelBottom(graphics, area.getX(), area.getY() + area.getHeight() - CreateUi.PANEL_BOTTOM, area.getWidth());

        Row parent = null;
        for (Row row : panel.rows()) {
            ClientSelection.Option option = row.option();
            boolean hovered = row.contains(mouseX, mouseY);
            if (option != null && option.regionIndex() >= 0 && parent != null) {
                CreateUi.thread(graphics, parent.x(), parent.y(), row.x(), row.y());
            } else {
                parent = row;
            }
            boolean selected = option != null && option.equals(current);
            CreateUi.card(graphics, row.x(), row.y(), row.width(), false, selected, hovered);
            int color = selected || option == null ? CreateUi.TEXT_DARK : CreateUi.TEXT_CARD;
            if (option == null) {
                graphics.renderItem(WRENCH, row.x() + CreateUi.CARD_ICON_X, row.y() + 1);
                graphics.drawString(font, Component.translatable(KEY + "manage"), row.x() + CreateUi.CARD_TEXT_X, row.y() + 5, color, false);
                continue;
            }
            if (option.regionIndex() < 0) {
                graphics.renderItem(option.isStandard() ? CRAFTING_TABLE : CRAFTER, row.x() + CreateUi.CARD_ICON_X, row.y() + 1);
            }
            int textX = textX(option);
            String text = CreateUi.fit(font, cardText(option), row.width() - textX - 6);
            graphics.drawString(font, text, row.x() + textX, row.y() + 5, color, false);
            CrafterRegion region = option.regionIndex() >= 0 ? option.profile().regions().get(option.regionIndex()) : null;
            if (region != null && !region.address().isEmpty()) {
                int addressX = row.x() + textX + font.width(text) + 6;
                String address = CreateUi.fit(font, "→ " + region.address(), row.x() + row.width() - 6 - addressX);
                graphics.drawString(font, address, addressX, row.y() + 5, CreateUi.TEXT_MUTED, false);
            }
        }
        graphics.pose().popPose();
    }

    private static int cardIndent(ClientSelection.Option option) {
        return option.regionIndex() >= 0 ? CreateUi.CHILD_INDENT : 0;
    }

    /** Region cards have no icon; their text starts right after the notch. */
    private static int textX(ClientSelection.Option option) {
        return option.regionIndex() >= 0 ? 14 : CreateUi.CARD_TEXT_X;
    }

    /** Card text: the full label, or for a region its name (its address is drawn after it). */
    private static String cardText(ClientSelection.Option option) {
        if (option.regionIndex() >= 0) {
            return option.profile().regions().get(option.regionIndex()).name();
        }
        return option.label().getString();
    }

    /** Width a card needs to show its text (and a region's address) in full. */
    private static int cardWidth(ClientSelection.Option option, Font font) {
        int width = textX(option) + font.width(cardText(option)) + 8;
        if (option.regionIndex() >= 0 && !option.profile().regions().get(option.regionIndex()).address().isEmpty()) {
            width += 6 + font.width("→ " + option.profile().regions().get(option.regionIndex()).address());
        }
        return width;
    }

    private static boolean contains(Rect2i area, double mouseX, double mouseY) {
        return mouseX >= area.getX() && mouseX < area.getX() + area.getWidth() && mouseY >= area.getY() && mouseY < area.getY() + area.getHeight();
    }
}
