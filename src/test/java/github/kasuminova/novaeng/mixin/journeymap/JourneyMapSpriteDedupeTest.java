package github.kasuminova.novaeng.mixin.journeymap;

import github.kasuminova.novaeng.common.performance.JourneyMapQuadScratch;
import github.kasuminova.novaeng.common.performance.JourneyMapStateMemo;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import journeymap.client.cartography.color.ColoredSprite;
import journeymap.client.mod.vanilla.VanillaBlockSpriteProxy;
import journeymap.client.model.block.BlockMD;
import journeymap.client.model.chunk.ChunkMD;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JourneyMapSpriteDedupeTest {
    private static final MethodHandle WRAPPER = wrapper();

    @Test
    void actualJourneyMapAndFastutilAgreeOnSpriteContentsAndSpecialCases() throws Throwable {
        final VanillaBlockSpriteProxy original = new VanillaBlockSpriteProxy();
        final Sprite first = new Sprite("audit:first"), shared = new Sprite("audit:shared"), missing = new Sprite("missingno");
        final Quad firstQuad = new Quad(first), sharedFirst = new Quad(shared), sharedSecond = new Quad(shared);
        final Quad absent = new Quad(null), missingQuad = new Quad(missing);
        final List<BakedQuad> source = new ObjectArrayList<>();
        for (int i = 0; i < 8; i++) { source.add(firstQuad); source.add(sharedFirst); source.add(sharedSecond); source.add(absent); source.add(missingQuad); }
        for (final List<BakedQuad> quads : new List[]{null, List.of(), List.of(firstQuad), List.of(absent), List.of(missingQuad), source}) {
            final HashMap<String, ColoredSprite> expected = new HashMap<>(), actual = new HashMap<>();
            assertEquals(original.addSprites(expected, quads), optimized(actual, quads));
            compareSprites(expected, actual);
        }
        final HashMap<String, ColoredSprite> expected = new HashMap<>(), actual = new HashMap<>();
        final ColoredSprite existing = new ColoredSprite(firstQuad);
        expected.put("audit:first", existing); actual.put("audit:first", existing);
        assertEquals(original.addSprites(expected, source), optimized(actual, source));
        compareSprites(expected, actual);
        assertSame(existing, actual.get("audit:first"));
    }

    @Test
    void duplicateQuadReferencesAreVisitedOnceAndColoredSpriteOnlyReadsTheirSprite() throws Throwable {
        final VanillaBlockSpriteProxy original = new VanillaBlockSpriteProxy();
        final Sprite shared = new Sprite("audit:shared");
        final Quad first = new Quad(shared), second = new Quad(shared), missing = new Quad(new Sprite("missingno")), absent = new Quad(null);
        final List<BakedQuad> quads = List.of(first, second, first, missing, absent, second, missing);
        original.addSprites(new HashMap<>(), quads);
        final int expectedCalls = first.reads + second.reads + missing.reads + absent.reads;
        first.reads = second.reads = missing.reads = absent.reads = 0;
        final HashMap<String, ColoredSprite> actual = new HashMap<>();
        assertTrue(optimized(actual, quads));
        assertEquals(expectedCalls, first.reads + second.reads + missing.reads + absent.reads);
        assertEquals(3, first.reads + second.reads, "two unique quads read once, inserted ColoredSprite reads once more");
        assertEquals(1, missing.reads); assertEquals(1, absent.reads);
        assertFalse(actual.get("audit:shared").hasColor());
    }

    @Test
    void nestedAndFailingSpriteCallbacksReleaseEveryScratchScope() throws Throwable {
        final Quad nested = new Quad(new Sprite("audit:nested"));
        final boolean[] entered = {false};
        final Quad outer = new Quad(new Sprite("audit:outer")) {
            @Override public TextureAtlasSprite getSprite() {
                if (!entered[0]) {
                    entered[0] = true;
                    final JourneyMapQuadScratch parent = JourneyMapQuadScratch.current();
                    assertNotNull(parent);
                    try { assertTrue(optimized(new HashMap<>(), List.of(nested, nested))); }
                    catch (Throwable failure) { throw new AssertionError(failure); }
                    assertSame(parent, JourneyMapQuadScratch.current());
                }
                return super.getSprite();
            }
        };
        assertTrue(optimized(new HashMap<>(), List.of(outer, outer)));
        assertNull(JourneyMapQuadScratch.current());
        final Quad failure = new Quad(null) { @Override public TextureAtlasSprite getSprite() { throw new IllegalStateException("sprite failure"); } };
        assertThrows(IllegalStateException.class, () -> optimized(new HashMap<>(), List.of(failure, failure)));
        assertNull(JourneyMapQuadScratch.current());
        final JourneyMapQuadScratch idle = JourneyMapQuadScratch.begin();
        try { assertTrue(idle.set.isEmpty()); } finally { idle.close(); }
    }

    @Test
    void headAndReturnRestoreTheMemoWithoutUsingCallbackObjects() throws Exception {
        final var begin = MixinVanillaBlockSpriteProxy.class.getDeclaredMethod("nova$beginExtension", BlockMD.class, ChunkMD.class, BlockPos.class, CallbackInfoReturnable.class);
        final var end = MixinVanillaBlockSpriteProxy.class.getDeclaredMethod("nova$endExtension", BlockMD.class, ChunkMD.class, BlockPos.class, CallbackInfoReturnable.class);
        begin.setAccessible(true); end.setAccessible(true);
        final var target = new MixinVanillaBlockSpriteProxy() { };
        begin.invoke(target, null, null, null, null);
        final JourneyMapStateMemo outer = JourneyMapStateMemo.current();
        assertNotNull(outer);
        begin.invoke(target, null, null, null, null);
        assertNotSame(outer, JourneyMapStateMemo.current());
        end.invoke(target, null, null, null, null);
        assertSame(outer, JourneyMapStateMemo.current());
        end.invoke(target, null, null, null, null);
        assertNull(JourneyMapStateMemo.current());
    }

    private static void compareSprites(final HashMap<String, ColoredSprite> expected, final HashMap<String, ColoredSprite> actual) throws Exception {
        assertEquals(expected.keySet(), actual.keySet());
        final Field sprite = ColoredSprite.class.getDeclaredField("sprite"); sprite.setAccessible(true);
        for (final String name : expected.keySet()) {
            assertSame(sprite.get(expected.get(name)), sprite.get(actual.get(name)));
            assertEquals(expected.get(name).getColor(), actual.get(name).getColor());
        }
    }

    private static boolean optimized(final HashMap<String, ColoredSprite> sprites, final List<BakedQuad> quads) throws Throwable {
        return (boolean) WRAPPER.invokeExact(sprites, quads);
    }

    private static MethodHandle wrapper() {
        try {
            final var method = MixinVanillaBlockSpriteDedupe.class.getDeclaredMethod("addSprites", HashMap.class, List.class);
            method.setAccessible(true);
            return MethodHandles.lookup().unreflect(method).bindTo(new MixinVanillaBlockSpriteDedupe() { });
        } catch (ReflectiveOperationException failure) { throw new ExceptionInInitializerError(failure); }
    }

    private static final class Sprite extends TextureAtlasSprite { private Sprite(final String name) { super(name); } }
    private static class Quad extends BakedQuad {
        private final TextureAtlasSprite texture;
        private int reads;
        private Quad(final TextureAtlasSprite texture) { super(new int[28], -1, EnumFacing.UP, texture, true, DefaultVertexFormats.ITEM); this.texture = texture; }
        @Override public TextureAtlasSprite getSprite() { reads++; return texture; }
        @Override public int getTintIndex() { throw new AssertionError("ColoredSprite must not read quad tint"); }
        @Override public int[] getVertexData() { throw new AssertionError("ColoredSprite must not read vertex data"); }
        @Override public EnumFacing getFace() { throw new AssertionError("ColoredSprite must not read face"); }
    }
}
