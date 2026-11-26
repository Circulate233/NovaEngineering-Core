package github.kasuminova.novaeng.client.memory;

import net.minecraft.client.resources.data.AnimationFrame;
import net.minecraft.client.resources.data.AnimationMetadataSection;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.VarInsnNode;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.nio.IntBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ATHROW;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.PUTFIELD;
import static org.objectweb.asm.Opcodes.PUTSTATIC;
import static org.objectweb.asm.Opcodes.RETURN;

/**
 * Replays the installed Cleanroom CPU path; no Minecraft singleton or OpenGL context is created.
 */
public class VanillaAnimationPathTest {
    private static final String SPRITE = "net/minecraft/client/renderer/texture/TextureAtlasSprite";
    private static final String UTIL = "net/minecraft/client/renderer/texture/TextureUtil";
    private static final String FIXTURE = "github/kasuminova/novaeng/client/memory/ReplaySprite";
    private static final String CPU_UTIL = "github/kasuminova/novaeng/client/memory/ReplayTextureUtil";
    private static final String CALLBACK = VanillaAnimationPathTest.class.getName().replace('.', '/');
    private static final String LIST = "Ljava/util/List;";
    private static final String UPLOAD = "([[IIIIIZZ)V";
    private static final ThreadLocal<List<Upload>> UPLOADS = new ThreadLocal<>();

    private static void replay(final boolean interpolate) throws Exception {
        final Class<?> fixture = fixture();
        final byte[] image = image();
        for (final int mipmaps : new int[]{0, 1, 4}) {
            for (final boolean explicitFrames : new boolean[]{false, true}) {
                // A resource contains only its base image. The native loader leaves the other four slots null.
                final Object ordinary = load(fixture, image, metadata(interpolate, explicitFrames), new ArrayList<>());
                final Object owned = load(fixture, image, metadata(interpolate, explicitFrames), new OwnedAnimationFrames());
                assertIncompleteLoaderFrames(ordinary);
                assertIncompleteLoaderFrames(owned);
                invoke(ordinary, "generateMipmaps", new Class<?>[]{int.class}, mipmaps);
                invoke(owned, "generateMipmaps", new Class<?>[]{int.class}, mipmaps);
                final OwnedAnimationFrames frames = (OwnedAnimationFrames) fixture.getField("framesTextureData").get(owned);
                frames.pack();
                assertPacked(frames);
                int uploads = 0;
                for (int tick = 0; tick < 90; tick++) {
                    final List<Upload> expected = animate(ordinary);
                    final List<Upload> actual = animate(owned);
                    final String context = "mips=" + mipmaps + ", explicit=" + explicitFrames + ", interpolate=" + interpolate + ", tick=" + tick;
                    assertEquals(expected.size(), actual.size(), context);
                    for (int n = 0; n < expected.size(); n++) {
                        final Upload left = expected.get(n), right = actual.get(n);
                        assertEquals(left.width, right.width, context);
                        assertEquals(left.height, right.height, context);
                        assertEquals(left.x, right.x, context);
                        assertEquals(left.y, right.y, context);
                        assertEquals(left.blur, right.blur, context);
                        assertEquals(left.clamp, right.clamp, context);
                        assertEquals(mipmaps + 1, right.levels.length, context);
                        for (int level = 0; level < left.levels.length; level++) {
                            assertArrayEquals(left.levels[level], right.levels[level], context + ", level=" + level);
                        }
                    }
                    uploads += actual.size();
                }
                assertTrue(uploads > 15, "must traverse multiple complete animation cycles");
            }
        }
    }

    private static Object load(final Class<?> fixture, final byte[] png,
                               final AnimationMetadataSection metadata, final List<int[][]> frames) throws Exception {
        final Object sprite = fixture.getConstructor().newInstance();
        fixture.getField("framesTextureData").set(sprite, frames);
        for (final String size : new String[]{"width", "height"}) fixture.getField(size).setInt(sprite, 64);
        fixture.getField("originX").setInt(sprite, 32);
        fixture.getField("originY").setInt(sprite, 48);
        final Class<?> resource = Class.forName("net.minecraft.client.resources.IResource", false, fixture.getClassLoader());
        final Object input = Proxy.newProxyInstance(resource.getClassLoader(), new Class<?>[]{resource}, (_, method, _) -> switch (method.getName()) {
            case "getInputStream" -> new ByteArrayInputStream(png);
            case "getMetadata" -> metadata;
            case "hasMetadata" -> true;
            case "close" -> null;
            case "toString" -> "animation replay resource";
            default -> throw new AssertionError("unexpected resource operation: " + method);
        });
        invoke(sprite, "loadSpriteFrames", new Class<?>[]{resource, int.class}, input, 5);
        return sprite;
    }

