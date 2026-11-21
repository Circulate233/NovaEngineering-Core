package github.kasuminova.novaeng.client.geometry.item;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.model.pipeline.IVertexConsumer;
import net.minecraftforge.client.model.pipeline.LightUtil;
import net.minecraftforge.client.model.pipeline.UnpackedBakedQuad;
import org.jspecify.annotations.NonNull;

import java.util.Arrays;

/**
 * A single trusted list's decode cursor. Never returned by the public model getQuads method.
 */
final class ItemQuadView extends BakedQuad {
    private static final EnumFacing[] DIRECTIONS = {EnumFacing.UP, EnumFacing.DOWN, EnumFacing.WEST, EnumFacing.EAST};
    private final float[] coordinates = new float[20];
    private final float[] component = new float[4];
    private final VertexFormat itemFormat;
    private TextureAtlasSprite currentSprite;
    private EnumFacing currentFace;
    private int currentTint;
    private boolean dirty;

    ItemQuadView() {
        this(DefaultVertexFormats.ITEM);
    }

    ItemQuadView(final VertexFormat format) {
        super(new int[format.getSize()], 0, EnumFacing.SOUTH, null, true, format);
        itemFormat = format;
    }

    void decode(final ItemContour contour, final TextureAtlasSprite sprite, final int tint, final int index) {
        currentSprite = sprite;
        currentTint = tint;
        dirty = true;
        if (index >= contour.size()) {
            final boolean south = index == contour.size() + 1;
            currentFace = south ? EnumFacing.SOUTH : EnumFacing.NORTH;
            for (int v = 0; v < 4; v++) {
                final int x = (south ? v == 1 || v == 2 : v >= 2) ? 1 : 0;
                final int y = (south ? v >= 2 : v == 1 || v == 2) ? 1 : 0;
                vertex(v, x, y, south ? 8.5f / 16f : 7.5f / 16f,
                    x == 0 ? sprite.getMinU() : sprite.getMaxU(), y == 0 ? sprite.getMaxV() : sprite.getMinV());
            }
        } else {
            final int bits = contour.bits();
            final int mask = (1 << bits) - 1;
            final int word = contour.word(index);
            final int direction = word >>> (3 * bits);
            final int fixed = ((word >>> (2 * bits)) & mask) + (direction & 1);
            final int start = (word >>> bits) & mask;
            final int length = (word & mask) + 1;
            final EnumFacing side = DIRECTIONS[direction];
            currentFace = direction < 2 ? side.getOpposite() : side;
            final int u = direction < 2 ? start : fixed;
            final int v = direction < 2 ? fixed : start;
            final float x0 = (float) u / contour.width();
            final float y0 = (float) v / contour.height();
            final float x1 = direction < 2 ? (float) (u + length) / contour.width() : x0;
            final float y1 = direction < 2 ? y0 : (float) (v + length) / contour.height();
            final float z0 = direction == 1 || direction == 2 ? 8.5f / 16f : 7.5f / 16f;
            final float z1 = direction == 1 || direction == 2 ? 7.5f / 16f : 8.5f / 16f;
            final float dx = side.getXOffset() * 1e-2f / contour.width();
            final float dy = side.getYOffset() * 1e-2f / contour.height();
            final float u0 = sprite.getInterpolatedU(16f * (x0 - dx));
            final float u1 = sprite.getInterpolatedU(16f * (x1 - dx));
            final float v0 = sprite.getInterpolatedV(16f * (1f - y0 - dy));
            final float v1 = sprite.getInterpolatedV(16f * (1f - y1 - dy));
            vertex(0, x0, y0, z0, u0, v0);
            vertex(1, x1, y1, z0, u1, v1);
            vertex(2, x1, y1, z1, u1, v1);
            vertex(3, x0, y0, z1, u0, v0);
        }
    }

    private void vertex(final int vertex, final float x, final float y, final float z, final float u, final float v) {
        final int offset = vertex * 5;
        coordinates[offset] = x;
        coordinates[offset + 1] = y;
        coordinates[offset + 2] = z;
        coordinates[offset + 3] = u;
        coordinates[offset + 4] = v;
    }

