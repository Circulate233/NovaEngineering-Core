package github.kasuminova.novaeng.common.util;

import github.kasuminova.novaeng.common.tile.ecotech.EPart;
import github.kasuminova.novaeng.common.tile.ecotech.EPartController;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class EPartMap<P extends EPart<?>> {

    protected final Map<Class<?>, List<P>> parts = new Reference2ObjectOpenHashMap<>();

    public P addPart(final P part) {
        parts.computeIfAbsent(part.getClass(), k -> new ObjectArrayList<>()).add(part);
        for (Class<?> clazz : ClassUtils.getAllSuperClasses(part.getClass(), EPart.class))
            parts.computeIfAbsent(clazz, k -> new ObjectArrayList<>()).add(part);
        return part;
    }

    public <R extends P> List<R> getParts(final Class<R> partClass) {
        return (List<R>) parts.getOrDefault(partClass, Collections.emptyList());
    }

    public void forEachPart(final Consumer<P> consumer) {
        for (List<P> partList : parts.values())
            for (P part : partList) consumer.accept(part);
    }

    public void assemble(final EPartController<P> controller) {
        forEachPart(part -> {
            part.setController(controller);
            part.onAssembled();
        });
    }

    public void disassemble() {
        forEachPart(part -> {
            part.onDisassembled();
            part.setController(null);
        });
        parts.clear();
    }

    public void clear() {
        forEachPart(part -> part.setController(null));
        parts.clear();
    }

}
