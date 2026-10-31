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
 * <p>Example: a large setup with 21 crafters is one region, a separate 3×3 group next to it is a second
 * region of the same profile. (A region always being a whole Create group follows risk R12 in docs/04-risks.md;
 * sub-areas of one group can be added later as an optional field without breaking saved data.)
 *
 * @param name       display name chosen by the player
 * @param controller position of the group's controller crafter (Create {@code ConnectedInput} controller)
 * @param facing     horizontal facing of the crafters in the group
 * @param layout     rows from top to bottom in Create's inventory order; {@code '#'} = crafter, {@code '.'} = gap.
 *                   Column direction relative to the viewer still has to be verified in-game (docs/06-todo.md, T5).
 */
public record CrafterRegion(String name, BlockPos controller, Direction facing, List<String> layout) {
    public static final int MAX_NAME_LENGTH = 32;
    public static final char CRAFTER = '#';
    public static final char GAP = '.';

    public static final Codec<CrafterRegion> CODEC = RecordCodecBuilder.<CrafterRegion>create(i -> i.group(
            Codec.STRING.fieldOf("name").forGetter(CrafterRegion::name),
            BlockPos.CODEC.fieldOf("controller").forGetter(CrafterRegion::controller),
            Direction.CODEC.fieldOf("facing").forGetter(CrafterRegion::facing),
            Codec.STRING.listOf().fieldOf("layout").forGetter(CrafterRegion::layout)
    ).apply(i, CrafterRegion::new)).validate(CrafterRegion::validate);

    public static final StreamCodec<ByteBuf, CrafterRegion> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, CrafterRegion::name,
            BlockPos.STREAM_CODEC, CrafterRegion::controller,
            Direction.STREAM_CODEC, CrafterRegion::facing,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), CrafterRegion::layout,
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

    /** @return number of crafters in this region */
    public int crafterCount() {
        int count = 0;
        for (String row : layout) {
            for (int x = 0; x < row.length(); x++) {
                if (row.charAt(x) == CRAFTER) {
                    count++;
                }
            }
        }
        return count;
    }

    public static DataResult<CrafterRegion> validate(CrafterRegion region) {
        if (region.name().isBlank() || region.name().length() > MAX_NAME_LENGTH) {
            return DataResult.error(() -> "Region name must be 1-" + MAX_NAME_LENGTH + " characters: '" + region.name() + "'");
        }
        if (!region.facing().getAxis().isHorizontal()) {
            return DataResult.error(() -> "Region facing must be horizontal: " + region.facing());
        }
        if (region.layout().isEmpty() || region.width() == 0) {
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
