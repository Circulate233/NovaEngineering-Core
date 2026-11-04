package github.kasuminova.novaeng.mixin.mmce;

import ae2.api.AECapabilities;
import ae2.api.networking.security.IActionSource;
import ae2.api.storage.MEStorage;
import github.kasuminova.novaeng.common.integration.ae2.AeNetworkItemHandler;
import hellfirepvp.modularmachinery.common.tiles.TileItemOutputBus;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = TileItemOutputBus.class, remap = false)
public abstract class MixinItemOutputBusNetworkTarget {

    @Redirect(method = "doRestrictedTick", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/tileentity/TileEntity;getCapability(Lnet/minecraftforge/common/capabilities/Capability;Lnet/minecraft/util/EnumFacing;)Ljava/lang/Object;",
        remap = false), require = 1)
    private Object nova$resolveNetworkTarget(final TileEntity target, final Capability<?> capability,
                                             final EnumFacing side) {
        final Object resolved = target.getCapability(capability, side);
        if (resolved != null || capability != CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return resolved;
        }
        if (AECapabilities.ME_STORAGE == null) {
            return null;
        }

        final MEStorage storage = target.getCapability(AECapabilities.ME_STORAGE, side);
        return storage == null ? null : new AeNetworkItemHandler(storage, IActionSource.empty());
    }

}
