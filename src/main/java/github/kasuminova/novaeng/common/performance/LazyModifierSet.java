package github.kasuminova.novaeng.common.performance;

import it.unimi.dsi.fastutil.objects.AbstractObjectSet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectIterators;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.entity.ai.attributes.AttributeModifier;

/**
 * A distinct live set per operation; the backing table is allocated by its first insertion.
 */
public final class LazyModifierSet extends AbstractObjectSet<AttributeModifier> {
    private ObjectOpenHashSet<AttributeModifier> values;

    @Override
    public int size() {
        return values == null ? 0 : values.size();
    }

    @Override
    public boolean contains(final Object value) {
        return values != null && values.contains(value);
    }

    @Override
    public boolean remove(final Object value) {
        return values != null && values.remove(value);
    }

    @Override
    public void clear() {
        if (values != null) values.clear();
    }

    @Override
    public boolean add(final AttributeModifier value) {
        if (values == null) values = new ObjectOpenHashSet<>(2);
        return values.add(value);
    }

    @Override
    public ObjectIterator<AttributeModifier> iterator() {
        return values == null ? ObjectIterators.emptyIterator() : values.iterator();
    }
}
