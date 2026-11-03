package github.kasuminova.novaeng.mixin.botania_r;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vazkii.botania.api.subtile.SubTileGenerating;
import vazkii.botania.common.block.subtile.generating.SubTileMunchdew;
import vazkii.botania.common.core.handler.ConfigHandler;

import java.util.concurrent.ThreadLocalRandom;

@Mixin(value = SubTileMunchdew.class, remap = false)
public class MixinSubTileMunchdew extends SubTileGenerating {
    @Shadow
    boolean ateOnce = false;
    @Shadow
    int ticksWithoutEating;
    @Shadow
    int cooldown;

    @Inject(method = "onUpdate", at = @At("HEAD"), cancellable = true)
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
                    final LongArrayList coords = new LongArrayList();
                    final World world = this.supertile.getWorld();
                    final BlockPos pos = this.supertile.getPos();
                    final BlockPos.MutableBlockPos neighbor = new BlockPos.MutableBlockPos();

                    for (BlockPos.MutableBlockPos cursor : BlockPos.getAllInBoxMutable(
                        pos.getX() - 8, pos.getY(), pos.getZ() - 8,
                        pos.getX() + 8, pos.getY() + 16, pos.getZ() + 8)) {
                        if (world.getBlockState(cursor).getMaterial() == Material.LEAVES) {
                            final int x = cursor.getX();
                            final int y = cursor.getY();
                            final int z = cursor.getZ();
                            boolean exposed = false;

                            for (EnumFacing dir : EnumFacing.VALUES) {
                                neighbor.setPos(x, y, z).move(dir);
                                IBlockState offState = world.getBlockState(neighbor);
                                if (offState.getBlock().isAir(offState, world, neighbor)) {
                                    exposed = true;
                                    break;
                                }
                            }

                            if (exposed) {
                                coords.add(cursor.toLong());
                            }
                        }
                    }

                    if (!coords.isEmpty()) {
                        final BlockPos breakCoords = BlockPos.fromLong(
                            coords.getLong(ThreadLocalRandom.current().nextInt(coords.size())));
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
