package github.kasuminova.novaeng.common.block.ecotech.efabricator;

import github.kasuminova.novaeng.novaeng_core.Tags;
import net.minecraft.util.ResourceLocation;

public class BlockEFabricatorCasing extends BlockEFabricator {

    public static final BlockEFabricatorCasing INSTANCE = new BlockEFabricatorCasing();

    protected BlockEFabricatorCasing() {
        this.setDefaultState(this.blockState.getBaseState());
        this.setRegistryName(new ResourceLocation(Tags.MOD_ID, "efabricator_casing"));
        this.setTranslationKey(Tags.MOD_ID + '.' + "efabricator_casing");
    }

}
