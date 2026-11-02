package github.kasuminova.novaeng.common.machine;

import github.kasuminova.mmce.common.event.machine.MachineStructureUpdateEvent;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import hellfirepvp.modularmachinery.common.util.MiscUtils;
import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.ProbeMode;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Arrays;
import java.util.List;

public class StardustDisintegrator implements MachineSpecial {
    public static final ResourceLocation REGISTRY_NAME = new ResourceLocation(ModularMachinery.MODID, "stardust_disintegrator");
    public static final StardustDisintegrator INSTANCE = new StardustDisintegrator();

    public static final List<BlockPos> CRYSTALS_POS_PRESET = Arrays.asList(
        // LEFT - MID - RIGHT
        withXZ(-6, 6), withXZ(0, 12), withXZ(6, 6)
    );

    private StardustDisintegrator() {
    }

    protected static void checkStructure(final MachineStructureUpdateEvent event) {
        TileMultiblockMachineController ctrl = event.getController();
        BlockPos ctrlPos = ctrl.getPos();
        World world = ctrl.getWorld();
        EnumFacing facing = ctrl.getControllerRotation();

        boolean obstructed = false;
        for (BlockPos pos : CRYSTALS_POS_PRESET) {
            if (!world.canSeeSky(ctrlPos.add(MiscUtils.rotateYCCWNorthUntil(pos, facing)))) {
                obstructed = true;
                break;
            }
        }
        if (obstructed) {
            ctrl.getCustomDataTag().removeTag("canSeeSky");
        } else {
            ctrl.getCustomDataTag().setBoolean("canSeeSky", true);
        }
    }

    protected static BlockPos withXZ(final int x, final int z) {
        return new BlockPos(x, 0, z);
    }

    @Override
    public void init(final DynamicMachine machine) {
//        machine.addMachineEventHandler(FactoryRecipeFinishEvent.class, event -> {
//
//        });
    }

    @Override
    public void onSyncTick(final TileMultiblockMachineController controller) {
        MachineSpecial.super.onSyncTick(controller);
    }

    @Override
    public void onTOPInfo(final ProbeMode probeMode,
                          final IProbeInfo probeInfo,
                          final EntityPlayer player,
                          final IProbeHitData data,
                          final TileMultiblockMachineController controller) {

        MachineSpecial.super.onTOPInfo(probeMode, probeInfo, player, data, controller);
    }

    @Override
    public ResourceLocation getRegistryName() {
        return REGISTRY_NAME;
    }

}
