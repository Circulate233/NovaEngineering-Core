package github.kasuminova.novaeng.common.performance;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.ResourceLocation;

import java.util.List;

public final class MeteorCompassMetadata {

    private MeteorCompassMetadata() {
    }

    public static boolean matches(final IBlockState state, final List<ResourceLocation> targets, final List<Integer> metadata) {
        final Block block = state.getBlock();
        final ResourceLocation name = block.getRegistryName();
        boolean decoded = false;
        int value = 0;
        for (int i = 0; i < targets.size(); i++) {
            if (name == targets.get(i)) {
                if (!decoded) {
                    value = block.getMetaFromState(state);
                    decoded = true;
                }
                if (value == metadata.get(i)) {
                    return true;
                }
            }
        }
        return false;
    }
}
