package github.kasuminova.novaeng.client.gui;

import neofontrender.addons.api.ui.navigation.UiAction;
import neofontrender.addons.api.ui.navigation.UiInteractionMode;
import neofontrender.addons.api.ui.navigation.UiNavigationHints;
import neofontrender.addons.api.ui.navigation.UiNode;
import neofontrender.addons.api.ui.navigation.UiNodeId;
import neofontrender.addons.api.ui.navigation.UiRect;
import neofontrender.addons.api.ui.navigation.UiRole;
import neofontrender.addons.api.ui.navigation.UiTreeSnapshot;
import neofontrender.addons.navigation.ImmutableUiNode;
import neofontrender.addons.navigation.ImmutableUiTreeSnapshot;
import neofontrender.addons.navigation.UiTreeValidator;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UiTreeRefreshCacheTest {
    private static final UiNodeId ROOT = new UiNodeId(new ResourceLocation("novaeng", "test"), "root");

    private static ImmutableUiTreeSnapshot tree(final UiNode node) {
        return new ImmutableUiTreeSnapshot(1, null, UiInteractionMode.HYBRID, List.of(ROOT), List.of(node), null, null);
    }

    @Test
    void validatedImmutableTreeAndItsFocusCopyAreReusedButDifferentSnapshotsAreNot() {
        final ImmutableUiTreeSnapshot raw = tree(ImmutableUiNode.builder(ROOT, UiRole.BUTTON).focusable(true).build());
        final UiTreeRefreshCache cache = new UiTreeRefreshCache();
        assertFalse(cache.alreadyValidated(raw));
        UiTreeValidator.validate(raw);
        cache.rememberValidated(raw);
        assertTrue(cache.alreadyValidated(raw));
        assertSame(raw, cache.withFocus(null, null));
        final ImmutableUiTreeSnapshot focused = raw.withFocus(ROOT, null);
        cache.rememberFocused(focused);
        assertSame(focused, cache.withFocus(ROOT, null));
        assertTrue(cache.sameSource(raw.revision(), raw.screen(), raw.interactionMode(), raw.roots(), raw.nodes()));
        assertFalse(cache.sameSource(raw.revision() + 1, raw.screen(), raw.interactionMode(), raw.roots(), raw.nodes()));
        assertFalse(cache.alreadyValidated(tree(ImmutableUiNode.builder(ROOT, UiRole.BUTTON).focusable(true).build())));
        assertNull(cache.withFocus(ROOT, null));
    }

    @Test
    void closingOrFailedRefreshReleasesBothCachedSnapshots() {
        final ImmutableUiTreeSnapshot raw = tree(ImmutableUiNode.builder(ROOT, UiRole.BUTTON).focusable(true).build());
        final UiTreeRefreshCache cache = new UiTreeRefreshCache();
        cache.rememberValidated(raw);
        cache.rememberFocused(raw.withFocus(ROOT, null));
        cache.clear();
        assertNull(cache.withFocus(ROOT, null));
        assertFalse(cache.sameSource(raw.revision(), raw.screen(), raw.interactionMode(), raw.roots(), raw.nodes()));
        assertFalse(cache.alreadyValidated(raw));
    }

    @Test
    void customResourceLocationCannotCertifyAnOtherwiseImmutableTree() {
        final UiNodeId customId = new UiNodeId(new ResourceLocation("novaeng", "custom") {
        }, "root");
        final ImmutableUiTreeSnapshot raw = new ImmutableUiTreeSnapshot(1, null, UiInteractionMode.HYBRID,
            List.of(customId), List.of(ImmutableUiNode.builder(customId, UiRole.BUTTON).build()), null, null);
        UiTreeValidator.validate(raw);
        final UiTreeRefreshCache cache = new UiTreeRefreshCache();
        cache.rememberValidated(raw);
        assertFalse(cache.alreadyValidated(raw));
    }

    @Test
    void immutableContainerWithMutableCustomNodesMustAlwaysBeValidatedAgain() {
        final MutableNode node = new MutableNode();
        final ImmutableUiTreeSnapshot raw = tree(node);
        final UiTreeRefreshCache cache = new UiTreeRefreshCache();
        UiTreeValidator.validate(raw);
        cache.rememberValidated(raw);
        node.parent = ROOT;
        assertFalse(cache.alreadyValidated(raw));
        assertThrows(IllegalArgumentException.class, () -> UiTreeValidator.validate(raw));
    }

    @Test
    void customSnapshotCannotUseIdentityOrRevisionToBypassValidation() {
        final MutableNode node = new MutableNode();
        final UiTreeSnapshot custom = new UiTreeSnapshot() {
            public long revision() {
                return 1;
            }

            public GuiScreen screen() {
                return null;
            }

            public UiInteractionMode interactionMode() {
                return UiInteractionMode.HYBRID;
            }

            public List<UiNodeId> roots() {
                return List.of(ROOT);
            }

            public UiNode node(UiNodeId id) {
                return ROOT.equals(id) ? node : null;
            }

            public Collection<UiNode> nodes() {
                return List.of(node);
            }

            public UiNodeId focusedNodeId() {
                return null;
            }

            public UiNodeId activeScopeId() {
                return null;
            }
        };
        final UiTreeRefreshCache cache = new UiTreeRefreshCache();
        UiTreeValidator.validate(custom);
        cache.rememberValidated(custom);
        assertFalse(cache.alreadyValidated(custom));
        node.parent = ROOT;
        assertThrows(IllegalArgumentException.class, () -> UiTreeValidator.validate(custom));
    }

    @Test
    void validationScratchPreservesOuterSetsAcrossNestedCallsAndClearsReferences() {
        UiValidationScratch.begin();
        final Set<UiNodeId> seen = UiValidationScratch.nextSet();
        final Set<UiNodeId> children = UiValidationScratch.nextSet();
        seen.add(ROOT);
        children.add(ROOT);
        UiValidationScratch.begin();
        try {
            assertNotSame(seen, UiValidationScratch.nextSet());
            assertNotSame(children, UiValidationScratch.nextSet());
            assertTrue(seen.contains(ROOT));
            assertTrue(children.contains(ROOT));
        } finally {
            UiValidationScratch.end();
        }
        assertSame(children, UiValidationScratch.nextSet());
        assertTrue(children.isEmpty());
        assertTrue(seen.contains(ROOT));
        UiValidationScratch.end();
        assertTrue(seen.isEmpty());
        assertTrue(children.isEmpty());
    }

    @Test
    void fastutilValidationMatchesDependencyFailuresAndTheirOrder() {
        final UiNodeId childId = new UiNodeId(ROOT.namespace(), "child");
        final UiNodeId missing = new UiNodeId(ROOT.namespace(), "missing");
        for (int mask = 0; mask < 1024; mask++) {
            final MutableNode root = new MutableNode();
            final MutableNode child = new MutableNode();
            child.identity = childId;
            child.parent = (mask & 16) != 0 ? missing : ROOT;
            root.childIds = (mask & 512) != 0 ? List.of(missing) : (mask & 8) != 0 ? List.of(childId, childId) : List.of(childId);
            if ((mask & 4) != 0) root.parent = childId;
            root.focusable = (mask & 64) == 0;
            final List<UiNode> nodes = new ObjectArrayList<>(List.of(root, child));
            if ((mask & 1) != 0) nodes.add(root);
            final List<UiNodeId> roots = (mask & 32) != 0 ? List.of() : (mask & 2) != 0 ? List.of(missing) : List.of(ROOT);
            final UiNodeId focus = (mask & 256) != 0 ? missing : (mask & 64) != 0 ? ROOT : null;
            final UiNodeId scope = (mask & 128) != 0 ? missing : null;
            final UiTreeSnapshot snapshot = new UiTreeSnapshot() {
                public long revision() { return 0; }
                public GuiScreen screen() { return null; }
                public UiInteractionMode interactionMode() { return UiInteractionMode.HYBRID; }
                public List<UiNodeId> roots() { return roots; }
                public Collection<UiNode> nodes() { return nodes; }
                public UiNode node(final UiNodeId id) {
                    for (int index = 0; index < nodes.size(); index++) {
                        final UiNode node = nodes.get(index);
                        if (Objects.equals(id, node.id())) return node;
                    }
                    return null;
                }
                public UiNodeId focusedNodeId() { return focus; }
                public UiNodeId activeScopeId() { return scope; }
            };
            assertEquals(validationFailure(() -> UiTreeValidator.validate(snapshot)),
                validationFailure(() -> UiValidationScratch.validate(snapshot)), "mask=" + mask);
        }
    }

    private static String validationFailure(final Runnable action) {
        try { action.run(); return "valid"; }
        catch (final RuntimeException failure) { return failure.getClass().getName() + ":" + failure.getMessage(); }
    }

    private static final class MutableNode implements UiNode {
        private UiNodeId identity = ROOT;
        private UiNodeId parent;
        private List<UiNodeId> childIds = List.of();
        private boolean focusable = true;

        public UiNodeId id() {
            return identity;
        }

        public UiNodeId parentId() {
            return parent;
        }

        public UiRole role() {
            return UiRole.BUTTON;
        }

        public String label() {
            return "test";
        }

        public UiRect bounds() {
            return UiRect.EMPTY;
        }

        public UiRect visibleBounds() {
            return UiRect.EMPTY;
        }

        public List<UiNodeId> children() {
            return childIds;
        }

        public Set<UiAction> actions() {
            return Set.of();
        }

        public UiNavigationHints navigation() {
            return UiNavigationHints.DEFAULT;
        }

        public boolean enabled() {
            return true;
        }

        public boolean visible() {
            return true;
        }

        public boolean focusable() {
            return focusable;
        }
    }
}
