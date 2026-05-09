package github.kasuminova.novaeng.common.util;

import github.kasuminova.novaeng.NovaEngineeringCore;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jspecify.annotations.NonNull;

import java.util.AbstractList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.Set;

public class SetList<T, S extends Set<?>> extends AbstractList<T> implements RandomAccess {

    private final List<T> list = new ObjectArrayList<>();
    private final S set;
    private final SetAdd<T, S> add;
    private final SetRemove<T, S> remove;

    public SetList(S set, SetAdd<T, S> add, SetRemove<T, S> remove) {
        this.set = set;
        this.add = add;
        this.remove = remove;
    }

    @Override
    public boolean add(T element) {
        if (!add.added(set, element)) {
            warnDuplicate(element);
            return false;
        }
        list.add(element);
        modCount++;
        return true;
    }

    @Override
    public void add(int index, T element) {
        if (!add.added(set, element)) {
            warnDuplicate(element);
            return;
        }
        list.add(index, element);
        modCount++;
    }

    @Override
    public T get(int index) {
        return list.get(index);
    }

    @Override
    public int size() {
        return list.size();
    }

    @Override
    public boolean contains(Object o) {
        return set.contains(o);
    }

    @Override
    public boolean addAll(int index, @NonNull Collection<? extends T> collection) {
        int insertIndex = index;
        boolean changed = false;
        for (T element : collection) {
            if (add.added(set, element)) {
                list.add(insertIndex, element);
                insertIndex++;
                changed = true;
            } else {
                warnDuplicate(element);
            }
        }
        if (changed) {
            modCount++;
        }
        return changed;
    }

    @Override
    public T set(int index, T element) {
        T oldValue = list.get(index);
        if (Objects.equals(oldValue, element)) {
            return oldValue;
        }

        int existingIndex = list.indexOf(element);
        if (existingIndex >= 0) {
            warnDuplicate(element);
            list.remove(existingIndex);
            if (existingIndex < index) {
                index--;
            }
            list.set(index, element);
            remove.removed(set, oldValue);
            modCount++;
            return oldValue;
        }

        list.set(index, element);
        remove.removed(set, oldValue);
        add.added(set, element);
        modCount++;
        return oldValue;
    }

    @Override
    public boolean addAll(@NonNull Collection<? extends T> collection) {
        return addAll(list.size(), collection);
    }

    @Override
    public T remove(int index) {
        T removed = list.remove(index);
        remove.removed(set, removed);
        modCount++;
        return removed;
    }

    @FunctionalInterface
    public interface SetAdd<T, S extends Set<?>> {

        boolean added(S set, T t);

    }

    @FunctionalInterface
    public interface SetRemove<T, S extends Set<?>> {

        void removed(S set, T t);

    }

    @Override
    public boolean remove(Object o) {
        int index = list.indexOf(o);
        if (index < 0) {
            return false;
        }
        remove(index);
        return true;
    }

    @Override
    public void clear() {
        if (list.isEmpty()) {
            return;
        }
        list.clear();
        set.clear();
        modCount++;
    }

    @Override
    public int indexOf(Object o) {
        return list.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return list.lastIndexOf(o);
    }

    private void warnDuplicate(T element) {
        NovaEngineeringCore.log.warn("[{}]SetList detected duplicate element: {}", Thread.currentThread().getName(), element);
    }
}
