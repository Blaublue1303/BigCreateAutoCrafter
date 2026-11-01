package com.blaublue1303.bigcreateautocrafter.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import com.blaublue1303.bigcreateautocrafter.BigCreateAutoCrafter;
import com.blaublue1303.bigcreateautocrafter.net.EditProfilePayload;
import com.blaublue1303.bigcreateautocrafter.net.EditProfilePayload.Action;
import com.blaublue1303.bigcreateautocrafter.profile.CrafterProfile;
import com.blaublue1303.bigcreateautocrafter.profile.CrafterRegion;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.trains.station.NoShadowFontWrapper;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.widget.IconButton;

import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.gui.AbstractSimiScreen;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * "Crafter profiles" menu in the look of Create's Stock Keeper category editor: one paper card per profile with its
 * regions tied below it. Click a name or an address to edit it (Enter or a click elsewhere saves, Esc cancels); the "×"
 * deletes a profile or removes a region after a second click. Edits go to the server, which answers with the updated
 * profile list; the list is rebuilt from it unless a text is being edited.
 */
public class ProfileManagerScreen extends AbstractSimiScreen {
    private static final String KEY = BigCreateAutoCrafter.MODID + ".manager.";
    private static final long CONFIRM_MILLIS = 3000;
    private static final int MIN_ROWS = 3;
    private static final int MAX_ROWS = 8;
    /** Card positions inside the 192 px window, as in Create's category editor. */
    private static final int CARD_X = 7;
    private static final int CARD_WIDTH = 171;
    private static final int FIRST_CARD_Y = 7;
    private static final int TEXT_COLOR_FOOTER = 0x5A5A5A;
    private static final ItemStack CRAFTER = AllBlocks.MECHANICAL_CRAFTER.asStack();

    @Nullable
    private final Screen parent;
    private final LerpedFloat scroll = LerpedFloat.linear().startWithValue(0);
    private final List<Entry> entries = new ArrayList<>();
    @Nullable
    private List<CrafterProfile> shownProfiles;
    private int rows;

    /** Text being edited: which entry and field, and the box drawn over it. */
    @Nullable
    private Entry editing;
    @Nullable
    private EditBox editBox;
    /** Entry whose "×" was clicked once; a second click within {@link #CONFIRM_MILLIS} acts. */
    @Nullable
    private Entry armed;
    private long armedUntil;

    /** A profile card (regionIndex -1) or one of its region cards. */
    private record Entry(CrafterProfile profile, int regionIndex) {
        boolean isRegion() {
            return regionIndex >= 0;
        }

        @Nullable
        CrafterRegion region() {
            return isRegion() ? profile.regions().get(regionIndex) : null;
        }

        int indent() {
            return isRegion() ? CreateUi.CHILD_INDENT : 0;
        }

        /** Same card after a sync (profiles are records, so compare by id). */
        boolean sameAs(@Nullable Entry other) {
            return other != null && other.profile.id().equals(profile.id()) && other.regionIndex == regionIndex;
        }
    }

    private enum Part { NAME, ADDRESS, CROSS, NONE }

