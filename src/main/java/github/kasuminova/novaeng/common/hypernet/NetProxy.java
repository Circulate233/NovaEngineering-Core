package github.kasuminova.novaeng.common.hypernet;

import com.github.bsideup.jabel.Desugar;
import github.kasuminova.novaeng.common.util.WorldPos;
import net.minecraft.tileentity.TileEntity;

@Desugar
public record NetProxy(TileEntity tile) implements NetNode {

    @Override
    public WorldPos getPos() {
        return WorldPos.of(tile);
    }

}
