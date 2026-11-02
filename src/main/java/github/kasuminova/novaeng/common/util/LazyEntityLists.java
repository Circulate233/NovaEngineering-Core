package github.kasuminova.novaeng.common.util;

import net.minecraft.entity.Entity;
import net.minecraft.util.ClassInheritanceMultiMap;

/**
 * Helpers for {@code Chunk.entityLists}, whose 16 sections are created lazily.
 * <p>
 * A chunk that never holds an entity now keeps 16 nulls instead of 16 fully built
 * ClassInheritanceMultiMap instances, each of which carries a map, a key set and a value
 * list.
 */
public final class LazyEntityLists {

    /**
     * Shared template for the constructor's length probe. Never written to.
     */
    public static final ClassInheritanceMultiMap<Entity>[] EMPTY = newSectionArray(0);

    private LazyEntityLists() {
    }

    @SuppressWarnings("unchecked")
    public static ClassInheritanceMultiMap<Entity>[] newSectionArray(final int sections) {
        return new ClassInheritanceMultiMap[sections];
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static ClassInheritanceMultiMap<Entity> create(final Class<?> baseClass) {
        return new ClassInheritanceMultiMap(baseClass);
    }

    /**
     * Materialises every null slot, for the paths that walk the whole array.
     */
    public static void fill(final ClassInheritanceMultiMap<Entity>[] sections) {
        for (int i = 0; i < sections.length; i++) {
            if (sections[i] == null) {
                sections[i] = create(Entity.class);
            }
        }
    }
}
