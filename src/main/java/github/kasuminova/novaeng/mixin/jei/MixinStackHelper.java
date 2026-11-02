package github.kasuminova.novaeng.mixin.jei;

import com.llamalad7.mixinextras.sugar.Local;
import github.kasuminova.novaeng.common.performance.StackUidCache;
import mezz.jei.api.ISubtypeRegistry;
import mezz.jei.startup.StackHelper;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Map;

@Mixin(value = StackHelper.class, remap = false)
public abstract class MixinStackHelper {
    @Shadow
    @Final
    private Map<StackHelper.UidMode, Map<ItemStack, String>> uidCache;
    @Shadow
    @Final
    private ISubtypeRegistry subtypeRegistry;

    @Redirect(method = {"<init>", "disableUidCache"}, at = @At(value = "INVOKE",
        target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"), require = 2)
    private Object nova$modeAwareCache(final Map<?, ?> instance, final Object key, final Object value,
                                       @Local(name = "mode") final StackHelper.UidMode mode) {
        return uidCache.put(mode, new StackUidCache(mode, subtypeRegistry));
    }
}
