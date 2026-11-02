package github.kasuminova.novaeng.common.performance;

import com.github.lunatrius.ingameinfo.tag.Tag;

import java.util.function.Supplier;

/**
 * Only used inside TagRegistry; enumeration exposes the original concrete Tag instances.
 */
public final class DeferredTag extends Tag {
    private final String category;
    private final int index;
    private final int maximum;
    private Supplier<Tag> factory;
    private volatile Tag delegate;

    public DeferredTag(final String category, final int index, final int maximum, final Supplier<Tag> factory) {
        this.category = category;
        this.index = index;
        this.maximum = maximum;
        this.factory = factory;
    }

    public Tag materialize() {
        Tag current = delegate;
        if (current == null) {
            synchronized (this) {
                current = delegate;
                if (current == null) {
                    current = factory.get();
                    current.setName(super.getName()).setAliases(super.getAliases());
                    delegate = current;
                    factory = null;
                }
            }
        }
        return current;
    }

    @Override
    public String getValue() {
        return materialize().getValue();
    }

    @Override
    public String getCategory() {
        return category;
    }

    @Override
    public boolean isIndexed() {
        return index >= 0;
    }

    @Override
    public int getMaximumIndex() {
        return maximum;
    }

    @Override
    public String getName() {
        return index < 0 ? super.getName() : super.getName() + index;
    }

    @Override
    public String[] getAliases() {
        final String[] aliases = super.getAliases();
        if (index < 0) {
            return aliases;
        }
        final String[] indexed = new String[aliases.length];
        for (int i = 0; i < aliases.length; i++) {
            indexed[i] = aliases[i] + index;
        }
        return indexed;
    }

    @Override
    public synchronized Tag setName(final String name) {
        super.setName(name);
        if (delegate != null) {
            delegate.setName(name);
        }
        return this;
    }

    @Override
    public synchronized Tag setAliases(final String... aliases) {
        super.setAliases(aliases);
        if (delegate != null) {
            delegate.setAliases(aliases);
        }
        return this;
    }
}
