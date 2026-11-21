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

/** Public reads publish stable arrays; only private vanilla animation reads borrow the decode window. */
public final class OwnedAnimationFrames extends AbstractList<int[][]> implements RandomAccess {
    private Ownership ownership;
    private final ObjectArrayList<Frame> frames = new ObjectArrayList<>(0);
    private List<int[][]> internal;
    private Window first;
    private Window second;

    public OwnedAnimationFrames() { }
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

    /** The new list and its arrays were just created by vanilla generateMipmaps. */
    public OwnedAnimationFrames mipmaps(final List<int[][]> generated) {
        final Ownership owner = owner();
        final OwnedAnimationFrames next = new OwnedAnimationFrames(owner);
        next.frames.ensureCapacity(generated.size());
        for (final int[][] frame : generated) next.frames.add(owner.frame(frame, false));
        return next;
    }

    public List<int[][]> internalView() {
        if (internal == null) internal = new InternalView();
        return internal;
    }

    @Override public int size() { return frames.size(); }
    @Override public int[][] get(final int index) {
        final Frame frame = frames.get(index);
        if (frame == null) return null;
        final int[][] result = frame.publish(owner());
        // This frame now has permanent public storage and no longer borrows either decode window.
        if (first != null && first.frame == frame) first = null;
        if (second != null && second.frame == frame) second = null;
        return result;
    }
    @Override public void add(final int index, final int[][] value) {
        frames.add(index, owner().frame(value, true));
        changed();
    }
    @Override public int[][] set(final int index, final int[][] value) {
        final int[][] old = get(index);
        frames.set(index, owner().frame(value, true));
        changed();
        return old;
    }
    @Override public int[][] remove(final int index) {
        final int[][] old = get(index);
        frames.remove(index);
        changed();
        return old;
    }
    @Override public void clear() {
        frames.clear();
        // Other mipmap-list generations can still own this index and its pixel aliases.
        ownership = null;
        changed();
    }
    private void changed() { modCount++; first = null; second = null; }

    /** Mipmap generation may keep input arrays, so it must receive stable owned storage. */
    public void prepareMipmaps() {
        final Ownership owner = owner();
        owner.index();
        for (final Frame frame : frames) if (frame != null) frame.stable(owner);
        first = null;
        second = null;
    }

    public void pack() {
        pack(null);
    }

