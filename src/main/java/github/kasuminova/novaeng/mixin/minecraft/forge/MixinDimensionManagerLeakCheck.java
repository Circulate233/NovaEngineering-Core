package github.kasuminova.novaeng.mixin.minecraft.forge;

import github.kasuminova.novaeng.common.performance.DimensionTickIds;
import net.minecraftforge.common.DimensionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = DimensionManager.class, remap = false)
public abstract class MixinDimensionManagerLeakCheck {

    @ModifyVariable(method = "getIDs(Z)[Ljava/lang/Integer;", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 1)
    private static boolean nova$primitiveLeakCounts(final boolean checkLeaks) {
        if (checkLeaks) {
            DimensionTickIds.checkLeaks();
        }
        // Keep Forge's public boxed-array API, but use the same primitive diagnostic state as the server tick.
        return false;
    }
}
