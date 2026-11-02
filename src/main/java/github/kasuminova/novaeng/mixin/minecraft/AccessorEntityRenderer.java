package github.kasuminova.novaeng.mixin.minecraft;

import net.minecraft.client.renderer.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntityRenderer.class)
public interface AccessorEntityRenderer {

    @Accessor("lightmapUpdateNeeded")
    void nova$setLightmapUpdateNeeded(boolean needed);
}
