package github.kasuminova.novaeng.mixin.minecraft;

import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ExtendedBlockStorage.class)
public interface AccessorSectionCounts {
    @Accessor("blockRefCount")
    void nova$setBlockRefCount(int count);

    @Accessor("tickRefCount")
    void nova$setTickRefCount(int count);
}
