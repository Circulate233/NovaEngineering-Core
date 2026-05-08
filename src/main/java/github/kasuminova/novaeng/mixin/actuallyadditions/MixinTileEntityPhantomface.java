package github.kasuminova.novaeng.mixin.actuallyadditions;

import de.ellpeck.actuallyadditions.api.tile.IPhantomTile;
import de.ellpeck.actuallyadditions.mod.tile.TileEntityInventoryBase;
import de.ellpeck.actuallyadditions.mod.tile.TileEntityPhantomface;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TileEntityPhantomface.class)
public abstract class MixinTileEntityPhantomface extends TileEntityInventoryBase {

    @Shadow public BlockPos boundPosition;

    public MixinTileEntityPhantomface(int slots, String name) {
        super(slots, name);
    }

    @Inject(method = "updateEntity",at = @At(value = "INVOKE", target = "Lde/ellpeck/actuallyadditions/mod/tile/TileEntityPhantomface;upgradeRange(ILnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;)I",shift = At.Shift.BEFORE,remap = false), cancellable = true)
    public void updateEntity(CallbackInfo ci) {
        if (boundPosition != null) {
            if (!world.isBlockLoaded(boundPosition)) {
                ci.cancel();
            }
        }
    }

    /**
     * @author circulation
     * @reason 防止意外的区块加载
     */
    @Overwrite(remap = false)
    public boolean hasBoundPosition() {
        if (this.boundPosition == null || !world.isBlockLoaded(this.boundPosition)) {
            return false;
        } else if (!(this.world.getTileEntity(this.boundPosition) instanceof IPhantomTile) && (this.getPos().getX() != this.boundPosition.getX() || this.getPos().getY() != this.boundPosition.getY() || this.getPos().getZ() != this.boundPosition.getZ())) {
            return true;
        } else {
            this.boundPosition = null;
            return false;
        }
    }

}
