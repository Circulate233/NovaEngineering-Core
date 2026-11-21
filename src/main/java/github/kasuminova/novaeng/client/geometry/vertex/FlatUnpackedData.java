package github.kasuminova.novaeng.client.geometry.vertex;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;

/**
 * Each builder owns its working data; materialization permanently preserves the public alias.
 */
public final class FlatUnpackedData {
    private final int elements;
    private float[] flat;
    private float[][][] exposed;

    public FlatUnpackedData(final int elements) {
        this.elements = elements;
        this.flat = new float[Math.multiplyExact(elements, 16)];
    }

    public void put(final int vertex, final int element, final float[] data) {
        final int length = data.length;
        if (vertex < 0 || vertex >= 4) throw new ArrayIndexOutOfBoundsException(vertex);
        if (element < 0 || element >= elements) throw new ArrayIndexOutOfBoundsException(element);
        for (int c = 0; c < 4; c++) set(vertex, element, c, c < length ? data[c] : 0);
    }

    public float[][][] materialize() {
        if (exposed == null) {
            exposed = new float[4][elements][4];
            for (int v = 0; v < 4; v++) {
                for (int e = 0; e < elements; e++) {
                    System.arraycopy(flat, ((v * elements + e) << 2), exposed[v][e], 0, 4);
                }
            }
            flat = null;
        }
        return exposed;
    }

    public LosslessFloatRecord snapshot() {
        if (exposed == null) return LosslessFloatRecord.capture(flat, elements);
        return LosslessFloatRecord.captureTree(exposed, elements);
    }

    private float get(final int vertex, final int element, final int component) {
        return exposed == null ? flat[((vertex * elements + element) << 2) + component]
            : exposed[vertex][element][component];
    }

    private void set(final int vertex, final int element, final int component, final float value) {
        if (exposed == null) flat[((vertex * elements + element) << 2) + component] = value;
        else exposed[vertex][element][component] = value;
    }

    /**
     * Forge's exact operation order, including all four UV components and repeated contractions.
     */
    public void contractUVs(final VertexFormat format, final TextureAtlasSprite texture) {
        final float tX = texture.getIconWidth() / (texture.getMaxU() - texture.getMinU());
        final float tY = texture.getIconHeight() / (texture.getMaxV() - texture.getMinV());
        final float tS = Math.max(tX, tY);
        final float ep = 1f / (tS * 0x100);
        final float eps = 1f / 0x100;
        int uve = 0;
        while (uve < format.getElementCount()) {
            final VertexFormatElement e = format.getElement(uve);
            if (e.getUsage() == VertexFormatElement.EnumUsage.UV && e.getIndex() == 0) break;
            uve++;
        }
        if (uve == format.getElementCount()) {
            throw new IllegalStateException("Can't contract UVs: format doesn't contain UVs");
        }
        float center0 = 0, center1 = 0, center2 = 0, center3 = 0;
        for (int v = 0; v < 4; v++) {
            center0 += get(v, uve, 0) / 4;
            center1 += get(v, uve, 1) / 4;
            center2 += get(v, uve, 2) / 4;
            center3 += get(v, uve, 3) / 4;
        }
        for (int v = 0; v < 4; v++) {
            for (int c = 0; c < 4; c++) {
                final float center = switch (c) {
                    case 0 -> center0;
                    case 1 -> center1;
                    case 2 -> center2;
                    default -> center3;
                };
                final float uo = get(v, uve, c);
                float un = uo * (1 - eps) + center * eps;
                final float ud = uo - un;
                float aud = ud;
                if (aud < 0) aud = -aud;
                if (aud < ep) {
                    float udc = uo - center;
                    if (udc < 0) udc = -udc;
                    if (udc < 2 * ep) un = (uo + center) / 2;
                    else un = uo + (ud < 0 ? ep : -ep);
                }
                set(v, uve, c, un);
            }
        }
    }
}
