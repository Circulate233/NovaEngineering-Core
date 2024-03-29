package github.kasuminova.novaeng.common.item;

import appeng.me.helpers.AENetworkProxy;
import appeng.me.helpers.IGridProxyable;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

public class ItemBlockME extends ItemBlock {

    public ItemBlockME(final Block block) {
        super(block);
    }

    @Override
    public boolean placeBlockAt(@Nonnull final ItemStack stack,
                                @Nonnull final EntityPlayer player,
                                @Nonnull final World world,
                                @Nonnull final BlockPos pos,
                                @Nonnull final EnumFacing side,
                                final float hitX,
                                final float hitY,
                                final float hitZ,
                                @Nonnull final IBlockState newState)
    {
        if (super.placeBlockAt(stack, player, world, pos, side, hitX, hitY, hitZ, newState)) {
            TileEntity tile = world.getTileEntity(pos);
            if (tile instanceof IGridProxyable) {
                AENetworkProxy proxy = ((IGridProxyable) tile).getProxy();
                proxy.setOwner(player);
            }
            return true;
        }
        return false;
    }

}
