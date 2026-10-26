package github.kasuminova.novaeng.mixin.minecraft;

import github.kasuminova.novaeng.common.phys.NovaAabbIntercept;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;

@Mixin(AxisAlignedBB.class)
public abstract class MixinAxisAlignedBBIntercept {
    @Shadow
    @Final
    public double minX;
    @Shadow
    @Final
    public double minY;
    @Shadow
    @Final
    public double minZ;
    @Shadow
    @Final
    public double maxX;
    @Shadow
    @Final
    public double maxY;
    @Shadow
    @Final
    public double maxZ;

    /**
     * @author circulation
     * @reason Keep face candidates in primitive locals and retain vanilla's face order and tie
     * selection while avoiding the temporary Vec3d objects allocated by each face probe.
     */
    @Nullable
    @Overwrite
    public RayTraceResult calculateIntercept(final Vec3d start, final Vec3d end) {
        return NovaAabbIntercept.calculate(this.minX, this.minY, this.minZ,
            this.maxX, this.maxY, this.maxZ, start, end);
    }
}
