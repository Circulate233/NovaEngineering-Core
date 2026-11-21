package github.kasuminova.novaeng.client.gui;

import neofontrender.addons.api.ui.navigation.UiInteractionMode;
import neofontrender.addons.api.ui.navigation.UiNode;
import neofontrender.addons.api.ui.navigation.UiNodeId;
import neofontrender.addons.api.ui.navigation.UiTreeSnapshot;
import neofontrender.addons.navigation.ImmutableUiNode;
import neofontrender.addons.navigation.ImmutableUiTreeSnapshot;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ResourceLocation;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * A one-entry cache restricted to the renderer's defensively copied, final node types.
 */
public final class UiTreeRefreshCache {
    private ImmutableUiTreeSnapshot validated;
    private ImmutableUiTreeSnapshot focused;

    private static boolean stable(final UiNodeId id) {
        // A custom mutable ResourceLocation subclass must not turn an immutable node into a cache bypass.
        return id == null || id.namespace().getClass() == ResourceLocation.class;
    }

    public boolean alreadyValidated(final UiTreeSnapshot tree) {
        if (tree == validated) return true;
        clear();
        return false;
    }

    public void rememberValidated(final UiTreeSnapshot tree) {
        if (tree.getClass() != ImmutableUiTreeSnapshot.class) return;
        for (final UiNode node : tree.nodes()) {
            if (node.getClass() != ImmutableUiNode.class || !stable(node.id()) || !stable(node.parentId())) return;
            for (final UiNodeId child : node.children()) if (!stable(child)) return;
            for (final UiNodeId neighbor : node.navigation().explicitNeighbors().values())
                if (!stable(neighbor)) return;
        }
        for (final UiNodeId root : tree.roots()) if (!stable(root)) return;
        if (!stable(tree.focusedNodeId()) || !stable(tree.activeScopeId())) return;
        validated = (ImmutableUiTreeSnapshot) tree;
    }

    public ImmutableUiTreeSnapshot withFocus(final UiNodeId focus, final UiNodeId scope) {
        if (validated == null) return null;
        if (Objects.equals(focus, validated.focusedNodeId()) && Objects.equals(scope, validated.activeScopeId()))
            return validated;
        return focused != null && Objects.equals(focus, focused.focusedNodeId()) && Objects.equals(scope, focused.activeScopeId()) ? focused : null;
    }

    public boolean sameSource(final long revision, final GuiScreen screen, final UiInteractionMode mode,
                              final List<UiNodeId> roots, final Collection<? extends UiNode> nodes) {
        return validated != null && revision == validated.revision() && screen == validated.screen()
            && mode == validated.interactionMode() && roots == validated.roots() && nodes == validated.nodes();
    }

    public void rememberFocused(final ImmutableUiTreeSnapshot tree) {
        if (validated != null) focused = tree;
    }

    public void clear() {
        validated = null;
        focused = null;
    }
}
