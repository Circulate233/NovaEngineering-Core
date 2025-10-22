package github.kasuminova.novaeng.mixin.botania;

import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import vazkii.botania.common.block.tile.string.TileRedString;
import vazkii.botania.common.block.tile.string.TileRedStringContainer;

import javax.annotation.Nonnull;

@Mixin(value = TileRedStringContainer.class, remap = false)
public abstract class MixinTileRedStringContainer extends TileRedString {

    /**
     * @author circulaiton
     * @reason 禁用红线容器
     */
    @Overwrite
    public <T> T getCapability(@Nonnull Capability<T> cap, EnumFacing side) {
        return null;
    }
}
