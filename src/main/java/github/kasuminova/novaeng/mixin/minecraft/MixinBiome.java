package github.kasuminova.novaeng.mixin.minecraft;

import github.kasuminova.novaeng.common.util.NovaBiomeIdCache;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.RegistryNamespaced;
import net.minecraft.world.biome.Biome;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Biome.class)
public class MixinBiome {

    @Shadow
    @Final
    public static RegistryNamespaced<ResourceLocation, Biome> REGISTRY;

    /**
     * @author circulaiton
     * @reason Answer from an id table instead of the registry; see NovaBiomeIdCache. A miss still
     * asks the registry, so this cannot report a biome that the registry does not have.
     */
    @Overwrite
    public static @Nullable Biome getBiomeForId(int id) {
        final Biome cached = NovaBiomeIdCache.get(id);
        return cached != null ? cached : REGISTRY.getObjectById(id);
    }

}
