package github.kasuminova.novaeng.mixin.immersiveengineering;

import blusunrize.immersiveengineering.api.crafting.IMultiblockRecipe;
import blusunrize.immersiveengineering.common.blocks.metal.TileEntityMultiblockMetal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = TileEntityMultiblockMetal.class, remap = false)
public abstract class MixinTileEntityMultiblockMetal<T extends TileEntityMultiblockMetal<T, R>, R extends IMultiblockRecipe> extends MixinTileEntityMultiblockPart<T> {

    @Unique
    private int n_cache;

    @Shadow
    public abstract int[] getEnergyPos();

    /**
     * @author circulation
     * @reason 重写方法在field_174879_c不变化的情况下直接返回true
     */
    @Overwrite
    public boolean isEnergyPos() {
        if (n_cache == field_174879_c) {
            return true;
        }
        for (int i : this.getEnergyPos()) {
            if (this.field_174879_c == i) {
                n_cache = field_174879_c;
                return true;
            }
        }

        return false;
    }

}
