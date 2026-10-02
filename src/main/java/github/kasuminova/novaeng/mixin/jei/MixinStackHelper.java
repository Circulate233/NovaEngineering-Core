package github.kasuminova.novaeng.mixin.jei;

import com.llamalad7.mixinextras.sugar.Local;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import mezz.jei.startup.StackHelper;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Map;
import java.util.Objects;

@Mixin(value = StackHelper.class, remap = false)
public abstract class MixinStackHelper {

    @Shadow
    @Final
    private Map<StackHelper.UidMode, Map<ItemStack, String>> uidCache;
    @Unique
    private static final Hash.Strategy<ItemStack> nova$uidHash = new Hash.Strategy<>() {
        @Override
        public int hashCode(ItemStack o) {
            if (o == null || o.isEmpty()) {
                return 0;
            }
            return Objects.hash(o.getItem(), o.getMetadata(), o.getTagCompound());
        }

        @Override
        public boolean equals(ItemStack a, ItemStack b) {
            if (a == b) {
                return true;
            }
            if (a == null || b == null) {
                return false;
            }
            return a.isItemEqual(b) && Objects.equals(a.getTagCompound(), b.getTagCompound());
        }
    };

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
    public Object setCustomMap(Map<?, ?> instance, Object k, Object v, @Local(name = "mode") StackHelper.UidMode mode) {
        return this.uidCache.put(mode, new Object2ObjectOpenCustomHashMap<>(nova$uidHash));
    }

    @Redirect(method = "disableUidCache", at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
    public Object setCustomMap1(Map<?, ?> instance, Object k, Object v, @Local(name = "mode") StackHelper.UidMode mode) {
        return this.uidCache.put(mode, new Object2ObjectOpenCustomHashMap<>(nova$uidHash));
    }

}
