package github.kasuminova.novaeng.mixin.rtg;

import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import rtg.api.util.noise.SpacedCellularNoise;
import rtg.api.util.noise.VoronoiResult;

import java.awt.Point;
import java.awt.geom.Point2D;

@Mixin(value = SpacedCellularNoise.class, remap = false)
public abstract class MixinSpacedCellularNoiseBounds {

    @Unique
    private static final Point2D.Double[] nova$emptyPoints = new Point2D.Double[0];

    @Shadow
    private Point2D.Double[] areaPoints(final Point area) {
        throw new AssertionError();
    }

    @Redirect(method = "eval2D(DD)Lrtg/api/util/noise/VoronoiResult;",
        at = @At(value = "INVOKE", target = "Lrtg/api/util/noise/SpacedCellularNoise;areaPoints(Ljava/awt/Point;)[Ljava/awt/geom/Point2D$Double;"),
        require = 9)
    private Point2D.Double[] nova$skipDistantRegion(final SpacedCellularNoise noise, final Point area,
                                                    @Local(name = "result") final VoronoiResult result,
                                                    @Local(argsOnly = true, ordinal = 0) final double x,
                                                    @Local(argsOnly = true, ordinal = 1) final double y) {
        final double dx = x < area.x ? area.x - x : x > area.x + 1.0 ? x - (area.x + 1.0) : 0.0;
        final double dy = y < area.y ? area.y - y : y > area.y + 1.0 ? y - (area.y + 1.0) : 0.0;
        // Every generated point lies in this closed unit square. Equal distances never replace a result.
        return dx * dx + dy * dy >= result.getNextDistance() ? nova$emptyPoints : this.areaPoints(area);
    }
}
