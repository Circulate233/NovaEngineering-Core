package github.kasuminova.novaeng.mixin.mekanism;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import github.kasuminova.novaeng.common.performance.NarrowSnapshot;
import mekanism.client.render.DimensionalStabilizerOverlayRenderer;
import mekanism.common.tile.machine.TileEntityDimensionalStabilizer;
import net.minecraft.tileentity.TileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.Collection;

@Mixin(value = DimensionalStabilizerOverlayRenderer.class, remap = false)
public abstract class MixinDimensionalStabilizerOverlaySnapshot {
    @WrapOperation(method = "render", at = @At(value = "NEW", target = "(Ljava/util/Collection;)Ljava/util/ArrayList;"), require = 1)
    private static ArrayList<TileEntity> nova$narrowTileSnapshot(final Collection<TileEntity> c,
                                                                 final Operation<ArrayList<TileEntity>> original) {
        final ArrayList<TileEntity> snapshot = NarrowSnapshot.arrayListOfType(c, TileEntityDimensionalStabilizer.class);
        return snapshot != null ? snapshot : original.call(c);
    }
}
