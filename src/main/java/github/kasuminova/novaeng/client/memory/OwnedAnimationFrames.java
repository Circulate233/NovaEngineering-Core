package github.kasuminova.novaeng.client.memory;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;

import java.lang.ref.WeakReference;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.function.BiFunction;

/** Public reads publish stable arrays; only private vanilla animation reads borrow the decode window. */
public final class OwnedAnimationFrames extends AbstractList<int[][]> implements RandomAccess {
    private Ownership ownership;
    private final ObjectArrayList<Frame> frames = new ObjectArrayList<>(0);
    private List<int[][]> internal;
    private Window first;
    private Window second;

    public OwnedAnimationFrames() {
    }

    private OwnedAnimationFrames(final Ownership ownership) {
        this.ownership = ownership;
        ownership.lists.add(new WeakReference<>(this));
    }

    private Ownership owner() {
        if (ownership == null) {
            ownership = new Ownership();
            ownership.lists.add(new WeakReference<>(this));
        }
        return ownership;
    }

    /**
     * Takes ownership of the completed mipmap arrays, including reused outer arrays.
     */
    public OwnedAnimationFrames mipmaps(final List<int[][]> generated) {
        final Ownership owner = owner();
        synchronized (owner) {
            final OwnedAnimationFrames next = new OwnedAnimationFrames(owner);
            next.frames.ensureCapacity(generated.size());
            for (final int[][] frame : generated) next.frames.add(owner.frame(frame, false));
            return next;
        }
    }

    /**
     * Loading owns the list exclusively; later readers hold readLock() for the entire borrow.
     */
    public List<int[][]> internalView() {
        if (internal == null) internal = new InternalView();
        return internal;
    }

    /**
     * Hold this monitor until all borrowed pixels have been consumed, including an animation upload.
     */
    public Object readLock() {
        return owner();
    }

    /**
     * Immediate-copy callers may retain source arrays, but must never race a borrowed decode window.
     */
    public int[][] forImmediateCopy(final int index) {
        final Ownership owner = owner();
        synchronized (owner) {
            final Frame frame = frames.get(index);
            return frame == null ? null : frame.source != null ? frame.source : get(index);
        }
    }

    /**
     * Edits the actual base mip without publishing an outer array or a decode window.
     */
    public <S> S editBaseLevels(S state, final BiFunction<S, int[], S> editor) {
        if (frames.isEmpty()) return state;
        final Ownership owner = owner();
        synchronized (owner) {
            owner.activeEdits++;
            try {
                final int size = frames.size();
                for (var i = 0; i < size; i++) {
                    Frame frame = frames.get(i);
                    if (frame == null || frame.mips.length == 0) continue;
                    // A caller holding a published outer array may have replaced its base level.
                    final int[] pixels;
                    if (frame.source != null) pixels = frame.source[0];
                    else if (frame.mips[0] != null) pixels = frame.mips[0].stable(owner);
                    else continue;
                    if (pixels != null) state = editor.apply(state, pixels);
                }
                return state;
            } finally {
                if (--owner.activeEdits == 0 && owner.packAfterEdit) {
                    owner.packAfterEdit = false;
                    owner.pack(true, null);
                }
            }
        }
    }

    @Override
    public int size() {
        return frames.size();
    }

    @Override
    public int[][] get(final int index) {
        final Frame frame = frames.get(index);
        if (frame == null) return null;
        final Ownership owner = owner();
        synchronized (owner) {
            final int[][] result = frame.publish(owner);
            // This frame now has permanent public storage and no longer borrows either decode window.
            if (first != null && first.frame == frame) first = null;
            if (second != null && second.frame == frame) second = null;
            return result;
        }
    }

    @Override
    public void add(final int index, final int[][] value) {
        frames.add(index, owner().frame(value, true));
        changed();
    }

    @Override
    public int[][] set(final int index, final int[][] value) {
        final int[][] old = get(index);
        frames.set(index, owner().frame(value, true));
        changed();
        return old;
    }

    @Override
    public int[][] remove(final int index) {
        final int[][] old = get(index);
        frames.remove(index);
        changed();
        return old;
    }

    @Override
    public void clear() {
        frames.clear();
        // Other mipmap-list generations can still own this index and its pixel aliases.
        ownership = null;
        changed();
    }

    private void changed() {
        modCount++;
        first = null;
        second = null;
    }

    /**
     * Mipmap generation may keep input arrays, so it must receive stable owned storage.
     */
    public void prepareMipmaps() {
        final Ownership owner = owner();
        synchronized (owner) {
            owner.index();
            final int size = frames.size();
            for (var i = 0; i < size; i++) {
                Frame frame = frames.get(i);
                if (frame != null) frame.stable(owner);
            }
            first = null;
            second = null;
        }
    }

    public void pack() {
        pack(null);
    }

