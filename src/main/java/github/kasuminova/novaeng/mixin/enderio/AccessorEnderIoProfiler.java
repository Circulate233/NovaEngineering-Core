package github.kasuminova.novaeng.mixin.enderio;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

@Pseudo
@Mixin(targets = "crazypants.enderio.util.Profiler", remap = false)
public interface AccessorEnderIoProfiler {
    @Accessor("on")
    boolean nova$isEnabled();
}
