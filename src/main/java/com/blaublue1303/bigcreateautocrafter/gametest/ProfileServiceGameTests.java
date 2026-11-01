package com.blaublue1303.bigcreateautocrafter.gametest;

import java.util.List;
import java.util.UUID;

import com.blaublue1303.bigcreateautocrafter.BigCreateAutoCrafter;
import com.blaublue1303.bigcreateautocrafter.compat.create.ConnectedCrafterGroup;
import com.blaublue1303.bigcreateautocrafter.net.EditProfilePayload;
import com.blaublue1303.bigcreateautocrafter.profile.CrafterProfile;
import com.blaublue1303.bigcreateautocrafter.profile.ProfileManager;
import com.blaublue1303.bigcreateautocrafter.profile.ProfileService;
import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** The Ctrl + right-click flow on the server, with a mock player and real crafter groups. */
@GameTestHolder(BigCreateAutoCrafter.MODID)
@PrefixGameTestTemplate(false)
public class ProfileServiceGameTests {
    @GameTest(template = TestCrafters.ARENA)
    public static void ctrlClickCreatesAndEditsProfiles(GameTestHelper helper) {
        ConnectedCrafterGroup big = TestCrafters.build(helper, Direction.NORTH, TestCrafters.positionsXY(ProfileCodecGameTests.LAYOUT_21, 3));
        ConnectedCrafterGroup small = TestCrafters.build(helper, Direction.NORTH, TestCrafters.positionsXY(ProfileCodecGameTests.LAYOUT_3X3, 5));
        // A FakePlayer is a real ServerPlayer without network connection or login events.
        ServerPlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "bcac-test"));
        player.moveTo(Vec3.atCenterOf(helper.absolutePos(new BlockPos(3, 3, 1))));
        ProfileManager manager = ProfileManager.get(player.server);
        try {
            ProfileService.handleCrafterClick(player, big.members().get(4), false);
            List<CrafterProfile> own = manager.byOwner(player.getUUID());
            helper.assertTrue(own.size() == 1 && own.get(0).name().equals("Anlage 1") && own.get(0).regions().size() == 1,
                    "First click must create profile 'Anlage 1': " + own);
            helper.assertTrue(own.get(0).regions().get(0).crafterCount() == 21 && own.get(0).regions().get(0).name().equals("5×5 (21)"),
                    "Region must be the 21-crafter group: " + own.get(0).regions());

            ProfileService.handleCrafterClick(player, small.members().get(0), false);
            own = manager.byOwner(player.getUUID());
            helper.assertTrue(own.size() == 1 && own.get(0).regions().size() == 2 && own.get(0).regions().get(1).crafterCount() == 9,
                    "Second group must be added to the active profile: " + own);

            ProfileService.handleCrafterClick(player, big.controller(), false);
            helper.assertTrue(manager.byOwner(player.getUUID()).get(0).regions().size() == 2, "Clicking a known group must not add it again");

            ProfileService.handleCrafterClick(player, small.controller(), true);
            own = manager.byOwner(player.getUUID());
            helper.assertTrue(own.size() == 1 && own.get(0).regions().size() == 1, "Shift click must remove the 3×3 region: " + own);

            ProfileService.handleCrafterClick(player, big.controller(), true);
            helper.assertTrue(manager.byOwner(player.getUUID()).isEmpty(), "Removing the last region must delete the profile");

            ProfileService.handleCrafterClick(player, small.controller(), true);
            own = manager.byOwner(player.getUUID());
            helper.assertTrue(own.size() == 1 && own.get(0).regions().get(0).crafterCount() == 9, "Shift click on a new group must start a profile: " + own);

            player.moveTo(Vec3.atCenterOf(helper.absolutePos(new BlockPos(3, 3, 1))).add(40, 0, 0));
            ProfileService.handleCrafterClick(player, big.controller(), false);
            helper.assertTrue(manager.byOwner(player.getUUID()).get(0).regions().size() == 1, "Clicks out of reach must be ignored");
        } finally {
            manager.byOwner(player.getUUID()).forEach(p -> manager.remove(p.id()));
        }
        helper.succeed();
    }

    /** Edits from the profile menu (EditProfilePayload) as the server handles them, including the ownership check. */
    @GameTest(template = TestCrafters.ARENA)
    public static void menuEditsProfiles(GameTestHelper helper) {
        ConnectedCrafterGroup big = TestCrafters.build(helper, Direction.NORTH, TestCrafters.positionsXY(ProfileCodecGameTests.LAYOUT_21, 3));
        ConnectedCrafterGroup small = TestCrafters.build(helper, Direction.NORTH, TestCrafters.positionsXY(ProfileCodecGameTests.LAYOUT_3X3, 5));
        ServerPlayer owner = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "bcac-owner"));
        ServerPlayer stranger = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "bcac-stranger"));
        owner.moveTo(Vec3.atCenterOf(helper.absolutePos(new BlockPos(3, 3, 1))));
        ProfileManager manager = ProfileManager.get(owner.server);
        try {
            ProfileService.handleCrafterClick(owner, big.controller(), false);
            ProfileService.handleCrafterClick(owner, small.controller(), false);
            UUID id = manager.byOwner(owner.getUUID()).get(0).id();

            ProfileService.handleEdit(owner, new EditProfilePayload(id, EditProfilePayload.Action.RENAME, -1, "  Hauptanlage  "));
            ProfileService.handleEdit(owner, new EditProfilePayload(id, EditProfilePayload.Action.SET_ADDRESS, 0, "crafter_gross"));
            ProfileService.handleEdit(owner, new EditProfilePayload(id, EditProfilePayload.Action.RENAME, -1, "   "));
            ProfileService.handleEdit(stranger, new EditProfilePayload(id, EditProfilePayload.Action.RENAME, -1, "Fremd"));
            ProfileService.handleEdit(stranger, new EditProfilePayload(id, EditProfilePayload.Action.DELETE, -1, ""));
            CrafterProfile profile = manager.byId(id).orElseThrow();
            helper.assertTrue(profile.name().equals("Hauptanlage"), "Rename failed or was overwritten: " + profile.name());
            helper.assertTrue(profile.regions().get(0).address().equals("crafter_gross"), "Address not set: " + profile.regions().get(0));

            ProfileService.handleEdit(owner, new EditProfilePayload(id, EditProfilePayload.Action.REMOVE_REGION, 1, ""));
            helper.assertTrue(manager.byId(id).orElseThrow().regions().size() == 1, "Region not removed");
            ProfileService.handleEdit(owner, new EditProfilePayload(id, EditProfilePayload.Action.DELETE, -1, ""));
            helper.assertTrue(manager.byId(id).isEmpty(), "Profile not deleted");
        } finally {
            manager.byOwner(owner.getUUID()).forEach(p -> manager.remove(p.id()));
        }
        helper.succeed();
    }
}
