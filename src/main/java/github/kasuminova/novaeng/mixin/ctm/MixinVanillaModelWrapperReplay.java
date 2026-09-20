package github.kasuminova.novaeng.mixin.ctm;

import github.kasuminova.novaeng.client.ctm.CtmReplayModel;
import net.minecraftforge.client.model.IModel;
import org.spongepowered.asm.mixin.Mixin;

import java.util.Collection;
import java.util.List;

/** Marks Forge's exact vanilla model wrapper as a replay-safe leaf. */
@Mixin(targets = "net.minecraftforge.client.model.ModelLoader$VanillaModelWrapper", remap = false)
public abstract class MixinVanillaModelWrapperReplay implements CtmReplayModel {

    /** Returns no children because the vanilla wrapper is a replay-safe leaf. */
    @Override
    public Collection<IModel> nova$getCtmReplayChildren() {
        return List.of();
    }
}
