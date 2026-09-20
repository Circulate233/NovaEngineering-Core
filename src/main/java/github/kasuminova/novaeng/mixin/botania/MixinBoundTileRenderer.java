package github.kasuminova.novaeng.mixin.botania;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vazkii.botania.api.BotaniaAPI;
import vazkii.botania.api.item.IWireframeCoordinateListProvider;
import vazkii.botania.api.wand.ICoordBoundItem;
import vazkii.botania.client.core.handler.BoundTileRenderer;

@Mixin(value = BoundTileRenderer.class, remap = false)
public class MixinBoundTileRenderer {

    @Inject(method = "onWorldRenderLast", at = @At("HEAD"), cancellable = true)
    private static void nova$skipWhenNoSource(final RenderWorldLastEvent event, final CallbackInfo ci) {
        final EntityPlayer player = Minecraft.getMinecraft().player;
        if (player == null) {
            return;
        }
        if (!nova$computeRenderSource(player)) {
            ci.cancel();
        }
    }

    @Unique
    private static boolean nova$computeRenderSource(final EntityPlayer player) {
        if (nova$isCoordBound(player.getHeldItemMainhand()) || nova$isCoordBound(player.getHeldItemOffhand())) {
            return true;
        }
        final IItemHandlerModifiable playerInv = (IItemHandlerModifiable) player.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.UP);
        final IItemHandlerModifiable baubles = BotaniaAPI.internalHandler.getBaublesInventoryWrapped(player);
        return nova$hasWireframeProvider(playerInv) || nova$hasWireframeProvider(baubles);
    }

    @Unique
    private static boolean nova$isCoordBound(final ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof ICoordBoundItem;
    }

    @Unique
    private static boolean nova$hasWireframeProvider(final IItemHandlerModifiable inventory) {
        if (inventory == null) {
            return false;
        }
        for (int i = 0; i < inventory.getSlots(); i++) {
            final ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() instanceof IWireframeCoordinateListProvider) {
                return true;
            }
        }
        return false;
    }
}
