package github.kasuminova.novaeng.mixin.ar;

import net.minecraft.client.renderer.BufferBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import zmaster587.advancedRocketry.backwardCompat.Face;
import zmaster587.advancedRocketry.backwardCompat.TextureCoordinate;
import zmaster587.advancedRocketry.backwardCompat.Vertex;

@Mixin(value = Face.class, remap = false)
public abstract class MixinFaceTextureAverage {

    @Shadow
    public Vertex[] vertices;

    @Shadow
    public Vertex faceNormal;

    @Shadow
    public TextureCoordinate[] textureCoordinates;

    @Shadow
    public abstract Vertex calculateFaceNormal();

    @Unique
    private TextureCoordinate[] nova$averagedCoordinates;

    @Unique
    private float nova$averageU;

    @Unique
    private float nova$averageV;

    /**
     * @author circulation
     * @reason Cache immutable OBJ face UV averages instead of summing them on every render.
     */
    @Overwrite
    public void addFaceForRender(final BufferBuilder tessellator, final float textureOffset) {
        if (this.faceNormal == null) {
            this.faceNormal = this.calculateFaceNormal();
        }

        final TextureCoordinate[] coordinates = this.textureCoordinates;
        float averageU = 0.0F;
        float averageV = 0.0F;
        if (coordinates != null && coordinates.length > 0) {
            if (this.nova$averagedCoordinates != coordinates) {
                for (final TextureCoordinate coordinate : coordinates) {
                    averageU += coordinate.u;
                    averageV += coordinate.v;
                }
                this.nova$averageU = averageU / (float) coordinates.length;
                this.nova$averageV = averageV / (float) coordinates.length;
                this.nova$averagedCoordinates = coordinates;
            }
            averageU = this.nova$averageU;
            averageV = this.nova$averageV;
        }

        final Vertex[] faceVertices = this.vertices;
        for (int i = 0; i < faceVertices.length; ++i) {
            final Vertex vertex = faceVertices[i];
            if (coordinates != null && coordinates.length > 0) {
                final TextureCoordinate coordinate = coordinates[i];
                float offsetU = textureOffset;
                float offsetV = textureOffset;
                if (coordinate.u > averageU) {
                    offsetU = -textureOffset;
                }
                if (coordinate.v > averageV) {
                    offsetV = -textureOffset;
                }
                tessellator.pos(vertex.x, vertex.y, vertex.z)
                        .tex(coordinate.u + offsetU, coordinate.v + offsetV)
                        .normal(this.faceNormal.x, this.faceNormal.y, this.faceNormal.z)
                        .endVertex();
            } else {
                tessellator.pos(vertex.x, vertex.y, vertex.z)
                        .normal(this.faceNormal.x, this.faceNormal.y, this.faceNormal.z)
                        .endVertex();
            }
        }
    }
}
