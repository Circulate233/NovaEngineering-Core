package github.kasuminova.novaeng.mixin.ae2;

import appeng.api.storage.data.IAEItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Map;

// ItemVariantList is package-private; its concrete subclasses retain their existing maps.
@Mixin(targets = "appeng.util.item.ItemVariantList", remap = false)
public interface AccessorItemVariantRecords {
    @Invoker("getRecords")
    Map<?, IAEItemStack> nova$records();
}
