package com.blaublue1303.bigcreateautocrafter.client.smoketest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCursorPosCallback;

import com.blaublue1303.bigcreateautocrafter.BigCreateAutoCrafter;
import com.blaublue1303.bigcreateautocrafter.client.ClientProfileCache;
import com.blaublue1303.bigcreateautocrafter.client.ClientSelection;
import com.blaublue1303.bigcreateautocrafter.client.ProfileManagerScreen;
import com.blaublue1303.bigcreateautocrafter.client.ProfileSelector;
import com.blaublue1303.bigcreateautocrafter.compat.jei.JeiSmokeCheck;
import com.blaublue1303.bigcreateautocrafter.profile.ProfileService;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.crafter.ConnectedInputHandler;
import com.simibubi.create.content.logistics.AddressEditBox;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock.PanelSlot;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlockEntity;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelConnection;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelScreen;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestMenu;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestScreen;
import com.simibubi.create.content.logistics.stockTicker.StockTickerInteractionHandler;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.foundation.gui.widget.IconButton;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * Automated client check (dev only, run config {@code clientSmokeTest}, enabled by the system property
 * {@value #PROPERTY}): creates a flat world, builds a 21-crafter group and a 3×3 field, creates a profile through the
 * same server code as Ctrl + right-click, opens a real Stock Keeper screen, checks the JEI handlers, the selector and the
 * profile menu, then configures a real Factory Gauge for the crushing wheel through its screen and checks what the
 * server saved. Screenshots go to {@code screenshots/}, results to the log and to {@code bcac-smoketest.txt} in the game
 * directory.
 */
public final class ClientSmokeTest {
    public static final String PROPERTY = "bigcreateautocrafter.clientSmokeTest";
    private static final int TIMEOUT_TICKS = 20 * 60;

    private enum Phase { WAIT_TITLE, LOADING_WORLD, WORLD, OPEN_SCREEN, WAIT_SCREEN, SETTLE, TAG_TOOLTIP, DROPDOWN, MANAGER, RENAMED,
        GAUGE_SETUP, GAUGE_OPEN, GAUGE_SAVE, GAUGE_CHECK, ITEM_RENDERS, DONE }

    private static Phase phase = Phase.WAIT_TITLE;
    private static int ticks;
    private static final List<String> RESULTS = new ArrayList<>();
    private static volatile BlockPos origin;
    private static volatile BlockPos tickerPos;
    private static volatile BlockPos gaugePos;
    private static CompletableFuture<String> gaugeCheck;
    private static int[] gaugeFirstInput = {0, 0};
    private static String lastScreen = "";

    private ClientSmokeTest() {
    }

    public static boolean enabled() {
        return Boolean.getBoolean(PROPERTY);
    }

    public static void onScreenOpening(ScreenEvent.Opening event) {
        BigCreateAutoCrafter.LOGGER.info("[smoke] opening {}", event.getNewScreen() == null ? "none" : event.getNewScreen().getClass().getSimpleName());
    }

    public static void onScreenClosing(ScreenEvent.Closing event) {
        BigCreateAutoCrafter.LOGGER.info("[smoke] closing {}", event.getScreen().getClass().getSimpleName());
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ticks++;
        String screen = mc.screen == null ? "none" : mc.screen.getClass().getSimpleName();
        if (!screen.equals(lastScreen)) {
            BigCreateAutoCrafter.LOGGER.info("[smoke] phase {}: screen {} (profiles: {})", phase, screen, ClientProfileCache.all().size());
            lastScreen = screen;
        }
        try {
            switch (phase) {
                case WAIT_TITLE -> {
                    // First start shows the accessibility onboarding screen instead of the title screen.
                    boolean menu = mc.screen instanceof TitleScreen || mc.screen instanceof AccessibilityOnboardingScreen;
                    if (menu && mc.getOverlay() == null && ticks > 20) {
                        RESULTS.add("PASS client started (" + mc.screen.getClass().getSimpleName() + ")");
                        checkClientMixinTarget();
                        createWorld(mc);
                        next(Phase.LOADING_WORLD);
                    }
                }
                case LOADING_WORLD -> {
                    if (mc.player != null && mc.screen == null && mc.getSingleplayerServer() != null && mc.player.onGround() && ticks > 40) {
                        setUpOnServer(mc.getSingleplayerServer(), mc.player.getUUID());
                        next(Phase.WORLD);
                    }
                }
                case WORLD -> {
                    IntegratedServer server = mc.getSingleplayerServer();
                    UUID playerId = mc.player.getUUID();
                    if (ticks == 10) {
                        server.execute(() -> Showcase.stageWorld(server.getPlayerList().getPlayer(playerId), origin));
                    }
                    if (ticks == 90) {
                        // the chat still shows the confirmations of the Ctrl + right-clicks
                        screenshot(mc, "bcac-world-chat.png");
                        mc.options.hideGui = true;
                    }
                    if (ticks == 95) {
                        screenshot(mc, "bcac-world.png");
                        mc.options.hideGui = false;
                        server.execute(() -> Showcase.returnPlayer(server.getPlayerList().getPlayer(playerId), origin));
                    }
                    if (ticks == 115) {
                        next(Phase.OPEN_SCREEN);
                    }
                }
                case OPEN_SCREEN -> {
                    // the client must have received the Stock Ticker block entity before the menu opens
                    if (ticks > 20) {
                        openStockKeeper(mc.getSingleplayerServer(), mc.player.getUUID());
                        next(Phase.WAIT_SCREEN);
                    }
                }
                case WAIT_SCREEN -> {
                    if (ticks % 100 == 0) {
                        BigCreateAutoCrafter.LOGGER.info("[smoke] waiting: menu {}, screen {}, profiles {}",
                                mc.player.containerMenu.getClass().getSimpleName(), screen, ClientProfileCache.all().size());
                    }
                    if (mc.screen instanceof StockKeeperRequestScreen && !ClientProfileCache.all().isEmpty()) {
                        RESULTS.add("PASS Stock Keeper screen opened, " + ClientProfileCache.all().size() + " profile(s) synced: "
                                + ClientProfileCache.all().get(0).name() + " " + ClientProfileCache.all().get(0).regions().get(0).name());
                        ClientSelection.select(ClientSelection.options(mc.level.dimension()).get(1));
                        RESULTS.add("PASS selector shows: " + ClientSelection.current(mc.level.dimension()).label().getString());
                        next(Phase.SETTLE);
                    }
                }
                case SETTLE -> {
                    if (ticks > 40) {
                        if (ModList.get().isLoaded("jei") && mc.player.containerMenu instanceof StockKeeperRequestMenu menu) {
                            RESULTS.addAll(JeiSmokeCheck.check(menu));
                        }
                        screenshot(mc, "bcac-smoke-stockkeeper.png");
                        next(Phase.TAG_TOOLTIP);
                        ProfileSelector.tagArea().ifPresent(tag -> moveMouse(mc, tag.getX() + tag.getWidth() / 2, tag.getY() + tag.getHeight() / 2));
                    }
                }
                case TAG_TOOLTIP -> {
                    if (ticks == 10) {
                        screenshot(mc, "bcac-smoke-tag.png");
                        if (mc.screen instanceof StockKeeperRequestScreen stockKeeper) {
                            ProfileSelector.openPanel(stockKeeper);
                        }
                        next(Phase.DROPDOWN);
                    }
                }
                case DROPDOWN -> {
                    if (ticks == 10) {
                        screenshot(mc, "bcac-smoke-dropdown.png");
                        ProfileSelector.openManager(mc.screen);
                    }
                    if (ticks > 20 && mc.screen instanceof ProfileManagerScreen manager) {
                        RESULTS.add((manager.rowCount() == 3 ? "PASS" : "FAIL") + " profile menu shows " + manager.rowCount() + " cards (profile + 2 regions)");
                        manager.editCardForTest(0, "Wheel Factory");
                        manager.editCardForTest(1, "big_crafter");
                        next(Phase.MANAGER);
                    }
                }
                case MANAGER -> {
                    // the rename goes to the server and comes back with the next profile sync
                    if (!ClientProfileCache.all().isEmpty() && ClientProfileCache.all().get(0).name().equals("Wheel Factory")
                            && ClientProfileCache.all().get(0).regions().get(0).address().equals("big_crafter")) {
                        RESULTS.add("PASS rename and address from the profile menu reached the server and came back");
                        next(Phase.RENAMED);
                    }
                }
                case RENAMED -> {
                    if (ticks == 10) {
                        screenshot(mc, "bcac-smoke-manager.png");
                        mc.screen.onClose();
                    }
                    if (ticks > 20) {
                        RESULTS.add((mc.screen instanceof StockKeeperRequestScreen ? "PASS" : "FAIL") + " closing the menu returns to the Stock Keeper");
                        mc.player.closeContainer();
                        setUpGauge(mc.getSingleplayerServer(), mc.player.getUUID());
                        next(Phase.GAUGE_SETUP);
                    }
                }
                case GAUGE_SETUP -> {
                    // the client needs the gauge's connections before its screen can find a recipe
                    if (ticks > 20 && mc.level.getBlockEntity(gaugePos) instanceof FactoryPanelBlockEntity gauge
                            && gauge.panels.get(PanelSlot.TOP_LEFT).targetedBy.size() == 3) {
                        mc.setScreen(new FactoryPanelScreen(gauge.panels.get(PanelSlot.TOP_LEFT)));
                        next(Phase.GAUGE_OPEN);
                    }
                }
                case GAUGE_OPEN -> {
                    if (ticks == 5 && mc.screen instanceof FactoryPanelScreen gaugeScreen) {
                        next(Phase.GAUGE_SAVE);
                        useMechanicalCrafting(gaugeScreen);
                    }
                }
                case GAUGE_SAVE -> {
                    if (ticks == 10) {
                        screenshot(mc, "bcac-smoke-gauge.png");
                        moveMouse(mc, gaugeFirstInput[0], gaugeFirstInput[1]);
                    }
                    if (ticks == 20) {
                        screenshot(mc, "bcac-smoke-gauge-tooltip.png");
                        mc.screen.onClose(); // saves like Create's confirm button
                    }
                    if (ticks == 40) {
                        gaugeCheck = mc.getSingleplayerServer().submit(ClientSmokeTest::checkGaugeOnServer);
                        next(Phase.GAUGE_CHECK);
                    }
                }
                case GAUGE_CHECK -> {
                    if (gaugeCheck.isDone()) {
                        RESULTS.add(gaugeCheck.join());
                        mc.setScreen(new Showcase.ItemRenders());
                        next(Phase.ITEM_RENDERS);
                    }
                }
                case ITEM_RENDERS -> {
                    if (ticks == 10) {
                        screenshot(mc, "bcac-item-renders.png");
                        next(Phase.DONE);
                    }
                }
                case DONE -> {
                    if (ticks == 20) {
                        finish(mc);
                    }
                }
            }
            if (phase != Phase.DONE && ticks > TIMEOUT_TICKS) {
                RESULTS.add("FAIL timeout in phase " + phase + " (screen: " + (mc.screen == null ? "none" : mc.screen.getClass().getName()) + ")");
                next(Phase.DONE);
                finish(mc);
            }
        } catch (RuntimeException e) {
            BigCreateAutoCrafter.LOGGER.error("[smoke] exception", e);
            RESULTS.add("FAIL exception in phase " + phase + ": " + e);
            next(Phase.DONE);
            finish(mc);
        }
    }

    /** Loading the classes applies the client mixins; a broken injection target fails here (defaultRequire = 1). */
    private static void checkClientMixinTarget() {
        for (Class<?> target : List.of(StockKeeperRequestScreen.class, FactoryPanelScreen.class)) {
            try {
                Class.forName(target.getName(), true, ClientSmokeTest.class.getClassLoader());
                RESULTS.add("PASS client mixin target " + target.getSimpleName() + " loaded");
            } catch (ClassNotFoundException | LinkageError e) {
                RESULTS.add("FAIL client mixin " + target.getSimpleName() + ": " + e);
            }
        }
    }

    private static void createWorld(Minecraft mc) {
        // no movement tutorial toast over the screenshots
        mc.getTutorial().setStep(TutorialSteps.NONE);
        String name = "bcac-smoke-" + System.currentTimeMillis();
        LevelSettings settings = new LevelSettings(name, GameType.CREATIVE, false, Difficulty.PEACEFUL, true, new GameRules(),
                WorldDataConfiguration.DEFAULT);
        mc.createWorldOpenFlows().createFreshLevel(name, settings, new WorldOptions(0L, false, false),
                access -> access.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
                mc.screen);
    }

    /** 5×5 crafters without corners and a 3×3 field 3 blocks north of the player facing south; Stock Ticker + Blaze Burner to the east. */
    private static void setUpOnServer(IntegratedServer server, UUID playerId) {
        server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            ServerLevel level = player.serverLevel();
            origin = player.blockPosition();
            BlockState crafter = AllBlocks.MECHANICAL_CRAFTER.getDefaultState().setValue(HorizontalKineticBlock.HORIZONTAL_FACING, Direction.SOUTH);
            List<BlockPos> positions = new ArrayList<>();
            for (int dy = 0; dy < 5; dy++) {
                for (int dx = -2; dx <= 2; dx++) {
                    boolean corner = (dy == 0 || dy == 4) && Math.abs(dx) == 2;
                    if (!corner) {
                        positions.add(origin.offset(dx, dy, -3));
                    }
                }
            }
            for (BlockPos pos : positions) {
                level.setBlock(pos, crafter, 3);
            }
            BlockPos first = origin.offset(0, 0, -3);
            for (BlockPos pos : positions) {
                if (!pos.equals(first)) {
                    ConnectedInputHandler.toggleConnection(level, first, pos);
                }
            }
            ProfileService.handleCrafterClick(player, first, false);
            // a 3×3 field to the west (one block gap, within reach) goes into the same profile as a second region
            BlockPos small = origin.offset(-4, 0, -3);
            List<BlockPos> field = new ArrayList<>();
            for (int dy = 0; dy < 3; dy++) {
                for (int dx = -2; dx <= 0; dx++) {
                    field.add(small.offset(dx, dy, 0));
                }
            }
            for (BlockPos pos : field) {
                level.setBlock(pos, crafter, 3);
                if (!pos.equals(small)) {
                    ConnectedInputHandler.toggleConnection(level, small, pos);
                }
            }
            ProfileService.handleCrafterClick(player, small, false);
            tickerPos = origin.east(2);
            level.setBlock(tickerPos, AllBlocks.STOCK_TICKER.getDefaultState(), 3);
            // Create closes the Stock Keeper screen without a seated keeper or a Blaze Burner next to the ticker
            level.setBlock(tickerPos.east(), AllBlocks.BLAZE_BURNER.getDefaultState()
                    .setValue(BlazeBurnerBlock.HEAT_LEVEL, BlazeBurnerBlock.HeatLevel.SMOULDERING), 3);
        });
    }

    /** A Factory Gauge block with four panels: crushing wheel (top left) fed by andesite alloy, planks and stone. */
    private static void setUpGauge(IntegratedServer server, UUID playerId) {
        server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            ServerLevel level = player.serverLevel();
            gaugePos = player.blockPosition().south(2);
            level.setBlock(gaugePos, AllBlocks.FACTORY_GAUGE.getDefaultState().setValue(BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR), 3);
            if (!(level.getBlockEntity(gaugePos) instanceof FactoryPanelBlockEntity gauge)) {
                return;
            }
            UUID network = UUID.randomUUID();
            List<ItemStack> filters = List.of(AllBlocks.CRUSHING_WHEEL.asStack(), AllItems.ANDESITE_ALLOY.asStack(),
                    new ItemStack(Items.OAK_PLANKS), new ItemStack(Items.STONE));
            for (PanelSlot slot : PanelSlot.values()) {
                gauge.addPanel(slot, network);
                gauge.panels.get(slot).setFilter(filters.get(slot.ordinal()));
            }
            FactoryPanelBehaviour wheel = gauge.panels.get(PanelSlot.TOP_LEFT);
            for (PanelSlot slot : List.of(PanelSlot.TOP_RIGHT, PanelSlot.BOTTOM_LEFT, PanelSlot.BOTTOM_RIGHT)) {
                wheel.addConnection(gauge.panels.get(slot).getPanelPosition());
            }
            gauge.notifyUpdate();
        });
    }

    /**
     * Clicks "Use Mechanical Crafting" and sends to an address that is no region's address (the case from the first
     * Factory Gauge test in the game); remembers where the first ingredient is drawn.
     */
    private static void useMechanicalCrafting(FactoryPanelScreen screen) {
        IconButton activate = screen.children().stream()
                .filter(IconButton.class::isInstance).map(IconButton.class::cast)
                .filter(button -> !button.getToolTip().isEmpty()
                        && button.getToolTip().get(0).getContents() instanceof TranslatableContents key
                        && key.getKey().equals("create.gui.factory_panel.activate_crafting"))
                .findFirst().orElse(null);
        if (activate == null) {
            RESULTS.add("FAIL gauge offers no mechanical crafting for the crushing wheel");
            return;
        }
        RESULTS.add("PASS gauge offers mechanical crafting for the crushing wheel");
        int guiLeft = activate.getX() - 31;
        int guiTop = activate.getY() - 27;
        screen.mouseClicked(activate.getX() + 4, activate.getY() + 4, 0);
        screen.children().stream().filter(AddressEditBox.class::isInstance).map(AddressEditBox.class::cast)
                .findFirst().ifPresent(box -> box.setValue("wheel_factory"));
        gaugeFirstInput = new int[] {guiLeft + 68 + 8, guiTop + 28 + 8};
    }

    /** What the gauge saved on the server: the 21-crafter arrangement and 16 / 4 / 1 per craft. */
    private static String checkGaugeOnServer() {
        ServerLevel level = Minecraft.getInstance().getSingleplayerServer().overworld();
        if (!(level.getBlockEntity(gaugePos) instanceof FactoryPanelBlockEntity gauge)) {
            return "FAIL gauge block entity missing";
        }
        FactoryPanelBehaviour wheel = gauge.panels.get(PanelSlot.TOP_LEFT);
        long filled = wheel.activeCraftingArrangement.stream().filter(stack -> !stack.isEmpty()).count();
        Map<String, Integer> amounts = new TreeMap<>();
        for (FactoryPanelConnection connection : wheel.targetedBy.values()) {
            FactoryPanelBehaviour source = FactoryPanelBehaviour.at(level, connection);
            amounts.put(source == null ? "?" : source.getFilter().getItem().toString(), connection.amount);
        }
        boolean ok = filled == 21 && wheel.activeCraftingArrangement.size() == 21 && wheel.recipeAddress.equals("wheel_factory")
                && amounts.equals(Map.of("create:andesite_alloy", 16, "minecraft:oak_planks", 4, "minecraft:stone", 1));
        return (ok ? "PASS" : "FAIL") + " gauge saved " + filled + "/" + wheel.activeCraftingArrangement.size()
                + " crafter slots, amounts " + amounts + ", address " + wheel.recipeAddress;
    }

    private static void openStockKeeper(IntegratedServer server, UUID playerId) {
        server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            boolean opened = StockTickerInteractionHandler.interactWithLogisticsManagerAt(player, player.serverLevel(), tickerPos);
            BigCreateAutoCrafter.LOGGER.info("[smoke] open Stock Keeper at {}: {}", tickerPos, opened);
        });
    }

    private static void screenshot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), message -> {});
        RESULTS.add("PASS screenshot screenshots/" + name);
    }

    private static void next(Phase newPhase) {
        phase = newPhase;
        ticks = 0;
        // keep the mouse in the corner so no tooltip covers the screenshots (in the world it would turn the camera)
        if (Minecraft.getInstance().screen != null) {
            moveMouse(Minecraft.getInstance(), 0, 0);
        }
    }

    /**
     * Moves Minecraft's mouse to GUI coordinates by calling its cursor callback. The real cursor is not warped: GLFW
     * would report an older warp later and undo the move.
     */
    private static void moveMouse(Minecraft mc, int guiX, int guiY) {
        long window = mc.getWindow().getWindow();
        double x = guiX * mc.getWindow().getGuiScale();
        double y = guiY * mc.getWindow().getGuiScale();
        GLFWCursorPosCallback callback = GLFW.glfwSetCursorPosCallback(window, null);
        GLFW.glfwSetCursorPosCallback(window, callback);
        if (callback != null) {
            callback.invoke(window, x, y);
        }
    }

    private static void finish(Minecraft mc) {
        boolean failed = RESULTS.stream().anyMatch(r -> r.startsWith("FAIL"));
        RESULTS.add(failed ? "RESULT FAIL" : "RESULT PASS");
        RESULTS.forEach(line -> BigCreateAutoCrafter.LOGGER.info("[smoke] {}", line));
        try {
            Files.write(Path.of(mc.gameDirectory.getPath(), "bcac-smoketest.txt"), RESULTS, StandardCharsets.UTF_8);
        } catch (IOException e) {
            BigCreateAutoCrafter.LOGGER.error("[smoke] could not write result file", e);
        }
        mc.stop();
    }
}
