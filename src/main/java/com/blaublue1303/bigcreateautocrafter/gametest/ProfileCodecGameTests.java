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
    private static CrafterProfile sampleProfile() {
        return new CrafterProfile(UUID.randomUUID(), "Hauptanlage", Level.OVERWORLD, "crafting", UUID.randomUUID(), List.of(
                new CrafterRegion("Groß", new BlockPos(10, 64, -3), Direction.NORTH,
                        List.of("#####", "#####", "#####", "###..", "###..")),
                new CrafterRegion("3x3", new BlockPos(20, 64, -3), Direction.EAST,
                        List.of("###", "###", "###"))));
    }

    @GameTest(template = "empty7")
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
        helper.assertTrue(big.hasCrafterAt(0, 0) && !big.hasCrafterAt(4, 4), "hasCrafterAt is wrong");
        helper.succeed();
    }

    @GameTest(template = "empty7")
    public static void invalidRegionsAreRejected(GameTestHelper helper) {
        BlockPos pos = BlockPos.ZERO;
        List<CrafterRegion> invalid = List.of(
                new CrafterRegion("ragged", pos, Direction.NORTH, List.of("###", "##")),
                new CrafterRegion("bad char", pos, Direction.NORTH, List.of("#x#")),
                new CrafterRegion("no crafter", pos, Direction.NORTH, List.of("...")),
                new CrafterRegion("empty", pos, Direction.NORTH, List.of()),
                new CrafterRegion("vertical", pos, Direction.UP, List.of("#")),
                new CrafterRegion(" ", pos, Direction.NORTH, List.of("#")));
        for (CrafterRegion region : invalid) {
            helper.assertTrue(CrafterRegion.validate(region).error().isPresent(), "Region should be invalid: " + region);
        }
        helper.assertTrue(CrafterRegion.validate(new CrafterRegion("ok", pos, Direction.SOUTH, List.of("#.#"))).result().isPresent(),
                "Valid region rejected");
        helper.succeed();
    }
}
