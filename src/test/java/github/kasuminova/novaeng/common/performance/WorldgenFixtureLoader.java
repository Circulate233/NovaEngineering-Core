package github.kasuminova.novaeng.common.performance;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.io.IOException;
import java.io.InputStream;

final class WorldgenFixtureLoader extends ClassLoader {

    private static final String MIXINS = "github/kasuminova/novaeng/mixin/";
    private final boolean optimized;

    WorldgenFixtureLoader(final boolean optimized) {
        super(WorldgenFixtureLoader.class.getClassLoader());
        this.optimized = optimized;
    }

    private static byte[] merge(final byte[] original, final String mixinName, final String redirectTarget,
                                final String redirectName) throws IOException {
        final ClassNode target = new ClassNode();
        new ClassReader(original).accept(target, 0);
        final ClassNode mixin = new ClassNode();
        new ClassReader(resource(mixinName)).accept(mixin, 0);
        final String targetName = target.name;
        target.version = Math.max(target.version, mixin.version);
        target.interfaces.addAll(mixin.interfaces);
        for (final FieldNode field : mixin.fields) {
            if (field.visibleAnnotations == null || field.visibleAnnotations.stream()
                                                                            .noneMatch(annotation -> annotation.desc.equals("Lorg/spongepowered/asm/mixin/Shadow;"))) {
                target.fields.add(field);
            }
        }
        for (final MethodNode method : mixin.methods) {
            if (method.name.equals("<init>") || method.visibleAnnotations != null && method.visibleAnnotations.stream()
                                                                                                              .anyMatch(annotation -> annotation.desc.equals("Lorg/spongepowered/asm/mixin/Shadow;"))) {
                continue;
            }
            for (final AbstractInsnNode instruction : method.instructions) {
                if (instruction instanceof MethodInsnNode call && call.owner.equals(mixinName)) {
                    call.owner = targetName;
                } else if (instruction instanceof FieldInsnNode field && field.owner.equals(mixinName)) {
                    field.owner = targetName;
                } else if (instruction instanceof FrameNode frame) {
                    if (frame.local != null) {
                        frame.local.replaceAll(type -> mixinName.equals(type) ? targetName : type);
                    }
                    if (frame.stack != null) {
                        frame.stack.replaceAll(type -> mixinName.equals(type) ? targetName : type);
                    }
                }
            }
            if (method.name.equals("<clinit>")) {
                MethodNode clinit = target.methods.stream().filter(m -> m.name.equals("<clinit>"))
                                                  .findFirst().orElse(null);
                if (clinit == null) {
                    clinit = new MethodNode(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
                    clinit.instructions.add(new InsnNode(Opcodes.RETURN));
                    target.methods.add(clinit);
                }
                final InsnList extra = method.instructions;
                for (final AbstractInsnNode instruction : extra.toArray()) {
                    if (instruction.getOpcode() == Opcodes.RETURN) {
                        extra.remove(instruction);
                    }
                }
                AbstractInsnNode end = clinit.instructions.getLast();
                while (end != null && end.getOpcode() != Opcodes.RETURN) {
                    end = end.getPrevious();
                }
                if (end == null) {
                    throw new AssertionError("Missing class initializer return");
                }
                clinit.instructions.insertBefore(end, extra);
            } else {
                target.methods.removeIf(existing -> existing.name.equals(method.name) && existing.desc.equals(method.desc));
                target.methods.add(method);
            }
        }
        if (mixinName.endsWith("/MixinGenLayerZoomRandom")) {
            final MethodNode originalMethod = target.methods.stream().filter(method -> method.name.equals("getInts")
                && method.desc.equals("(IIII)[I")).findFirst().orElseThrow();
            originalMethod.name = "nova$unwrappedGetInts";
            final MethodNode entry = new MethodNode(Opcodes.ACC_PUBLIC, "getInts", "(IIII)[I", null, null);
            entry.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            for (int local = 1; local <= 4; local++) {
                entry.instructions.add(new VarInsnNode(Opcodes.ILOAD, local));
            }
            entry.instructions.add(new InsnNode(Opcodes.ACONST_NULL));
            entry.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, targetName, "nova$fixedRandomChoices",
                "(IIIILcom/llamalad7/mixinextras/injector/wrapoperation/Operation;)[I", false));
            entry.instructions.add(new InsnNode(Opcodes.ARETURN));
            target.methods.add(entry);
        }
        if (redirectTarget != null) {
            int redirects = 0;
            for (final MethodNode method : target.methods) {
                if (method.name.equals(redirectTarget)) {
                    for (final AbstractInsnNode instruction : method.instructions) {
                        if (instruction instanceof MethodInsnNode call && call.owner.equals(targetName) && call.name.equals("values")) {
                            call.name = redirectName;
                            redirects++;
                        }
                    }
                }
            }
            if (redirects != 1) {
                throw new AssertionError("Expected one enum classification redirect, got " + redirects);
            }
        }
        final ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        target.accept(writer);
        return writer.toByteArray();
    }

    private static byte[] resource(final String internalName) throws IOException {
        try (final InputStream stream = WorldgenFixtureLoader.class.getClassLoader().getResourceAsStream(internalName + ".class")) {
            if (stream == null) {
                throw new IOException("Missing fixture class: " + internalName);
            }
            return stream.readAllBytes();
        }
    }

    @Override
    protected synchronized Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
        final boolean atmosphere = name.equals("zmaster587.advancedRocketry.util.AstronomicalBodyHelper")
            || name.equals("zmaster587.advancedRocketry.dimension.DimensionProperties$AtmosphereTypes")
            || name.equals("zmaster587.advancedRocketry.dimension.DimensionProperties$Temps");
        final boolean weighted = name.equals("cofh.cofhworld.util.random.WeightedBlock");
        final boolean placement = name.equals("cofh.cofhworld.world.generator.WorldGen");
        final boolean cluster = name.equals("cofh.cofhworld.world.generator.WorldGenMinableCluster");
        final boolean vanillaOre = name.equals("net.minecraft.world.gen.feature.WorldGenMinable");
        final boolean stonePredicate = name.startsWith("net.minecraft.world.gen.feature.WorldGenMinable$");
        final boolean attributes = name.equals("net.minecraft.entity.ai.attributes.ModifiableAttributeInstance");
        final boolean zoom = name.equals("net.minecraft.world.gen.layer.GenLayerZoom");
        final boolean fuzzy = name.equals("net.minecraft.world.gen.layer.GenLayerFuzzyZoom");
        if (!atmosphere && !weighted && !placement && !cluster && !vanillaOre && !stonePredicate && !attributes && !zoom && !fuzzy) {
            return super.loadClass(name, resolve);
        }
        Class<?> loaded = this.findLoadedClass(name);
        if (loaded == null) {
            try {
                final String internal = name.replace('.', '/');
                byte[] bytes = resource(internal);
                if (this.optimized && zoom) {
                    bytes = merge(bytes, MIXINS + "minecraft/MixinGenLayerZoomRandom", null, null);
                } else if (this.optimized && vanillaOre) {
                    bytes = merge(bytes, MIXINS + "minecraft/MixinWorldGenMinableGeometry", null, null);
                } else if (this.optimized && attributes) {
                    bytes = merge(bytes, MIXINS + "minecraft/MixinAttributeModifierSnapshot", null, null);
                } else if (this.optimized && cluster) {
                    bytes = merge(bytes, MIXINS + "cofh/MixinWorldGenMinableClusterGeometry", null, null);
                } else if (this.optimized && placement) {
                    bytes = merge(bytes, MIXINS + "cofh/MixinWorldGenSinglePosition", null, null);
                } else if (this.optimized && weighted) {
                    bytes = merge(bytes, MIXINS + "cofh/MixinWeightedBlockPredicate", null, null);
                } else if (this.optimized && name.endsWith("AstronomicalBodyHelper")) {
                    bytes = merge(bytes, MIXINS + "ar/MixinAtmosphereTemperature", null, null);
                } else if (this.optimized && name.endsWith("$AtmosphereTypes")) {
                    bytes = merge(bytes, MIXINS + "ar/MixinAtmosphereTypes", "getAtmosphereTypeFromValue", "nova$classificationValues");
                } else if (this.optimized && name.endsWith("$Temps")) {
                    bytes = merge(bytes, MIXINS + "ar/MixinAtmosphereTemps", "getTempFromValue", "nova$classificationValues");
                }
                loaded = this.defineClass(name, bytes, 0, bytes.length);
            } catch (final IOException exception) {
                throw new ClassNotFoundException(name, exception);
            }
        }
        if (resolve) {
            this.resolveClass(loaded);
        }
        return loaded;
    }
}
