package github.kasuminova.novaeng.mixin.ae2exttable;

import appeng.api.parts.IPart;
import appeng.container.AEBaseContainer;
import appeng.container.implementations.ContainerCraftConfirm;
import com._0xc4de.ae2exttable.network.ExtendedTerminalNetworkHandler;
import com._0xc4de.ae2exttable.network.packets.PacketSwitchGui;
import github.kasuminova.novaeng.common.item.ItemWirelessUniversalTerminal;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.tileentity.TileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value= ContainerCraftConfirm.class, remap=false)
public class MixinContainerCraftConfirm extends AEBaseContainer {

    public MixinContainerCraftConfirm(InventoryPlayer ip, TileEntity myTile, IPart myPart) {
        super(ip, myTile, myPart);
    }

    @Inject(method="startJob", at=@At(value="INVOKE", target="Lappeng/container/implementations/ContainerCraftConfirm;setAutoStart(Z)V", shift=At.Shift.AFTER), cancellable=true)
    public void startJobMixin(CallbackInfo ci) {
        if (this.obj != null && this.obj.getItemStack().getItem() instanceof ItemWirelessUniversalTerminal t) {
            switch (this.obj.getItemStack().getTagCompound().getInteger("mode")) {
                case 6, 7, 8, 9: {
                    ExtendedTerminalNetworkHandler.instance().sendToServer(new PacketSwitchGui(t.getGuiType(this.obj.getItemStack())));
                    ci.cancel();
                }
            }
        }
    }
}

