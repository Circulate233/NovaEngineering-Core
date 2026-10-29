package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.block.BlockChest;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = BlockChest.Type.class, remap = false)
public abstract class MixinBlockChestType {

    @Shadow
    @Final
    private static BlockChest.Type[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static BlockChest.Type[] values() {
        return $VALUES;
    }
}
