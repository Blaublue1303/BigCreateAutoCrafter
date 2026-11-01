package com.blaublue1303.bigcreateautocrafter.mixin.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.blaublue1303.bigcreateautocrafter.client.ClientProfileCache;
import com.blaublue1303.bigcreateautocrafter.profile.CrafterRegion;
import com.blaublue1303.bigcreateautocrafter.recipe.FactoryArrangements;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.logistics.AddressEditBox;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelConnection;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelPosition;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelScreen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;

/**
 * Factory Gauge on a crafter region: the gauge's recipe address selects the region (see {@link FactoryArrangements}).
 * Create's 3×3 behaviour is kept for every gauge whose address is not the address of a region.
 */
@Mixin(FactoryPanelScreen.class)
public abstract class FactoryPanelScreenMixin {
    @Shadow
    private FactoryPanelBehaviour behaviour;
    @Shadow
    private AddressEditBox addressBox;
    @Shadow
    private BigItemStack outputConfig;
    @Shadow
    private List<BigItemStack> inputConfig;
    @Shadow
    private CraftingRecipe availableCraftingRecipe;
    @Shadow
    private boolean craftingActive;
    @Shadow
    private List<FactoryPanelConnection> connections;

    private static final String PACKET_INIT = "Lcom/simibubi/create/content/logistics/factoryBoard/FactoryPanelConfigurationPacket;<init>"
            + "(Lcom/simibubi/create/content/logistics/factoryBoard/FactoryPanelPosition;Ljava/lang/String;Ljava/util/Map;Ljava/util/List;"
            + "IILcom/simibubi/create/content/logistics/factoryBoard/FactoryPanelPosition;ZZZ)V";

    /** Create only looks at regular crafting recipes; also offer mechanical crafting recipes that fit one of the player's regions. */
    @Inject(method = "searchForCraftingRecipe", at = @At("TAIL"))
    private void bcac$findMechanicalRecipe(CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (availableCraftingRecipe != null || mc.level == null || behaviour.targetedBy.isEmpty()) {
            return;
        }
        List<CrafterRegion> regions = ClientProfileCache.all().stream()
                .filter(p -> p.dimension().equals(mc.level.dimension()))
                .flatMap(p -> p.regions().stream())
                .toList();
        availableCraftingRecipe = FactoryArrangements.findMechanicalRecipe(mc.level, outputConfig.stack, inputConfig,
                recipe -> regions.stream().anyMatch(region -> FactoryArrangements.forRegion(recipe, inputConfig, region).isPresent()))
                .orElse(null);
    }

    /** The gauge previews the arrangement in a 3×3 grid; for larger recipes show the input items instead of a cut-off grid. */
    @WrapOperation(method = "updateConfigs", at = @At(value = "INVOKE",
            target = "Lcom/simibubi/create/content/logistics/factoryBoard/FactoryPanelScreen;convertRecipeToPackageOrderContext(Lnet/minecraft/world/item/crafting/CraftingRecipe;Ljava/util/List;Z)Ljava/util/List;"))
    private List<BigItemStack> bcac$previewLargeRecipe(CraftingRecipe recipe, List<BigItemStack> inputs, boolean respectAmounts,
                                                       Operation<List<BigItemStack>> original) {
        if (FactoryArrangements.fitsThreeByThree(recipe)) {
            return original.call(recipe, inputs, respectAmounts);
        }
        return inputs.stream().filter(input -> !input.stack.isEmpty()).map(input -> new BigItemStack(input.stack, 1)).toList();
    }

    /** The arrangement for the region whose address the gauge sends to, if crafting is on and the recipe fits it. */
    private Optional<List<ItemStack>> bcac$regionArrangement() {
        Minecraft mc = Minecraft.getInstance();
        if (!craftingActive || availableCraftingRecipe == null || mc.level == null) {
            return Optional.empty();
        }
        return FactoryArrangements.regionForAddress(ClientProfileCache.all(), mc.level.dimension(), addressBox.getValue())
                .flatMap(region -> FactoryArrangements.forRegion(availableCraftingRecipe, inputConfig, region));
    }

    /** Amount per input = how often its item occurs in the region arrangement (Create counts its own 3×3 arrangement). */
    @ModifyArg(method = "sendIt", index = 2, at = @At(value = "INVOKE", target = PACKET_INIT))
    private Map<FactoryPanelPosition, Integer> bcac$amountsForRegion(Map<FactoryPanelPosition, Integer> createAmounts) {
        Optional<List<ItemStack>> arrangement = bcac$regionArrangement();
        if (arrangement.isEmpty() || inputConfig.size() != connections.size()) {
            return createAmounts;
        }
        Map<FactoryPanelPosition, Integer> amounts = new HashMap<>();
        for (int i = 0; i < inputConfig.size(); i++) {
            ItemStack input = inputConfig.get(i).stack;
            int count = (int) arrangement.get().stream()
                    .filter(stack -> !stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, input))
                    .count();
            amounts.put(connections.get(i).from, count);
        }
        return amounts;
    }

    /** When saving, use the arrangement for the region whose address the gauge sends to. */
    @ModifyArg(method = "sendIt", index = 3, at = @At(value = "INVOKE", target = PACKET_INIT))
    private List<ItemStack> bcac$arrangementForRegion(List<ItemStack> createArrangement) {
        if (createArrangement.isEmpty()) {
            return createArrangement;
        }
        Optional<List<ItemStack>> regionArrangement = bcac$regionArrangement();
        if (regionArrangement.isPresent()) {
            return regionArrangement.get();
        }
        if (availableCraftingRecipe == null || FactoryArrangements.fitsThreeByThree(availableCraftingRecipe)) {
            return createArrangement;
        }
        // A larger recipe only works on a region: crafting stays off until the address is set to one.
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.translatable("message.bigcreateautocrafter.factory_needs_region")
                    .withStyle(ChatFormatting.RED), false);
        }
        return List.of();
    }
}
