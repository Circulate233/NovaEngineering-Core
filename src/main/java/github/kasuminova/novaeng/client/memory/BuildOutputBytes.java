package github.kasuminova.novaeng.client.memory;

import dhj.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildOutput;
import dhj.embeddedt.embeddium.impl.render.chunk.compile.ChunkSortOutput;
import dhj.embeddedt.embeddium.impl.render.chunk.compile.ChunkTaskOutput;
import dhj.embeddedt.embeddium.impl.render.chunk.data.BuiltSectionMeshParts;

/** Exact native payload size; Java object overhead and worker scratch are reported separately. */
public final class BuildOutputBytes {
    private BuildOutputBytes() { }

    public static long count(final ChunkTaskOutput output) {
        long bytes = 0;
        if (output instanceof ChunkBuildOutput build) {
            for (final BuiltSectionMeshParts mesh : build.meshes.values()) {
                bytes += mesh.vertexBuffer().getLength();
                if (mesh.indexBuffer() != null) { bytes += mesh.indexBuffer().getLength(); }
            }
        } else if (output instanceof ChunkSortOutput sort) {
            for (final ChunkSortOutput.SortedMesh mesh : sort.meshes.values()) {
                bytes += mesh.indexData().getLength();
            }
        }
        return bytes;
    }
}
