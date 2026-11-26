package github.kasuminova.novaeng.mixin.mcjtylib;

import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import github.kasuminova.novaeng.testutil.IsolatedMixinTransformer;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;

import java.lang.reflect.Proxy;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

class TrueTypeFontPixelsTest {
    @SuppressWarnings("unchecked")
    private static LocalRef<byte[]> scratch() {
        final byte[][] value = new byte[1][];
        return (LocalRef<byte[]>) Proxy.newProxyInstance(LocalRef.class.getClassLoader(), new Class<?>[]{LocalRef.class},
            (proxy, method, args) -> {
                if (method.getName().equals("get")) return value[0];
                if (method.getName().equals("set")) {
                    value[0] = (byte[]) args[0];
                    return null;
                }
                throw new AssertionError(method);
            });
    }

    @Test
    void pixelBytesMatchOriginalAndScratchBelongsToOneInvocation() throws Exception {
        final var method = MixinTrueTypeFontPixels.class.getDeclaredMethod("nova$reusePixelBytes", int.class, LocalRef.class);
        method.setAccessible(true);
        final LocalRef<byte[]> first = scratch();
        final LocalRef<byte[]> second = scratch();
        final Random random = new Random(4209);
        byte[] prior = null;
        for (int i = 0; i < 1000; i++) {
            final int pixel = random.nextInt();
            final byte[] bytes = (byte[]) method.invoke(null, pixel, first);
            assertArrayEquals(new byte[]{(byte) (pixel >>> 24), (byte) (pixel >>> 16), (byte) (pixel >>> 8), (byte) pixel}, bytes);
            if (prior != null) assertSame(prior, bytes);
            prior = bytes;
        }
        assertNotSame(prior, method.invoke(null, 0, second));
    }

    @Test
    void transformsActualFontWithoutReplacingGlOrBufferOperations() throws Exception {
        final byte[][] transformed = IsolatedMixinTransformer.transformInPackage("github.kasuminova.novaeng.mixin.mcjtylib",
            new String[]{"mcjty.lib.font.TrueTypeFont"}, "MixinTrueTypeFontPixels");
        final ClassNode target = new ClassNode();
        new ClassReader(transformed[0]).accept(target, 0);
        int oldConversions = 0;
        int glUpload = 0;
        for (final var method : target.methods) {
            if (!method.name.equals("loadImage")) continue;
            for (final var instruction : method.instructions) {
                if (!(instruction instanceof MethodInsnNode call)) continue;
                if (call.name.equals("intToByteArray")) oldConversions++;
                if (call.name.equals("gluBuild2DMipmaps")) glUpload++;
            }
        }
        assertEquals(0, oldConversions);
        assertEquals(1, glUpload);
    }
}
