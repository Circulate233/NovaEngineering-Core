package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.world.gen.structure.StructureStrongholdPieces;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = StructureStrongholdPieces.Stronghold.Door.class, remap = false)
public abstract class MixinDoor {

    @Shadow
    @Final
    private static StructureStrongholdPieces.Stronghold.Door[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static StructureStrongholdPieces.Stronghold.Door[] values() {
        return $VALUES;
    }
}
