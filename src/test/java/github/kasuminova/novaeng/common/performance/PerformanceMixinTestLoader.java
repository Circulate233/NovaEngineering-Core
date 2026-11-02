package github.kasuminova.novaeng.common.performance;

import com.google.gson.JsonParser;
import com.llamalad7.mixinextras.MixinExtrasBootstrap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.launch.platform.container.ContainerHandleVirtual;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import org.spongepowered.asm.service.IAdviceProvider;
import org.spongepowered.asm.service.IClassBytecodeProvider;
import org.spongepowered.asm.service.IClassProvider;
import org.spongepowered.asm.service.IClassTracker;
import org.spongepowered.asm.service.IFeatureValidator;
import org.spongepowered.asm.service.IGlobalPropertyService;
import org.spongepowered.asm.service.IMixinAuditTrail;
import org.spongepowered.asm.service.IPropertyKey;
import org.spongepowered.asm.service.ITransformerProvider;
import org.spongepowered.asm.service.MixinService;
import org.spongepowered.asm.service.MixinServiceAbstract;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;

final class PerformanceMixinTestLoader extends ClassLoader {

    private static final Map<String, byte[]> TRANSFORMED = new Object2ObjectOpenHashMap<>();
    private static IMixinTransformer transformer;

    PerformanceMixinTestLoader() {
        super(PerformanceMixinTestLoader.class.getClassLoader());
    }

    static synchronized byte[] transformed(final String name) throws Exception {
        if (transformer == null) {
            System.setProperty("mixin.service", Service.class.getName());
            final var instance = MixinService.class.getDeclaredMethod("getInstance");
            instance.setAccessible(true);
            final Field propertyService = MixinService.class.getDeclaredField("propertyService");
            propertyService.setAccessible(true);
            propertyService.set(instance.invoke(null), new Properties());
            MixinBootstrap.init();
            Mixins.addConfiguration("nova-performance-test.json");
            Mixins.addConfiguration("better-meteorite-performance-test.json");
            Mixins.addConfiguration("stellarcore-performance-test.json");
            final var constructor = Class.forName("org.spongepowered.asm.mixin.transformer.MixinTransformer").getDeclaredConstructor();
            constructor.setAccessible(true);
            transformer = (IMixinTransformer) constructor.newInstance();
            MixinExtrasBootstrap.init();
        }
        byte[] result = TRANSFORMED.get(name);
        if (result == null) {
            result = transformer.transformClassBytes(name, name, resource(name));
            TRANSFORMED.put(name, result);
        }
        return result;
    }

    static byte[] resource(final String name) throws IOException {
        try (final InputStream stream = PerformanceMixinTestLoader.class.getClassLoader()
                                                                        .getResourceAsStream(name.replace('.', '/') + ".class")) {
            if (stream == null) {
                throw new IOException("Missing performance fixture: " + name);
            }
            return stream.readAllBytes();
        }
    }

