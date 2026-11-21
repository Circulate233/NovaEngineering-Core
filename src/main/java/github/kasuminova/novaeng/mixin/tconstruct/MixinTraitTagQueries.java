package github.kasuminova.novaeng.mixin.tconstruct;

import github.kasuminova.novaeng.common.performance.TinkerTagQueries;
import net.minecraft.nbt.NBTTagCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import slimeknights.tconstruct.library.utils.TinkerUtil;

@Mixin(value = TinkerUtil.class, remap = false)
public abstract class MixinTraitTagQueries {
    /**
     * @author NovaEngineering
     * @reason Missing read-only trait data does not require a new writable NBT list.
     */
    @Overwrite
    public static boolean hasTrait(final NBTTagCompound tag, final String identifier) {
        return TinkerTagQueries.hasTrait(tag, identifier);
    }

    /**
     * @author NovaEngineering
     * @reason Avoid constructing empty TinkerData and Modifiers containers just to query them.
     */
    @Overwrite
    public static boolean hasModifier(final NBTTagCompound tag, final String identifier) {
        return TinkerTagQueries.hasBaseModifier(tag, identifier);
    }
}
