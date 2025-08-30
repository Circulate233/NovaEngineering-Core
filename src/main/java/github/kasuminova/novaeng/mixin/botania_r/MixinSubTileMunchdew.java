package github.kasuminova.novaeng.mixin.botania_r;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vazkii.botania.api.subtile.SubTileGenerating;
import vazkii.botania.common.block.subtile.generating.SubTileMunchdew;
import vazkii.botania.common.core.handler.ConfigHandler;

import java.util.Collections;
import java.util.List;

@Mixin(value = SubTileMunchdew.class,remap = false)
public class MixinSubTileMunchdew extends SubTileGenerating {
    @Shadow
    boolean ateOnce = false;
    @Shadow
    int ticksWithoutEating;
    @Shadow
    int cooldown;

    @Inject(method = "onUpdate",at = @At("HEAD"),cancellable = true)
    public void onUpdate(CallbackInfo ci) {
        super.onUpdate();
        if (!this.getWorld().isRemote) {
            if (this.cooldown > 0) {
                --this.cooldown;
                this.ticksWithoutEating = 0;
                this.ateOnce = false;
            } else {
                int manaPerLeaf = 160;
                if (this.getMaxMana() - this.mana >= manaPerLeaf && this.ticksExisted % 2 == 0) {
                    List<BlockPos> coords = new ObjectArrayList<>();
                    BlockPos pos = this.supertile.getPos();

                    for(BlockPos pos_ : BlockPos.getAllInBox(pos.add(-8, 0, -8), pos.add(8, 16, 8))) {
                        if (this.supertile.getWorld().getBlockState(pos_).getMaterial() == Material.LEAVES) {
                            boolean exposed = false;

                            for(EnumFacing dir : EnumFacing.VALUES) {
                                IBlockState offState = this.supertile.getWorld().getBlockState(pos_.offset(dir));
                                if (offState.getBlock().isAir(offState, this.supertile.getWorld(), pos_.offset(dir))) {
                                    exposed = true;
                                    break;
                                }
                            }

                            if (exposed) {
                                coords.add(pos_);
                            }
                        }
                    }

                    if (!coords.isEmpty()) {
                        Collections.shuffle(coords);
                        BlockPos breakCoords = (BlockPos)coords.get(0);
                        IBlockState state = this.supertile.getWorld().getBlockState(breakCoords);
                        this.supertile.getWorld().setBlockToAir(breakCoords);
                        this.ticksWithoutEating = 0;
                        this.ateOnce = true;
                        if (ConfigHandler.blockBreakParticles) {
                            this.supertile.getWorld().playEvent(2001, breakCoords, Block.getStateId(state));
                        }

                        this.mana += manaPerLeaf;
                    }
                }

                if (this.ateOnce) {
                    ++this.ticksWithoutEating;
                    if (this.ticksWithoutEating >= 5) {
                        this.cooldown = 3200;
                        this.sync();
                    }
                }

            }
        }
        ci.cancel();
    }
}
