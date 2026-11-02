package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraftforge.fml.common.discovery.json.ASMInfo;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ASMInfo.TargetType.class, remap = false)
public abstract class MixinTargetType {

    @Shadow
    @Final
    private static ASMInfo.TargetType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ASMInfo.TargetType[] values() {
        return $VALUES;
    }
}
