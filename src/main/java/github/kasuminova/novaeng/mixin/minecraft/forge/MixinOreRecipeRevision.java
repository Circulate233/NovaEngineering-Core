package github.kasuminova.novaeng.mixin.minecraft.forge;

import github.kasuminova.novaeng.common.performance.OreIdsCache;
import github.kasuminova.novaeng.common.performance.StartupCraftingScope;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventBus;
import net.minecraftforge.oredict.OreDictionary;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = OreDictionary.class, remap = false)
public abstract class MixinOreRecipeRevision {
    @Redirect(method = "registerOreImpl", at = @At(value = "INVOKE",
        target = "Lnet/minecraftforge/fml/common/eventhandler/EventBus;post(Lnet/minecraftforge/fml/common/eventhandler/Event;)Z"), require = 1)
    private static boolean nova$observeOreRegistration(final EventBus bus, final Event event) {
        StartupCraftingScope.ingredientsChanged();
        OreIdsCache.invalidate();
        try {
            return bus.post(event);
        } finally {
            StartupCraftingScope.ingredientsChanged();
            OreIdsCache.invalidate();
        }
    }

    @Inject(method = "rebakeMap", at = {@At("HEAD"), @At("RETURN")}, require = 1)
    private static void nova$observeOreRebuild(final CallbackInfo ci) {
        StartupCraftingScope.ingredientsChanged();
        OreIdsCache.invalidate();
    }
}
