package github.kasuminova.novaeng.mixin.biomesoplenty;

import net.minecraft.util.EnumFacing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * The shuffle in this AI writes through {@code Arrays.asList} into its backing
 * array, which is {@code EnumFacing.values()}. Cached, that array is
 * {@code $VALUES}, so the shuffle gets a private copy instead.
 */
@Mixin(targets = "biomesoplenty.common.entities.EntityWasp$AIWaspRandomFly", remap = false)
public abstract class MixinAIWaspRandomFly {

    @Unique
    private static final EnumFacing[] NOVA$SHUFFLE_TARGET = EnumFacing.values().clone();

    @Redirect(
        method = "startExecuting",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/util/EnumFacing;values()[Lnet/minecraft/util/EnumFacing;",
            remap = false
        ),
        remap = true,
        require = 1
    )
    private EnumFacing[] nova$shufflePrivateCopy() {
        return NOVA$SHUFFLE_TARGET;
    }
}
