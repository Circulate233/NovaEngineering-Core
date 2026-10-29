package github.kasuminova.novaeng.mixin.enumcache.patchouli;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import vazkii.patchouli.client.book.EntryDisplayState;

@Mixin(value = EntryDisplayState.class, remap = false)
public abstract class MixinEntryDisplayState {

    @Shadow
    @Final
    private static EntryDisplayState[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EntryDisplayState[] values() {
        return $VALUES;
    }
}
