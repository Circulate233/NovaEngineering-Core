package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.block.BlockTrapDoor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = BlockTrapDoor.DoorHalf.class, remap = false)
public abstract class MixinDoorHalf {

    @Shadow
    @Final
    private static BlockTrapDoor.DoorHalf[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static BlockTrapDoor.DoorHalf[] values() {
        return $VALUES;
    }
}
