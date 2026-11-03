package github.kasuminova.novaeng.common.performance;

import appeng.api.storage.data.IAEStack;

import java.util.function.Consumer;

public interface AvailableStackIteration<T extends IAEStack<T>> {
    void nova$forEachAvailable(Consumer<? super T> consumer);
}
