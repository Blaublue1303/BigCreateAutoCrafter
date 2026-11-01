package com.blaublue1303.bigcreateautocrafter.client;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.blaublue1303.bigcreateautocrafter.BigCreateAutoCrafter;
import com.blaublue1303.bigcreateautocrafter.profile.CrafterProfile;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * Which crafter setup the JEI "+" in the Stock Keeper uses: Create's standard behaviour, a profile with automatic
 * region choice, or one specific region. Kept for the whole session.
 */
public final class ClientSelection {
    static final String KEY = BigCreateAutoCrafter.MODID + ".selector.";

    @Nullable
    private static UUID selectedProfile;
    private static int selectedRegion = -1;

    private ClientSelection() {
    }

    /**
     * @param profile     null = Create standard (3×3, unchanged behaviour)
     * @param regionIndex -1 = automatic (smallest fitting region)
     */
    public record Option(@Nullable CrafterProfile profile, int regionIndex) {
        public static final Option STANDARD = new Option(null, -1);

        public boolean isStandard() {
            return profile == null;
        }

        public Component label() {
            if (profile == null) {
                return Component.translatable(KEY + "standard");
            }
            if (regionIndex < 0) {
                return Component.translatable(KEY + "auto", profile.name());
            }
            return Component.translatable(KEY + "region", profile.name(), profile.regions().get(regionIndex).name());
        }
    }

    /** Standard, then per profile of this dimension: Auto and (if it has several regions) each region. */
    public static List<Option> options(ResourceKey<Level> dimension) {
        List<Option> options = new ArrayList<>();
        options.add(Option.STANDARD);
        for (CrafterProfile profile : ClientProfileCache.all()) {
            if (!profile.dimension().equals(dimension)) {
                continue;
            }
            options.add(new Option(profile, -1));
            if (profile.regions().size() > 1) {
                for (int i = 0; i < profile.regions().size(); i++) {
                    options.add(new Option(profile, i));
                }
            }
        }
        return options;
    }

    /** The selected option, or Standard if the selected profile/region no longer exists in this dimension. */
    public static Option current(ResourceKey<Level> dimension) {
        if (selectedProfile == null) {
            return Option.STANDARD;
        }
        for (CrafterProfile profile : ClientProfileCache.all()) {
            if (profile.id().equals(selectedProfile) && profile.dimension().equals(dimension)) {
                if (selectedRegion < 0) {
                    return new Option(profile, -1);
                }
                boolean listed = profile.regions().size() > 1 && selectedRegion < profile.regions().size();
                return listed ? new Option(profile, selectedRegion) : Option.STANDARD;
            }
        }
        return Option.STANDARD;
    }

    public static void select(Option option) {
        selectedProfile = option.profile() == null ? null : option.profile().id();
        selectedRegion = option.regionIndex();
    }
}
