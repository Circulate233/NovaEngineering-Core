package github.kasuminova.novaeng.common.handler;

import codechicken.lib.util.ItemUtils;
import crafttweaker.api.item.IItemStack;
import crafttweaker.api.minecraft.CraftTweakerMC;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import static crafttweaker.CraftTweakerAPI.itemUtils;

public class DrillHandler {
    public static final DrillHandler INSTANCE = new DrillHandler();

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        var world = event.getWorld();
        if (world.isRemote) {
            return;
        }
        var tile = world.getTileEntity(event.getPos());
        if (!(tile instanceof TileMultiblockMachineController ctrl)) return;
        var pos = event.getPos();
        var data = ctrl.getCustomDataTag();
        for (int i = 0; i < 4; i++) {
            var component = data.getByte("additional_component_" + i);
            if (component == 1 && !event.getPlayer().isCreative()) {
                dropItem(world, pos, itemUtils.getItem("contenttweaker:additional_component_" + i, 0));
            }
        }
        var component = data.getBoolean("additional_component_raw_ore");
        if (component && !event.getPlayer().isCreative()) {
            dropItem(world, pos, itemUtils.getItem("contenttweaker:additional_component_raw_ore", 0));
        }
    }

    private static void dropItem(World world, BlockPos pos, IItemStack item) {
        ItemUtils.dropItem(world, pos, CraftTweakerMC.getItemStack(item));
    }
}
