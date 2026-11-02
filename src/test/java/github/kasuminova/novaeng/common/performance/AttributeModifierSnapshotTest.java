package github.kasuminova.novaeng.common.performance;

import net.minecraft.entity.ai.attributes.AbstractAttributeMap;
import net.minecraft.entity.ai.attributes.AttributeMap;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.ai.attributes.RangedAttribute;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.HashSet;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AttributeModifierSnapshotTest {

    static IAttributeInstance create(final boolean optimized, final int size) throws Exception {
        final Class<?> type = new WorldgenFixtureLoader(optimized)
            .loadClass("net.minecraft.entity.ai.attributes.ModifiableAttributeInstance");
        final IAttribute attribute = new RangedAttribute(null, "nova.snapshot.test", 2, -1024, 1024);
        final IAttributeInstance result = (IAttributeInstance) type.getConstructor(AbstractAttributeMap.class, IAttribute.class)
                                                                   .newInstance(new AttributeMap(), attribute);
        for (int i = 0; i < size; i++) {
            result.applyModifier(modifier(i));
        }
        return result;
    }

    private static AttributeModifier modifier(final int index) {
        return new AttributeModifier(new UUID(42, index), "modifier-" + index, index * 0.0001D, index % 3);
    }

    @Test
    void actualSnapshotsPreserveMembershipAndRemainMutableAndIndependent() throws Exception {
        for (final int size : new int[]{0, 1, 3, 8, 32, 128}) {
            final IAttributeInstance before = create(false, size);
            final IAttributeInstance after = create(true, size);
            assertEquals(new HashSet<>(before.getModifiers()), new HashSet<>(after.getModifiers()));
            final Collection<AttributeModifier> snapshot = after.getModifiers();
            final int count = snapshot.size();
            final AttributeModifier extra = modifier(size + 1000);
            snapshot.add(extra);
            assertFalse(after.hasModifier(extra));
            snapshot.clear();
            assertEquals(count, after.getModifiers().size());
            after.applyModifier(extra);
            assertTrue(snapshot.isEmpty());
            assertEquals(count + 1, after.getModifiers().size());
            assertEquals(before.getAttributeValue(), create(true, size).getAttributeValue());
        }
    }

    @Test
    void exposedOperationCollectionsRetainDeduplicationAndNullSupport() throws Exception {
        final IAttributeInstance before = create(false, 8);
        final IAttributeInstance after = create(true, 8);
        for (final IAttributeInstance instance : new IAttributeInstance[]{before, after}) {
            final AttributeModifier duplicate = instance.getModifiersByOperation(0).iterator().next();
            instance.getModifiersByOperation(1).add(duplicate);
            instance.getModifiersByOperation(0).add(null);
            instance.getModifiersByOperation(2).add(null);
        }
        assertEquals(new HashSet<>(before.getModifiers()), new HashSet<>(after.getModifiers()));
        assertEquals(9, after.getModifiers().size());
        assertTrue(after.getModifiers().contains(null));
    }
}
