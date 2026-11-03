package github.kasuminova.novaeng.client.gui;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSlot;
import net.minecraftforge.fml.client.GuiScrollingList;
import org.jspecify.annotations.NonNull;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Modifier;
import java.util.List;

/**
 * Re-reads live widget fields each refresh without recreating their reflection metadata or lists.
 */
public final class NavigationLists {
    private static final ClassValue<Fields> FIELDS = new ClassValue<>() {
        @Override
        protected Fields computeValue(final @NonNull Class<?> screenClass) {
            final ObjectArrayList<VarHandle> vanilla = new ObjectArrayList<>();
            final ObjectArrayList<VarHandle> forge = new ObjectArrayList<>();
            for (Class<?> type = screenClass; type != null && GuiScreen.class.isAssignableFrom(type); type = type.getSuperclass()) {
                for (final var field : type.getDeclaredFields()) {
                    if (Modifier.isStatic(field.getModifiers())) continue;
                    final boolean vanillaList = GuiSlot.class.isAssignableFrom(field.getType());
                    if (!vanillaList && !GuiScrollingList.class.isAssignableFrom(field.getType())) continue;
                    try {
                        final VarHandle getter = MethodHandles.privateLookupIn(type, MethodHandles.lookup()).unreflectVarHandle(field);
                        (vanillaList ? vanilla : forge).add(getter);
                    } catch (final IllegalAccessException ignored) {
                        // Match the original introspector's handling of inaccessible fields.
                    }
                }
            }
            return new Fields(vanilla.toArray(VarHandle[]::new), forge.toArray(VarHandle[]::new));
        }
    };

    private final ObjectArrayList<GuiSlot> vanilla = new ObjectArrayList<>(2);
    private final ObjectArrayList<GuiScrollingList> forge = new ObjectArrayList<>(2);
    private final ReferenceOpenHashSet<Object> seen = new ReferenceOpenHashSet<>(4);

    public List<GuiSlot> collect(final GuiScreen screen) {
        clear();
        final Fields fields = FIELDS.get(screen.getClass());
        for (final VarHandle getter : fields.vanilla) {
            final GuiSlot list = (GuiSlot) getter.get(screen);
            if (list != null && seen.add(list)) vanilla.add(list);
        }
        for (final VarHandle getter : fields.forge) {
            final GuiScrollingList list = (GuiScrollingList) getter.get(screen);
            if (list != null && seen.add(list)) forge.add(list);
        }
        seen.clear();
        return vanilla;
    }

    public List<GuiScrollingList> forgeLists() {
        return forge;
    }

    public void clear() {
        vanilla.clear();
        forge.clear();
        seen.clear();
    }

    private record Fields(VarHandle[] vanilla, VarHandle[] forge) {
    }
}
