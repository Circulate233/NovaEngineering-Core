package github.kasuminova.novaeng.mixin.minecraft;

import net.minecraft.block.state.IBlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(targets = "net.minecraftforge.registries.GameData$BlockCallbacks$1")
public class MixinBlockCallbacks$1<T> extends MixinObjectIntIdentityMap<T> {

    /**
     * @author circulation
     * @reason 避开旧Map调用
     */
    @Overwrite(remap = false)
    public int get(IBlockState key) {
        int integer = this.nova$identityMap.getInt(key);
        if (integer == -1 && key != null) {
            return this.nova$identityMap.getInt(key.getBlock().getStateFromMeta(key.getBlock().getMetaFromState(key)));
        }

        return integer;
    }

    @Override
    protected boolean nova$isMCMap() {
        return true;
    }

}
