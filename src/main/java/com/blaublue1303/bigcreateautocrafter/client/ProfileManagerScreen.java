package com.blaublue1303.bigcreateautocrafter.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import com.blaublue1303.bigcreateautocrafter.BigCreateAutoCrafter;
import com.blaublue1303.bigcreateautocrafter.net.EditProfilePayload;
import com.blaublue1303.bigcreateautocrafter.net.EditProfilePayload.Action;
import com.blaublue1303.bigcreateautocrafter.profile.CrafterProfile;
import com.blaublue1303.bigcreateautocrafter.profile.CrafterRegion;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * "Crafter profiles" menu: rename and delete profiles, set region addresses, remove regions. Edits are sent to the server,
 * which answers with the updated profile list; the list is rebuilt from it unless a text field is being edited.
 */
public class ProfileManagerScreen extends Screen {
    private static final String KEY = BigCreateAutoCrafter.MODID + ".manager.";
    private static final int ROW_HEIGHT = 24;
    private static final long CONFIRM_MILLIS = 3000;

    @Nullable
    private final Screen parent;
    private ProfileList list;
    @Nullable
    private List<CrafterProfile> shownProfiles;

    public ProfileManagerScreen(@Nullable Screen parent) {
        super(Component.translatable(KEY + "title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        list = addRenderableWidget(new ProfileList(minecraft, width, height - 72, 32));
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose()).bounds(width / 2 - 100, height - 30, 200, 20).build());
        rebuild();
    }

    @Override
    public void tick() {
        if (ClientProfileCache.all() != shownProfiles && !list.isEditing()) {
            rebuild();
        }
    }

    private void rebuild() {
        shownProfiles = ClientProfileCache.all();
        double scroll = list.getScrollAmount();
        List<Row> rows = new ArrayList<>();
        Minecraft mc = Minecraft.getInstance();
        for (CrafterProfile profile : shownProfiles) {
            if (mc.level != null && !profile.dimension().equals(mc.level.dimension())) {
                continue;
            }
            rows.add(new ProfileRow(profile));
            for (int i = 0; i < profile.regions().size(); i++) {
                rows.add(new RegionRow(profile, i));
            }
        }
        if (rows.isEmpty()) {
            rows.add(new TextRow(Component.translatable(KEY + "empty").withStyle(ChatFormatting.GRAY)));
            rows.add(new TextRow(Component.translatable(KEY + "empty_hint").withStyle(ChatFormatting.DARK_GRAY)));
        }
        list.setRows(rows);
        list.setScrollAmount(scroll);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 12, 0xFFFFFF);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    /** Number of rows shown (used by the client smoke test). */
    public int rowCount() {
        return list.children().size();
    }

    /** Renames the first profile through the same path as the name field (used by the client smoke test). */
    public void renameFirstProfileForTest(String name) {
        list.children().stream().filter(ProfileRow.class::isInstance).map(ProfileRow.class::cast).findFirst()
                .ifPresent(row -> row.nameBox.commit(name));
    }

    private static void send(CrafterProfile profile, Action action, int regionIndex, String text) {
        PacketDistributor.sendToServer(new EditProfilePayload(profile.id(), action, regionIndex, text));
    }

    // ---- list ----

    private static final class ProfileList extends ContainerObjectSelectionList<Row> {
        ProfileList(Minecraft minecraft, int width, int height, int y) {
            super(minecraft, width, height, y, ROW_HEIGHT);
        }

        void setRows(List<Row> rows) {
            replaceEntries(rows);
        }

        boolean isEditing() {
            return children().stream().anyMatch(Row::isEditing);
        }

        @Override
        public int getRowWidth() {
            return 340;
        }

        @Override
        protected int getScrollbarPosition() {
            return getRowLeft() + getRowWidth() + 6;
        }
    }

    private abstract static class Row extends ContainerObjectSelectionList.Entry<Row> {
        boolean isEditing() {
            return false;
        }
    }

    private static final class TextRow extends Row {
        private final Component text;

        TextRow(Component text) {
            this.text = text;
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY,
                           boolean hovering, float partialTick) {
            Font font = Minecraft.getInstance().font;
            graphics.drawCenteredString(font, text, left + width / 2, top + (height - font.lineHeight) / 2, 0xFFFFFF);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of();
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of();
        }
    }

    /** Profile name (edit to rename) and a delete button. */
    private static final class ProfileRow extends Row {
        private final CommitBox nameBox;
        private final ConfirmButton deleteButton;

