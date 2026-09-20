package github.kasuminova.novaeng.mixin.ctm;

import com.google.common.collect.ImmutableMap;
import github.kasuminova.novaeng.client.ctm.CtmReplayModel;
import net.minecraftforge.client.model.IModel;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Collection;

/** Exposes Forge MultipartModel's direct part models for recursive replay eligibility checks. */
@Mixin(targets = "net.minecraftforge.client.model.ModelLoader$MultipartModel", remap = false)
public abstract class MixinMultipartModelReplay implements CtmReplayModel {

    @Shadow
    @Final
    private ImmutableMap<?, IModel> partModels;

    /** Returns the exact model values baked by the multipart implementation. */
    @Override
    public Collection<IModel> nova$getCtmReplayChildren() {
        return this.partModels.values();
    }
}
