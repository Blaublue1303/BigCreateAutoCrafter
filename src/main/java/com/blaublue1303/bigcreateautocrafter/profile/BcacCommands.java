package com.blaublue1303.bigcreateautocrafter.profile;

import java.util.List;
import java.util.Optional;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /bcac list | select <p> | rename <p> <name> | address <p> <region> <address> | delete <p>}.
 * Profiles and regions are addressed by the 1-based numbers shown by {@code /bcac list}.
 */
public final class BcacCommands {
    private static final String MSG = ProfileService.MSG;

    private BcacCommands() {
    }

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("bcac")
                .then(Commands.literal("list").executes(BcacCommands::list))
                .then(Commands.literal("select")
                        .then(Commands.argument("profile", IntegerArgumentType.integer(1)).executes(BcacCommands::select)))
                .then(Commands.literal("rename")
                        .then(Commands.argument("profile", IntegerArgumentType.integer(1))
                                .then(Commands.argument("name", StringArgumentType.greedyString()).executes(BcacCommands::rename))))
                .then(Commands.literal("address")
                        .then(Commands.argument("profile", IntegerArgumentType.integer(1))
                                .then(Commands.argument("region", IntegerArgumentType.integer(1))
                                        .executes(ctx -> address(ctx, ""))
                                        .then(Commands.argument("address", StringArgumentType.greedyString())
                                                .executes(ctx -> address(ctx, StringArgumentType.getString(ctx, "address")))))))
                .then(Commands.literal("delete")
                        .then(Commands.argument("profile", IntegerArgumentType.integer(1)).executes(BcacCommands::delete))));
    }

    private static int list(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        List<CrafterProfile> own = ProfileManager.get(player.server).byOwner(player.getUUID());
        if (own.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.translatable(MSG + "list_empty").withStyle(ChatFormatting.GRAY), false);
            return 0;
        }
        for (int p = 0; p < own.size(); p++) {
            CrafterProfile profile = own.get(p);
            int profileNumber = p + 1;
            ctx.getSource().sendSuccess(() -> Component.literal(profileNumber + ". " + profile.name())
                    .withStyle(ChatFormatting.GOLD)
                    .append(Component.literal("  [" + profile.dimension().location() + "]").withStyle(ChatFormatting.DARK_GRAY)), false);
            for (int r = 0; r < profile.regions().size(); r++) {
                CrafterRegion region = profile.regions().get(r);
                int regionNumber = r + 1;
                ctx.getSource().sendSuccess(() -> Component.literal("   " + regionNumber + ") " + region.name()
                        + "  @ " + region.controller().toShortString() + "  → " + region.displayAddress()).withStyle(ChatFormatting.GRAY), false);
            }
        }
        return own.size();
    }

    private static int select(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        Optional<CrafterProfile> profile = profile(ctx, player);
        if (profile.isEmpty()) {
            return 0;
        }
        ProfileService.select(player, profile.get());
        ctx.getSource().sendSuccess(() -> Component.translatable(MSG + "selected", profile.get().name()), false);
        return 1;
    }

    private static int rename(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        Optional<CrafterProfile> profile = profile(ctx, player);
        if (profile.isEmpty()) {
            return 0;
        }
        String name = StringArgumentType.getString(ctx, "name").trim();
        CrafterProfile renamed = profile.get().withName(name);
        if (CrafterProfile.validate(renamed).error().isPresent()) {
            ctx.getSource().sendFailure(Component.translatable(MSG + "bad_name", CrafterProfile.MAX_NAME_LENGTH));
            return 0;
        }
        ProfileService.save(player, renamed);
        ctx.getSource().sendSuccess(() -> Component.translatable(MSG + "renamed", name), false);
        return 1;
    }

    private static int address(CommandContext<CommandSourceStack> ctx, String rawAddress) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        Optional<CrafterProfile> profile = profile(ctx, player);
        if (profile.isEmpty()) {
            return 0;
        }
        int regionIndex = IntegerArgumentType.getInteger(ctx, "region") - 1;
        if (regionIndex >= profile.get().regions().size()) {
            ctx.getSource().sendFailure(Component.translatable(MSG + "no_such_region", regionIndex + 1));
            return 0;
        }
        CrafterRegion region = profile.get().regions().get(regionIndex).withAddress(rawAddress.trim());
        if (CrafterRegion.validate(region).error().isPresent()) {
            ctx.getSource().sendFailure(Component.translatable(MSG + "bad_address", CrafterRegion.MAX_ADDRESS_LENGTH));
            return 0;
        }
        ProfileService.save(player, profile.get().withRegionReplaced(regionIndex, region));
        ctx.getSource().sendSuccess(() -> Component.translatable(MSG + "address_set", region.name(), region.displayAddress()), false);
        return 1;
    }

    private static int delete(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        Optional<CrafterProfile> profile = profile(ctx, player);
        if (profile.isEmpty()) {
            return 0;
        }
        ProfileService.delete(player, profile.get());
        ctx.getSource().sendSuccess(() -> Component.translatable(MSG + "profile_removed", profile.get().name()), false);
        return 1;
    }

    private static Optional<CrafterProfile> profile(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
        int index = IntegerArgumentType.getInteger(ctx, "profile");
        Optional<CrafterProfile> profile = ProfileService.profileByIndex(player, index);
        if (profile.isEmpty()) {
            ctx.getSource().sendFailure(Component.translatable(MSG + "no_such_profile", index));
        }
        return profile;
    }
}
