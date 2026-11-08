package github.kasuminova.novaeng.common.performance;

import io.github.chaosunity.forgelin.KotlinAdapter;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;
import kotlin.reflect.jvm.internal.impl.metadata.ProtoBuf;
import kotlin.reflect.jvm.internal.impl.metadata.deserialization.Flags;
import kotlin.reflect.jvm.internal.impl.metadata.jvm.deserialization.JvmProtoBufUtil;
import net.minecraftforge.fml.common.FMLModContainer;
import net.minecraftforge.fml.common.ILanguageAdapter;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.NonNull;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * Installed from the normal construction-event caller; the early adapter/container classes are not transformed.
 */
public final class ForgelinObjectAdapter implements ILanguageAdapter {
    private static final Logger LOGGER = LogManager.getLogger("KotlinAdapter");
    private static final VarHandle ADAPTER;
    private static final VarHandle DESCRIPTOR;
    private static final MethodHandle ORIGINAL_GETTER;
    private static final ClassValue<ObjectGetter> OBJECTS = new ClassValue<>() {
        @Override
        protected ObjectGetter computeValue(final @NonNull Class<?> type) {
            try {
                final Metadata metadata = type.getDeclaredAnnotation(Metadata.class);
                if (metadata == null || metadata.k() != 1 || metadata.d1().length == 0) {
                    return new ObjectGetter(null);
                }
                final int[] version = metadata.mv();
                if (version.length == 0 || version[0] < 1 || version[0] > 2) {
                    return new ObjectGetter(null);
                }
                final var data = JvmProtoBufUtil.readClassDataFrom(metadata.d1(), metadata.d2());
                if (Flags.CLASS_KIND.get(data.getSecond().getFlags()) != ProtoBuf.Class.Kind.OBJECT) {
                    return new ObjectGetter(null);
                }
                return new ObjectGetter(MethodHandles.publicLookup().findStaticGetter(type, "INSTANCE", type)
                                                     .asType(MethodType.methodType(Object.class)));
            } catch (Exception | LinkageError unknownShape) {
                // Unsupported metadata and inaccessible/companion shapes retain Kotlin reflection's own diagnostics.
                return new ObjectGetter(null);
            }
        }
    };

    static {
        try {
            final var lookup = MethodHandles.privateLookupIn(FMLModContainer.class, MethodHandles.lookup());
            ADAPTER = lookup.findVarHandle(FMLModContainer.class, "languageAdapter", ILanguageAdapter.class);
            DESCRIPTOR = lookup.findVarHandle(FMLModContainer.class, "descriptor", Map.class);
            ORIGINAL_GETTER = lookup.findVirtual(FMLModContainer.class, "getLanguageAdapter",
                MethodType.methodType(ILanguageAdapter.class));
        } catch (ReflectiveOperationException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    private final FMLModContainer owner;
    private ILanguageAdapter delegate;

    private ForgelinObjectAdapter(final FMLModContainer owner, final ILanguageAdapter delegate) {
        this.owner = owner;
        this.delegate = delegate;
    }

    public static void install(final ModContainer container) {
        if (container.getClass() != FMLModContainer.class) {
            return;
        }
        final FMLModContainer owner = (FMLModContainer) container;
        final ILanguageAdapter current = (ILanguageAdapter) ADAPTER.get(owner);
        if (current != null) {
            if (current.getClass() == KotlinAdapter.class) {
                ADAPTER.set(owner, new ForgelinObjectAdapter(owner, current));
            }
        } else {
            final Map<?, ?> descriptor = (Map<?, ?>) DESCRIPTOR.get(owner);
            if (KotlinAdapter.class.getName().equals(descriptor.get("modLanguageAdapter"))) {
                ADAPTER.set(owner, new ForgelinObjectAdapter(owner, null));
            }
        }
    }

    private synchronized ILanguageAdapter original() {
        if (delegate == null) {
            // Run the original getter at first adapter use, preserving constructor timing and its error handling.
            ADAPTER.set(owner, null);
            try {
                delegate = (ILanguageAdapter) ORIGINAL_GETTER.invokeExact(owner);
            } catch (RuntimeException | Error failure) {
                throw failure;
            } catch (Throwable failure) {
                throw new IllegalStateException(failure);
            } finally {
                ADAPTER.set(owner, this);
            }
        }
        return delegate;
    }

    @Override
    public Object getNewInstance(final FMLModContainer container, final Class<?> objectClass,
                                 final ClassLoader classLoader, final Method factory) throws Exception {
        final ILanguageAdapter adapter = original();
        if (adapter.getClass() != KotlinAdapter.class || objectClass == null || factory != null) {
            return adapter.getNewInstance(container, objectClass, classLoader, factory);
        }
        final MethodHandle getter = OBJECTS.get(objectClass).handle();
        if (getter == null) {
            return adapter.getNewInstance(container, objectClass, classLoader, factory);
        }
        Intrinsics.checkNotNullParameter(container, "container");
        Intrinsics.checkNotNullParameter(classLoader, "classLoader");
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("FML has asked for {} to be constructed", objectClass.getSimpleName());
        }
        try {
            Class.forName(objectClass.getName(), true, classLoader);
        } catch (AssertionError failure) {
            LOGGER.error("FML has failed to construct {}", objectClass.getSimpleName());
            throw failure;
        }
        try {
            return (Object) getter.invokeExact();
        } catch (Exception | Error failure) {
            throw failure;
        } catch (Throwable failure) {
            throw new IllegalStateException(failure);
        }
    }

    @Override
    public boolean supportsStatics() {
        return original().supportsStatics();
    }

    @Override
    public void setProxy(final Field target, final Class<?> proxyTarget, final Object proxy)
        throws IllegalAccessException, NoSuchFieldException {
        final ILanguageAdapter adapter = original();
        if (adapter.getClass() != KotlinAdapter.class || target == null || proxyTarget == null || proxy == null) {
            adapter.setProxy(target, proxyTarget, proxy);
            return;
        }
        final MethodHandle getter = OBJECTS.get(proxyTarget).handle();
        if (getter == null) {
            adapter.setProxy(target, proxyTarget, proxy);
            return;
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Setting proxy: {}.{} -> {}", target.getDeclaringClass().getSimpleName(), target.getName(), proxy);
        }
        final Object instance;
        try {
            instance = (Object) getter.invokeExact();
        } catch (RuntimeException | Error failure) {
            throw failure;
        } catch (Throwable failure) {
            throw new IllegalStateException(failure);
        }
        // Field.set retains the original access checks, receiver validation and static-field handling.
        target.set(instance, proxy);
    }

    @Override
    public void setInternalProxies(final ModContainer mod, final Side side, final ClassLoader loader) {
        original().setInternalProxies(mod, side, loader);
    }

    private record ObjectGetter(MethodHandle handle) {
    }
}
