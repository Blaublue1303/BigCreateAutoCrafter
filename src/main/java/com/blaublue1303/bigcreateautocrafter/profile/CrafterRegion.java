package com.blaublue1303.bigcreateautocrafter.profile;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * One craftable area of a {@link CrafterProfile}: exactly one connected Create Mechanical Crafter group.
 *
 * <p>Example: a large setup with 21 crafters is one region, a separate 3×3 group next to it is a second region of
 * the same profile. Small recipes also work inside a large region (Create starts a crafter group that was filled by a
 * package like a redstone start, empty crafters allowed), so a separate 3×3 group is optional.
 *
 * @param name       display name
 * @param controller position of the group's controller crafter (Create {@code ConnectedInput} controller)
 * @param facing     horizontal facing of the crafters in the group
 * @param layout     rows from top to bottom; {@code '#'} = crafter, {@code '.'} = gap. Reading the {@code '#'} cells row
 *                   by row gives exactly Create's crafter inventory order ({@code ConnectedInput.getInventories}), which
 *                   is the order Create's crafter unpacking handler fills recipe pattern slots in.
 * @param address    Create package address of the packager feeding this crafter group; empty = not set
 */
public record CrafterRegion(String name, BlockPos controller, Direction facing, List<String> layout, String address) {
    public static final int MAX_NAME_LENGTH = 32;
    public static final int MAX_ADDRESS_LENGTH = 64;
    public static final char CRAFTER = '#';
    public static final char GAP = '.';

    public static final Codec<CrafterRegion> CODEC = RecordCodecBuilder.<CrafterRegion>create(i -> i.group(
            Codec.STRING.fieldOf("name").forGetter(CrafterRegion::name),
            BlockPos.CODEC.fieldOf("controller").forGetter(CrafterRegion::controller),
            Direction.CODEC.fieldOf("facing").forGetter(CrafterRegion::facing),
            Codec.STRING.listOf().fieldOf("layout").forGetter(CrafterRegion::layout),
            Codec.STRING.optionalFieldOf("address", "").forGetter(CrafterRegion::address)
    ).apply(i, CrafterRegion::new)).validate(CrafterRegion::validate);

    public static final StreamCodec<ByteBuf, CrafterRegion> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, CrafterRegion::name,
            BlockPos.STREAM_CODEC, CrafterRegion::controller,
            Direction.STREAM_CODEC, CrafterRegion::facing,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), CrafterRegion::layout,
            ByteBufCodecs.STRING_UTF8, CrafterRegion::address,
            CrafterRegion::new);

    public CrafterRegion {
        controller = controller.immutable();
        layout = List.copyOf(layout);
    }

    public int width() {
        return layout.isEmpty() ? 0 : layout.get(0).length();
    }

    public int height() {
        return layout.size();
    }

    /** @return {@code true} if the cell (column {@code x}, row {@code y}, both 0-based from top-left) holds a crafter */
    public boolean hasCrafterAt(int x, int y) {
        return y >= 0 && y < height() && x >= 0 && x < width() && layout.get(y).charAt(x) == CRAFTER;
    }

    /** @return number of crafters in this region (= length of a recipe pattern for this region) */
    public int crafterCount() {
        return countCrafters(layout);
    }

    /** The address for display; a dash when none is set. */
    public String displayAddress() {
        return address.isEmpty() ? "—" : address;
    }

    public CrafterRegion withAddress(String newAddress) {
        return new CrafterRegion(name, controller, facing, layout, newAddress);
    }

    /** Short label like {@code 5×5 (21)} used for auto-generated names. */
    public static String sizeLabel(List<String> layout) {
        int width = layout.isEmpty() ? 0 : layout.get(0).length();
        int height = layout.size();
        int crafters = countCrafters(layout);
        String size = width + "×" + height;
        return crafters == width * height ? size : size + " (" + crafters + ")";
    }

    private static int countCrafters(List<String> layout) {
        int count = 0;
        for (String row : layout) {
            count += (int) row.chars().filter(c -> c == CRAFTER).count();
        }
        return count;
    }

    public static DataResult<CrafterRegion> validate(CrafterRegion region) {
        if (region.name().isBlank() || region.name().length() > MAX_NAME_LENGTH) {
            return DataResult.error(() -> "Region name must be 1-" + MAX_NAME_LENGTH + " characters: '" + region.name() + "'");
        }
        if (region.address().length() > MAX_ADDRESS_LENGTH) {
            return DataResult.error(() -> "Region address must be at most " + MAX_ADDRESS_LENGTH + " characters");
        }
        if (!region.facing().getAxis().isHorizontal()) {
            return DataResult.error(() -> "Region facing must be horizontal: " + region.facing());
        }
        if (region.width() == 0) {
            return DataResult.error(() -> "Region layout must not be empty");
        }
        int width = region.width();
        for (String row : region.layout()) {
            if (row.length() != width) {
                return DataResult.error(() -> "Region layout rows must all have length " + width + ": " + region.layout());
            }
            for (int x = 0; x < row.length(); x++) {
                char c = row.charAt(x);
                if (c != CRAFTER && c != GAP) {
                    return DataResult.error(() -> "Region layout may only contain '" + CRAFTER + "' and '" + GAP + "': " + region.layout());
                }
            }
        }
        if (region.crafterCount() == 0) {
            return DataResult.error(() -> "Region layout must contain at least one crafter");
        }
        return DataResult.success(region);
    }
}
