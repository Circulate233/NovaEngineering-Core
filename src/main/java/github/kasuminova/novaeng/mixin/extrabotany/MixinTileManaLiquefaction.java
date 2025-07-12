package github.kasuminova.novaeng.mixin.extrabotany;

import com.meteor.extrabotany.common.block.tile.TileManaLiquefaction;
import com.meteor.extrabotany.common.core.config.ConfigHandler;
import net.minecraft.block.Block;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vazkii.botania.common.block.tile.TileMod;
import vazkii.botania.common.block.tile.mana.TilePool;

@Mixin(value = TileManaLiquefaction.class,remap = false,priority = 0)
public abstract class MixinTileManaLiquefaction extends TileMod {

    @Shadow
    int mana;

    @Shadow
    public int energy;

    @Final
    @Shadow
    private static int MAX_ENERGY;

    @Inject(method = "update", at = @At("HEAD"),remap = true)
    public void updateMixin(CallbackInfo ci) {
        if (ConfigHandler.DISABLE_MANALIQUEFICATION) {
            var world = this.world;
            if (world.getWorldTime() % 20 != 0) return;

            var uppos = this.pos.up();

            if (world.getTileEntity(uppos) instanceof TilePool pool) {
                var blockState = world.getBlockState(uppos);
                Block upblock = blockState.getBlock();

                switch (upblock.getMetaFromState(blockState)) {
                    case 3 -> {
                        int mana = this.mana;
                        int energy = this.energy;
                        int upmana = pool.getCurrentMana();
                        int manaCap = pool.manaCap;

                        var totalMana = upmana + mana;

                        if (world.isBlockPowered(this.pos)) {
                            if ((mana < manaCap || energy < MAX_ENERGY) && upmana > 1000) {
                                if ((energy + totalMana / 1000) <= MAX_ENERGY) {
                                    this.mana = totalMana % 1000;
                                    this.energy = energy + totalMana / 1000;
                                } else {
                                    this.mana = totalMana - ((MAX_ENERGY - energy) * 1000);
                                    this.energy = MAX_ENERGY;
                                }
                                pool.recieveMana(-upmana);
                            }
                        } else {
                            var lsmana = mana;
                            var lsliquid = energy;
                            if (upmana < manaCap) {
                                if (totalMana > manaCap) {
                                    pool.recieveMana(manaCap);
                                    lsmana -= manaCap - upmana;
                                } else {
                                    pool.recieveMana(mana);
                                    lsmana = 0;
                                }
                            }

                            if (mana < manaCap || lsmana != mana) {
                                var qk = (manaCap - lsmana) / 1000 + 1;
                                if (energy <= qk) {
                                    lsmana += energy * 1000;
                                    lsliquid = 0;
                                } else {
                                    lsmana += qk * 1000;
                                    lsliquid -= qk;
                                }
                                this.mana = lsmana;
                                this.energy = lsliquid;
                            }
                        }
                    }
                    case 1 -> {
                        if (world.isBlockPowered(this.pos)) {
                            this.mana = 1000000;
                            this.energy = MAX_ENERGY;
                        } else {
                            this.mana = 0;
                            this.energy = 0;
                        }
                    }
                }
            }
        }
    }

}
