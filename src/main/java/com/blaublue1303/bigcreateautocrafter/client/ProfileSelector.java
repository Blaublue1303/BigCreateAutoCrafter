package com.blaublue1303.bigcreateautocrafter.client;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.blaublue1303.bigcreateautocrafter.BigCreateAutoCrafter;
import com.blaublue1303.bigcreateautocrafter.profile.CrafterRegion;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestScreen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * Button next to the Stock Keeper window that chooses the crafter setup for the JEI "+".
 * Drawn and clicked through screen events, so Create's own widget handling stays untouched.
 */
public final class ProfileSelector {
    private static final String KEY = BigCreateAutoCrafter.MODID + ".selector.";
    private static final int WIDTH = 120;
    private static final int HEIGHT = 16;

    @Nullable
    private static Button button;

    private ProfileSelector() {
    }

    /** Area for JEI to keep free. */
    public static List<Rect2i> extraAreas() {
        Button current = button;
        if (current == null || !(Minecraft.getInstance().screen instanceof StockKeeperRequestScreen)) {
            return List.of();
        }
        return List.of(new Rect2i(current.getX(), current.getY(), current.getWidth(), current.getHeight()));
    }

    public static void onInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof StockKeeperRequestScreen screen)) {
            return;
        }
        int x = screen.getGuiLeft() + screen.getXSize() + 4;
        if (x + WIDTH > screen.width - 2) {
            x = Math.max(2, screen.getGuiLeft() - WIDTH - 4);
        }
        button = Button.builder(Component.empty(), b -> {}).bounds(x, screen.getGuiTop() + 4, WIDTH, HEIGHT).build();
    }

    public static void onRender(ScreenEvent.Render.Post event) {
        Button current = button;
        if (current == null || !(event.getScreen() instanceof StockKeeperRequestScreen)) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        ClientSelection.Option option = ClientSelection.current(mc.level.dimension());
        String label = mc.font.plainSubstrByWidth(option.label().getString(), WIDTH - 8);
        current.setMessage(Component.literal(label));
        current.render(event.getGuiGraphics(), event.getMouseX(), event.getMouseY(), event.getPartialTick());
        if (current.isHovered()) {
            event.getGuiGraphics().renderComponentTooltip(mc.font, tooltip(option), event.getMouseX(), event.getMouseY());
        }
    }

    public static void onMouseClicked(ScreenEvent.MouseButtonPressed.Pre event) {
        Button current = button;
        if (current == null || !(event.getScreen() instanceof StockKeeperRequestScreen) || !current.isMouseOver(event.getMouseX(), event.getMouseY())) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && (event.getButton() == 0 || event.getButton() == 1)) {
            ClientSelection.cycle(mc.level.dimension(), event.getButton() == 0 ? 1 : -1);
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
        event.setCanceled(true);
    }

    private static List<Component> tooltip(ClientSelection.Option option) {
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
        if (ClientProfileCache.all().isEmpty()) {
            lines.add(Component.translatable(KEY + "no_profiles").withStyle(ChatFormatting.DARK_GRAY));
        }
        return lines;
    }

    private static Component regionLine(CrafterRegion region) {
        String address = region.address().isEmpty() ? "—" : region.address();
        return Component.literal(" • " + region.name() + "  → " + address).withStyle(ChatFormatting.GRAY);
    }
}
