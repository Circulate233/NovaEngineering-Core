package github.kasuminova.novaeng.mixin.ctm;

import github.kasuminova.novaeng.client.ctm.CtmReplayModel;
import net.minecraftforge.client.model.IModel;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Collection;
import java.util.List;

/** Exposes Forge WeightedRandomModel's direct variants for recursive eligibility checks. */
@Mixin(targets = "net.minecraftforge.client.model.ModelLoader$WeightedRandomModel", remap = false)
public abstract class MixinWeightedRandomModelReplay implements CtmReplayModel {

    @Shadow
    @Final
    private List<IModel> models;

    /** Returns the exact variant models baked by the weighted implementation. */
    @Override
    public Collection<IModel> nova$getCtmReplayChildren() {
        return this.models;
    }
}
