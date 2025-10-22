package github.kasuminova.novaeng.mixin.electroblobs;

import electroblob.wizardry.misc.Forfeit;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = Forfeit.class, remap = false)
public interface AccessorForfeit {

    @Accessor("name")
    ResourceLocation getName();
}
