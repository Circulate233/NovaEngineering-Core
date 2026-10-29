package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraftforge.fml.client.GuiModList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = GuiModList.SortType.class, remap = false)
public abstract class MixinSortType {

    @Shadow
    @Final
    private static GuiModList.SortType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static GuiModList.SortType[] values() {
        return $VALUES;
    }
}
