package github.kasuminova.novaeng.mixin.enumcache.armourers_workshop;

import moe.plushie.armourers_workshop.common.network.messages.client.MessageClientGuiLoadSaveArmour.LibraryPacketType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = LibraryPacketType.class, remap = false)
public abstract class MixinLibraryPacketType {

    @Shadow
    @Final
    private static LibraryPacketType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static LibraryPacketType[] values() {
        return $VALUES;
    }
}
