package github.kasuminova.novaeng.mixin.minecraft;

import net.minecraft.util.ClassInheritanceMultiMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(ClassInheritanceMultiMap.class)
public interface AccessorClassInheritanceMultiMap<T> {

    @Accessor("values")
    List<T> nova$getValues();
}
