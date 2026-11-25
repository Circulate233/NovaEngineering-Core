package github.kasuminova.novaeng.mixin.minecraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(targets = "net.minecraftforge.registries.GameData$ClearableObjectIntIdentityMap")
public class MixinClearableObjectIntIdentityMap<T> extends MixinObjectIntIdentityMap<T> {

    /**
     * @author circulation
     * @reason 避开旧Map调用
     */
    @Overwrite(remap = false)
    void clear() {
        this.nova$identityMap.clear();
        this.objectList.clear();
    }

    /**
     * @author circulation
     * @reason 避开旧Map调用
     */
    @Overwrite(remap = false)
    void remove(T key) {
        int prev = this.nova$identityMap.removeInt(key);
        if (prev != -1) {
            this.objectList.set(prev, null);
        }
    }

    @Override
    protected boolean nova$isMCMap() {
        return true;
    }

}
