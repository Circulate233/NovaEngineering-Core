package github.kasuminova.novaeng.client.gui;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import neofontrender.addons.api.ui.navigation.UiNode;
import neofontrender.addons.api.ui.navigation.UiNodeId;
import neofontrender.addons.api.ui.navigation.UiTreeSnapshot;

import java.util.Set;

/**
 * Reentrant scopes keep validation sets separate while releasing node references at every return.
 */
public final class UiValidationScratch {
    private static final ThreadLocal<State> LOCAL = ThreadLocal.withInitial(State::new);

    private UiValidationScratch() {
    }

    public static void begin() {
        final State state = LOCAL.get();
        if (++state.depth == state.frames.size()) state.frames.add(new Frame());
        state.frames.get(state.depth).first = true;
    }

    public static Set<UiNodeId> nextSet() {
        final State state = LOCAL.get();
        final Frame frame = state.frames.get(state.depth);
        if (frame.first) {
            frame.first = false;
            return frame.seen;
        }
        frame.largeChildren |= frame.children.size() > 1024;
        frame.children.clear();
        return frame.children;
    }

    public static void end() {
        final State state = LOCAL.get();
        final Frame frame = state.frames.get(state.depth--);
        // Ordinary menus keep their tiny arrays; a one-off very large tree cannot set a permanent high-water mark.
        if (frame.seen.size() > 1024) frame.seen = new ObjectOpenHashSet<>();
        else frame.seen.clear();
        if (frame.largeChildren || frame.children.size() > 1024) frame.children = new ObjectOpenHashSet<>();
        else frame.children.clear();
        frame.largeChildren = false;
    }

    /** The dependency's validation order and failures, using reusable flat sets instead of HashMap nodes. */
    public static void validate(final UiTreeSnapshot tree) {
        begin();
        try {
            final Set<UiNodeId> seen = nextSet();
            for (final UiNode node : tree.nodes()) {
                if (!seen.add(node.id())) throw new IllegalArgumentException("duplicate node id " + node.id());
            }
            for (final UiNodeId root : tree.roots()) {
                final UiNode node = require(tree, root, "missing root ");
                if (node.parentId() != null) throw new IllegalArgumentException("root has parent " + root);
            }
            for (final UiNode node : tree.nodes()) {
                if (node.parentId() != null) {
                    final UiNode parent = require(tree, node.parentId(), "missing parent ");
                    if (!parent.children().contains(node.id())) throw new IllegalArgumentException("parent does not reference child " + node.id());
                } else if (!tree.roots().contains(node.id())) {
                    throw new IllegalArgumentException("detached root " + node.id());
                }
                final Set<UiNodeId> children = nextSet();
                for (final UiNodeId childId : node.children()) {
                    if (!children.add(childId)) throw new IllegalArgumentException("duplicate child " + childId + " in " + node.id());
                    final UiNode child = require(tree, childId, "missing child ");
                    if (!node.id().equals(child.parentId())) throw new IllegalArgumentException("child parent mismatch " + childId);
                }
                for (final UiNodeId neighbor : node.navigation().explicitNeighbors().values()) require(tree, neighbor, "missing explicit neighbor ");
            }
            if (tree.activeScopeId() != null) require(tree, tree.activeScopeId(), "missing active scope ");
            if (tree.focusedNodeId() != null) {
                final UiNode focus = require(tree, tree.focusedNodeId(), "missing focused node ");
                if (!focus.focusable()) throw new IllegalArgumentException("focused node is not focusable " + focus.id());
            }
        } finally {
            end();
        }
    }

    private static UiNode require(final UiTreeSnapshot tree, final UiNodeId id, final String message) {
        final UiNode node = tree.node(id);
        if (node == null) throw new IllegalArgumentException(message + id);
        return node;
    }

    private static final class State {
        private final ObjectArrayList<Frame> frames = new ObjectArrayList<>(0);
        private int depth = -1;
    }

    private static final class Frame {
        private ObjectOpenHashSet<UiNodeId> seen = new ObjectOpenHashSet<>();
        private ObjectOpenHashSet<UiNodeId> children = new ObjectOpenHashSet<>();
        private boolean first;
        private boolean largeChildren;
    }
}
