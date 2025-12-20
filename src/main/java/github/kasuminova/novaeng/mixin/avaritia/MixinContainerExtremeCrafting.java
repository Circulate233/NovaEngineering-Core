package github.kasuminova.novaeng.mixin.avaritia;

import github.kasuminova.novaeng.NovaEngCoreConfig;
import morph.avaritia.container.ContainerExtremeCrafting;
import morph.avaritia.tile.TileDireCraftingTable;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ContainerExtremeCrafting.class, remap = false)
public abstract class MixinContainerExtremeCrafting extends Container {

    @Redirect(method = "<init>", at =
    @At(value = "INVOKE", target = "Lmorph/avaritia/container/ContainerExtremeCrafting;addSlotToContainer(Lnet/minecraft/inventory/Slot;)Lnet/minecraft/inventory/Slot;", ordinal = 2))
    public Slot onInitO(ContainerExtremeCrafting instance, Slot slot) {
        if (NovaEngCoreConfig.CLIENT.ExtremeCraftingUIModification) {
            return slot;
        } else {
            return this.addSlotToContainer(slot);
        }
    }

    @Redirect(method = "<init>", at =
    @At(value = "INVOKE", target = "Lmorph/avaritia/container/ContainerExtremeCrafting;addSlotToContainer(Lnet/minecraft/inventory/Slot;)Lnet/minecraft/inventory/Slot;", ordinal = 3))
    public Slot onInitT(ContainerExtremeCrafting instance, Slot slot) {
        if (NovaEngCoreConfig.CLIENT.ExtremeCraftingUIModification) {
            return slot;
        } else {
            return this.addSlotToContainer(slot);
        }
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    public void onInit(InventoryPlayer player, World world, BlockPos pos, TileDireCraftingTable table, CallbackInfo ci) {
        if (NovaEngCoreConfig.CLIENT.ExtremeCraftingUIModification) {
            for (int wy = 0; wy < 3; ++wy) {
                for (int ex = 0; ex < 9; ++ex) {
                    addSlotToContainer(new Slot(player, ex + wy * 9 + 9, 39 + ex * 18, 174 + wy * 18));
                }
            }

            for (int ex = 0; ex < 9; ++ex) {
                this.addSlotToContainer(new Slot(player, ex, 39 + ex * 18, 232));
            }
        }
    }
}
