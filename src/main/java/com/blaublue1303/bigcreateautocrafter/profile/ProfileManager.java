package com.blaublue1303.bigcreateautocrafter.profile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.blaublue1303.bigcreateautocrafter.BigCreateAutoCrafter;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Server-side storage of all {@link CrafterProfile}s of a world (saved in the overworld's data storage,
 * file {@code data/bigcreateautocrafter_profiles.dat}). Profiles that fail to decode are logged and skipped.
 */
public class ProfileManager extends SavedData {
    public static final String DATA_NAME = BigCreateAutoCrafter.MODID + "_profiles";
    private static final String PROFILES_KEY = "profiles";
    private static final SavedData.Factory<ProfileManager> FACTORY =
            new SavedData.Factory<>(ProfileManager::new, ProfileManager::load, null);

    private final Map<UUID, CrafterProfile> profiles = new LinkedHashMap<>();

    public static ProfileManager get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    public Optional<CrafterProfile> byId(UUID id) {
        return Optional.ofNullable(profiles.get(id));
    }

    public List<CrafterProfile> byOwner(UUID owner) {
        return profiles.values().stream().filter(p -> p.owner().equals(owner)).toList();
    }

    /** Adds or replaces the profile with the same id. */
    public void put(CrafterProfile profile) {
        profiles.put(profile.id(), profile);
        setDirty();
    }

    public void remove(UUID id) {
        if (profiles.remove(id) != null) {
            setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put(PROFILES_KEY, CrafterProfile.CODEC.listOf()
                .encodeStart(NbtOps.INSTANCE, List.copyOf(profiles.values()))
                .getOrThrow());
        return tag;
    }

    private static ProfileManager load(CompoundTag tag, HolderLookup.Provider registries) {
        ProfileManager manager = new ProfileManager();
        for (Tag entry : tag.getList(PROFILES_KEY, Tag.TAG_COMPOUND)) {
            CrafterProfile.CODEC.parse(NbtOps.INSTANCE, entry)
                    .resultOrPartial(error -> BigCreateAutoCrafter.LOGGER.error("Skipping invalid crafter profile: {}", error))
                    .ifPresent(profile -> manager.profiles.put(profile.id(), profile));
        }
        return manager;
    }
}
