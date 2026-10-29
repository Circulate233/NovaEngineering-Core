package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraftforge.fml.common.discovery.json.ASMInfo;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ASMInfo.ValueType.class, remap = false)
public abstract class MixinASMInfoValueType {

    @Shadow
    @Final
    private static ASMInfo.ValueType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ASMInfo.ValueType[] values() {
        return $VALUES;
    }
}
