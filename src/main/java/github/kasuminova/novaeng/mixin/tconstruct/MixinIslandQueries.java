package github.kasuminova.novaeng.mixin.tconstruct;

import github.kasuminova.novaeng.common.performance.IslandBoxList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import slimeknights.tconstruct.world.worldgen.SlimeIslandData;
import slimeknights.tconstruct.world.worldgen.SlimeIslandGenerator;

import java.util.List;

@Mixin(value = SlimeIslandGenerator.class, remap = false)
public abstract class MixinIslandQueries {
    @Shadow
    protected abstract SlimeIslandData getIslandData(World world);

    /**
     * @author NovaEngineering
     * @reason Revalidate positive spatial hints while preserving live list changes, mutable bounds, and custom queries.
     */
    @Overwrite
    public boolean isSlimeIslandAt(final World world, final BlockPos pos) {
        final List<StructureBoundingBox> boxes = getIslandData(world).getIslands();
        if (boxes instanceof IslandBoxList indexed) return indexed.containsPosition(pos);
        for (final StructureBoundingBox box : boxes) if (box.isVecInside(pos)) return true;
        return false;
    }
}
