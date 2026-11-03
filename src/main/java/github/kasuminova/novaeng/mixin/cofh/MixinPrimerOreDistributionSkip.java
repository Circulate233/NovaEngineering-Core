package github.kasuminova.novaeng.mixin.cofh;

import cofh.cofhworld.world.distribution.Distribution;
import com.llamalad7.mixinextras.sugar.Local;
import github.kasuminova.novaeng.common.performance.PrimerOreGeneration;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Random;

@Mixin(value = Distribution.class, remap = false)
public abstract class MixinPrimerOreDistributionSkip {
    @Shadow
    @Final
    public String name;

    @Shadow
    protected int rarity;

    @Unique
    private long nova$identifier;
    @Unique
    private volatile boolean nova$identifierInitialized;

    /**
     * Return a nonzero result to take CoFH's existing failed-rarity return without consuming RNG.
     */
    @Redirect(method = "generateFeature(Ljava/util/Random;IILnet/minecraft/world/World;ZZ)Z",
        at = @At(value = "INVOKE", target = "Ljava/util/Random;nextInt(I)I"), require = 1)
    private int nova$skipMarkedRarityRoll(final Random random, final int bound,
                                          @Local(argsOnly = true, ordinal = 0) final int chunkX,
                                          @Local(argsOnly = true, ordinal = 1) final int chunkZ,
                                          @Local(argsOnly = true) final World world) {
        return this.nova$wasPreviouslyGenerated(world, chunkX, chunkZ) ? 1 : random.nextInt(bound);
    }

    /**
     * Rarity values of one or less bypass the random call entirely.
     */
    @Redirect(method = "generateFeature(Ljava/util/Random;IILnet/minecraft/world/World;ZZ)Z",
        at = @At(value = "INVOKE",
            target = "Lcofh/cofhworld/world/distribution/Distribution;generateFeature(Ljava/util/Random;IILnet/minecraft/world/World;)Z"),
        require = 1)
    private boolean nova$skipMarkedUnrolledFeature(final Distribution distribution, final Random random,
                                                   final int x, final int z, final World world,
                                                   @Local(argsOnly = true, ordinal = 0) final int chunkX,
                                                   @Local(argsOnly = true, ordinal = 1) final int chunkZ) {
        if (this.rarity <= 1 && this.nova$wasPreviouslyGenerated(world, chunkX, chunkZ)) {
            return false;
        }
        return distribution.generateFeature(random, x, z, world);
    }

    @Unique
    private boolean nova$wasPreviouslyGenerated(final World world, final int chunkX, final int chunkZ) {
        if (!this.nova$identifierInitialized) {
            this.nova$identifier = PrimerOreGeneration.identifier("cofh", this.name);
            this.nova$identifierInitialized = true;
        }
        return PrimerOreGeneration.wasGenerated(world, chunkX, chunkZ,
            this.nova$identifier);
    }
}
