package com.blaublue1303.bigcreateautocrafter.mixin.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.blaublue1303.bigcreateautocrafter.client.ClientProfileCache;
import com.blaublue1303.bigcreateautocrafter.profile.CrafterRegion;
import com.blaublue1303.bigcreateautocrafter.recipe.FactoryArrangements;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.logistics.AddressEditBox;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelConnection;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelPosition;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelScreen;
import com.simibubi.create.foundation.gui.widget.IconButton;

import net.createmod.catnip.gui.AbstractSimiScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;

/**
 * Factory Gauge on a crafter region (see {@link FactoryArrangements#target}). For mechanical crafting recipes the gauge
 * crafts on the smallest of the player's regions the recipe fits, or on the region whose address the gauge sends to;
 * the gauge window then shows the real amount per ingredient and the region. Create's 3×3 behaviour is kept for regular
 * recipes unless the gauge sends to a region's address.
 */
@Mixin(FactoryPanelScreen.class)
public abstract class FactoryPanelScreenMixin extends AbstractSimiScreen {
    private static final String KEY = "bigcreateautocrafter.factory.";
    private static final String PACKET_INIT = "Lcom/simibubi/create/content/logistics/factoryBoard/FactoryPanelConfigurationPacket;<init>"
            + "(Lcom/simibubi/create/content/logistics/factoryBoard/FactoryPanelPosition;Ljava/lang/String;Ljava/util/Map;Ljava/util/List;"
            + "IILcom/simibubi/create/content/logistics/factoryBoard/FactoryPanelPosition;ZZZ)V";

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
    @Shadow
    private IconButton activateCraftingButton;

    /** Whether the crafting preview shows the inputs with amounts (recipe larger than 3×3) instead of Create's 3×3 grid. */
    @Unique
    private boolean bcac$inputPreview;

    /** Region and arrangement for the current recipe and address, if the gauge crafts on a region. */
    @Unique
    private Optional<FactoryArrangements.Target> bcac$target() {
        Minecraft mc = Minecraft.getInstance();
        if (availableCraftingRecipe == null || mc.level == null) {
            return Optional.empty();
        }
        // the address box only exists after init(); before that the saved address is the current one
        String address = addressBox != null ? addressBox.getValue() : behaviour.recipeAddress;
        return FactoryArrangements.target(ClientProfileCache.all(), mc.level.dimension(), address, availableCraftingRecipe, inputConfig);
    }

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

    /** A recipe larger than 3×3 cannot be shown in the gauge's 3×3 grid: show each input once with its amount per craft. */
    @WrapOperation(method = "updateConfigs", at = @At(value = "INVOKE",
            target = "Lcom/simibubi/create/content/logistics/factoryBoard/FactoryPanelScreen;convertRecipeToPackageOrderContext(Lnet/minecraft/world/item/crafting/CraftingRecipe;Ljava/util/List;Z)Ljava/util/List;"))
    private List<BigItemStack> bcac$previewLargeRecipe(CraftingRecipe recipe, List<BigItemStack> inputs, boolean respectAmounts,
                                                       Operation<List<BigItemStack>> original) {
        bcac$inputPreview = !FactoryArrangements.fitsThreeByThree(recipe);
        if (!bcac$inputPreview) {
            return original.call(recipe, inputs, respectAmounts);
        }
        Optional<FactoryArrangements.Target> target = bcac$target();
        return inputs.stream()
                .filter(input -> !input.stack.isEmpty())
                .map(input -> new BigItemStack(input.stack, target.map(t -> t.countOf(input.stack)).orElse(1)))
                .toList();
    }

    /** Tell on the "Use Mechanical Crafting" button which region is used; fill in the region's address if the gauge has none. */
    @Inject(method = "init", at = @At("TAIL"))
    private void bcac$describeRegion(CallbackInfo ci) {
        Optional<FactoryArrangements.Target> target = bcac$target();
        if (activateCraftingButton == null || target.isEmpty() || !bcac$inputPreview) {
            return;
        }
        CrafterRegion region = target.get().region();
        List<Component> tooltip = activateCraftingButton.getToolTip();
        tooltip.add(Component.translatable(KEY + "region", region.name()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(KEY + "ingredients", ingredientCount(target.get())).withStyle(ChatFormatting.GRAY));
        if (craftingActive && addressBox.getValue().isBlank() && !region.address().isEmpty()) {
            addressBox.setValue(region.address());
        }
    }

    /** Create hides amounts while crafting is on; the input preview needs them. */
    @Inject(method = "renderInputItem", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
            target = "Lnet/minecraft/client/gui/GuiGraphics;renderItem(Lnet/minecraft/world/item/ItemStack;II)V"))
    private void bcac$renderAmount(GuiGraphics graphics, int slot, BigItemStack itemStack, int mouseX, int mouseY, CallbackInfo ci) {
        if (craftingActive && bcac$inputPreview && !itemStack.stack.isEmpty()) {
            int x = guiLeft + 68 + slot % 3 * 20;
            int y = guiTop + 28 + slot / 3 * 20;
            graphics.renderItemDecorations(font, itemStack.stack, x, y, String.valueOf(itemStack.count));
        }
    }

