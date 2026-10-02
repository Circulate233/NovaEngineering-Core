package github.kasuminova.novaeng.mixin.minecraft;

import net.minecraft.util.NonNullList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.AbstractList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.RandomAccess;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

@Mixin(NonNullList.class)
public abstract class MixinNonNullList<E> extends AbstractList<E> {

    @Shadow @Final private List<E> delegate;

    @Redirect(method = "clear", at = @At(value = "INVOKE", target = "Ljava/util/AbstractList;clear()V"))
    private void nova$clearThroughDelegate(AbstractList<Object> instance) {
        delegate.clear();
    }

    @Unique
    @Override
    public boolean contains(Object o) {
        return delegate.contains(o);
    }

    @Unique
    @Override
    public int indexOf(Object o) {
        return delegate.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return delegate.lastIndexOf(o);
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends E> c) {
        if (c instanceof List<?> && c instanceof RandomAccess) {
            var l = (List<E>) c;
            boolean modified = false;
            for (var i = 0; i < l.size(); i++) {
                add(l.get(i));
            }
            return modified;
        }
        return super.addAll(c);
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> c) {
        return delegate.addAll(index, c);
    }

    @Override
    public @NonNull List<E> subList(int fromIndex, int toIndex) {
        return delegate.subList(fromIndex, toIndex);
    }

    @Override
    public boolean equals(Object o) {
        return delegate.equals(o);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public Object @NotNull [] toArray() {
        return delegate.toArray();
    }

    @Override
    public <T> T @NotNull [] toArray(T @NotNull [] a) {
        return delegate.toArray(a);
    }

    @Override
    public boolean remove(Object o) {
        return delegate.remove(o);
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        return delegate.containsAll(c);
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        return delegate.removeAll(c);
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        return delegate.retainAll(c);
    }

    @Override
    public void forEach(Consumer<? super E> action) {
        delegate.forEach(action);
    }

    @Override
    public @NotNull Stream<E> parallelStream() {
        return delegate.parallelStream();
    }

    @Override
    public @NotNull Stream<E> stream() {
        return delegate.stream();
    }

    @Override
    public boolean removeIf(@NotNull Predicate<? super E> filter) {
        return delegate.removeIf(filter);
    }

    @Override
    public <T> T[] toArray(@NotNull IntFunction<T[]> generator) {
        return delegate.toArray(generator);
    }

    @Override
    public @NonNull List<E> reversed() {
        return delegate.reversed();
    }

    @Override
    public E removeLast() {
        return delegate.removeLast();
    }

    @Override
    public E removeFirst() {
        return delegate.removeFirst();
    }

    @Override
    public E getLast() {
        return delegate.getLast();
    }

    @Override
    public E getFirst() {
        return delegate.getFirst();
    }

    @Override
    public void addLast(E e) {
        delegate.addLast(e);
    }

    @Override
    public @NotNull Spliterator<E> spliterator() {
        return delegate.spliterator();
    }

    @Override
    public void addFirst(E e) {
        delegate.addFirst(e);
    }

    @Override
    public void sort(@Nullable Comparator<? super E> c) {
        delegate.sort(c);
    }

    @Override
    public void replaceAll(@NotNull UnaryOperator<E> operator) {
        delegate.replaceAll(operator);
    }

    @Override
    public String toString() {
        return delegate.toString();
    }
}
