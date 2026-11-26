package github.kasuminova.novaeng.mixin.ic2;

import github.kasuminova.novaeng.common.util.UuGraphNodeView;
import ic2.core.uu.LeanItemStack;
import ic2.core.uu.UuGraph;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.Set;

@Mixin(value = UuGraph.Node.class, remap = false)
public class MixinUuGraph$Node implements UuGraphNodeView {

    @Unique
    private volatile Collection<UuGraph.Node> nova$singleNodeView;

    @Override
    public Collection<UuGraph.Node> nova$singleNodeView() {
        Collection<UuGraph.Node> view = nova$singleNodeView;
        if (view == null) {
            synchronized (this) {
                view = nova$singleNodeView;
                if (view == null) {
                    view = ObjectLists.singleton((UuGraph.Node) (Object) this);
                    nova$singleNodeView = view;
                }
            }
        }
        return view;
    }

    @Shadow
    public Set<UuGraph.NodeTransform> provides;

    @Inject(method = "<init>", at = @At("TAIL"))
    public void onInit(LeanItemStack stack, CallbackInfo ci) {
        provides = new ObjectOpenHashSet<>();
    }
}
