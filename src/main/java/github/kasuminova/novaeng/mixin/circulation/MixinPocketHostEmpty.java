package github.kasuminova.novaeng.mixin.circulation;

import com.circulation.circulation_networks.manager.PocketNodeManager;
import com.circulation.circulation_networks.manager.PocketNodePendingIndex;
import com.circulation.circulation_networks.pocket.PocketNodeHost;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PocketNodeManager.class, remap = false)
public abstract class MixinPocketHostEmpty {
    @Shadow
    @Final
    private Int2ObjectMap<Long2ObjectMap<PocketNodeHost>> activeHosts;
    @Shadow
    @Final
    private PocketNodePendingIndex pendingHosts;

    @Inject(method = "onHostBlockStateChanged", at = @At("HEAD"), cancellable = true, require = 1)
    private void nova$skipEmptyHosts(final World world, final BlockPos pos, final CallbackInfo ci) {
        if (activeHosts.isEmpty() && pendingHosts.isEmpty()) ci.cancel();
    }
}
