package github.kasuminova.novaeng.common.container;

import github.kasuminova.novaeng.common.tile.TileModularServerFrame;
import hellfirepvp.modularmachinery.common.container.ContainerBase;
import net.minecraft.entity.player.EntityPlayer;

public final class ContainerModularServerFrame extends ContainerBase<TileModularServerFrame> {

    public ContainerModularServerFrame(final TileModularServerFrame owner, final EntityPlayer opening) {
        super(owner, opening);
    }

    @Override
    public boolean canInteractWith(final EntityPlayer player) {
        return true;
    }
}
