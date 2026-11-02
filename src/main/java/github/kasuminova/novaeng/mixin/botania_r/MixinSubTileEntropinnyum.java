package github.kasuminova.novaeng.mixin.botania_r;

import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vazkii.botania.api.subtile.SubTileGenerating;
import vazkii.botania.common.block.subtile.generating.SubTileEntropinnyum;

@Mixin(value = SubTileEntropinnyum.class, remap = false)
public class MixinSubTileEntropinnyum extends SubTileGenerating {

    @Unique
    private final BlockPos.MutableBlockPos nova$tntPosition = new BlockPos.MutableBlockPos();
    @Unique
    private AxisAlignedBB nova$tntSearchBounds;
    @Unique
    private int nova$tntBoundsX = Integer.MIN_VALUE;
    @Unique
    private int nova$tntBoundsY = Integer.MIN_VALUE;
    @Unique
    private int nova$tntBoundsZ = Integer.MIN_VALUE;

    @Inject(method = "getMaxMana", at = @At("HEAD"), cancellable = true)
    public void getMaxManaI(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(1200);
    }

    @Inject(method = "onUpdate", at = @At(value = "INVOKE", target = "Lvazkii/botania/api/subtile/SubTileGenerating;onUpdate()V", shift = At.Shift.AFTER), cancellable = true)
    public void onUpdateI(CallbackInfo ci) {
        if (!this.supertile.getWorld().isRemote && this.mana != 0) {
            final BlockPos center = this.supertile.getPos();
            final int centerX = center.getX();
            final int centerY = center.getY();
            final int centerZ = center.getZ();
            if (this.nova$tntSearchBounds == null || this.nova$tntBoundsX != centerX
                || this.nova$tntBoundsY != centerY || this.nova$tntBoundsZ != centerZ) {
                this.nova$tntBoundsX = centerX;
                this.nova$tntBoundsY = centerY;
                this.nova$tntBoundsZ = centerZ;
                this.nova$tntSearchBounds = new AxisAlignedBB(
                    centerX - 12, centerY - 12, centerZ - 12,
                    centerX + 13, centerY + 13, centerZ + 13);
            }
            for (EntityTNTPrimed tnt : this.supertile.getWorld().getEntitiesWithinAABB(
                EntityTNTPrimed.class, this.nova$tntSearchBounds)) {
                if (tnt.getFuse() == 1 && !tnt.isDead
                    && !this.supertile.getWorld().getBlockState(
                    this.nova$tntPosition.setPos(tnt.posX, tnt.posY, tnt.posZ)).getMaterial().isLiquid()) {
                    this.getWorld().setBlockToAir(this.getPos());
                    ci.cancel();
                }
            }
        }
    }
}
