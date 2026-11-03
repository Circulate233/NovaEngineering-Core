package github.kasuminova.novaeng.mixin.minecraft.forge;

import com.llamalad7.mixinextras.sugar.Local;
import github.kasuminova.novaeng.common.util.NovaBiomeIdCache;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;
import net.minecraftforge.registries.RegistryManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.Set;

/**
 * Drops {@link NovaBiomeIdCache} whenever a registry rewrites the mapping from an id to an entry.
 *
 * <p>The table in that cache is a copy of {@code ForgeRegistry#ids}, so it is only as correct as the hooks below are
 * complete. Every write to {@code ids} in {@code ForgeRegistry} happens in one of these five methods: {@code add}
 * registers an entry and is also how {@code loadIds} and {@code sync} reach the map, {@code remove} and
 * {@code markDummy} drop one, {@code clear} drops all of them, and {@code loadIds} renumbers the entries of a
 * registry against the ids a saved world was written with. A hook that stopped matching would report a stale biome
 * for an id, which is why they are matched by full descriptor and left to fail loudly rather than be optional.</p>
 *
 * <p>{@code NovaBiomeIdCache#invalidate} returns immediately while the table has never been built, so the
 * registration storm at startup costs one volatile read per entry.</p>
 */
@Mixin(value = ForgeRegistry.class, remap = false)
public class MixinForgeRegistry<V extends IForgeRegistryEntry<V>> {

    @Mutable
    @Shadow
    @Final
    private Set<Integer> blocked;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(Class<V> superType, ResourceLocation defaultKey, int min, int max, @Nullable IForgeRegistry.CreateCallback<V> create, @Nullable IForgeRegistry.AddCallback<V> add, @Nullable IForgeRegistry.ClearCallback<V> clear, @Nullable IForgeRegistry.ValidateCallback<V> validate, RegistryManager stage, boolean allowOverrides, boolean isModifiable, @Nullable IForgeRegistry.DummyFactory<V> dummyFactory, @Nullable IForgeRegistry.MissingFactory<V> missing, CallbackInfo ci) {
        blocked = new IntOpenHashSet();
    }

    @Redirect(method = "block", at = @At(value = "INVOKE", target = "Ljava/lang/Integer;valueOf(I)Ljava/lang/Integer;"))
    private Integer add(int i) {
        return null;
    }

    @Redirect(method = "block", at = @At(value = "INVOKE", target = "Ljava/util/Set;add(Ljava/lang/Object;)Z"))
    private boolean add(Set<Integer> instance, Object e, @Local(argsOnly = true) int i) {
        return ((IntSet) blocked).add(i);
    }

    @Inject(method = "add(ILnet/minecraftforge/registries/IForgeRegistryEntry;Ljava/lang/String;)I",
        at = @At("HEAD"), remap = false)
    private void nova$invalidateOnAdd(final CallbackInfoReturnable<Integer> cir) {
        NovaBiomeIdCache.invalidate();
    }

    @Inject(method = "remove(Lnet/minecraft/util/ResourceLocation;)Lnet/minecraftforge/registries/IForgeRegistryEntry;",
        at = @At("HEAD"), remap = false)
    private void nova$invalidateOnRemove(final CallbackInfoReturnable<IForgeRegistryEntry<?>> cir) {
        NovaBiomeIdCache.invalidate();
    }

    @Inject(method = "clear()V", at = @At("HEAD"), remap = false)
    private void nova$invalidateOnClear(final CallbackInfo ci) {
        NovaBiomeIdCache.invalidate();
    }

    @Inject(method = "loadIds(Ljava/util/Map;Ljava/util/Map;Ljava/util/Map;Ljava/util/Map;Lnet/minecraftforge/registries/ForgeRegistry;Lnet/minecraft/util/ResourceLocation;)V",
        at = @At("HEAD"), remap = false)
    private void nova$invalidateOnLoadIds(final CallbackInfo ci) {
        NovaBiomeIdCache.invalidate();
    }

    @Inject(method = "markDummy(Lnet/minecraft/util/ResourceLocation;I)Z", at = @At("HEAD"), remap = false)
    private void nova$invalidateOnMarkDummy(final CallbackInfoReturnable<Boolean> cir) {
        NovaBiomeIdCache.invalidate();
    }

}
