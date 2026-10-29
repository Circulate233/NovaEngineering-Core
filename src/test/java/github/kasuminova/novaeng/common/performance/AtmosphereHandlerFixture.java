package github.kasuminova.novaeng.common.performance;

import net.minecraft.util.math.BlockPos;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import zmaster587.advancedRocketry.api.AreaBlob;
import zmaster587.advancedRocketry.api.IAtmosphere;
import zmaster587.advancedRocketry.api.util.IBlobHandler;
import zmaster587.libVulpes.util.HashedBlockPosition;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;

public class AtmosphereHandlerFixture {

    public static boolean oxygen = true;
    public final HashMap<IBlobHandler, AreaBlob> blobs = new HashMap<>();
    public IAtmosphere atmosphere;
    public int defaultCalls;

    static Class<?> create(final boolean optimized) throws Exception {
        final String fixtureName = AtmosphereHandlerFixture.class.getName().replace('.', '/');
        final ClassNode fixture = read(fixtureName);
        final ClassNode source = read(optimized ? "github/kasuminova/novaeng/mixin/ar/MixinAtmosphereHandler"
            : "zmaster587/advancedRocketry/atmosphere/AtmosphereHandler");
        fixture.version = Math.max(fixture.version, source.version);
        for (final MethodNode method : source.methods) {
            if ((!method.name.equals("getAtmosphereType") || !method.desc.startsWith("(Lnet/minecraft/util/math/BlockPos;"))
                && !method.name.equals("getBlobWithinRadius")) {
                continue;
            }
            method.access = Opcodes.ACC_PUBLIC;
            for (final AbstractInsnNode instruction : method.instructions.toArray()) {
                if (instruction instanceof MethodInsnNode call && call.owner.equals(source.name)) {
                    call.owner = fixture.name;
                } else if (instruction instanceof FieldInsnNode field && field.owner.equals(source.name)) {
                    field.owner = fixture.name;
                } else if (instruction instanceof MethodInsnNode call && call.owner.endsWith("/ARConfiguration")
                    && call.name.equals("getCurrentConfig")) {
                    AbstractInsnNode field = call.getNext();
                    while (field.getOpcode() < 0) {
                        field = field.getNext();
                    }
                    if (!(field instanceof FieldInsnNode access) || !access.name.equals("enableOxygen")) {
                        throw new AssertionError("Unexpected atmosphere configuration access");
                    }
                    method.instructions.remove(field);
                    method.instructions.set(call, new FieldInsnNode(Opcodes.GETSTATIC, fixture.name, "oxygen", "Z"));
                }
            }
            fixture.methods.removeIf(existing -> existing.name.equals(method.name) && existing.desc.equals(method.desc));
            fixture.methods.add(method);
        }
        final ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        fixture.accept(writer);
        final byte[] bytes = writer.toByteArray();
        return new ClassLoader(AtmosphereHandlerFixture.class.getClassLoader()) {
            Class<?> define() {
                return this.defineClass(AtmosphereHandlerFixture.class.getName(), bytes, 0, bytes.length);
            }
        }.define();
    }

    private static ClassNode read(final String internalName) throws IOException {
        final ClassNode node = new ClassNode();
        try (final InputStream stream = AtmosphereHandlerFixture.class.getClassLoader().getResourceAsStream(internalName + ".class")) {
            if (stream == null) {
                throw new IOException(internalName);
            }
            new ClassReader(stream).accept(node, 0);
        }
        return node;
    }

    public IAtmosphere getDefaultAtmosphereType() {
        this.defaultCalls++;
        return this.atmosphere;
    }

    public IAtmosphere getAtmosphereType(final BlockPos position) {
        throw new AssertionError();
    }

    public List<AreaBlob> getBlobWithinRadius(final HashedBlockPosition position, final int radius) {
        throw new AssertionError();
    }
}
