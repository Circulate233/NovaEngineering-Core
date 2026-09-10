package github.kasuminova.novaeng.common.block.ecotech.estorage;

import github.kasuminova.novaeng.novaeng_core.Tags;
import net.minecraft.util.ResourceLocation;

public class BlockEStorageCasing extends BlockEStorage {

    public static final BlockEStorageCasing INSTANCE = new BlockEStorageCasing();

    protected BlockEStorageCasing() {
        this.setDefaultState(this.blockState.getBaseState());
        this.setRegistryName(new ResourceLocation(Tags.MOD_ID, "estorage_casing"));
        this.setTranslationKey(Tags.MOD_ID + '.' + "estorage_casing");
    }

}