    /** Atlas loading shares one encoder across sprites instead of allocating its scratch tables for each one. */
    public void pack(final PixelFrameCodec.Encoder encoder) {
        long bytes = 0;
        for (final Frame frame : frames) if (frame != null) for (final Mip mip : frame.mips) if (mip != null) bytes += mip.length * 4L;
        // One or two frames already fit the decode window; keeping a payload too would add storage.
        if (ownership != null) ownership.pack(frames.size() > 2 && bytes >= 32 * 1024, encoder);
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

    private final class InternalView extends AbstractList<int[][]> implements RandomAccess {
        @Override public int size() { return frames.size(); }
        @Override public int[][] get(final int index) { return read(index); }
        @Override public void add(final int index, final int[][] value) { frames.add(index, owner().frame(value, false)); changed(); }
        @Override public int[][] set(final int index, final int[][] value) {
            final Frame previous = frames.set(index, owner().frame(value, false));
            changed();
            // Vanilla discards the return value, but preserve List.set for any internal caller.
            return previous == null ? null : previous.stable(owner());
        }
        @Override public int[][] remove(final int index) { return OwnedAnimationFrames.this.remove(index); }
        @Override public void clear() { OwnedAnimationFrames.this.clear(); }
    }

    private static final class Ownership {
        private Reference2ObjectOpenHashMap<int[], Mip> pixels;
        private Reference2ObjectOpenHashMap<int[][], Frame> sourceFrames;
        private final List<WeakReference<OwnedAnimationFrames>> lists = new ObjectArrayList<>(2);
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
            }
            if (published) frame.publish(this);
            return frame;
        }
        private void pruneLists() {
            for (int i = lists.size(); i-- > 0;) {
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
                for (final Frame frame : list.frames) {
                    if (frame == null) continue;
                    for (int i = 0; i < frame.mips.length; i++) {
                        final Mip mip = frame.mips[i];
                        if (mip == null || mip.raw == null) continue;
                        final Mip canonical = pixels.putIfAbsent(mip.raw, mip);
                        if (canonical != null && canonical != mip) {
                            canonical.published |= mip.published;
                            frame.mips[i] = canonical;
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
                for (final Frame frame : list.frames) if (frame != null && frame.published) frame.publish(this);
            }
        }
        void pack(final boolean compress, final PixelFrameCodec.Encoder sharedEncoder) {
            if (!compress) {
                releaseIndexes();
                return;
            }
            // Include earlier mipmap-list generations: old public lists must keep aliasing the same pixels.
            final ReferenceOpenHashSet<Mip> active = new ReferenceOpenHashSet<>();
            pruneLists();
            if (pixels == null) {
                boolean published = false;
                for (final WeakReference<OwnedAnimationFrames> reference : lists) {
                    final OwnedAnimationFrames list = reference.get();
                    if (list == null) continue;
                    for (final Frame frame : list.frames) if (frame != null && frame.published) { published = true; break; }
                    if (published) break;
                }
                // Public outer arrays can acquire aliases after the previous pack released its index.
                // Rebuild only for this explicit repack, without decoding or cloning pixel arrays.
                if (published) index();
            }
            for (final WeakReference<OwnedAnimationFrames> reference : lists) {
                final OwnedAnimationFrames list = reference.get();
                if (list == null) continue;
                list.first = null;
                list.second = null;
                for (final Frame frame : list.frames) {
                    if (frame == null) continue;
                    if (frame.published) frame.publish(this); else frame.source = null;
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
        private Frame(final int[][] source, final Mip[] mips) { this.source = source; this.mips = mips; }
        int[][] stable(final Ownership owner) {
            if (source == null) {
                source = new int[mips.length][];
                for (int i = 0; i < mips.length; i++) if (mips[i] != null) source[i] = mips[i].stable(owner);
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
            // A public caller can replace a level in the outer array as well as mutate a pixel.
            for (int i = 0; i < result.length; i++) {
                final int[] level = result[i];
                if (level == null) {
                    mips[i] = null;
                    continue;
                }
                Mip mip = mips[i];
                if (owner.pixels != null) mip = owner.pixels.computeIfAbsent(level, Mip::new);
                else if (mip == null || mip.raw != level) mip = new Mip(level);
                mips[i] = mip;
                mip.published = true;
            }
            return result;
        }
    }

    private static final class Mip {
        private int[] raw;
        private Payload payload;
        private final int length;
        private boolean published;
        private Mip(final int[] raw) { this.raw = raw; length = raw.length; }
        int[] stable(final Ownership owner) {
            if (raw == null) {
                raw = new int[length];
                PixelFrameCodec.decode(Objects.requireNonNull(payload).bytes, raw);
                payload = null;
            }
            if (owner.pixels != null) owner.pixels.put(raw, this);
            return raw;
        }
    }

    private record Payload(byte[] bytes) { }
    private record PixelKey(int[] words, int hash) {
        private PixelKey(final int[] words) { this(words, Arrays.hashCode(words)); }
        @Override public int hashCode() { return hash; }
        @Override public boolean equals(final Object other) { return other instanceof PixelKey key && hash == key.hash && Arrays.equals(words, key.words); }
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
                if (mip == null) { view[i] = null; continue; }
                if (mip.raw != null) { view[i] = mip.raw; continue; }
                if (decoded[i] == null || decoded[i].length != mip.length) decoded[i] = new int[mip.length];
                if (!cached || view[i] != decoded[i]) PixelFrameCodec.decode(mip.payload.bytes, decoded[i]);
                view[i] = decoded[i];
            }
            frame = next;
            return view;
        }
    }
}
