package github.kasuminova.novaeng.client.geometry.vertex;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.model.pipeline.LightUtil;
import net.minecraftforge.client.model.pipeline.UnpackedBakedQuad;

import java.util.Arrays;

/**
 * A snapshot from a proven private producer, never a replacement for an external float tree.
 */
public record OwnedUnpackedQuadRecord(LosslessFloatRecord data, int tint, EnumFacing face,
                                      TextureAtlasSprite sprite, boolean diffuse, VertexFormat format) {
    @Override
    public boolean equals(final Object other) {
        return other == this || other instanceof OwnedUnpackedQuadRecord(
            LosslessFloatRecord data1, int tint1, EnumFacing face1, TextureAtlasSprite sprite1, boolean diffuse1,
            VertexFormat format1
        )
            && tint == tint1 && face == face1 && sprite == sprite1
            && diffuse == diffuse1 && format == format1 && data.equals(data1);
    }

    @Override
    public int hashCode() {
        int hash = data.hashCode();
        hash = 31 * hash + tint;
        hash = 31 * hash + (face == null ? 0 : face.hashCode());
        hash = 31 * hash + System.identityHashCode(sprite);
        hash = 31 * hash + Boolean.hashCode(diffuse);
        return 31 * hash + System.identityHashCode(format);
    }

    public UnpackedBakedQuad materialize() {
        return new UnpackedBakedQuad(data.materialize(), tint, face, sprite, diffuse, format);
    }

    public int[] packed() {
        final int[] target = new int[format.getSize()];
        packInto(target);
        return target;
    }

    public void packInto(final int[] target) {
        Arrays.fill(target, 0, format.getSize(), 0);
        final float[] element = new float[4];
        for (int v = 0; v < 4; v++) {
            for (int e = 0; e < format.getElementCount(); e++) {
                data.elementInto(v, e, element);
                LightUtil.pack(element, target, format, v, e);
            }
        }
    }
}
