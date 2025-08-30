package github.kasuminova.novaeng.mixin.botania_r;

import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vazkii.botania.api.subtile.SubTileGenerating;
import vazkii.botania.common.block.subtile.generating.SubTileEntropinnyum;

@Mixin(value = SubTileEntropinnyum.class,remap = false)
public class MixinSubTileEntropinnyum extends SubTileGenerating {

    @Inject(method = "getMaxMana",at = @At("HEAD"), cancellable = true)
    public void getMaxManaI(CallbackInfoReturnable<Integer> cir){
        cir.setReturnValue(1200);
    }

    @Inject(method = "onUpdate",at = @At(value = "INVOKE", target = "Lvazkii/botania/api/subtile/SubTileGenerating;onUpdate()V",shift = At.Shift.AFTER), cancellable = true)
    public void onUpdateI(CallbackInfo ci) {
        if (!this.supertile.getWorld().isRemote && this.mana != 0) {
            for (EntityTNTPrimed tnt : this.supertile.getWorld().getEntitiesWithinAABB(EntityTNTPrimed.class, new AxisAlignedBB(this.supertile.getPos().add(-12, -12, -12), this.supertile.getPos().add(13, 13, 13)))) {
                if (tnt.getFuse() == 1 && !tnt.isDead && !this.supertile.getWorld().getBlockState(new BlockPos(tnt)).getMaterial().isLiquid()) {
                    this.getWorld().setBlockToAir(this.getPos());
                    ci.cancel();
                }
            }
        }
    }
}
