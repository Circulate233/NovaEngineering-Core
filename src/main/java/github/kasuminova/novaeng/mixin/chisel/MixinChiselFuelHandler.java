package github.kasuminova.novaeng.mixin.chisel;

import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import team.chisel.common.init.ChiselFuelHandler;

@Mixin(value = ChiselFuelHandler.class, remap = false)
public abstract class MixinChiselFuelHandler {
    /**
     * @author NovaEngineering
     * @reason These ore names are literals. Keep the ore ID order and compare each name once.
     */
    @Overwrite
    public int getBurnTime(final ItemStack fuel) {
        if (fuel.isEmpty()) return 0;
        for (final int id : OreDictionary.getOreIDs(fuel)) {
            switch (OreDictionary.getOreName(id)) {
                case "blockFuelCoke" -> {
                    return 32000;
                }
                case "blockCoal", "blockCharcoal" -> {
                    return 16000;
                }
            }
        }
        return 0;
    }
}
