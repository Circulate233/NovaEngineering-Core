package github.kasuminova.novaeng.mixin.minecraft.forge;

import com.google.common.collect.ListMultimap;
import net.minecraftforge.fml.common.LoadController;
import net.minecraftforge.fml.common.ModContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Collections;
import java.util.List;

@Mixin(value = LoadController.class, remap = false)
public abstract class MixinLoadControllerOwnerLookup {
    @Redirect(method = "lambda$findActiveContainerFromStack$2", at = @At(value = "INVOKE",
        target = "Lcom/google/common/collect/ListMultimap;get(Ljava/lang/Object;)Ljava/util/List;"), require = 1)
    private List<ModContainer> nova$skipAbsentOwnerList(final ListMultimap<String, ModContainer> owners,
                                                        final Object packageName) {
        // ArrayListMultimap.get allocates an empty backing list and a live wrapper on misses.
        // This lambda's consumer only checks emptiness, then reads index zero on a hit.
        // Do not cache results: buildModObjectList can still change owners, and list order matters.
        return owners.containsKey(packageName) ? owners.get((String) packageName) : Collections.emptyList();
    }
}
