package github.kasuminova.novaeng.mixin.neofontrender;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

/** Read-only SNBT traversal must not invalidate StellarCore's state through getTag. */
@Mixin(NBTTagCompound.class)
public interface AccessorTooltipNbtCompound {
    @Accessor("tagMap")
    Map<String, NBTBase> nova$tooltipTagMap();
}
