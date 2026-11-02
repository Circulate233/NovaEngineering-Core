package github.kasuminova.novaeng.mixin.ingameinfoxml;

import com.github.lunatrius.ingameinfo.tag.Tag;
import com.github.lunatrius.ingameinfo.tag.registry.TagRegistry;
import github.kasuminova.novaeng.common.performance.DeferredTag;
import github.kasuminova.novaeng.common.performance.LazyBuiltinTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Map;

@Mixin(value = TagRegistry.class, remap = false)
public abstract class MixinLazyTagFactories {
    @Shadow
    private Map<String, Tag> stringTagMap;

    @Inject(method = "getRegisteredTags", at = @At("HEAD"), require = 1)
    private void nova$exposeConcreteTags(final CallbackInfoReturnable<List<Tag>> cir) {
        stringTagMap.replaceAll((name, tag) -> tag instanceof DeferredTag deferred ? deferred.materialize() : tag);
    }

    @Redirect(method = "init", at = @At(value = "INVOKE",
        target = "Lcom/github/lunatrius/ingameinfo/tag/TagFormatting;register()V"), require = 1)
    private void nova$registerFormatting() {
        LazyBuiltinTags.registerFormatting();
    }

    @Redirect(method = "init", at = @At(value = "INVOKE",
        target = "Lcom/github/lunatrius/ingameinfo/tag/TagMisc;register()V"), require = 1)
    private void nova$registerMisc() {
        LazyBuiltinTags.registerMisc();
    }

    @Redirect(method = "init", at = @At(value = "INVOKE",
        target = "Lcom/github/lunatrius/ingameinfo/tag/TagMouseOver;register()V"), require = 1)
    private void nova$registerMouseOver() {
        LazyBuiltinTags.registerMouseOver();
    }

    @Redirect(method = "init", at = @At(value = "INVOKE",
        target = "Lcom/github/lunatrius/ingameinfo/tag/TagNearbyPlayer;register()V"), require = 1)
    private void nova$registerNearbyPlayer() {
        LazyBuiltinTags.registerNearbyPlayer();
    }

    @Redirect(method = "init", at = @At(value = "INVOKE",
        target = "Lcom/github/lunatrius/ingameinfo/tag/TagPlayerEquipment;register()V"), require = 1)
    private void nova$registerPlayerEquipment() {
        LazyBuiltinTags.registerPlayerEquipment();
    }

    @Redirect(method = "init", at = @At(value = "INVOKE",
        target = "Lcom/github/lunatrius/ingameinfo/tag/TagPlayerGeneral;register()V"), require = 1)
    private void nova$registerPlayerGeneral() {
        LazyBuiltinTags.registerPlayerGeneral();
    }

    @Redirect(method = "init", at = @At(value = "INVOKE",
        target = "Lcom/github/lunatrius/ingameinfo/tag/TagPlayerPosition;register()V"), require = 1)
    private void nova$registerPlayerPosition() {
        LazyBuiltinTags.registerPlayerPosition();
    }

    @Redirect(method = "init", at = @At(value = "INVOKE",
        target = "Lcom/github/lunatrius/ingameinfo/tag/TagPlayerPotion;register()V"), require = 1)
    private void nova$registerPlayerPotion() {
        LazyBuiltinTags.registerPlayerPotion();
    }

    @Redirect(method = "init", at = @At(value = "INVOKE",
        target = "Lcom/github/lunatrius/ingameinfo/tag/TagRiding;register()V"), require = 1)
    private void nova$registerRiding() {
        LazyBuiltinTags.registerRiding();
    }

    @Redirect(method = "init", at = @At(value = "INVOKE",
        target = "Lcom/github/lunatrius/ingameinfo/tag/TagTime;register()V"), require = 1)
    private void nova$registerTime() {
        LazyBuiltinTags.registerTime();
    }

    @Redirect(method = "init", at = @At(value = "INVOKE",
        target = "Lcom/github/lunatrius/ingameinfo/tag/TagWorld;register()V"), require = 1)
    private void nova$registerWorld() {
        LazyBuiltinTags.registerWorld();
    }

}
