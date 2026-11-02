package github.kasuminova.novaeng.mixin.minecraft.forge;

import github.kasuminova.novaeng.common.util.RegistryAvailabilityAccess;
import github.kasuminova.novaeng.common.util.RegistryIdLookupAccess;

import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "net.minecraftforge.registries.ForgeRegistry$1", remap = false)
@SuppressWarnings("unresolvable-target")
public abstract class MixinForgeRegistryIteratorPrimitiveBiMap {
    @Shadow
    @Final
    ForgeRegistry<?> this$0;

    @Shadow
    int cur;
    @Shadow
    IForgeRegistryEntry<?> next;

    /**
     * @author NovaEngineering
     * @reason Traverse available registry IDs without Integer wrappers.
     */
    @Overwrite
    public IForgeRegistryEntry<?> next() {
        final IForgeRegistryEntry<?> result = next;
        do {
            cur = ((RegistryAvailabilityAccess) this$0).nova$nextAvailableRegistryId(cur + 1);
            next =
                (IForgeRegistryEntry<?>)
                    ((RegistryIdLookupAccess) this$0).nova$getIndexedId(cur);
        } while (next == null && cur != -1);
        return result;
    }
}
