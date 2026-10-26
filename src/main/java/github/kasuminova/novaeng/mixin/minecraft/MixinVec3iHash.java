package github.kasuminova.novaeng.mixin.minecraft;

import net.minecraft.util.math.Vec3i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = Vec3i.class, priority = 999)
public abstract class MixinVec3iHash {

    @Unique
    private static final int MIX_CONSTANT = 0xAD265D9;

    @Shadow
    public abstract int getX();

    @Shadow
    public abstract int getY();

    @Shadow
    public abstract int getZ();

    /**
     * @author circulation
     * @reason Vanilla's degree-two polynomial has very weak low bits for neighbouring block
     * positions. Mix each coordinate before the next one is folded in, then avalanche the final
     * value. The function is deterministic, allocation-free and preserves the equals/hashCode
     * contract for mutable BlockPos instances because it reads the current coordinates every time.
     */
    @Overwrite
    public int hashCode() {
        int hash = Integer.rotateLeft(getX() * MIX_CONSTANT, 16) ^ getY();
        hash = Integer.rotateLeft(hash * MIX_CONSTANT, 16) ^ getZ();
        return hash ^ (hash >>> 16);
    }
}
