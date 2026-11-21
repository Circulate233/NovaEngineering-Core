package github.kasuminova.novaeng.client.geometry.vertex;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraftforge.client.model.pipeline.IVertexConsumer;
import net.minecraftforge.client.model.pipeline.UnpackedBakedQuad;
import org.jspecify.annotations.NonNull;

/**
 * Private producer token. Neither public model calls nor trusted renderers receive this token.
 */
public final class OwnedUnpackedQuadView extends UnpackedBakedQuad {
    private static final VertexFormat EMPTY_FORMAT = new VertexFormat();
    private static final float[][][] EMPTY_DATA = new float[0][][];
    private final OwnedUnpackedQuadRecord record;

    public OwnedUnpackedQuadView(final OwnedUnpackedQuadRecord record) {
        // Builder.build's return descriptor requires UnpackedBakedQuad. No published geometry is
        // stored in its compatibility fields: this token stays inside the private fluid producer.
        super(EMPTY_DATA, record.tint(), record.face(), record.sprite(), record.diffuse(), EMPTY_FORMAT);
        this.record = record;
    }

    public UnpackedBakedQuad materialize() {
        return record.materialize();
    }

    public BakedQuad read() {
        return new ReadQuad(record);
    }

    @Override
    public @NonNull VertexFormat getFormat() {
        return record.format();
    }

    @Override
    public int @NonNull [] getVertexData() {
        // No second, permanent packed copy accompanies the compressed float record.
        return record.packed();
    }

    @Override
    public void pipe(final @NonNull IVertexConsumer consumer) {
        // Unknown consumers may retain or mutate each source array. Give them a genuine source.
        materialize().pipe(consumer);
    }

    /**
     * A transient real packed quad, safe for third-party constructor-injected lighting views.
     */
    private static final class ReadQuad extends BakedQuad {
        private final OwnedUnpackedQuadRecord record;
        private UnpackedBakedQuad floatSource;

        private ReadQuad(final OwnedUnpackedQuadRecord record) {
            super(record.packed(), record.tint(), record.face(), record.sprite(), record.diffuse(), record.format());
            this.record = record;
        }

        @Override
        public void pipe(final @NonNull IVertexConsumer consumer) {
            if (floatSource == null) floatSource = record.materialize();
            floatSource.pipe(consumer);
        }
    }
}
