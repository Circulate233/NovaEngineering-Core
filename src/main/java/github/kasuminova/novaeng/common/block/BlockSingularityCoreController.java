package github.kasuminova.novaeng.common.block;

import github.kasuminova.novaeng.NovaEngineeringCore;
import github.kasuminova.novaeng.common.CommonProxy;
import github.kasuminova.novaeng.common.tile.machine.SingularityCore;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.block.BlockController;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.machine.MachineRegistry;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BlockSingularityCoreController extends BlockController {
    public static final BlockSingularityCoreController INSTANCE = new BlockSingularityCoreController();

    private BlockSingularityCoreController() {
        setRegistryName(new ResourceLocation(NovaEngineeringCore.MOD_ID, "singularity_core_controller"));
        setTranslationKey(NovaEngineeringCore.MOD_ID + '.' + "singularity_core_controller");
    }

    public DynamicMachine getParentMachine() {
        return MachineRegistry.getRegistry().getMachine(
                new ResourceLocation(ModularMachinery.MODID, "singularity_core")
        );
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new SingularityCore(state);
    }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new SingularityCore();
    }

    @Override
    public boolean onBlockActivated(final World worldIn, @Nonnull final BlockPos pos, @Nonnull final IBlockState state, @Nonnull final EntityPlayer playerIn, @Nonnull final EnumHand hand, @Nonnull final EnumFacing facing, final float hitX, final float hitY, final float hitZ) {
        if (!worldIn.isRemote) {
            TileEntity te = worldIn.getTileEntity(pos);
            if (te instanceof SingularityCore) {
                playerIn.openGui(NovaEngineeringCore.MOD_ID, CommonProxy.GuiType.SINGULARITY_CORE.ordinal(), worldIn, pos.getX(), pos.getY(), pos.getZ());
            }
        }
        return true;
    }

    @Nonnull
    @Override
    @SideOnly(Side.CLIENT)
    public String getLocalizedName() {
        return I18n.format("tile.novaeng_core.singularity_core_controller.name");
    }

    @Override
    public boolean canConnectRedstone(@Nonnull IBlockState state, @Nonnull IBlockAccess world, @Nonnull BlockPos pos, @Nullable EnumFacing side) {
        return false;
    }

    @Override
    public boolean hasComparatorInputOverride(@Nonnull IBlockState state) {
        return false;
    }
}
