package github.kasuminova.novaeng.mixin.immersiveengineering;

import blusunrize.immersiveengineering.common.blocks.TileEntityIEBase;
import blusunrize.immersiveengineering.common.blocks.TileEntityMultiblockPart;
import blusunrize.immersiveengineering.common.util.Utils;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import javax.annotation.Nullable;

@Mixin(value = TileEntityMultiblockPart.class, remap = false)
public abstract class MixinTileEntityMultiblockPart<T extends TileEntityMultiblockPart<T>> extends TileEntityIEBase {

    @Shadow
    public int[] offset;
    @Shadow
    public int field_174879_c;
    @Unique
    private T n_te;
    @Unique
    private long n_cachePos;

    /**
     * @author circulation
     * @reason 重写方法缓存上一次捕获到的结果
     */
    @SuppressWarnings({"unchecked", "DataFlowIssue"})
    @Nullable
    @Overwrite
    public T master() {
        if (this.offset[0] == 0 && this.offset[1] == 0 && this.offset[2] == 0) {
            return (T) (Object) this;
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
            if (this.getClass().isInstance(te)) {
                n_cachePos = masterPos.toLong();
                return n_te = (T) te;
            }
            return null;
        }
    }
}
