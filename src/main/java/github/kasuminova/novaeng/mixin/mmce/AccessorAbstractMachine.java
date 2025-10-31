package github.kasuminova.novaeng.mixin.mmce;

import hellfirepvp.modularmachinery.common.machine.AbstractMachine;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = AbstractMachine.class, remap = false)
public interface AccessorAbstractMachine {

    @Accessor("registryName")
    @Mutable
    void setRL(ResourceLocation newRl);
}