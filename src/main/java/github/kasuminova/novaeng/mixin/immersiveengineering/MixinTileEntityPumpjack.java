package github.kasuminova.novaeng.mixin.immersiveengineering;

import blusunrize.immersiveengineering.api.crafting.IMultiblockRecipe;
import blusunrize.immersiveengineering.common.util.Utils;
import flaxbeard.immersivepetroleum.common.blocks.metal.TileEntityPumpjack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

import javax.annotation.Nullable;

@Mixin(value = TileEntityPumpjack.class, remap = false)
public abstract class MixinTileEntityPumpjack extends MixinTileEntityMultiblockMetal<TileEntityPumpjack, IMultiblockRecipe> {

    @Unique
    private TileEntityPumpjack n_te;
    @Unique
    private long n_cachePos;

    /**
     * @author circulation
     * @reason 重写方法缓存上一次捕获到的结果
     */
    @Nullable
    @Overwrite
    public TileEntityPumpjack master() {
        if (this.offset[0] == 0 && this.offset[1] == 0 && this.offset[2] == 0) {
            return (TileEntityPumpjack) (Object) this;
        } else {
            BlockPos masterPos = this.getPos().add(-this.offset[0], -this.offset[1], -this.offset[2]);
            if (masterPos.toLong() == n_cachePos) {
                if (n_te != null && !n_te.isInvalid()) {
                    return n_te;
                } else {
                    n_te = null;
                    n_cachePos = 0;
                }
            }
            TileEntity te = Utils.getExistingTileEntity(this.world, masterPos);
            if (te instanceof TileEntityPumpjack p) {
                n_cachePos = masterPos.toLong();
                return n_te = p;
            }
            return null;
        }
    }
}
