package github.kasuminova.novaeng.common.block;

import github.kasuminova.novaeng.NovaEngineeringCore;
import github.kasuminova.novaeng.common.core.CreativeTabNovaEng;
import github.kasuminova.novaeng.common.tile.TileDreamEnergyPort;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import sonar.fluxnetworks.common.block.BlockFluxStorage;
import sonar.fluxnetworks.common.registry.RegistryBlocks;
import sonar.fluxnetworks.common.registry.RegistryItems;

import javax.annotation.Nullable;
import java.util.List;

public class BlockDreamEnergyPort extends BlockFluxStorage {
    public static final BlockDreamEnergyPort INSTANCE = new BlockDreamEnergyPort();

    private BlockDreamEnergyPort() {
        super("DreamEnergyPort");
        this.setTranslationKey(NovaEngineeringCore.MOD_ID + '.' + "dream_energy_port");
        this.setCreativeTab(CreativeTabNovaEng.INSTANCE);
        RegistryBlocks.BLOCKS.remove(this);
        RegistryItems.ITEMS.remove(RegistryItems.ITEMS.size() - 1);
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileDreamEnergyPort();
    }

    @Override
    public int getMaxStorage() {
        return Integer.MAX_VALUE;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World player, List<String> tooltip, ITooltipFlag advanced) {
        super.addInformation(stack, player, tooltip, advanced);
        tooltip.add(I18n.format("text.dream_energy_port.0"));
        tooltip.add(I18n.format("text.dream_energy_port.1"));
    }
}
