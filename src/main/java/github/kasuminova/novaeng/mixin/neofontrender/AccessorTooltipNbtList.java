package github.kasuminova.novaeng.mixin.neofontrender;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(NBTTagList.class)
public interface AccessorTooltipNbtList {
    @Accessor("tagList")
    List<NBTBase> nova$tooltipTagList();
}
