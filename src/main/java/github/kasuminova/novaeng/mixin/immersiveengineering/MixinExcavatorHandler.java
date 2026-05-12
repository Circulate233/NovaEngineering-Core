package github.kasuminova.novaeng.mixin.immersiveengineering;

import blusunrize.immersiveengineering.api.DimensionChunkCoords;
import blusunrize.immersiveengineering.api.tool.ExcavatorHandler;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

@Mixin(value = ExcavatorHandler.class, remap = false)
public class MixinExcavatorHandler {

    @Shadow
    public static HashMap<DimensionChunkCoords, ExcavatorHandler.MineralWorldInfo> mineralCache;

    @Shadow
    public static LinkedHashMap<ExcavatorHandler.MineralMix, Integer> mineralList;

    @Shadow
    public static double mineralChance;

    /**
     * @author Circulation
     * @reason 让没有任何矿脉的维度生成主世界的矿脉
     */
    @Overwrite
    public static ExcavatorHandler.MineralWorldInfo getMineralWorldInfo(final World world,
                                                                        final DimensionChunkCoords chunkCoords,
                                                                        final boolean guaranteed) {
        if (world.isRemote) {
            return null;
        }

        ExcavatorHandler.MineralWorldInfo worldInfo = mineralCache.get(chunkCoords);
        if (worldInfo == null) {
            ExcavatorHandler.MineralMix mix = null;
            Random random = world.getChunk(chunkCoords.x, chunkCoords.z).getRandomWithSeed(940610L);
            boolean empty = !guaranteed && random.nextDouble() > mineralChance;

            if (!empty) {
                int mineralDimension = chunkCoords.dimension;
                if (mineralDimension != 0 && novaeng_core$getDimensionTotalWeight(mineralDimension) <= 0) {
                    mineralDimension = 0;
                }

                Set<Map.Entry<ExcavatorHandler.MineralMix, Integer>> selection =
                    novaeng_core$selectMinerals(chunkCoords, mineralDimension);
                int totalWeight = novaeng_core$getTotalWeight(selection);

                if (totalWeight > 0) {
                    int weight = Math.abs(random.nextInt() % totalWeight);

                    for (Map.Entry<ExcavatorHandler.MineralMix, Integer> entry : selection) {
                        weight -= entry.getValue();
                        if (weight < 0) {
                            mix = entry.getKey();
                            break;
                        }
                    }
                }
            }

            worldInfo = new ExcavatorHandler.MineralWorldInfo();
            worldInfo.mineral = mix;
            mineralCache.put(chunkCoords, worldInfo);
        }

        return worldInfo;
    }

    @Unique
    private static int novaeng_core$getDimensionTotalWeight(final int mineralDimension) {
        int totalWeight = 0;
        for (Map.Entry<ExcavatorHandler.MineralMix, Integer> entry : mineralList.entrySet()) {
            ExcavatorHandler.MineralMix mineral = entry.getKey();
            if (mineral.isValid() && mineral.validDimension(mineralDimension)) {
                totalWeight += entry.getValue();
            }
        }
        return totalWeight;
    }

    @Unique
    private static Set<ExcavatorHandler.MineralMix> novaeng_core$getSurroundingMinerals(
        final DimensionChunkCoords chunkCoords) {
        Set<ExcavatorHandler.MineralMix> surrounding = new HashSet<>();
        for (int xx = -2; xx <= 2; ++xx) {
            for (int zz = -2; zz <= 2; ++zz) {
                if (xx == 0 && zz == 0) {
                    continue;
                }

                DimensionChunkCoords offset = chunkCoords.withOffset(xx, zz);
                ExcavatorHandler.MineralWorldInfo worldInfo = mineralCache.get(offset);
                if (worldInfo != null && worldInfo.mineral != null) {
                    surrounding.add(worldInfo.mineral);
                }
            }
        }
        return surrounding;
    }

    @Unique
    private static Set<Map.Entry<ExcavatorHandler.MineralMix, Integer>> novaeng_core$selectMinerals(
        final DimensionChunkCoords chunkCoords,
        final int mineralDimension) {
        Set<ExcavatorHandler.MineralMix> surrounding = novaeng_core$getSurroundingMinerals(chunkCoords);
        Set<Map.Entry<ExcavatorHandler.MineralMix, Integer>> validMinerals = new HashSet<>();

        for (Map.Entry<ExcavatorHandler.MineralMix, Integer> entry : mineralList.entrySet()) {
            ExcavatorHandler.MineralMix mineral = entry.getKey();
            if (mineral.isValid() && mineral.validDimension(mineralDimension) && !surrounding.contains(mineral)) {
                validMinerals.add(entry);
            }
        }

        return validMinerals;
    }

    @Unique
    private static int novaeng_core$getTotalWeight(
        final Set<Map.Entry<ExcavatorHandler.MineralMix, Integer>> minerals) {
        int totalWeight = 0;
        for (Map.Entry<ExcavatorHandler.MineralMix, Integer> entry : minerals) {
            totalWeight += entry.getValue();
        }
        return totalWeight;
    }

}
