package github.kasuminova.novaeng.client.util;

import com.cleanroommc.multiblocked.persistence.MultiblockWorldSavedData;
import hellfirepvp.modularmachinery.common.base.Mods;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import hellfirepvp.modularmachinery.common.util.MiscUtils;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;
import java.util.ArrayList;

public class BlockModelHider {

    @SideOnly(Side.CLIENT)
    public static void hideOrShowBlocks(final List<BlockPos> posList, final TileMultiblockMachineController ctrlPos) {
        if (Mods.MBD.isPresent()) {
            hideOrShowBlocksMBD(posList, ctrlPos);
        } else if (Mods.COMPONENT_MODEL_HIDER.isPresent()) {
            hideOrShowBlocksPlugin(posList, ctrlPos);
        }
    }

    @SuppressWarnings("DuplicatedCode")
    @Optional.Method(modid = "multiblocked")
    private static void hideOrShowBlocksMBD(final List<BlockPos> posList, final TileMultiblockMachineController ctrl) {
        DynamicMachine foundMachine = ctrl.getFoundMachine();
        BlockPos ctrlPos = ctrl.getPos();
        if (ctrl.isInvalid() || foundMachine == null) {
            MultiblockWorldSavedData.removeDisableModel(ctrlPos);
            return;
        }

        final List<BlockPos> hiddenPositions = new ArrayList<>(posList.size());
        for (BlockPos pos : posList)
            hiddenPositions.add(MiscUtils.rotateYCCWNorthUntil(pos, ctrl.getControllerRotation()).add(ctrlPos));
        MultiblockWorldSavedData.addDisableModel(ctrlPos, hiddenPositions);
    }

    @SuppressWarnings("DuplicatedCode")
    @Optional.Method(modid = "component_model_hider")
    private static void hideOrShowBlocksPlugin(final List<BlockPos> posList, final TileMultiblockMachineController ctrl) {
        DynamicMachine foundMachine = ctrl.getFoundMachine();
        BlockPos ctrlPos = ctrl.getPos();
        if (ctrl.isInvalid() || foundMachine == null) {
            MultiblockWorldSavedData.removeDisableModel(ctrlPos);
            return;
        }

        final List<BlockPos> hiddenPositions = new ArrayList<>(posList.size());
        for (BlockPos pos : posList)
            hiddenPositions.add(MiscUtils.rotateYCCWNorthUntil(pos, ctrl.getControllerRotation()).add(ctrlPos));
        MultiblockWorldSavedData.addDisableModel(ctrlPos, hiddenPositions);
    }

}
