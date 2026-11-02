package github.kasuminova.novaeng.mixin.tconstruct;

import github.kasuminova.novaeng.common.performance.RevisionedList;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import slimeknights.tconstruct.library.TinkerRegistry;
import slimeknights.tconstruct.library.smeltery.MeltingRecipe;

import java.util.List;

@Mixin(value = TinkerRegistry.class, remap = false)
public abstract class MixinTinkerMeltingRegistry {
    @Shadow
    private static List<MeltingRecipe> meltingRegistry;

    @Redirect(method = "<clinit>", at = @At(value = "FIELD",
        target = "Lslimeknights/tconstruct/library/TinkerRegistry;meltingRegistry:Ljava/util/List;",
        opcode = Opcodes.PUTSTATIC), require = 1)
    private static void nova$trackMeltingChanges(final List<MeltingRecipe> original) {
        meltingRegistry = new RevisionedList<>(original);
    }
}
