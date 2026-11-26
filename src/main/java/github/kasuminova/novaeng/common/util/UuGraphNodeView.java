package github.kasuminova.novaeng.common.util;

import ic2.core.uu.UuGraph;

import java.util.Collection;

/**
 * An immutable exact-query view owned by its node, never by the mutable query stack.
 */
public interface UuGraphNodeView {
    Collection<UuGraph.Node> nova$singleNodeView();
}
