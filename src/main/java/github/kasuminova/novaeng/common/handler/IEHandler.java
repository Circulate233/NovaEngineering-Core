package github.kasuminova.novaeng.common.handler;

import blusunrize.immersiveengineering.api.tool.ExcavatorHandler;
import blusunrize.immersiveengineering.common.blocks.metal.BlockMetalDevice1;
import blusunrize.immersiveengineering.common.blocks.metal.BlockTypes_MetalDevice1;
import blusunrize.immersiveengineering.common.blocks.metal.TileEntitySampleDrill;
import codechicken.lib.util.ItemUtils;
import crafttweaker.api.item.IItemStack;
import crafttweaker.api.minecraft.CraftTweakerMC;
import flaxbeard.immersivepetroleum.api.crafting.PumpjackHandler;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import lombok.val;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import static crafttweaker.CraftTweakerAPI.itemUtils;

public class IEHandler {
    public static final IEHandler INSTANCE = new IEHandler();

    private static final ResourceLocation tky = new ResourceLocation("contenttweaker","tky");
    private static final ResourceLocation scanner = new ResourceLocation("orevisualdetector,scanner");

    private static final byte[] NEXT_MODES = {1, 2, 3, 0};
    private static final String[] MESSAGE_KEYS = {
            "new.orevisualdetector.advanced.tooltips1",
            "new.orevisualdetector.advanced.tooltips2",
            "new.orevisualdetector.advanced.tooltips3",
            "new.orevisualdetector.advanced.tooltips0"
    };

    @SubscribeEvent
    @Optional.Method(modid = "immersivepetroleum")
    public void onPlayerRightClickItem(PlayerInteractEvent.RightClickItem event) {
        val world = event.getWorld();
        val item = event.getEntityPlayer().getHeldItem(EnumHand.MAIN_HAND);
        EntityPlayer player;
        if (event.getHand() != EnumHand.MAIN_HAND
                ||(player = event.getEntityPlayer()) instanceof FakePlayer
                ||event.isCanceled()
                ||world.isRemote
        ) return;
        val eventpos = new BlockPos(event.getPos().getX(),250,event.getPos().getZ());
        if (tky.equals(item.getItem().getRegistryName())){
            giveCoresample(event, world, eventpos, player, item);
        } else if (scanner.equals(item.getItem().getRegistryName())){
            var nbt = item.getTagCompound();
            var mode = nbt.getByte("mode");
            if (player.isSneaking()) {
                nbt.setByte("mode", NEXT_MODES[mode]);
                player.sendMessage(
                        new TextComponentTranslation(
                                "new.orevisualdetector.scan",
                                new TextComponentTranslation(MESSAGE_KEYS[mode])
                        )
                );
                event.setCanceled(true);
            } else if (mode == 3)
                giveCoresample(event, world, eventpos, player, item);
        }
    }

    private static void giveCoresample(PlayerInteractEvent.RightClickItem event, World world, BlockPos eventpos, EntityPlayer player, ItemStack item) {
        world.setBlockState(eventpos, BlockMetalDevice1.getStateById(BlockTypes_MetalDevice1.SAMPLE_DRILL.getMeta()));
        val drill = (TileEntitySampleDrill) world.getTileEntity(eventpos);
        val worldInfo = ExcavatorHandler.getMineralWorldInfo(
                world,
                player.chunkCoordX,
                player.chunkCoordZ
        );
        var coresample = drill.createCoreSample(
                world,
                player.chunkCoordX,
                player.chunkCoordZ,
                worldInfo
        );
        val oilInfo = PumpjackHandler.getOilWorldInfo(
                world,
                player.chunkCoordX,
                player.chunkCoordZ
        );
        if (oilInfo != null && oilInfo.getType() != null) {
            var nbt = coresample.getTagCompound();
            nbt.setString("resType", oilInfo.getType().name);
            nbt.setInteger("oil", oilInfo.current);
        }
        player.inventory.placeItemBackInInventory(player.getEntityWorld(), coresample);
        player.getCooldownTracker().setCooldown(item.getItem(), 20);
        world.setBlockToAir(eventpos);
        event.setCanceled(true);
    }

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
