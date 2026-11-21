package github.kasuminova.novaeng.mixin.mekanism;

import github.kasuminova.novaeng.common.util.FrequencyManagerRegistryAccess;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import mekanism.common.frequency.Frequency;
import mekanism.common.frequency.FrequencyManager;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Set;

@Mixin(value = FrequencyManager.class, remap = false)
public abstract class MixinFrequencyManagerTick implements FrequencyManagerRegistryAccess {
    @Shadow
    public static boolean loaded;
    @Shadow
    private static Set<FrequencyManager> managers;
    @Shadow
    private Int2ObjectMap<Frequency> frequencies;

    @Shadow
    public static void load(final World world) {
        throw new AssertionError();
    }

    /**
     * @author NovaEngineering
     * @reason Avoid a parallel stream per world tick and keep world reads on the server thread.
     */
    @Overwrite
    public static void tick(final World world) {
        if (!loaded) {
            load(world);
        }
        if (managers.isEmpty()) {
            return;
        }
        for (final FrequencyManager manager : managers) {
            // A virtual call preserves tickSelf overrides in third-party manager subclasses.
            manager.tickSelf(world);
        }
    }

    // Keep Mekanism's coordinate checks, dimension filtering and IFrequencyHandler callback intact.
    @Shadow
    private void lambda$tickSelf$1(final World world, final Frequency frequency) {
        throw new AssertionError();
    }

    /**
     * @author NovaEngineering
     * @reason Avoid stream/lambda allocation, especially for the many empty private managers.
     */
    @Overwrite
    public void tickSelf(final World world) {
        if (this.frequencies.isEmpty()) {
            return;
        }
        for (final Frequency frequency : this.frequencies.values()) {
            this.lambda$tickSelf$1(world, frequency);
        }
    }

    @Override
    public void nova$unregisterDiscardedManager() {
        managers.remove((FrequencyManager) (Object) this);
    }
}
