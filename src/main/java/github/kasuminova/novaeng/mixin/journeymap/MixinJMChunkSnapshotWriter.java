package github.kasuminova.novaeng.mixin.journeymap;

import github.kasuminova.novaeng.common.performance.JourneyMapSectionWriter;
import journeymap.common.chunk.JMChunkSnapshot;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.io.DataOutput;
import java.io.IOException;

@Mixin(value = JMChunkSnapshot.class, remap = false)
public abstract class MixinJMChunkSnapshotWriter {
    /**
     * @author circulation
     * @reason Reuse section palette work and encode immutable state metadata once, preserving the binary format.
     */
    @Overwrite(remap = false)
    private static void writeSectionBlockStates(final DataOutput out, final ExtendedBlockStorage section) throws IOException {
        JourneyMapSectionWriter.write(out, section);
    }
}
