package github.kasuminova.novaeng.mixin.jetif;

import lykrast.jetif.JETIFCompat;
import lykrast.jetif.JETIFWrapper;
import lykrast.jetif.compat.CompatFluxNetworks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.List;

@Mixin(value = CompatFluxNetworks.class, remap = false)
public class MixinCompatFluxNetworks extends JETIFCompat {
    public MixinCompatFluxNetworks() {
        super("fluxnetworks");
    }

    /**
     * @author Circulation_
     * @reason 直接注释掉通量网络的错误配方
     */
    @Overwrite
    public void addRecipes(List<JETIFWrapper> list) {
    }
}
