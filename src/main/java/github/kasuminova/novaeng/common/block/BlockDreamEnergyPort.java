package github.kasuminova.novaeng.common.block;

import com.circulation.circulation_networks.api.API;
import com.circulation.circulation_networks.api.node.NodeType;
import com.circulation.circulation_networks.blocks.nodes.BaseNodeBlock;
import com.circulation.circulation_networks.tooltip.LocalizedComponent;
import com.github.bsideup.jabel.Desugar;
import github.kasuminova.novaeng.NovaEngineeringCore;
import github.kasuminova.novaeng.common.core.CreativeTabNovaEng;
import github.kasuminova.novaeng.common.tile.TileDreamEnergyPort;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.List;

public class BlockDreamEnergyPort extends BaseNodeBlock {
    public static final BlockDreamEnergyPort INSTANCE = new BlockDreamEnergyPort();
    public static final NodeType<TileDreamEnergyPort.DreamNode> TYPE = new DreamNodeType("dream_core", TileDreamEnergyPort.DreamNode.class, false);

    static {
        API.registerNodeType(TYPE, TileDreamEnergyPort.DreamNode::new, TileDreamEnergyPort.DreamNode::new);
    }

    private BlockDreamEnergyPort() {
        super(NovaEngineeringCore.MOD_ID, "DreamEnergyPort");
        this.setTranslationKey(NovaEngineeringCore.MOD_ID + '.' + "dream_energy_port");
        this.setCreativeTab(CreativeTabNovaEng.INSTANCE);
        this.setNodeTileClass(TileDreamEnergyPort.class);
    }

    public @NotNull String getTranslationKey() {
        return "tile.novaeng_core.dream_energy_port";
    }

    @Override
    protected List<LocalizedComponent> buildTooltips(ItemStack stack) {
        var tooltip = super.buildTooltips(stack);
        tooltip.add(LocalizedComponent.of("text.dream_energy_port.0"));
        tooltip.add(LocalizedComponent.of("text.dream_energy_port.1"));
        return tooltip;
    }

    @Nonnull
    @Override
    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    @Nonnull
    @Override
    public EnumBlockRenderType getRenderType(@Nonnull IBlockState state) {
        return EnumBlockRenderType.MODEL;
    }

    @Desugar
    private record DreamNodeType(String id, Class<TileDreamEnergyPort.DreamNode> nodeClass,
                                 boolean allowsPocketNode) implements NodeType<TileDreamEnergyPort.DreamNode> {

    }
}