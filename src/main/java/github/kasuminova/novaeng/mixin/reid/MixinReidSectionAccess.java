package github.kasuminova.novaeng.mixin.reid;

import github.kasuminova.novaeng.common.performance.ReidSectionAccess;
import net.minecraft.world.chunk.BlockStateContainer;
import net.minecraft.world.chunk.NibbleArray;
import org.dimdev.jeid.mixin.core.world.MixinBlockStateContainer;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Applied after REID, whose private temporary fields are deliberately not MCP-remapped.
 */
@Mixin(value = BlockStateContainer.class, priority = 1001)
public abstract class MixinReidSectionAccess implements ReidSectionAccess {
    @Shadow(remap = false)
    @Dynamic(mixin = MixinBlockStateContainer.class)
    private int[] temporaryPalette;
    @Shadow(remap = false)
    @Dynamic(mixin = MixinBlockStateContainer.class)
    private NibbleArray add2;

    @Override
    public int[] nova$getTemporaryPalette() {
        return temporaryPalette;
    }

    @Override
    public void nova$setTemporaryPalette(final int[] palette) {
        temporaryPalette = palette;
    }

    @Override
    public NibbleArray nova$getLegacyAdd2() {
        return add2;
    }

    @Override
    public void nova$clearLegacyAdd2() {
        add2 = null;
    }
}
