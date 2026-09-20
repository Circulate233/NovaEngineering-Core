package github.kasuminova.novaeng.client.ctm;

import net.minecraftforge.client.model.IModel;

import java.util.Collection;

/**
 * Exposes the child model graph of the exact Forge model implementations approved for CTM replay.
 */
public interface CtmReplayModel {

    /**
     * Returns the direct child models which must also belong to the approved closed model set.
     *
     * @return direct children, or an empty collection for a vanilla leaf
     */
    Collection<IModel> nova$getCtmReplayChildren();
}
