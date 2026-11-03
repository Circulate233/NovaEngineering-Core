package github.kasuminova.novaeng.mixin.minecraft.forge;

import com.google.common.collect.BiMap;
import github.kasuminova.novaeng.common.util.RegistryIdLookupAccess;
import net.minecraftforge.registries.ForgeRegistry;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// Forge's iterator is an anonymous class, so it has no accessible class literal.
@Mixin(targets = "net.minecraftforge.registries.ForgeRegistry$1", remap = false)
@SuppressWarnings("unresolvable-target") // Present in Forge bytecode, but absent from javac's named-type model.
public abstract class MixinForgeRegistryIterator {

    @Shadow
    @Final
    ForgeRegistry<?> this$0;

    @Redirect(method = "next()Lnet/minecraftforge/registries/IForgeRegistryEntry;",
        at = @At(value = "INVOKE", target = "Lcom/google/common/collect/BiMap;get(Ljava/lang/Object;)Ljava/lang/Object;"), require = 1)
    private Object nova$lookupId(final BiMap<?, ?> map, final Object key) {
        return ((RegistryIdLookupAccess) this$0).nova$getIndexedId((Integer) key);
    }
}