    /** Create's tooltip on a crafting input speaks of "3x3"; for the input preview name the amount and the region instead. */
    @WrapOperation(method = "renderInputItem", at = @At(value = "INVOKE", ordinal = 0,
            target = "Lnet/minecraft/client/gui/GuiGraphics;renderComponentTooltip(Lnet/minecraft/client/gui/Font;Ljava/util/List;II)V"))
    private void bcac$inputTooltip(GuiGraphics graphics, Font font, List<Component> lines, int mouseX, int mouseY, Operation<Void> original,
                                   @Local(argsOnly = true) BigItemStack itemStack) {
        if (!bcac$inputPreview) {
            original.call(graphics, font, lines, mouseX, mouseY);
            return;
        }
        Optional<FactoryArrangements.Target> target = bcac$target();
        List<Component> ours = target.<List<Component>>map(t -> List.of(
                        lines.get(0),
                        Component.translatable(KEY + "per_craft", itemStack.count, itemStack.stack.getHoverName()).withStyle(ChatFormatting.WHITE),
                        Component.translatable(KEY + "region", t.region().name()).withStyle(ChatFormatting.GRAY),
                        Component.translatable(KEY + "address_tip").withStyle(ChatFormatting.GRAY)))
                .orElseGet(() -> List.of(lines.get(0),
                        Component.translatable("message.bigcreateautocrafter.factory_needs_region").withStyle(ChatFormatting.RED)));
        original.call(graphics, font, ours, mouseX, mouseY);
    }

    /** Amount per input = how often its item occurs in the region arrangement (Create counts its own 3×3 arrangement). */
    @ModifyArg(method = "sendIt", index = 2, at = @At(value = "INVOKE", target = PACKET_INIT))
    private Map<FactoryPanelPosition, Integer> bcac$amountsForRegion(Map<FactoryPanelPosition, Integer> createAmounts) {
        Optional<FactoryArrangements.Target> target = craftingActive ? bcac$target() : Optional.empty();
        if (target.isEmpty() || inputConfig.size() != connections.size()) {
            return createAmounts;
        }
        Map<FactoryPanelPosition, Integer> amounts = new HashMap<>();
        for (int i = 0; i < inputConfig.size(); i++) {
            amounts.put(connections.get(i).from, target.get().countOf(inputConfig.get(i).stack));
        }
        return amounts;
    }

    /** When saving, use the arrangement for the region the gauge crafts on. */
    @ModifyArg(method = "sendIt", index = 3, at = @At(value = "INVOKE", target = PACKET_INIT))
    private List<ItemStack> bcac$arrangementForRegion(List<ItemStack> createArrangement) {
        if (createArrangement.isEmpty()) {
            return createArrangement;
        }
        Minecraft mc = Minecraft.getInstance();
        Optional<FactoryArrangements.Target> target = bcac$target();
        if (target.isPresent()) {
            if (mc.player != null) {
                mc.player.displayClientMessage(Component.translatable("message.bigcreateautocrafter.factory_saved",
                        target.get().region().name(), ingredientCount(target.get())).withStyle(ChatFormatting.GREEN), true);
            }
            return target.get().arrangement();
        }
        if (availableCraftingRecipe == null || FactoryArrangements.fitsThreeByThree(availableCraftingRecipe)) {
            return createArrangement;
        }
        // a larger recipe only works on a region, and none fits (any more): crafting stays off
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.translatable("message.bigcreateautocrafter.factory_needs_region")
                    .withStyle(ChatFormatting.RED), false);
        }
        return List.of();
    }

    @Unique
    private static int ingredientCount(FactoryArrangements.Target target) {
        return (int) target.arrangement().stream().filter(stack -> !stack.isEmpty()).count();
    }
}