    /**
     * Atlas loading shares one encoder across sprites instead of allocating its scratch tables for each one.
     */
    public void pack(final PixelFrameCodec.Encoder encoder) {
        final Ownership owner = ownership;
        if (owner == null) return;
        synchronized (owner) {
            long bytes = 0;
            final int size = frames.size();
            for (var i = 0; i < size; i++) {
                Frame frame = frames.get(i);
                if (frame == null) continue;
                if (frame.source != null) {
                    for (final int[] level : frame.source) if (level != null) bytes += level.length * 4L;
                } else {
                    for (final Mip mip : frame.mips) if (mip != null) bytes += mip.length * 4L;
                }
            }
            // One or two frames already fit the decode window; keeping a payload too would add storage.
            owner.pack(frames.size() > 2 && bytes >= 32 * 1024, encoder);
        }
    }

    private int[][] read(final int index) {
        final Frame frame = frames.get(index);
        if (frame == null) return null;
        if (frame.source != null) return frame.source;
        if (first == null) first = new Window();
        if (first.frame == frame) return first.read(frame);
        if (second == null) second = new Window();
        if (second.frame == frame) return second.read(frame);
        final Window previous = second;
        second = first;
        first = previous;
        return first.read(frame);
    }

    private static final class Ownership {
        private Reference2ObjectOpenHashMap<int[], Mip> pixels;
        private Reference2ObjectOpenHashMap<int[][], Frame> sourceFrames;
        private final List<WeakReference<OwnedAnimationFrames>> lists = new ObjectArrayList<>(2);
        private int activeEdits;
        private boolean packAfterEdit;

        Frame frame(final int[][] source, final boolean published) {
            if (source == null) return null;
            index();
            Frame frame = sourceFrames.get(source);
            if (frame == null) {
                final Mip[] mips = new Mip[source.length];
                for (int i = 0; i < source.length; i++) {
                    final int[] data = source[i];
                    if (data != null) mips[i] = pixels.computeIfAbsent(data, Mip::new);
                }
                frame = new Frame(source, mips);
                sourceFrames.put(source, frame);
            } else {
                frame.captureSource(this);
            }
            if (published) frame.publish(this);
            return frame;
        }

        private void pruneLists() {
            for (int i = lists.size(); i-- > 0; ) {
                final OwnedAnimationFrames list = lists.get(i).get();
                if (list == null || list.ownership != this) lists.remove(i);
            }
        }

        private void index() {
            if (pixels != null) return;
            pixels = new Reference2ObjectOpenHashMap<>(0);
            sourceFrames = new Reference2ObjectOpenHashMap<>(0);
            pruneLists();
            // Packing discards the lookup tables. A later insertion must recover the
            // live aliases before accepting arrays shared with a published frame.
            for (final WeakReference<OwnedAnimationFrames> reference : lists) {
                final OwnedAnimationFrames list = reference.get();
                if (list == null) continue;
                final int size = list.frames.size();
                for (var i = 0; i < size; i++) {
                    Frame frame = list.frames.get(i);
                    if (frame == null) continue;
                    for (int ii = 0; ii < frame.mips.length; ii++) {
                        final Mip mip = frame.mips[ii];
                        if (mip == null || mip.raw == null) continue;
                        final Mip canonical = pixels.putIfAbsent(mip.raw, mip);
                        if (canonical != null && canonical != mip) {
                            canonical.published |= mip.published;
                            frame.mips[ii] = canonical;
                        }
                    }
                    if (frame.source != null) sourceFrames.put(frame.source, frame);
                }
            }
            // Resolve every raw identity before published outer arrays can replace mip entries.
            // Otherwise a later private frame could overwrite an already published canonical mip.
            for (final WeakReference<OwnedAnimationFrames> reference : lists) {
                final OwnedAnimationFrames list = reference.get();
                if (list == null) continue;
                final int size = list.frames.size();
                for (var i = 0; i < size; i++) {
                    Frame frame = list.frames.get(i);
                    if (frame != null && frame.published) frame.publish(this);
                }
            }
        }

        void pack(final boolean compress, final PixelFrameCodec.Encoder sharedEncoder) {
            if (!compress) {
                releaseIndexes();
                return;
            }
            // Keep an editor's pixels attached to every sharing frame, even across a nested pack.
            if (activeEdits != 0) {
                packAfterEdit = true;
                return;
            }
            // Include earlier mipmap-list generations: old public lists must keep aliasing the same pixels.
            final ReferenceOpenHashSet<Mip> active = new ReferenceOpenHashSet<>();
            pruneLists();
            if (pixels == null) {
                boolean hasSource = false;
                for (final WeakReference<OwnedAnimationFrames> reference : lists) {
                    final OwnedAnimationFrames list = reference.get();
                    if (list == null) continue;
                    final int size = list.frames.size();
                    for (var i = 0; i < size; i++) {
                        Frame frame = list.frames.get(i);
                        if (frame != null && frame.source != null) {
                            hasSource = true;
                            break;
                        }
                    }
                    if (hasSource) break;
                }
                // Materialized outer arrays can acquire new mip references after the last pack.
                if (hasSource) index();
            }
            for (final WeakReference<OwnedAnimationFrames> reference : lists) {
                final OwnedAnimationFrames list = reference.get();
                if (list == null) continue;
                list.first = null;
                list.second = null;
                for (final Frame frame : list.frames) {
                    if (frame == null) continue;
                    frame.captureSource(this);
                    if (!frame.published) frame.source = null;
                    for (final Mip mip : frame.mips) if (mip != null) active.add(mip);
                }
            }
            final Map<PixelKey, Payload> values = new Object2ObjectOpenHashMap<>();
            PixelFrameCodec.Encoder encoder = sharedEncoder;
            for (final Mip mip : active) {
                if (mip.published || mip.raw == null) continue;
                final PixelKey key = new PixelKey(mip.raw);
                Payload payload = values.get(key);
                if (payload == null) {
                    if (encoder == null) encoder = new PixelFrameCodec.Encoder();
                    final byte[] encoded = encoder.encodeIfSmaller(mip.raw);
                    if (encoded == null) continue;
                    payload = new Payload(encoded);
                    values.put(key, payload);
                }
                mip.payload = payload;
                mip.raw = null;
            }
            releaseIndexes();
        }

