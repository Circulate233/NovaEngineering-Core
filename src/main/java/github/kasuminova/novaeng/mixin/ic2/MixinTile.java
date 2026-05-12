package github.kasuminova.novaeng.mixin.ic2;

import ic2.api.energy.EnergyNet;
import ic2.api.energy.tile.IEnergyTile;
import ic2.core.energy.grid.Tile;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.List;

@Mixin(value = Tile.class, remap = false)
public class MixinTile {

    @Shadow
    @Final
    List<IEnergyTile> subTiles;
    @Unique
    private static final IEnergyTile N_E = new IEnergyTile() {};
    @Unique
    private final Long2ObjectMap<IEnergyTile> n_cache = new Long2ObjectOpenHashMap<>();

    {
        n_cache.defaultReturnValue(N_E);
    }

    /**
     * @author circulation
     * @reason 一个索引式优化尝试？
     */
    @Overwrite
    IEnergyTile getSubTileAt(BlockPos pos) {
        if (subTiles.size() < 8) {
            return n_g(pos);
        }
        var t = n_cache.get(pos.toLong());
        if (t == null) {
            return null;
        }
        if (t == n_cache.defaultReturnValue()) {
            n_cache.put(pos.toLong(), t = n_g(pos));
        }
        return t;
    }

    @Unique
    private IEnergyTile n_g(BlockPos pos) {
        for(IEnergyTile subTile : this.subTiles) {
            if (EnergyNet.instance.getPos(subTile).equals(pos)) {
                return subTile;
            }
        }

        return null;
    }

}
