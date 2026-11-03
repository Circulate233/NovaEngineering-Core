package github.kasuminova.novaeng.mixin.neofontrender;

import neofontrender.client.render.sign.SignOcclusionCuller;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = SignOcclusionCuller.class, remap = false)
public interface AccessorSignOcclusionCuller {
    @Accessor("currentWorld")
    static World nova$currentWorld() {
        throw new AssertionError();
    }
}
