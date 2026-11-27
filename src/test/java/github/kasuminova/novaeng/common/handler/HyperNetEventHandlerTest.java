package github.kasuminova.novaeng.common.handler;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraftforge.fml.common.eventhandler.EventBus;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.IEventListener;
import net.minecraftforge.fml.common.eventhandler.ListenerList;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HyperNetEventHandlerTest {
    @Test
    void endActionsQueuedByLowPriorityMachinesExecuteInTheSameTick() throws Exception {
        try (TickBus bus = new TickBus()) {
            List<String> executed = new ObjectArrayList<>();
            AtomicBoolean queueWork = new AtomicBoolean(true);
            bus.register(EventPriority.LOW, event -> {
                if (((TickEvent.ServerTickEvent) event).phase == TickEvent.Phase.END && queueWork.get()) {
                    executed.add("base research");
                    HyperNetEventHandler.addTickEndAction(() -> executed.add("extra research"));
                }
            });

            bus.post(TickEvent.Phase.END);
            assertEquals(List.of("base research", "extra research"), executed);

            queueWork.set(false);
            bus.post(TickEvent.Phase.END);
            assertEquals(List.of("base research", "extra research"), executed, "queued actions execute only once");
        }
    }

    @Test
    void consecutiveTicksSpendBaseBeforeExtraResearch() throws Exception {
        for (int budget : new int[]{150, 300}) {
            try (TickBus bus = new TickBus()) {
                AtomicInteger tick = new AtomicInteger();
                AtomicInteger spent = new AtomicInteger();
                bus.register(EventPriority.LOW, event -> {
                    if (((TickEvent.ServerTickEvent) event).phase != TickEvent.Phase.END) return;
                    int queuedTick = tick.get();
                    spent.addAndGet(100);
                    HyperNetEventHandler.addTickEndAction(() -> {
                        assertEquals(queuedTick, tick.get(), "extra research must not slip into the next tick");
                        spent.addAndGet(budget - 100);
                    });
                });

                for (int current = 1; current <= 3; current++) {
                    tick.set(current);
                    spent.set(0);
                    bus.post(TickEvent.Phase.END);
                    assertEquals(budget, spent.get(), "budget " + budget + ", tick " + current);
                }
            }
        }
    }

    @Test
    void startActionsKeepTheirOriginalPriorityAndEndActionsWaitForEnd() throws Exception {
        try (TickBus bus = new TickBus()) {
            List<String> executed = new ObjectArrayList<>();
            HyperNetEventHandler.addTickStartAction(() -> executed.add("start"));
            HyperNetEventHandler.addTickEndAction(() -> executed.add("end"));
            bus.register(EventPriority.LOW, event -> {
                if (((TickEvent.ServerTickEvent) event).phase == TickEvent.Phase.START) {
                    assertEquals(List.of("start"), executed);
                }
            });

            bus.post(TickEvent.Phase.START);
            assertEquals(List.of("start"), executed);
            bus.post(TickEvent.Phase.END);
            assertEquals(List.of("start", "end"), executed);
        }
    }

    @Test
    void installedMmceExecutorUsesLowPriority() throws Exception {
        Class<?> executor = Class.forName("github.kasuminova.mmce.common.concurrent.TaskExecutor", false, getClass().getClassLoader());
        SubscribeEvent subscription = executor.getMethod("onServerTick", TickEvent.ServerTickEvent.class)
                                              .getAnnotation(SubscribeEvent.class);
        assertEquals(EventPriority.LOW, subscription.priority());
    }

    /**
     * Use Forge's actual bus, listener ordering and annotated production callbacks. Register
     * listeners directly because ordinary JUnit does not run Forge's Loader or the transformer
     * that adds per-event listener lists and zero-argument event constructors for EventBus.register.
     */
    private static final class TickBus implements AutoCloseable {
        private final EventBus bus = new EventBus();
        private final List<IEventListener> listeners = new ObjectArrayList<>();
        private final ListenerList listenerList = new TickEvent.ServerTickEvent(TickEvent.Phase.END).getListenerList();
        private final int busId;

        private TickBus() throws Exception {
            var idField = EventBus.class.getDeclaredField("busID");
            idField.setAccessible(true);
            busId = idField.getInt(bus);
            for (Method method : HyperNetEventHandler.class.getDeclaredMethods()) {
                SubscribeEvent subscription = method.getAnnotation(SubscribeEvent.class);
                if (subscription != null && method.getParameterCount() == 1
                    && method.getParameterTypes()[0] == TickEvent.ServerTickEvent.class) {
                    register(subscription.priority(), event -> {
                        try {
                            method.invoke(HyperNetEventHandler.INSTANCE, event);
                        } catch (InvocationTargetException failure) {
                            if (failure.getCause() instanceof Error error) throw error;
                            throw new IllegalStateException(failure.getCause());
                        } catch (ReflectiveOperationException failure) {
                            throw new IllegalStateException(failure);
                        }
                    });
                }
            }
        }

        private void register(final EventPriority priority, final IEventListener listener) {
            listenerList.register(busId, priority, listener);
            listeners.add(listener);
        }

        private void post(final TickEvent.Phase phase) {
            bus.post(new TickEvent.ServerTickEvent(phase));
        }

        @Override
        public void close() {
            // Flush any queued work after a failed assertion before removing this isolated bus.
            for (Method method : HyperNetEventHandler.class.getDeclaredMethods()) {
                if (method.isAnnotationPresent(SubscribeEvent.class) && method.getParameterCount() == 1
                    && method.getParameterTypes()[0] == TickEvent.ServerTickEvent.class) {
                    try {
                        method.invoke(HyperNetEventHandler.INSTANCE, new TickEvent.ServerTickEvent(TickEvent.Phase.START));
                        method.invoke(HyperNetEventHandler.INSTANCE, new TickEvent.ServerTickEvent(TickEvent.Phase.END));
                    } catch (ReflectiveOperationException failure) {
                        throw new IllegalStateException(failure);
                    }
                }
            }
            for (IEventListener listener : listeners) ListenerList.unregisterAll(busId, listener);
        }
    }
}
