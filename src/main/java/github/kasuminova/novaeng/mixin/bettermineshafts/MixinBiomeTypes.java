package github.kasuminova.novaeng.mixin.bettermineshafts;

import com.yungnickyoung.minecraft.bettermineshafts.world.MapGenBetterMineshaft;
import github.kasuminova.novaeng.common.performance.BiomeTypeViews;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MapGenBetterMineshaft.class)
public abstract class MixinBiomeTypes {
    @Redirect(method = {"canSpawnStructureAtCoords", "getStructureStart"}, at = @At(value = "INVOKE", remap = false,
        target = "Lnet/minecraftforge/common/BiomeDictionary;hasType(Lnet/minecraft/world/biome/Biome;Lnet/minecraftforge/common/BiomeDictionary$Type;)Z"), require = 3)
    private boolean nova$liveTypeView(final Biome biome, final BiomeDictionary.Type type) {
        return BiomeTypeViews.hasType(biome, type);
    }
}
