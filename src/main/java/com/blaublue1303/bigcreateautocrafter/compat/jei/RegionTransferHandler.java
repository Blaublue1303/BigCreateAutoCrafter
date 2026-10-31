package com.blaublue1303.bigcreateautocrafter.compat.jei;

import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.blaublue1303.bigcreateautocrafter.BigCreateAutoCrafter;
import com.blaublue1303.bigcreateautocrafter.client.ClientPlacements;
import com.blaublue1303.bigcreateautocrafter.client.ClientSelection;
import com.blaublue1303.bigcreateautocrafter.recipe.GridRecipe;
import com.blaublue1303.bigcreateautocrafter.recipe.Placement;
import com.blaublue1303.bigcreateautocrafter.recipe.RegionPlacer;
import com.simibubi.create.AllMenuTypes;
import com.simibubi.create.compat.jei.StockKeeperTransferHandler;
import com.simibubi.create.content.logistics.packager.InventorySummary;
import com.simibubi.create.content.logistics.stockTicker.CraftableBigItemStack;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestMenu;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestScreen;
import com.simibubi.create.content.logistics.stockTicker.StockTickerBlockEntity;

import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * JEI "+" for crafting recipes in the Stock Keeper. Registered for specific recipe types, so JEI prefers it over
 * Create's universal {@code StockKeeperTransferHandler} (docs/02, T2).
 *
 * <p>With "Standard" selected, the call is handed to Create's handler unchanged. With a profile selected, the recipe is
 * placed into a crafter region (limit L1 – more than 9 ingredients – no longer applies) and added to the order the same
 * way Create does it; the pattern for the region is produced later when the order is sent.
 */
public class RegionTransferHandler implements IRecipeTransferHandler<StockKeeperRequestMenu, RecipeHolder<CraftingRecipe>> {
    private static final String KEY = BigCreateAutoCrafter.MODID + ".transfer.";
    /** Create allows at most this many different item types in one Stock Keeper order (limit L3, unchanged). */
    private static final int CREATE_MAX_ORDER_TYPES = 9;

    private final RecipeType<RecipeHolder<CraftingRecipe>> recipeType;
    private final IRecipeTransferHandlerHelper helper;
    private final StockKeeperTransferHandler createHandler;

    public RegionTransferHandler(RecipeType<RecipeHolder<CraftingRecipe>> recipeType, IRecipeTransferHandlerHelper helper,
                                 StockKeeperTransferHandler createHandler) {
        this.recipeType = recipeType;
        this.helper = helper;
        this.createHandler = createHandler;
    }

    @Override
    public Class<? extends StockKeeperRequestMenu> getContainerClass() {
        return StockKeeperRequestMenu.class;
    }

    @Override
    public Optional<MenuType<StockKeeperRequestMenu>> getMenuType() {
        return Optional.of(AllMenuTypes.STOCK_KEEPER_REQUEST.get());
    }

    @Override
    public RecipeType<RecipeHolder<CraftingRecipe>> getRecipeType() {
        return recipeType;
    }

    /**
     * Still abstract in JEI 19.70 (deprecated since 19.52 in favour of a context-based default method that delegates here).
     * Implementing it keeps the handler working with every JEI 19.x, as the pack's exact JEI build is not confirmed yet.
     */
    @SuppressWarnings("removal")
    @Override
    @Nullable
    public IRecipeTransferError transferRecipe(StockKeeperRequestMenu menu, RecipeHolder<CraftingRecipe> recipe, IRecipeSlotsView recipeSlots,
                                               Player player, boolean maxTransfer, boolean doTransfer) {
        if (!player.level().isClientSide()) {
            return null;
        }
        ClientSelection.Option option = ClientSelection.current(player.level().dimension());
        if (option.isStandard()) {
            if (doTransfer) {
                ClientPlacements.remove(recipe.value());
            }
            return createHandler.transferRecipe(menu, recipe, recipeSlots, player, maxTransfer, doTransfer);
        }
        if (!(menu.screenReference instanceof StockKeeperRequestScreen screen)) {
            return helper.createInternalError();
        }

        Optional<GridRecipe> grid = GridRecipe.of(recipe.value());
        if (grid.isEmpty()) {
            return tooltip(Component.translatable(KEY + "unsupported"));
        }
        Optional<Placement> placement = RegionPlacer.place(grid.get(), option.profile(), option.regionIndex());
        if (placement.isEmpty()) {
            return tooltip(Component.translatable(KEY + "no_region", option.profile().name()));
        }
        for (CraftableBigItemStack ordered : screen.recipesToOrder) {
            if (ordered.recipe == recipe.value()) {
                return tooltip(Component.translatable("create.gui.stock_keeper.already_ordering_recipe"));
            }
        }
        if (screen.itemsToOrder.size() >= CREATE_MAX_ORDER_TYPES) {
            return tooltip(Component.translatable("create.gui.stock_keeper.slots_full"));
        }

        InventorySummary stock = ((StockTickerBlockEntity) menu.contentHolder).getLastClientsideStockSnapshotAsSummary();
        if (stock == null) {
            return helper.createInternalError();
        }
        List<IRecipeSlotView> missing = recipeSlots.getSlotViews(RecipeIngredientRole.INPUT).stream()
                .filter(slot -> !slot.isEmpty() && slot.getItemStacks().noneMatch(stack -> stock.getCountOf(stack) > 0))
                .toList();
        if (!missing.isEmpty()) {
            return helper.createUserErrorForMissingSlots(Component.translatable("create.gui.stock_keeper.not_in_stock"), missing);
        }
        if (!doTransfer) {
            return null;
        }

        ItemStack result = recipe.value().getResultItem(player.level().registryAccess());
        if (result.isEmpty()) {
            return tooltip(Component.translatable("create.gui.stock_keeper.recipe_result_empty"));
        }
        CraftableBigItemStack craftable = new CraftableBigItemStack(result, recipe.value());
        ClientPlacements.put(recipe.value(), placement.get());
        screen.recipesToOrder.add(craftable);
        screen.searchBox.setValue("");
        screen.refreshSearchNextTick = true;
        screen.requestCraftable(craftable, maxTransfer ? craftable.stack.getMaxStackSize() : 1);
        String address = placement.get().region().address();
        if (!address.isEmpty()) {
            screen.addressBox.setValue(address);
        }
        return null;
    }

    private IRecipeTransferError tooltip(Component message) {
        return helper.createUserErrorWithTooltip(message);
    }
}
