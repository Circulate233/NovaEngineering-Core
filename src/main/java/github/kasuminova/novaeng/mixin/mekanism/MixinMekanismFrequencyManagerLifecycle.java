package github.kasuminova.novaeng.mixin.mekanism;

import github.kasuminova.novaeng.common.util.FrequencyManagerRegistryAccess;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import mekanism.common.Mekanism;
import mekanism.common.frequency.FrequencyManager;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Mixin(value = Mekanism.class, remap = false)
public abstract class MixinMekanismFrequencyManagerLifecycle {
    @Shadow
    public static Map<UUID, FrequencyManager> privateTeleporters;

    @Shadow
    public static Map<UUID, FrequencyManager> privateEntangloporters;

    @Shadow
    public static FrequencyManager publicTeleporters;

    @Shadow
    public static FrequencyManager publicEntangloporters;

    @Shadow
    public static FrequencyManager securityFrequencies;

    @Unique
    private Set<FrequencyManager> nova$discardedFrequencyManagers;

    @Inject(method = "serverStopping", at = @At("HEAD"), require = 1)
    private void nova$capturePrivateManagers(final FMLServerStoppingEvent event, final CallbackInfo ci) {
        // These two maps are cleared by serverStopping before FrequencyManager.reset(). Ownership is
        // proven by membership, not by UUID/name; other mods may register their own private managers.
        final Set<FrequencyManager> discarded = new ReferenceOpenHashSet<>(privateTeleporters.values());
        discarded.addAll(privateEntangloporters.values());
        this.nova$discardedFrequencyManagers = discarded;
    }

    @Inject(method = "serverStopping", at = @At(value = "INVOKE",
        target = "Lmekanism/common/frequency/FrequencyManager;reset()V", shift = At.Shift.AFTER), require = 1)
    private void nova$releaseDiscardedPrivateManagers(final FMLServerStoppingEvent event, final CallbackInfo ci) {
        final Set<FrequencyManager> discarded = this.nova$discardedFrequencyManagers;
        this.nova$discardedFrequencyManagers = null;
        if (discarded == null) {
            return;
        }
        for (final FrequencyManager manager : discarded) {
            // Leave any manager still owned by a core map or public singleton registered. Never
            // detach a FrequencyDataHandler while the world is running or on tile invalidation.
            if (manager != publicTeleporters && manager != publicEntangloporters && manager != securityFrequencies
                && !privateTeleporters.containsValue(manager) && !privateEntangloporters.containsValue(manager)) {
                ((FrequencyManagerRegistryAccess) manager).nova$unregisterDiscardedManager();
            }
        }
    }
}
