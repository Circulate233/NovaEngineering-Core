package github.kasuminova.novaeng.mixin.geometry.vertex;

import github.kasuminova.novaeng.client.geometry.GeometryGeneration;
import github.kasuminova.novaeng.client.geometry.vertex.CompactQuadBuilderAccess;
import github.kasuminova.novaeng.client.geometry.vertex.FlatUnpackedData;
import github.kasuminova.novaeng.client.geometry.vertex.OwnedUnpackedQuadRecord;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.model.pipeline.UnpackedBakedQuad;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = UnpackedBakedQuad.Builder.class, remap = false)
public abstract class MixinUnpackedBakedQuadBuilder implements CompactQuadBuilderAccess {
    @Shadow
    @Final
    private VertexFormat format;
    @Shadow
    @Final
    @Mutable
    private float[][][] unpackedData;
    @Shadow
    private int vertices;
    @Shadow
    private int elements;
    @Shadow
    private boolean full;
    @Shadow
    private boolean contractUVs;
    @Shadow
    private int tint;
    @Shadow
    private EnumFacing orientation;
    @Shadow
    private TextureAtlasSprite texture;
    @Shadow
    private boolean applyDiffuseLighting;
    @Unique
    private FlatUnpackedData novaeng$data;

    @ModifyConstant(method = "<init>", constant = @Constant(intValue = 4, ordinal = 0), require = 1)
    private int novaeng$emptyOriginalTree(final int original) {
        return 0;
    }

    @Inject(method = "<init>", at = @At("RETURN"), require = 1)
    private void novaeng$allocateFlat(final VertexFormat format, final CallbackInfo ci) {
        novaeng$data = new FlatUnpackedData(format.getElementCount());
    }

    /**
     * @author NovaEngineering
     * @reason Keep the original element/vertex accounting with one private flat working buffer.
     */
    @Overwrite
    public void put(final int element, final float... data) {
        novaeng$data.put(vertices, element, data);
        elements++;
        if (elements == format.getElementCount()) {
            vertices++;
            elements = 0;
        }
        if (vertices == 4) full = true;
    }

    @Inject(method = "build", at = @At("HEAD"), require = 1)
    private void novaeng$exposeOriginalTree(final CallbackInfoReturnable<UnpackedBakedQuad> cir) {
        // Vanilla retains responsibility for validation, contraction, construction, and lazy packing.
        if (full && texture != null) unpackedData = novaeng$data.materialize();
    }

    @Override
    public OwnedUnpackedQuadRecord novaeng$buildRecord() {
        if (!full) throw new IllegalStateException("not enough data");
        if (texture == null) throw new IllegalStateException("texture not set");
        if (contractUVs) novaeng$data.contractUVs(format, texture);
        return GeometryGeneration.intern(new OwnedUnpackedQuadRecord(
            GeometryGeneration.intern(novaeng$data.snapshot()), tint, orientation, texture,
            applyDiffuseLighting, format));
    }
}
