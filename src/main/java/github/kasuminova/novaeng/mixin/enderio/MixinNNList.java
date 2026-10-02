package github.kasuminova.novaeng.mixin.enderio;

import com.enderio.core.common.util.NNList;
import github.kasuminova.novaeng.common.util.NovaMirrorHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.Set;

@Mixin(value = NNList.class, remap = false)
public abstract class MixinNNList<E> implements NovaMirrorHolder {

    @Unique
    private Set<Object> nova$mirror;

    @Override
    public Set<Object> nova$getMirror() {
        return this.nova$mirror;
    }

    @Override
    public void nova$setMirror(final Set<Object> mirror) {
        this.nova$mirror = mirror;
    }
}