        ProfileRow(CrafterProfile profile) {
            Font font = Minecraft.getInstance().font;
            nameBox = new CommitBox(font, 200, Component.translatable(KEY + "name"), CrafterProfile.MAX_NAME_LENGTH, profile.name(), name -> {
                if (!name.isBlank()) {
                    send(profile, Action.RENAME, -1, name);
                }
            });
            deleteButton = new ConfirmButton(Component.translatable(KEY + "delete"), () -> send(profile, Action.DELETE, -1, ""));
        }

        @Override
        boolean isEditing() {
            return nameBox.isFocused();
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY,
                           boolean hovering, float partialTick) {
            nameBox.setPosition(left, top + 2);
            nameBox.render(graphics, mouseX, mouseY, partialTick);
            deleteButton.setPosition(left + width - deleteButton.getWidth(), top + 1);
            deleteButton.render(graphics, mouseX, mouseY, partialTick);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of(nameBox, deleteButton);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of(nameBox, deleteButton);
        }
    }

    /** Region size, its package address (edit to change) and a remove button. */
    private static final class RegionRow extends Row {
        private final Component label;
        private final CommitBox addressBox;
        private final ConfirmButton removeButton;

        RegionRow(CrafterProfile profile, int regionIndex) {
            Font font = Minecraft.getInstance().font;
            CrafterRegion region = profile.regions().get(regionIndex);
            label = Component.literal("↳ " + region.name()).withStyle(ChatFormatting.GRAY);
            addressBox = new CommitBox(font, 150, Component.translatable(KEY + "address"), CrafterRegion.MAX_ADDRESS_LENGTH, region.address(),
                    address -> send(profile, Action.SET_ADDRESS, regionIndex, address));
            addressBox.setHint(Component.translatable(KEY + "address_hint").withStyle(ChatFormatting.DARK_GRAY));
            removeButton = new ConfirmButton(Component.translatable(KEY + "remove"), () -> send(profile, Action.REMOVE_REGION, regionIndex, ""));
        }

        @Override
        boolean isEditing() {
            return addressBox.isFocused();
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY,
                           boolean hovering, float partialTick) {
            Font font = Minecraft.getInstance().font;
            graphics.drawString(font, label, left + 12, top + (height - font.lineHeight) / 2, 0xFFFFFF);
            removeButton.setPosition(left + width - removeButton.getWidth(), top + 1);
            addressBox.setPosition(removeButton.getX() - 6 - addressBox.getWidth(), top + 2);
            addressBox.render(graphics, mouseX, mouseY, partialTick);
            removeButton.render(graphics, mouseX, mouseY, partialTick);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of(addressBox, removeButton);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of(addressBox, removeButton);
        }
    }

    // ---- widgets ----

    /** Text field that reports its value when Enter is pressed or it loses focus, if the value changed. */
    private static final class CommitBox extends EditBox {
        private final Consumer<String> onCommit;
        private String committed;

        CommitBox(Font font, int width, Component name, int maxLength, String value, Consumer<String> onCommit) {
            super(font, 0, 0, width, 18, name);
            setMaxLength(maxLength);
            setValue(value);
            this.committed = value;
            this.onCommit = onCommit;
        }

        void commit(String value) {
            setValue(value);
            commit();
        }

        private void commit() {
            String value = getValue().trim();
            if (!value.equals(committed)) {
                committed = value;
                onCommit.accept(value);
            }
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                commit();
                setFocused(false);
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        @Override
        public void setFocused(boolean focused) {
            if (!focused && isFocused()) {
                commit();
            }
            super.setFocused(focused);
        }
    }

    /** Button that needs a second click within a few seconds ("Sure?") before it acts. */
    private static final class ConfirmButton extends Button {
        private final Component label;
        private final Runnable action;
        private long armedUntil;

        ConfirmButton(Component label, Runnable action) {
            super(0, 0, 80, 20, label, b -> {}, DEFAULT_NARRATION);
            this.label = label;
            this.action = action;
        }

        @Override
        public void onPress() {
            if (Util.getMillis() < armedUntil) {
                armedUntil = 0;
                action.run();
            } else {
                armedUntil = Util.getMillis() + CONFIRM_MILLIS;
            }
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            boolean armed = Util.getMillis() < armedUntil;
            setMessage(armed ? Component.translatable(KEY + "confirm").withStyle(ChatFormatting.RED) : label);
            super.renderWidget(graphics, mouseX, mouseY, partialTick);
        }
    }
}
