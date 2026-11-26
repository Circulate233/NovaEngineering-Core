package github.kasuminova.novaeng.mixin.minecraft.forge;

import com.google.common.collect.Lists;
import net.minecraftforge.registries.ForgeRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.Collection;

@Mixin(value = ForgeRegistry.class, remap = false)
public abstract class MixinForgeRegistrySyncAllocations {
    // The factory descriptor requires ArrayList. In sync the empty branch only tests isEmpty,
    // then discards this local; only the nonempty branch appends values and replays callbacks.
    @Unique
    private static final ArrayList<Object> nova$emptyOverrides = new ArrayList<>(0);

    @Redirect(method = "sync", at = @At(value = "INVOKE",
        target = "Lcom/google/common/collect/Lists;newArrayList(Ljava/lang/Iterable;)Ljava/util/ArrayList;"), require = 1)
    private <T> ArrayList<T> nova$copyOnlyPopulatedOverrides(final Iterable<T> overrides) {
        if (overrides instanceof Collection<?> collection && collection.isEmpty()) {
            @SuppressWarnings("unchecked") final ArrayList<T> empty = (ArrayList<T>) nova$emptyOverrides;
            return empty;
        }
        // Copy before add callbacks: they can change the source multimap during replay.
        return Lists.newArrayList(overrides);
    }
}
