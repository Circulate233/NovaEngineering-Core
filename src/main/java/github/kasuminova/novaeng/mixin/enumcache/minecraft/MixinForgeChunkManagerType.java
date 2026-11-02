package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraftforge.common.ForgeChunkManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ForgeChunkManager.Type.class, remap = false)
public abstract class MixinForgeChunkManagerType {

    @Shadow
    @Final
    private static ForgeChunkManager.Type[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ForgeChunkManager.Type[] values() {
        return $VALUES;
    }
}
