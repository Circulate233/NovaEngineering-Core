package github.kasuminova.novaeng.mixin.neofontrender;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import github.kasuminova.novaeng.client.gui.UiTreeRefreshCache;
import neofontrender.addons.api.ui.navigation.UiInteractionMode;
import neofontrender.addons.api.ui.navigation.UiNode;
import neofontrender.addons.api.ui.navigation.UiNodeId;
import neofontrender.addons.api.ui.navigation.UiTreeSnapshot;
import neofontrender.addons.navigation.ImmutableUiTreeSnapshot;
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Pseudo
@Mixin(targets = "neofontrender.addons.navigation.UiNavigationRuntime", remap = false)
public abstract class MixinUiNavigationRuntime {
    @Unique
    private final UiTreeRefreshCache nova$treeRefreshCache = new UiTreeRefreshCache();

    @WrapOperation(method = "refresh", at = @At(value = "INVOKE",
        target = "Lneofontrender/addons/navigation/UiTreeValidator;validate(Lneofontrender/addons/api/ui/navigation/UiTreeSnapshot;)V"), require = 1)
    private void nova$validateImmutableTreeOnce(final UiTreeSnapshot raw, final Operation<Void> original) {
        if (nova$treeRefreshCache.alreadyValidated(raw)) return;
        original.call(raw);
        nova$treeRefreshCache.rememberValidated(raw);
    }

    @WrapOperation(method = "refresh", at = @At(value = "NEW",
        target = "(JLnet/minecraft/client/gui/GuiScreen;Lneofontrender/addons/api/ui/navigation/UiInteractionMode;Ljava/util/List;Ljava/util/Collection;Lneofontrender/addons/api/ui/navigation/UiNodeId;Lneofontrender/addons/api/ui/navigation/UiNodeId;)Lneofontrender/addons/navigation/ImmutableUiTreeSnapshot;"), require = 1)
    private ImmutableUiTreeSnapshot nova$reuseFocusedSnapshot(final long revision, final GuiScreen screen, final UiInteractionMode mode,
                                                              final List<UiNodeId> roots, final Collection<? extends UiNode> nodes,
                                                              final UiNodeId focus, final UiNodeId scope,
                                                              final Operation<ImmutableUiTreeSnapshot> original) {
        final boolean sameSource = nova$treeRefreshCache.sameSource(revision, screen, mode, roots, nodes);
        if (sameSource) {
            final ImmutableUiTreeSnapshot cached = nova$treeRefreshCache.withFocus(focus, scope);
            if (cached != null) return cached;
        }
        final ImmutableUiTreeSnapshot result = original.call(revision, screen, mode, roots, nodes, focus, scope);
        if (sameSource) nova$treeRefreshCache.rememberFocused(result);
        return result;
    }

    @WrapOperation(method = "attachFocus", at = @At(value = "INVOKE",
        target = "Lneofontrender/addons/navigation/ImmutableUiTreeSnapshot;withFocus(Lneofontrender/addons/api/ui/navigation/UiNodeId;Lneofontrender/addons/api/ui/navigation/UiNodeId;)Lneofontrender/addons/navigation/ImmutableUiTreeSnapshot;"), require = 1)
    private ImmutableUiTreeSnapshot nova$keepUnchangedFocus(final ImmutableUiTreeSnapshot tree, final UiNodeId focus,
                                                            final UiNodeId scope, final Operation<ImmutableUiTreeSnapshot> original) {
        return Objects.equals(focus, tree.focusedNodeId()) && Objects.equals(scope, tree.activeScopeId())
            ? tree : original.call(tree, focus, scope);
    }

    @Inject(method = "closeSession", at = @At("HEAD"), require = 1)
    private void nova$releasePreviousTree(final CallbackInfo ci) {
        nova$treeRefreshCache.clear();
    }

    @Inject(method = "refresh", at = @At(value = "INVOKE",
        target = "Lneofontrender/addons/navigation/UiNavigationDiagnostics;failure(Ljava/lang/String;Lnet/minecraft/client/gui/GuiScreen;Ljava/lang/Throwable;)V"), require = 1)
    private void nova$releaseFailedRefreshTree(final CallbackInfo ci) {
        nova$treeRefreshCache.clear();
    }
}
