package github.kasuminova.novaeng.client.render;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraftforge.client.ForgeHooksClient;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.List;
import java.util.RandomAccess;

/**
 * Installs the client thread's scratch list after Actinium has added its optional Iris fields.
 */
public final class LitItemQuadScratch {
    private LitItemQuadScratch() {
    }

    @SuppressWarnings("unchecked")
    public static void install() {
        try {
            final var field = MethodHandles.privateLookupIn(ForgeHooksClient.class, MethodHandles.lookup())
                                           .findStaticVarHandle(ForgeHooksClient.class, "actinium$fastLitItemQuads", ThreadLocal.class);
            final ThreadLocal<List<BakedQuad>> local = (ThreadLocal<List<BakedQuad>>) field.get();
            final List<BakedQuad> current = local.get();
            if (!(current instanceof Quads)) {
                final Quads replacement = new Quads();
                replacement.addAll(current);
                local.set(replacement);
            }
        } catch (final NoSuchFieldException ignored) {
            // The optional Actinium Iris mixin is not enabled in this configuration.
        } catch (final IllegalAccessException error) {
            throw new IllegalStateException("Unable to access Actinium's item quad scratch list", error);
        }
    }

    public static final class Quads extends ObjectArrayList<BakedQuad> {
        @Override
        public boolean addAll(final Collection<? extends BakedQuad> source) {
            if (source.isEmpty()) return false;
            if (source == this) return super.addAll(source);
            ensureCapacity(size() + source.size());
            if (source instanceof List<? extends BakedQuad> list && source instanceof RandomAccess) {
                for (int index = 0, size = list.size(); index < size; index++) add(list.get(index));
            } else {
                for (final BakedQuad quad : source) add(quad);
            }
            return true;
        }
    }
}