        private void releaseIndexes() {
            pixels = null;
            sourceFrames = null;
        }
    }

    private static final class Frame {
        private int[][] source;
        private final Mip[] mips;
        private boolean published;

        private Frame(final int[][] source, final Mip[] mips) {
            this.source = source;
            this.mips = mips;
        }

        int[][] stable(final Ownership owner) {
            if (source == null) {
                final int[][] complete = new int[mips.length][];
                for (int i = 0; i < mips.length; i++) if (mips[i] != null) complete[i] = mips[i].stable(owner);
                source = complete;
            }
            if (owner.pixels != null) {
                owner.sourceFrames.put(source, this);
                for (int i = 0; i < mips.length; i++) {
                    if (mips[i] != null && mips[i].raw == source[i]) owner.pixels.put(source[i], mips[i]);
                }
            }
            return source;
        }

        int[][] publish(final Ownership owner) {
            final int[][] result = stable(owner);
            published = true;
            captureSource(owner);
            return result;
        }

        void captureSource(final Ownership owner) {
            if (source == null) return;
            // Private mipmap generation can fill or replace levels before compression too.
            for (int i = 0; i < source.length; i++) {
                final int[] level = source[i];
                if (level == null) {
                    mips[i] = null;
                    continue;
                }
                Mip mip = mips[i];
                if (owner.pixels != null) mip = owner.pixels.computeIfAbsent(level, Mip::new);
                else if (mip == null || mip.raw != level) mip = new Mip(level);
                mips[i] = mip;
                if (published) mip.published = true;
            }
        }
    }

    private static final class Mip {
        private int[] raw;
        private Payload payload;
        private final int length;
        private boolean published;

        private Mip(final int[] raw) {
            this.raw = raw;
            length = raw.length;
        }

        int[] stable(final Ownership owner) {
            if (raw == null) {
                final int[] complete = new int[length];
                PixelFrameCodec.decode(Objects.requireNonNull(payload).bytes, complete);
                raw = complete;
                payload = null;
            }
            if (owner.pixels != null) owner.pixels.put(raw, this);
            return raw;
        }
    }

    private record Payload(byte[] bytes) {
    }

    private record PixelKey(int[] words, int hash) {
        private PixelKey(final int[] words) {
            this(words, Arrays.hashCode(words));
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(final Object other) {
            return other instanceof PixelKey(
                int[] words1, int hash1
            ) && hash == hash1 && Arrays.equals(words, words1);
        }
    }

    private static final class Window {
        private Frame frame;
        private int[][] decoded;
        private int[][] view;

        int[][] read(final Frame next) {
            final boolean cached = frame == next;
            if (view == null || view.length != next.mips.length) {
                view = new int[next.mips.length][];
                decoded = new int[next.mips.length][];
            }
            for (int i = 0; i < next.mips.length; i++) {
                final Mip mip = next.mips[i];
                if (mip == null) {
                    view[i] = null;
                    continue;
                }
                if (mip.raw != null) {
                    view[i] = mip.raw;
                    continue;
                }
                if (decoded[i] == null || decoded[i].length != mip.length) decoded[i] = new int[mip.length];
                if (!cached || view[i] != decoded[i]) PixelFrameCodec.decode(mip.payload.bytes, decoded[i]);
                view[i] = decoded[i];
            }
            frame = next;
            return view;
        }
    }

    private final class InternalView extends AbstractList<int[][]> implements RandomAccess {
        @Override
        public int size() {
            return frames.size();
        }

        @Override
        public int[][] get(final int index) {
            return read(index);
        }

        @Override
        public void add(final int index, final int[][] value) {
            frames.add(index, owner().frame(value, false));
            changed();
        }

        @Override
        public int[][] set(final int index, final int[][] value) {
            final Frame previous = frames.set(index, owner().frame(value, false));
            changed();
            // Vanilla discards the return value, but preserve List.set for any internal caller.
            return previous == null ? null : previous.stable(owner());
        }

        @Override
        public int[][] remove(final int index) {
            return OwnedAnimationFrames.this.remove(index);
        }

        @Override
        public void clear() {
            OwnedAnimationFrames.this.clear();
        }
    }
}
