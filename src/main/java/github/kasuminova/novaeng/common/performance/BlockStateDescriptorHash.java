package github.kasuminova.novaeng.common.performance;

import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;

import java.util.List;

public final class BlockStateDescriptorHash {

    private BlockStateDescriptorHash() {
    }

    public static int hash(final List<IBlockState> states) {
        if (states.size() == 1) {
            final IBlockState state = states.getFirst();
            if (state != null && state.getClass() == BlockStateContainer.StateImplementation.class) {
                // StateImplementation.equals uses identity, but its hash only includes
                // properties. Unrelated blocks with the same properties otherwise all
                // collide in MMCE's descriptor pool. Apply this to every singleton list
                // representation so equal singleton descriptors retain equal hashes.
                return 31 + System.identityHashCode(state);
            }
        }
        return states.hashCode();
    }
}