    private void element(final float[] out, final int vertex, final int element) {
        Arrays.fill(out, 0f);
        final int offset = vertex * 5;
        switch (itemFormat.getElement(element).getUsage()) {
            case POSITION -> {
                out[0] = coordinates[offset];
                out[1] = coordinates[offset + 1];
                out[2] = coordinates[offset + 2];
                out[3] = 1f;
            }
            case COLOR -> Arrays.fill(out, 1f);
            case NORMAL -> {
                out[0] = currentFace.getXOffset();
                out[1] = currentFace.getYOffset();
                out[2] = currentFace.getZOffset();
            }
            case UV -> {
                if (itemFormat.getElement(element).getIndex() == 0) {
                    out[0] = coordinates[offset + 3];
                    out[1] = coordinates[offset + 4];
                    out[3] = 1f;
                }
            }
            default -> {
            }
        }
    }

    @Override
    public int @NonNull [] getVertexData() {
        final int[] packed = super.getVertexData();
        if (!dirty) return packed;
        final VertexFormat format = itemFormat;
        if (format.getSize() == 28 && format.getElementCount() == 5
            && format.getElement(0).equals(DefaultVertexFormats.POSITION_3F)
            && format.getElement(1).equals(DefaultVertexFormats.COLOR_4UB)
            && format.getElement(2).equals(DefaultVertexFormats.TEX_2F)
            && format.getElement(3).equals(DefaultVertexFormats.NORMAL_3B)
            && format.getElement(4).equals(DefaultVertexFormats.PADDING_1B)) {
            final int normal = (currentFace.getXOffset() * 127 & 255)
                | (currentFace.getYOffset() * 127 & 255) << 8
                | (currentFace.getZOffset() * 127 & 255) << 16;
            for (int v = 0; v < 4; v++) {
                final int source = v * 5, destination = v * 7;
                packed[destination] = Float.floatToRawIntBits(coordinates[source]);
                packed[destination + 1] = Float.floatToRawIntBits(coordinates[source + 1]);
                packed[destination + 2] = Float.floatToRawIntBits(coordinates[source + 2]);
                packed[destination + 3] = -1;
                packed[destination + 4] = Float.floatToRawIntBits(coordinates[source + 3]);
                packed[destination + 5] = Float.floatToRawIntBits(coordinates[source + 4]);
                packed[destination + 6] = normal;
            }
            dirty = false;
            return packed;
        }
        Arrays.fill(packed, 0);
        for (int v = 0; v < 4; v++)
            for (int e = 0; e < itemFormat.getElementCount(); e++) {
                element(component, v, e);
                LightUtil.pack(component, packed, itemFormat, v, e);
            }
        dirty = false;
        return packed;
    }

    BakedQuad materialize() {
        final UnpackedBakedQuad.Builder builder = new UnpackedBakedQuad.Builder(itemFormat);
        builder.setTexture(currentSprite);
        builder.setQuadTint(currentTint);
        builder.setQuadOrientation(currentFace);
        for (int v = 0; v < 4; v++)
            for (int e = 0; e < itemFormat.getElementCount(); e++) {
                element(component, v, e);
                builder.put(e, component);
            }
        return builder.build();
    }

    @Override
    public void pipe(final IVertexConsumer consumer) {
        consumer.setTexture(currentSprite);
        consumer.setQuadOrientation(currentFace);
        if (currentTint != -1) consumer.setQuadTint(currentTint);
        consumer.setApplyDiffuseLighting(true);
        final VertexFormat from = consumer.getVertexFormat();
        final int count = itemFormat.getElementCount();
        final int[] mapping = LightUtil.mapFormats(from, itemFormat);
        // Consumers can retain or alter arrays. Even this trusted cursor never passes its scratch component array.
        final float[][] values = new float[count][];
        for (int v = 0; v < 4; v++) {
            Arrays.fill(values, null);
            for (int e = 0; e < from.getElementCount(); e++) {
                final int mapped = mapping[e];
                if (mapped == count) {
                    consumer.put(e);
                } else {
                    if (values[mapped] == null) {
                        values[mapped] = new float[4];
                        element(values[mapped], v, mapped);
                    }
                    consumer.put(e, values[mapped]);
                }
            }
        }
    }

    @Override
    public int getTintIndex() {
        return currentTint;
    }

    @Override
    public boolean hasTintIndex() {
        return currentTint != -1;
    }

    @Override
    public @NonNull EnumFacing getFace() {
        return currentFace;
    }

    @Override
    public @NonNull TextureAtlasSprite getSprite() {
        return currentSprite;
    }
}
