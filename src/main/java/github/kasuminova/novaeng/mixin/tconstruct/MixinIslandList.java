package github.kasuminova.novaeng.mixin.tconstruct;

import github.kasuminova.novaeng.common.performance.IslandBoxList;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import slimeknights.tconstruct.world.worldgen.SlimeIslandData;

import java.util.List;

@Mixin(value = SlimeIslandData.class, remap = false)
public abstract class MixinIslandList {
    @Shadow
    @Final
    @Mutable
    private List<StructureBoundingBox> islands;

    @Inject(method = "<init>", at = @At("RETURN"), require = 1)
    private void nova$trackListChanges(final String name, final CallbackInfo ci) {
        islands = new IslandBoxList(islands);
    }
}
