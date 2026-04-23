package github.kasuminova.novaeng.common.hypernet;

import github.kasuminova.novaeng.common.util.WorldPos;
import net.minecraft.tileentity.TileEntity;

public record NetProxy(TileEntity tile) implements NetNode {

    @Override
    public WorldPos getPos() {
        return WorldPos.of(tile);
    }

}
