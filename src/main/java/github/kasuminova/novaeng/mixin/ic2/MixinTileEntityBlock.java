package github.kasuminova.novaeng.mixin.ic2;

import github.kasuminova.novaeng.NovaEngineeringCore;
import ic2.core.block.TileEntityBlock;
import net.minecraft.tileentity.TileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TileEntityBlock.class)
public class MixinTileEntityBlock extends TileEntity {

    @Shadow
    private byte loadState;

    @Inject(method = "validate", at = @At(value = "INVOKE", target = "Lic2/core/block/TileEntityBlock;getWorld()Lnet/minecraft/world/World;"), cancellable = true)
    public void validate(CallbackInfo ci) {
        if (world != null && this.pos != null) {
            if (this.loadState != 0 && this.loadState != 3) {
                NovaEngineeringCore.log.error("[NoveEng-IC2-DEBUG] Incorrect device status:world:{},pos:{}", this.world, this.pos);
                world.destroyBlock(pos, false);
                ci.cancel();
            }
        }
    }
}
