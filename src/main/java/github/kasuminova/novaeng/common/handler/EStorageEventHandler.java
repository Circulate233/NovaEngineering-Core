package github.kasuminova.novaeng.common.handler;

import ae2.api.networking.IGridNode;
import ae2.api.networking.security.IActionHost;
import ae2.api.storage.StorageCells;
import ae2.api.util.DimensionalBlockPos;
import ae2.util.inv.AppEngCellInventory;
import ae2.util.Platform;
import github.kasuminova.novaeng.NovaEngineeringCore;
import github.kasuminova.novaeng.common.container.ContainerEStorageController;
import github.kasuminova.novaeng.common.network.PktEStorageGUIData;
import github.kasuminova.novaeng.common.tile.ecotech.estorage.EStorageCellDrive;
import github.kasuminova.novaeng.common.tile.ecotech.estorage.EStorageController;
import github.kasuminova.novaeng.common.tile.ecotech.estorage.EStorageMEChannel;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

@SuppressWarnings("MethodMayBeStatic")
public class EStorageEventHandler {

    public static final EStorageEventHandler INSTANCE = new EStorageEventHandler();

    private static boolean canInteract(final EntityPlayer player,
                                      final TileEntity target,
                                      final IActionHost actionHost) {
        final IGridNode gn = actionHost.getActionableNode();
        if (gn != null) {
            if (!gn.isPowered()) {
                return true;
            }
            return Platform.hasPermissions(new DimensionalBlockPos(target), player);
        }
        return true;
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        World world = event.getWorld();
        if (world.isRemote) {
            return;
        }

        EnumHand hand = event.getHand();
        if (hand != EnumHand.MAIN_HAND) {
            return;
        }

        EntityPlayer player = event.getEntityPlayer();
        if (!player.isSneaking()) {
            return;
        }

        TileEntity te = world.getTileEntity(event.getPos());
        if (!(te instanceof final EStorageCellDrive drive)) {
            return;
        }

        EStorageController controller = drive.getController();
        if (controller != null) {
            EStorageMEChannel channel = controller.getChannel();
            if (channel != null && !canInteract(player, drive, channel)) {
                player.sendMessage(new TextComponentTranslation("novaeng.estorage_cell_drive.player.no_permission"));
                event.setCanceled(true);
                return;
            }
        }

        ItemStack stackInHand = player.getHeldItem(hand);

        AppEngCellInventory inv = drive.getDriveInv();
        ItemStack stackInSlot = inv.getStackInSlot(0);
        if (stackInSlot.isEmpty()) {
            if (stackInHand.isEmpty() || !StorageCells.isCellHandled(stackInHand)) {
                return;
            }
            player.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, inv.insertItem(0, stackInHand.copy(), false));
            player.sendMessage(new TextComponentTranslation("novaeng.estorage_cell_drive.player.inserted"));
            event.setCanceled(true);
            return;
        }

        if (!stackInHand.isEmpty()) {
            return;
        }

        player.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, inv.extractItem(0, stackInSlot.getCount(), false));
        player.sendMessage(new TextComponentTranslation("novaeng.estorage_cell_drive.player.removed"));
        event.setCanceled(true);
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START || event.side == Side.CLIENT) {
            return;
        }
        if (!(event.player instanceof final EntityPlayerMP player)) {
            return;
        }
        if (!(player.openContainer instanceof ContainerEStorageController containerESController)) {
            return;
        }
        World world = player.getEntityWorld();
        int tickExisted = containerESController.getTickExisted();
        containerESController.setTickExisted(tickExisted + 1);
        if (world.getTotalWorldTime() % 20 != 0 && tickExisted > 1) {
            return;
        }
        EStorageController controller = containerESController.getOwner();
        NovaEngineeringCore.NET_CHANNEL.sendTo(new PktEStorageGUIData(controller), player);
    }

}
