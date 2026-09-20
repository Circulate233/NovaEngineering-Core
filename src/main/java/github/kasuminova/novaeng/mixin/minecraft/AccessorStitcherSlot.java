package github.kasuminova.novaeng.mixin.minecraft;

import net.minecraft.client.renderer.texture.Stitcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Stitcher.Slot.class)
public interface AccessorStitcherSlot {

    @Accessor("width")
    int nova$getWidth();

    @Accessor("height")
    int nova$getHeight();
}
