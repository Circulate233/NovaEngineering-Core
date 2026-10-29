package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.util.text.ChatType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ChatType.class, remap = false)
public abstract class MixinChatType {

    @Shadow
    @Final
    private static ChatType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ChatType[] values() {
        return $VALUES;
    }
}