    @Override
    protected synchronized Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
        final boolean chunkMap = name.equals("net.minecraft.server.management.PlayerChunkMap")
            || name.startsWith("net.minecraft.server.management.PlayerChunkMap$");
        if (!name.startsWith("appeng.services.compass.") && !chunkMap
            && !name.equals("net.minecraft.world.WorldEntitySpawner")) {
            return super.loadClass(name, resolve);
        }
        Class<?> type = this.findLoadedClass(name);
        if (type == null) {
            try {
                final byte[] bytes = name.equals("appeng.services.compass.CompassReader")
                    || name.equals("net.minecraft.server.management.PlayerChunkMap")
                    || name.equals("net.minecraft.world.WorldEntitySpawner") ? transformed(name) : resource(name);
                type = this.defineClass(name, bytes, 0, bytes.length);
            } catch (final Exception failure) {
                throw new ClassNotFoundException(name, failure);
            }
        }
        if (resolve) {
            this.resolveClass(type);
        }
        return type;
    }

    public static final class Service extends MixinServiceAbstract implements IClassProvider, IClassBytecodeProvider {
        @Override
        public String getName() {
            return "Nova performance Mixin test";
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public MixinEnvironment.Phase getInitialPhase() {
            return MixinEnvironment.Phase.DEFAULT;
        }

        @Override
        public IClassProvider getClassProvider() {
            return this;
        }

        @Override
        public IClassBytecodeProvider getBytecodeProvider() {
            return this;
        }

        @Override
        public ITransformerProvider getTransformerProvider() {
            return null;
        }

        @Override
        public IClassTracker getClassTracker() {
            return null;
        }

        @Override
        public IMixinAuditTrail getAuditTrail() {
            return null;
        }

        @Override
        public IFeatureValidator getFeatureValidator() {
            return IFeatureValidator.ALLOW_ALL;
        }

        @Override
        public IAdviceProvider getAdviceProvider() {
            return IAdviceProvider.GENERIC;
        }

        @Override
        public Collection<String> getPlatformAgents() {
            return List.of();
        }

        @Override
        public IContainerHandle getPrimaryContainer() {
            return new ContainerHandleVirtual("nova-performance-test");
        }

        @Override
        public String getSideName() {
            return "SERVER";
        }

        @Override
        public InputStream getResourceAsStream(final String name) {
            final String configuration = switch (name) {
                case "nova-performance-test.json" -> """
                    {"required":true,"minVersion":"0.8","compatibilityLevel":"JAVA_25",
                     "package":"github.kasuminova.novaeng.mixin",
                     "mixins":["ae2.MixinCompassReaderRegions","ar.MixinAtmosphereDimensionMap",
                               "minecraft.MixinMinecraftServerDimensionIds","ae2.MixinBetterMeteoriteCompassMetadata",
                               "minecraft.MixinPlayerChunkMapCache","stellarcore.MixinPlayerChunkMapSnapshot",
                               "minecraft.MixinWorldEntitySpawner","minecraft.MixinBlockStoneMetadata",
                               "betterp2p.MixinServerLinkP2PHandler"],
                     "injectors":{"defaultRequire":1}}
                    """;
                case "better-meteorite-performance-test.json" -> """
                    {"required":true,"minVersion":"0.8","compatibilityLevel":"JAVA_25",
                     "package":"com.circulation.better_meteorite.mixin","mixins":["ae2.MixinCompassService"]}
                    """;
                case "stellarcore-performance-test.json" -> """
                    {"required":true,"minVersion":"0.8","compatibilityLevel":"JAVA_25",
                     "package":"github.kasuminova.stellarcore.mixin",
                     "mixins":["minecraft.forge.chunkmanager.MixinPlayerChunkMap"]}
                    """;
                default -> null;
            };
            if (configuration == null) {
                return PerformanceMixinTestLoader.class.getClassLoader().getResourceAsStream(name);
            }
            final var parsed = JsonParser.parseString(configuration).getAsJsonObject();
            final String root = parsed.get("package").getAsString();
            for (final var entry : parsed.getAsJsonArray("mixins")) {
                final String mixin = root + '.' + entry.getAsString();
                if (PerformanceMixinTestLoader.class.getClassLoader().getResource(mixin.replace('.', '/') + ".class") == null) {
                    throw new IllegalStateException("Missing registered test Mixin: " + mixin + " in " + name);
                }
            }
            return new ByteArrayInputStream(configuration.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public URL[] getClassPath() {
            return new URL[0];
        }

        @Override
        public Class<?> findClass(final String name) throws ClassNotFoundException {
            return this.findClass(name, false);
        }

        @Override
        public Class<?> findClass(final String name, final boolean initialize) throws ClassNotFoundException {
            return Class.forName(name, initialize, PerformanceMixinTestLoader.class.getClassLoader());
        }

        @Override
        public Class<?> findAgentClass(final String name, final boolean initialize) throws ClassNotFoundException {
            return this.findClass(name, initialize);
        }

        @Override
        public ClassNode getClassNode(final String name) throws ClassNotFoundException, IOException {
            return this.getClassNode(name, false, 0);
        }

        @Override
        public ClassNode getClassNode(final String name, final boolean runTransformers) throws ClassNotFoundException, IOException {
            return this.getClassNode(name, runTransformers, 0);
        }

        @Override
        public ClassNode getClassNode(final String name, final boolean runTransformers, final int flags) throws ClassNotFoundException, IOException {
            final ClassNode node = new ClassNode();
            new ClassReader(resource(name)).accept(node, flags);
            return node;
        }
    }

    private static final class Properties implements IGlobalPropertyService {
        private final Map<IPropertyKey, Object> values = new Object2ObjectOpenHashMap<>();

        @Override
        public IPropertyKey resolveKey(final String name) {
            return new Key(name);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getProperty(final IPropertyKey key) {
            return (T) this.values.get(key);
        }

        @Override
        public void setProperty(final IPropertyKey key, final Object value) {
            this.values.put(key, value);
        }

        @Override
        public <T> T getProperty(final IPropertyKey key, final T fallback) {
            final T value = this.getProperty(key);
            return value == null ? fallback : value;
        }

        @Override
        public String getPropertyString(final IPropertyKey key, final String fallback) {
            final Object value = this.values.get(key);
            return value == null ? fallback : value.toString();
        }

        private record Key(String name) implements IPropertyKey {
        }
    }
}