    public ProfileManagerScreen(@Nullable Screen parent) {
        super(Component.translatable(KEY + "title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        rebuildEntries();
        rows = rowsFor(entries.size());
        setWindowSize(AllGuiTextures.STOCK_KEEPER_CATEGORY.getWidth(), chromeHeight() + rows * AllGuiTextures.STOCK_KEEPER_CATEGORY.getHeight());
        // leave room on the right for the crafter that "says" the menu, like Create's editor shows the Stock Ticker
        setWindowOffset(-20, 0);
        super.init();
        clearWidgets();
        stopEditing(false);
        IconButton done = new IconButton(guiLeft + windowWidth - 25, guiTop + windowHeight - 25, AllIcons.I_CONFIRM);
        done.withCallback(this::onClose);
        done.setToolTip(Component.translatable(KEY + "done"));
        addRenderableWidget(done);
    }

    @Override
    public void tick() {
        super.tick();
        scroll.tickChaser();
        if (ClientProfileCache.all() != shownProfiles && editing == null) {
            rebuildEntries();
            if (rowsFor(entries.size()) != rows) {
                rebuildWidgets();
            }
        }
        if (armed != null && Util.getMillis() > armedUntil) {
            armed = null;
        }
    }

    private void rebuildEntries() {
        shownProfiles = ClientProfileCache.all();
        entries.clear();
        for (CrafterProfile profile : shownProfiles) {
            if (minecraft.level != null && !profile.dimension().equals(minecraft.level.dimension())) {
                continue;
            }
            entries.add(new Entry(profile, -1));
            for (int i = 0; i < profile.regions().size(); i++) {
                entries.add(new Entry(profile, i));
            }
        }
        if (armed != null && entries.stream().noneMatch(armed::sameAs)) {
            armed = null;
        }
        scroll.chase(Mth.clamp(scroll.getChaseTarget(), 0, maxScroll()), 0.7, LerpedFloat.Chaser.EXP);
    }

    private static int chromeHeight() {
        return AllGuiTextures.STOCK_KEEPER_CATEGORY_HEADER.getHeight() + AllGuiTextures.STOCK_KEEPER_CATEGORY_FOOTER.getHeight();
    }

    /** Wood rows for {@code cards} cards: as many as needed (the empty text needs three), as many as fit on screen. */
    private int rowsFor(int cards) {
        int row = AllGuiTextures.STOCK_KEEPER_CATEGORY.getHeight();
        int needed = Mth.positiveCeilDiv(FIRST_CARD_Y + Math.max(cards, 2) * CreateUi.PANEL_ROW + 6, row);
        int fitting = Mth.clamp((height - chromeHeight() - 20) / row, MIN_ROWS, MAX_ROWS);
        return Mth.clamp(needed, MIN_ROWS, fitting);
    }

    // ---- layout ----

    private int listTop() {
        return guiTop + AllGuiTextures.STOCK_KEEPER_CATEGORY_HEADER.getHeight();
    }

    private int listBottom() {
        return listTop() + rows * AllGuiTextures.STOCK_KEEPER_CATEGORY.getHeight();
    }

    private int maxScroll() {
        int content = FIRST_CARD_Y + entries.size() * CreateUi.PANEL_ROW;
        return Math.max(0, content - (listBottom() - listTop()));
    }

    private int cardX(Entry entry) {
        return guiLeft + CARD_X + entry.indent();
    }

    private int cardWidth(Entry entry) {
        return CARD_WIDTH - entry.indent();
    }

    private int cardY(int index, float partialTicks) {
        return listTop() + FIRST_CARD_Y + index * CreateUi.PANEL_ROW - Math.round(scroll.getValue(partialTicks));
    }

    /** Left edge of a region's address field. */
    private int addressX(Entry entry) {
        return cardX(entry) + 14 + font.width(Objects.requireNonNull(entry.region()).name()) + 7;
    }

    /** Index of the card under the mouse, or -1. */
    private int cardAt(double mouseX, double mouseY) {
        if (mouseY < listTop() || mouseY >= listBottom()) {
            return -1;
        }
        for (int i = 0; i < entries.size(); i++) {
            Entry entry = entries.get(i);
            int y = cardY(i, 0);
            if (mouseX >= cardX(entry) && mouseX < cardX(entry) + cardWidth(entry) && mouseY >= y && mouseY < y + CreateUi.CARD_HEIGHT) {
                return i;
            }
        }
        return -1;
    }

    private Part partAt(int index, double mouseX, double mouseY) {
        Entry entry = entries.get(index);
        int x = cardX(entry);
        if (CreateUi.overCross(x, cardY(index, 0), cardWidth(entry), mouseX, mouseY)) {
            return Part.CROSS;
        }
        if (!entry.isRegion()) {
            return mouseX >= x + CreateUi.CARD_TEXT_X - 2 ? Part.NAME : Part.NONE;
        }
        return mouseX >= addressX(entry) - 2 ? Part.ADDRESS : Part.NONE;
    }

    // ---- rendering ----

    @Override
    protected void renderWindow(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        int y = guiTop;
        AllGuiTextures.STOCK_KEEPER_CATEGORY_HEADER.render(graphics, guiLeft, y);
        y += AllGuiTextures.STOCK_KEEPER_CATEGORY_HEADER.getHeight();
        for (int i = 0; i < rows; i++) {
            AllGuiTextures.STOCK_KEEPER_CATEGORY.render(graphics, guiLeft, y);
            y += AllGuiTextures.STOCK_KEEPER_CATEGORY.getHeight();
        }
        AllGuiTextures.STOCK_KEEPER_CATEGORY_FOOTER.render(graphics, guiLeft, y);
        AllGuiTextures.STOCK_KEEPER_CATEGORY_SAYS.render(graphics, guiLeft + windowWidth - 6, y + 7);
        graphics.drawString(font, title, guiLeft + (windowWidth - font.width(title)) / 2, guiTop + 4, CreateUi.TEXT_HEADER, false);
        renderFooterHint(graphics, y);

        graphics.enableScissor(guiLeft + 3, listTop() - 2, guiLeft + windowWidth - 3, listBottom() + 1);
        if (entries.isEmpty()) {
            renderEmpty(graphics);
        }
        Entry parentCard = null;
        int parentY = 0;
        for (int i = 0; i < entries.size(); i++) {
            Entry entry = entries.get(i);
            int cardY = cardY(i, partialTicks);
            if (!entry.isRegion()) {
                parentCard = entry;
                parentY = cardY;
            } else if (parentCard != null) {
                CreateUi.thread(graphics, cardX(parentCard), parentY, cardX(entry), cardY);
            }
            if (cardY > listBottom() || cardY + CreateUi.CARD_HEIGHT < listTop() - 2) {
                continue;
            }
            renderCard(graphics, entry, cardY);
        }
        graphics.disableScissor();
    }

    private void renderCard(GuiGraphics graphics, Entry entry, int y) {
        int x = cardX(entry);
        int width = cardWidth(entry);
        CreateUi.card(graphics, x, y, width, true);
        if (entry.sameAs(armed)) {
            CreateUi.armedCross(graphics, x, y, width);
        }
        boolean editingThis = entry.sameAs(editing);
        if (!entry.isRegion()) {
            graphics.renderItem(CRAFTER, x + CreateUi.CARD_ICON_X, y + 1);
            if (!editingThis) {
                String name = CreateUi.fit(font, entry.profile().name(), width - CreateUi.CARD_TEXT_X - CreateUi.CARD_CROSS_WIDTH - 2);
                graphics.drawString(font, name, x + CreateUi.CARD_TEXT_X, y + 5, CreateUi.TEXT_CARD, false);
            }
            return;
        }
        CrafterRegion region = Objects.requireNonNull(entry.region());
        graphics.drawString(font, region.name(), x + 14, y + 5, CreateUi.TEXT_CARD, false);
        // the address sits in a slightly darker paper inset, like a field to write in
        int fieldX = addressX(entry);
        int fieldEnd = x + width - CreateUi.CARD_CROSS_WIDTH + 3;
        graphics.fill(fieldX - 3, y + 3, fieldEnd, y + CreateUi.CARD_HEIGHT - 3, 0xFFEDD8BB);
        if (!editingThis) {
            boolean empty = region.address().isEmpty();
            String text = empty ? Component.translatable(KEY + "address_hint").getString() : region.address();
            graphics.drawString(font, CreateUi.fit(font, text, fieldEnd - fieldX - 2), fieldX, y + 5,
                    empty ? CreateUi.TEXT_MUTED : CreateUi.TEXT_DARK, false);
        }
    }

    private void renderEmpty(GuiGraphics graphics) {
        int y = listTop() + FIRST_CARD_Y + 4;
        int center = guiLeft + windowWidth / 2;
        Component empty = Component.translatable(KEY + "empty");
        CreateUi.lightText(graphics, font, empty, center - font.width(empty) / 2, y);
        y += 16;
        for (FormattedCharSequence line : font.split(Component.translatable(KEY + "empty_hint"), windowWidth - 30)) {
            graphics.drawString(font, line, center - font.width(line) / 2 + 1, y + 1, CreateUi.TEXT_LIGHT_SHADOW, false);
            graphics.drawString(font, line, center - font.width(line) / 2, y, CreateUi.TEXT_LIGHT, false);
            y += 10;
        }
    }

    /** How to add regions, on the grey footer left of the confirm button. */
    private void renderFooterHint(GuiGraphics graphics, int footerY) {
        List<FormattedCharSequence> lines = font.split(Component.translatable(KEY + "footer_hint"), windowWidth - 40);
        int y = footerY + 5 + (lines.size() == 1 ? 7 : 2);
        for (FormattedCharSequence line : lines.subList(0, Math.min(2, lines.size()))) {
            graphics.drawString(font, line, guiLeft + 8, y, TEXT_COLOR_FOOTER, false);
            y += 10;
        }
    }

    @Override
    protected void renderWindowForeground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        GuiGameElement.of(CRAFTER).<GuiGameElement.GuiRenderBuilder>at(guiLeft + windowWidth + 12, guiTop + windowHeight - 39, -190)
                .scale(3)
                .render(graphics);
        if (editBox != null) {
            return;
        }
        for (Renderable renderable : getRenderables()) {
            if (renderable instanceof IconButton button && button.isHovered() && !button.getToolTip().isEmpty()) {
                graphics.renderComponentTooltip(font, button.getToolTip(), mouseX, mouseY);
                return;
            }
        }
        int index = cardAt(mouseX, mouseY);
        if (index >= 0) {
            List<Component> lines = tooltip(entries.get(index), partAt(index, mouseX, mouseY));
            if (!lines.isEmpty()) {
                graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
            }
        }
    }

    private List<Component> tooltip(Entry entry, Part part) {
        List<Component> lines = new ArrayList<>();
        CrafterRegion region = entry.region();
        switch (part) {
            case NAME -> {
                lines.add(CreateUi.header(Component.translatable(KEY + "name")));
                lines.add(Component.literal(entry.profile().name()).withStyle(ChatFormatting.WHITE));
                lines.add(CreateUi.controls(Component.translatable(KEY + "click_to_edit")));
            }
            case ADDRESS -> {
                lines.add(CreateUi.header(Component.translatable(KEY + "address")));
                if (!Objects.requireNonNull(region).address().isEmpty()) {
                    lines.add(Component.literal(region.address()).withStyle(ChatFormatting.WHITE));
                }
                lines.add(Component.translatable(KEY + "address_info").withStyle(ChatFormatting.GRAY));
                lines.add(Component.translatable(KEY + "address_info_2").withStyle(ChatFormatting.GRAY));
                lines.add(CreateUi.controls(Component.translatable(KEY + "click_to_edit")));
            }
            case CROSS -> {
                lines.add(CreateUi.header(Component.translatable(KEY + (entry.isRegion() ? "remove" : "delete"))));
                if (entry.sameAs(armed)) {
                    lines.add(Component.translatable(KEY + "confirm").withStyle(ChatFormatting.RED));
                } else {
                    lines.add(CreateUi.controls(Component.translatable(KEY + "click_twice")));
                }
            }
            case NONE -> {
                if (region != null) {
                    lines.add(CreateUi.header(Component.translatable(KEY + "region", region.name())));
                    lines.add(Component.translatable(KEY + "region_crafters", region.crafterCount()).withStyle(ChatFormatting.GRAY));
                }
            }
        }
        return lines;
    }

    // ---- input ----

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (editBox != null) {
            if (editBox.isMouseOver(mouseX, mouseY)) {
                return editBox.mouseClicked(mouseX, mouseY, button);
            }
            stopEditing(true);
        }
        int index = button == GLFW.GLFW_MOUSE_BUTTON_LEFT ? cardAt(mouseX, mouseY) : -1;
        if (index >= 0) {
            Entry entry = entries.get(index);
            switch (partAt(index, mouseX, mouseY)) {
                case CROSS -> {
                    CreateUi.click();
                    if (entry.sameAs(armed) && Util.getMillis() <= armedUntil) {
                        armed = null;
                        if (entry.isRegion()) {
                            send(entry.profile(), Action.REMOVE_REGION, entry.regionIndex(), "");
                        } else {
                            send(entry.profile(), Action.DELETE, -1, "");
                        }
                    } else {
                        armed = entry;
                        armedUntil = Util.getMillis() + CONFIRM_MILLIS;
                    }
                    return true;
                }
                case NAME, ADDRESS -> {
                    CreateUi.click();
                    startEditing(index);
                    return true;
                }
                case NONE -> {
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (editBox != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                stopEditing(false);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                stopEditing(true);
                return true;
            }
            return editBox.keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (editBox == null) {
            float target = Mth.clamp(scroll.getChaseTarget() - (float) scrollY * 12, 0, maxScroll());
            scroll.chase(target, 0.7, LerpedFloat.Chaser.EXP);
        }
        return true;
    }

    private void startEditing(int index) {
        stopEditing(true);
        Entry entry = entries.get(index);
        int y = cardY(index, 0) + 5;
        boolean region = entry.isRegion();
        int x = region ? addressX(entry) : cardX(entry) + CreateUi.CARD_TEXT_X;
        int width = cardX(entry) + cardWidth(entry) - CreateUi.CARD_CROSS_WIDTH + 2 - x;
        EditBox box = new EditBox(new NoShadowFontWrapper(font), x, y, width, 10,
                Component.translatable(KEY + (region ? "address" : "name")));
        box.setBordered(false);
        box.setTextColor(region ? CreateUi.TEXT_DARK : CreateUi.TEXT_CARD);
        box.setMaxLength(region ? CrafterRegion.MAX_ADDRESS_LENGTH : CrafterProfile.MAX_NAME_LENGTH);
        box.setValue(region ? Objects.requireNonNull(entry.region()).address() : entry.profile().name());
        box.moveCursorToEnd(false);
        box.setHighlightPos(0);
        box.setFocused(true);
        editing = entry;
        editBox = box;
        armed = null;
        addRenderableWidget(box);
        setFocused(box);
    }

    /** Ends editing; with {@code save} the changed text goes to the server. */
    private void stopEditing(boolean save) {
        Entry entry = editing;
        EditBox box = editBox;
        editing = null;
        editBox = null;
        if (box == null || entry == null) {
            return;
        }
        removeWidget(box);
        setFocused(null);
        if (!save) {
            return;
        }
        String value = box.getValue().trim();
        if (entry.isRegion()) {
            if (!value.equals(Objects.requireNonNull(entry.region()).address())) {
                send(entry.profile(), Action.SET_ADDRESS, entry.regionIndex(), value);
            }
        } else if (!value.isEmpty() && !value.equals(entry.profile().name())) {
            send(entry.profile(), Action.RENAME, -1, value);
        }
    }

    @Override
    public void onClose() {
        stopEditing(true);
        minecraft.setScreen(parent);
    }

    @Override
    public void removed() {
        stopEditing(true);
        super.removed();
    }

    private static void send(CrafterProfile profile, Action action, int regionIndex, String text) {
        PacketDistributor.sendToServer(new EditProfilePayload(profile.id(), action, regionIndex, text));
    }

    // ---- client smoke test ----

    /** Number of cards shown (profiles and regions). */
    public int rowCount() {
        return entries.size();
    }

    /** Edits the name (profile card) or address (region card) of card {@code index} like clicking it, typing and pressing Enter. */
    public void editCardForTest(int index, String text) {
        startEditing(index);
        Objects.requireNonNull(editBox).setValue(text);
        keyPressed(GLFW.GLFW_KEY_ENTER, 0, 0);
    }
}
