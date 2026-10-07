package github.kasuminova.novaeng.client.gl;

import it.unimi.dsi.fastutil.ints.IntArrayList;

/**
 * Thread-local implementation of {@link GenericAttributeState} using exact raw float bits.
 */
public final class GenericAttributeStateImpl implements GenericAttributeState {

    private static final GenericAttributeState INSTANCE = new GenericAttributeStateImpl();
    private static final int COLOR_ARRAY = 1;
    private static final int SECONDARY_UV_ARRAY = 2;
    private static final int NO_DRAW_FORMAT = -1;

    private final ThreadLocal<ContextState> contexts = ThreadLocal.withInitial(ContextState::new);

    private GenericAttributeStateImpl() {
    }

    /** Returns the shared facade whose actual state remains isolated per GL thread. */
    public static GenericAttributeState instance() {
        return INSTANCE;
    }

    @Override
    public void pushDrawFormat(final boolean colorArrayEnabled, final boolean secondaryUvArrayEnabled) {
        this.contexts.get().drawFormats.add((colorArrayEnabled ? COLOR_ARRAY : 0)
            | (secondaryUvArrayEnabled ? SECONDARY_UV_ARRAY : 0));
    }

    @Override
    public void finishDrawFormat(final boolean colorArrayEnabled, final boolean secondaryUvArrayEnabled) {
        final ContextState context = this.contexts.get();
        if (colorArrayEnabled) {
            context.color.known = false;
        }
        if (secondaryUvArrayEnabled) {
            context.secondaryUv.known = false;
        }
        context.popDrawFormat();
    }

    @Override
    public boolean shouldUploadColor(final int index,
                                     final float x,
                                     final float y,
                                     final float z,
                                     final float w) {
        final ContextState context = this.contexts.get();
        context.color.bindIndex(index);
        final int format = context.peekDrawFormat();
        return format == NO_DRAW_FORMAT || (format & COLOR_ARRAY) == 0 && !context.color.matches(x, y, z, w);
    }

    @Override
    public boolean shouldUploadSecondaryUv(final int index,
                                           final float x,
                                           final float y,
                                           final float z,
                                           final float w) {
        final ContextState context = this.contexts.get();
        context.secondaryUv.bindIndex(index);
        final int format = context.peekDrawFormat();
        return format == NO_DRAW_FORMAT || (format & SECONDARY_UV_ARRAY) == 0 && !context.secondaryUv.matches(x, y, z, w);
    }

    @Override
    public void recordAttribute(final int index,
                                final float x,
                                final float y,
                                final float z,
                                final float w) {
        final ContextState context = this.contexts.get();
        context.color.recordIfIndex(index, x, y, z, w);
        context.secondaryUv.recordIfIndex(index, x, y, z, w);
    }

    @Override
    public void invalidateAttributes() {
        final ContextState context = this.contexts.get();
        context.color.invalidate();
        context.secondaryUv.invalidate();
        context.drawFormats.clear();
    }

    @Override
    public boolean shouldBindVertexArray(final int normalizedVertexArray) {
        final ContextState context = this.contexts.get();
        return !context.vertexArrayKnown || context.vertexArray != normalizedVertexArray;
    }

    @Override
    public void recordBoundVertexArray(final int normalizedVertexArray) {
        final ContextState context = this.contexts.get();
        context.vertexArray = normalizedVertexArray;
        context.vertexArrayKnown = true;
    }

    @Override
    public void invalidateVertexArray() {
        this.contexts.get().vertexArrayKnown = false;
    }

    @Override
    public void invalidateAll() {
        final ContextState context = this.contexts.get();
        context.color.invalidate();
        context.secondaryUv.invalidate();
        context.drawFormats.clear();
        context.vertexArrayKnown = false;
    }

    private static final class ContextState {
        private final AttributeValue color = new AttributeValue();
        private final AttributeValue secondaryUv = new AttributeValue();
        private final IntArrayList drawFormats = new IntArrayList();
        private boolean vertexArrayKnown;
        private int vertexArray;

        private int peekDrawFormat() {
            return this.drawFormats.isEmpty() ? NO_DRAW_FORMAT : this.drawFormats.getInt(this.drawFormats.size() - 1);
        }

        private void popDrawFormat() {
            if (!this.drawFormats.isEmpty()) {
                this.drawFormats.removeInt(this.drawFormats.size() - 1);
            }
        }
    }

    private static final class AttributeValue {
        private boolean indexKnown;
        private int index;
        private boolean known;
        private int x;
        private int y;
        private int z;
        private int w;

        private void bindIndex(final int index) {
            if (!this.indexKnown || this.index != index) {
                this.index = index;
                this.indexKnown = true;
                this.known = false;
            }
        }

        private boolean matches(final float x, final float y, final float z, final float w) {
            return this.known
                && this.x == Float.floatToRawIntBits(x)
                && this.y == Float.floatToRawIntBits(y)
                && this.z == Float.floatToRawIntBits(z)
                && this.w == Float.floatToRawIntBits(w);
        }

        private void recordIfIndex(final int index,
                                   final float x,
                                   final float y,
                                   final float z,
                                   final float w) {
            if (!this.indexKnown || this.index != index) {
                return;
            }
            this.x = Float.floatToRawIntBits(x);
            this.y = Float.floatToRawIntBits(y);
            this.z = Float.floatToRawIntBits(z);
            this.w = Float.floatToRawIntBits(w);
            this.known = true;
        }

        private void invalidate() {
            this.indexKnown = false;
            this.known = false;
        }
    }

}
