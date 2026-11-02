package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = Chunk.EnumCreateEntityType.class, remap = false)
public abstract class MixinEnumCreateEntityType {

    @Shadow
    @Final
    private static Chunk.EnumCreateEntityType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static Chunk.EnumCreateEntityType[] values() {
        return $VALUES;
    }
}