    private static AnimationMetadataSection metadata(final boolean interpolate, final boolean explicit) {
        final List<AnimationFrame> frames = new ArrayList<>();
        if (explicit) for (final int frame : new int[]{0, 2, 2, 5, 1, 4, 3}) frames.add(new AnimationFrame(frame, -1));
        return new AnimationMetadataSection(frames, 64, 64, 3, interpolate);
    }

    private static byte[] image() throws Exception {
        final BufferedImage image = new BufferedImage(64, 64 * 6, BufferedImage.TYPE_INT_ARGB);
        for (int frame = 0; frame < 6; frame++) {
            for (int y = 0; y < 64; y++)
                for (int x = 0; x < 64; x++) {
                    final int alpha = (x / 8 + y / 8 + frame) % 4 == 0 ? 0 : 255;
                    final int color = (frame * 31 + x / 16 * 17 + y / 16 * 13) & 255;
                    image.setRGB(x, y + frame * 64, alpha << 24 | color << 16 | (255 - color) << 8 | frame * 29);
                }
        }
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(image, "PNG", bytes));
        return bytes.toByteArray();
    }

    private static void assertIncompleteLoaderFrames(final Object sprite) throws Exception {
        @SuppressWarnings("unchecked") final List<int[][]> raw = (List<int[][]>) sprite.getClass().getField("framesTextureData").get(sprite);
        final List<int[][]> frames = readPrivate(raw);
        assertEquals(6, frames.size());
        for (final int[][] frame : frames) {
            assertEquals(5, frame.length);
            assertNotNull(frame[0]);
            for (int level = 1; level < 5; level++) assertNull(frame[level]);
        }
    }

    private static void assertPacked(final OwnedAnimationFrames frames) throws Exception {
        final var field = OwnedAnimationFrames.class.getDeclaredField("frames");
        field.setAccessible(true);
        int privateFrames = 0;
        for (final Object frame : (List<?>) field.get(frames)) {
            final var source = frame.getClass().getDeclaredField("source");
            source.setAccessible(true);
            if (source.get(frame) == null) privateFrames++;
        }
        assertEquals(6, privateFrames, "the test must exercise packed decode windows");
    }

    private static List<Upload> animate(final Object sprite) throws Exception {
        final List<Upload> uploads = new ArrayList<>();
        UPLOADS.set(uploads);
        try {
            invoke(sprite, "updateAnimation", new Class<?>[0]);
        } finally {
            UPLOADS.remove();
        }
        return uploads;
    }

    private static void invoke(final Object target, final String method, final Class<?>[] signature, final Object... args) throws Exception {
        try {
            target.getClass().getMethod(method, signature).invoke(target, args);
        } catch (final InvocationTargetException failure) {
            if (failure.getCause() instanceof Exception exception) throw exception;
            if (failure.getCause() instanceof Error error) throw error;
            throw failure;
        }
    }

    // These three callbacks reproduce only the current mixin's read, HEAD, and setter hooks.
    public static List<int[][]> readPrivate(final List<int[][]> frames) {
        return frames instanceof OwnedAnimationFrames owned ? owned.internalView() : frames;
    }

    // Called by the transplanted Sprite bytecode.
    @SuppressWarnings("unused")
    public static void prepareFrames(final List<int[][]> frames) {
        if (frames instanceof OwnedAnimationFrames owned) owned.prepareMipmaps();
    }

    // Called by the transplanted Sprite bytecode.
    @SuppressWarnings("unused")
    public static List<int[][]> replaceFrames(final List<int[][]> before, final List<int[][]> generated) {
        return before instanceof OwnedAnimationFrames owned ? owned.mipmaps(generated) : generated;
    }

    // Called by the transplanted Sprite bytecode in place of the OpenGL upload.
    @SuppressWarnings("unused")
    public static void captureUpload(final int[][] levels, final int width, final int height,
                                     final int x, final int y, final boolean blur, final boolean clamp) {
        final int[][] captured = new int[levels.length][];
        for (int level = 0; level < levels.length; level++) {
            // TextureUtil's actual upload fails here for a null source; never conceal missing mips.
            final IntBuffer buffer = IntBuffer.allocate((width >> level) * (height >> level));
            buffer.put(levels[level]);
            captured[level] = buffer.array();
        }
        UPLOADS.get().add(new Upload(captured, width, height, x, y, blur, clamp));
    }

    private static Class<?> fixture() throws Exception {
        final ClassNode sourceSprite = read(SPRITE), sourceUtil = read(UTIL);
        final ClassNode sprite = empty(FIXTURE), util = empty(CPU_UTIL);
        for (final FieldNode field : sourceSprite.fields) {
            sprite.fields.add(new FieldNode(ACC_PUBLIC, field.name, field.desc, null, null));
        }
        final MethodNode ctor = new MethodNode(ACC_PUBLIC, "<init>", "()V", null, null);
        ctor.instructions.add(new VarInsnNode(ALOAD, 0));
        ctor.instructions.add(new MethodInsnNode(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false));
        ctor.instructions.add(new InsnNode(RETURN));
        sprite.methods.add(ctor);
        final Set<String> spriteMethods = Set.of("loadSpriteFrames", "allocateFrameTextureData", "generateMipmaps", "updateAnimation", "updateAnimationInterpolated", "interpolateColor");
        for (final MethodNode original : sourceSprite.methods) {
            if (!spriteMethods.contains(original.name) && !(original.name.equals("getFrameTextureData") && (original.access & ACC_STATIC) != 0))
                continue;
            final MethodNode method = copy(original);
            if (method.name.equals("generateMipmaps")) {
                // Keep valid-input CPU behavior; remove native crash-report wrapping tied to the real sprite type.
                for (final TryCatchBlockNode block : method.tryCatchBlocks) {
                    AbstractInsnNode cursor = block.handler;
                    while (cursor != null) {
                        final AbstractInsnNode next = cursor.getNext();
                        method.instructions.remove(cursor);
                        if (cursor.getOpcode() == ATHROW) break;
                        cursor = next;
                    }
                }
                method.tryCatchBlocks.clear();
            }
            remap(method);
            for (final AbstractInsnNode instruction : method.instructions.toArray()) {
                if (instruction instanceof FieldInsnNode field && field.getOpcode() == GETFIELD && field.owner.equals(FIXTURE) && field.name.equals("framesTextureData")) {
                    method.instructions.insert(field, new MethodInsnNode(INVOKESTATIC, CALLBACK, "readPrivate", "(" + LIST + ")" + LIST, false));
                }
            }
            if (method.name.equals("generateMipmaps")) {
                final InsnList head = new InsnList();
                head.add(new VarInsnNode(ALOAD, 0));
                head.add(new FieldInsnNode(GETFIELD, FIXTURE, "framesTextureData", LIST));
                head.add(new MethodInsnNode(INVOKESTATIC, CALLBACK, "prepareFrames", "(" + LIST + ")V", false));
                method.instructions.insert(head);
            }
            sprite.methods.add(method);
        }
        final MethodNode setter = new MethodNode(ACC_PUBLIC, "setFramesTextureData", "(" + LIST + ")V", null, null);
        setter.instructions.add(new VarInsnNode(ALOAD, 0));
        setter.instructions.add(new VarInsnNode(ALOAD, 0));
        setter.instructions.add(new FieldInsnNode(GETFIELD, FIXTURE, "framesTextureData", LIST));
        setter.instructions.add(new VarInsnNode(ALOAD, 1));
        setter.instructions.add(new MethodInsnNode(INVOKESTATIC, CALLBACK, "replaceFrames", "(" + LIST + LIST + ")" + LIST, false));
        setter.instructions.add(new FieldInsnNode(PUTFIELD, FIXTURE, "framesTextureData", LIST));
        setter.instructions.add(new InsnNode(RETURN));
        sprite.methods.add(setter);
        for (final FieldNode field : sourceUtil.fields) {
            if (Set.of("COLOR_GAMMAS", "MIPMAP_BUFFER").contains(field.name))
                util.fields.add(new FieldNode(ACC_PRIVATE | ACC_STATIC | ACC_FINAL, field.name, field.desc, null, null));
        }
        for (final MethodNode original : sourceUtil.methods) {
            if (Set.of("generateMipmapData", "blendColors", "blendColorComponent", "getColorGamma", "readBufferedImage").contains(original.name)) {
                final MethodNode method = copy(original);
                remap(method);
                util.methods.add(method);
            } else if (original.name.equals("<clinit>")) {
                final MethodNode method = copy(original);
                AbstractInsnNode start = null;
                for (final AbstractInsnNode instruction : method.instructions) {
                    if (instruction instanceof FieldInsnNode field && field.getOpcode() == PUTSTATIC && field.name.equals("COLOR_GAMMAS")) {
                        start = field.getPrevious().getPrevious();
                        break;
                    }
                }
                assertNotNull(start, "native gamma table initialization");
                while (method.instructions.getFirst() != start)
                    method.instructions.remove(method.instructions.getFirst());
                method.tryCatchBlocks.clear();
                remap(method);
                util.methods.add(method);
            }
        }
        final MethodNode upload = new MethodNode(ACC_PUBLIC | ACC_STATIC, "uploadTextureMipmap", UPLOAD, null, null);
        upload.instructions.add(new VarInsnNode(ALOAD, 0));
        for (int argument = 1; argument <= 6; argument++) upload.instructions.add(new VarInsnNode(ILOAD, argument));
        upload.instructions.add(new MethodInsnNode(INVOKESTATIC, CALLBACK, "captureUpload", UPLOAD, false));
        upload.instructions.add(new InsnNode(RETURN));
        util.methods.add(upload);
        final FixtureLoader loader = new FixtureLoader();
        loader.define(util);
        return loader.define(sprite);
    }

    private static ClassNode read(final String name) throws Exception {
        final Path jar = Path.of(System.getProperty("novaeng.test.projectDir"), "build/moddev/artifacts/cleanroom-0.6.13-alpha.jar");
        try (final ZipFile zip = new ZipFile(jar.toFile())) {
            final ClassNode node = new ClassNode();
            new ClassReader(zip.getInputStream(zip.getEntry(name + ".class"))).accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return node;
        }
    }

    private static MethodNode copy(final MethodNode original) {
        final MethodNode result = new MethodNode(original.access, original.name, original.desc, null, original.exceptions.toArray(String[]::new));
        original.accept(result);
        return result;
    }

    private static ClassNode empty(final String name) {
        final ClassNode node = new ClassNode();
        node.version = Opcodes.V17;
        node.access = ACC_PUBLIC;
        node.name = name;
        node.superName = "java/lang/Object";
        return node;
    }

    private static void remap(final MethodNode method) {
        for (final AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof FieldInsnNode field) field.owner = remapOwner(field.owner);
            else if (instruction instanceof MethodInsnNode call) call.owner = remapOwner(call.owner);
        }
    }

    private static String remapOwner(final String owner) {
        return owner.equals(SPRITE) ? FIXTURE : owner.equals(UTIL) ? CPU_UTIL : owner;
    }

    @Test
    void nativeLoaderMipmapAndAnimationMatchArrayListForAllMipCounts() throws Exception {
        replay(false);
    }

    @Test
    void nativeLoaderMipmapAndInterpolatedAnimationMatchArrayListForAllMipCounts() throws Exception {
        replay(true);
    }

    private record Upload(int[][] levels, int width, int height, int x, int y, boolean blur, boolean clamp) {
    }

    private static final class FixtureLoader extends ClassLoader {
        private FixtureLoader() {
            super(VanillaAnimationPathTest.class.getClassLoader());
        }

        private Class<?> define(final ClassNode node) {
            final ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
            node.accept(writer);
            final byte[] bytes = writer.toByteArray();
            return defineClass(node.name.replace('/', '.'), bytes, 0, bytes.length);
        }
    }
}
