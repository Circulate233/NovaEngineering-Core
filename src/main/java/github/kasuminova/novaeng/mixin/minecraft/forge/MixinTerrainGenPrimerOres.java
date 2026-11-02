package github.kasuminova.novaeng.mixin.minecraft.forge;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import github.kasuminova.novaeng.common.performance.PrimerOreGeneration;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenerator;
import net.minecraftforge.event.terraingen.OreGenEvent.GenerateMinable.EventType;
import net.minecraftforge.event.terraingen.TerrainGen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Random;

@Mixin(value = TerrainGen.class, remap = false)
public abstract class MixinTerrainGenPrimerOres {
    @ModifyReturnValue(method = "generateOre(Lnet/minecraft/world/World;Ljava/util/Random;Lnet/minecraft/world/gen/feature/WorldGenerator;Lnet/minecraft/util/math/BlockPos;Lnet/minecraftforge/event/terraingen/OreGenEvent$GenerateMinable$EventType;)Z",
        at = @At("RETURN"))
    private static boolean nova$skipPreviouslyGenerated(final boolean original, final World world,
                                                        final Random rand, final WorldGenerator generator,
                                                        final BlockPos pos, final EventType type) {
        if (!original || PrimerOreGeneration.isGenerating()) return original;
        return !PrimerOreGeneration.wasGenerated(world, pos.getX() >> 4, pos.getZ() >> 4,
            PrimerOreGeneration.identifier("vanilla", type.name()));
    }
}
