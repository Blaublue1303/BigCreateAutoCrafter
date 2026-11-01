package com.blaublue1303.bigcreateautocrafter.gametest;

import java.util.List;
import java.util.UUID;

import com.blaublue1303.bigcreateautocrafter.BigCreateAutoCrafter;
import com.blaublue1303.bigcreateautocrafter.profile.CrafterProfile;
import com.blaublue1303.bigcreateautocrafter.profile.CrafterRegion;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.connection.ConnectionType;

/** Save (NBT) and network round trips of the profile data model, plus layout validation. */
@GameTestHolder(BigCreateAutoCrafter.MODID)
@PrefixGameTestTemplate(false)
public class ProfileCodecGameTests {
    /** 5×5 without corners = 21 crafters, the shape of Create's crushing wheel recipe. */
    static final List<String> LAYOUT_21 = List.of(".###.", "#####", "#####", "#####", ".###.");
    static final List<String> LAYOUT_3X3 = List.of("###", "###", "###");

    static CrafterProfile sampleProfile() {
        return new CrafterProfile(UUID.randomUUID(), "Hauptanlage", Level.OVERWORLD, UUID.randomUUID(), List.of(
                new CrafterRegion("Groß", new BlockPos(10, 64, -3), Direction.NORTH, LAYOUT_21, "crafter_gross"),
                new CrafterRegion("3x3", new BlockPos(20, 64, -3), Direction.EAST, LAYOUT_3X3, "")));
    }

    @GameTest(template = TestCrafters.ARENA)
    public static void profileSurvivesNbtAndNetwork(GameTestHelper helper) {
        CrafterProfile profile = sampleProfile();

        Tag nbt = CrafterProfile.CODEC.encodeStart(NbtOps.INSTANCE, profile).getOrThrow();
        CrafterProfile fromNbt = CrafterProfile.CODEC.parse(NbtOps.INSTANCE, nbt).getOrThrow();
        helper.assertTrue(profile.equals(fromNbt), "NBT round trip changed the profile: " + fromNbt);

        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess(),
                ConnectionType.NEOFORGE);
        CrafterProfile.STREAM_CODEC.encode(buf, profile);
        CrafterProfile fromNetwork = CrafterProfile.STREAM_CODEC.decode(buf);
        helper.assertTrue(profile.equals(fromNetwork), "Network round trip changed the profile: " + fromNetwork);

        CrafterRegion big = profile.regions().get(0);
        helper.assertTrue(big.width() == 5 && big.height() == 5 && big.crafterCount() == 21,
                "Unexpected size of 21-crafter region: " + big.width() + "x" + big.height() + ", " + big.crafterCount());
        helper.assertTrue(!big.hasCrafterAt(0, 0) && big.hasCrafterAt(1, 0) && !big.hasCrafterAt(4, 4), "hasCrafterAt is wrong");
        helper.assertTrue(CrafterRegion.sizeLabel(LAYOUT_21).equals("5×5 (21)") && CrafterRegion.sizeLabel(LAYOUT_3X3).equals("3×3"),
                "Unexpected size labels");
        helper.succeed();
    }

    @GameTest(template = TestCrafters.ARENA)
    public static void invalidRegionsAreRejected(GameTestHelper helper) {
        BlockPos pos = BlockPos.ZERO;
        List<CrafterRegion> invalid = List.of(
                new CrafterRegion("ragged", pos, Direction.NORTH, List.of("###", "##"), ""),
                new CrafterRegion("bad char", pos, Direction.NORTH, List.of("#x#"), ""),
                new CrafterRegion("no crafter", pos, Direction.NORTH, List.of("..."), ""),
                new CrafterRegion("empty", pos, Direction.NORTH, List.of(), ""),
                new CrafterRegion("vertical", pos, Direction.UP, List.of("#"), ""),
                new CrafterRegion(" ", pos, Direction.NORTH, List.of("#"), ""),
                new CrafterRegion("long address", pos, Direction.NORTH, List.of("#"), "x".repeat(CrafterRegion.MAX_ADDRESS_LENGTH + 1)));
        for (CrafterRegion region : invalid) {
            helper.assertTrue(CrafterRegion.validate(region).error().isPresent(), "Region should be invalid: " + region);
        }
        helper.assertTrue(CrafterRegion.validate(new CrafterRegion("ok", pos, Direction.SOUTH, List.of("#.#"), "a")).result().isPresent(),
                "Valid region rejected");
        helper.succeed();
    }
}
