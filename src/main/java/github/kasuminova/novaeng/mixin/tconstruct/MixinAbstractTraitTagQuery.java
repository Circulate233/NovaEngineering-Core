package github.kasuminova.novaeng.mixin.tconstruct;

import github.kasuminova.novaeng.common.performance.TinkerTagQueries;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import slimeknights.tconstruct.library.traits.AbstractTrait;

@Mixin(value = AbstractTrait.class, remap = false)
public abstract class MixinAbstractTraitTagQuery {
    @Shadow
    public abstract String getIdentifier();

    /**
     * @author NovaEngineering
     * @reason Ordinary inventory items do not need an empty NBT graph for each trait listener.
     */
    @Overwrite
    protected boolean isToolWithTrait(final ItemStack stack) {
        final NBTTagCompound tag = TinkerTagQueries.stackTag(stack);
        // Keep the virtual identifier call after observing the stack, as in AbstractTrait.
        return TinkerTagQueries.hasTrait(tag, getIdentifier());
    }
}
