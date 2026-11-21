package github.kasuminova.novaeng.mixin.geometry.vertex;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import github.kasuminova.novaeng.client.geometry.CompactModel;
import github.kasuminova.novaeng.client.geometry.vertex.CompactQuadBuilderAccess;
import github.kasuminova.novaeng.client.geometry.vertex.OwnedFluidGeometry;
import github.kasuminova.novaeng.client.geometry.vertex.OwnedFluidQuadList;
import github.kasuminova.novaeng.client.geometry.vertex.OwnedUnpackedQuadView;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.model.pipeline.UnpackedBakedQuad;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.List;

@Mixin(targets = "net.minecraftforge.client.model.ModelFluid$BakedFluid", remap = false)
public abstract class MixinModelFluidOwnedGeometry implements CompactModel, OwnedFluidGeometry {
    // Preserve Forge's field descriptor and its immutable map/list contract.
    @Shadow
    @Final
    @Mutable
    protected ImmutableMap<EnumFacing, ImmutableList<BakedQuad>> faceQuads;
    @Unique
    private volatile boolean novaeng$exposed;

    @Redirect(method = "buildQuad", at = @At(value = "INVOKE",
        target = "Lnet/minecraftforge/client/model/pipeline/UnpackedBakedQuad$Builder;build()Lnet/minecraftforge/client/model/pipeline/UnpackedBakedQuad;"), require = 1)
    private UnpackedBakedQuad novaeng$capturePrivateBuilder(final UnpackedBakedQuad.Builder builder) {
        return new OwnedUnpackedQuadView(((CompactQuadBuilderAccess) builder).novaeng$buildRecord());
    }

    // The Forge class implements a Minecraft interface method, so this selector needs remapping.
    @Inject(method = "getQuads", at = @At("HEAD"), remap = true, require = 1)
    private void novaeng$beforePublicRead(@Nullable final IBlockState state, @Nullable final EnumFacing side,
                                          final long rand, final CallbackInfoReturnable<List<BakedQuad>> cir) {
        if (side != null) novaeng$materialize();
    }

    @Unique
    private synchronized void novaeng$materialize() {
        if (novaeng$exposed) return;
        final ImmutableMap.Builder<EnumFacing, ImmutableList<BakedQuad>> faces = ImmutableMap.builder();
        for (final var entry : faceQuads.entrySet()) {
            final ImmutableList.Builder<BakedQuad> quads = ImmutableList.builder();
            for (final BakedQuad quad : entry.getValue()) {
                quads.add(quad instanceof OwnedUnpackedQuadView view ? view.materialize() : quad);
            }
            faces.put(entry.getKey(), quads.build());
        }
        faceQuads = faces.build();
        novaeng$exposed = true;
    }

    @Override
    public List<BakedQuad> novaeng$readQuads(@Nullable final IBlockState state, @Nullable final EnumFacing face,
                                             final long seed) {
        return novaeng$readBaseQuads(face);
    }

    @Override
    public synchronized List<BakedQuad> novaeng$readBaseQuads(@Nullable final EnumFacing side) {
        if (side == null) return ObjectLists.emptyList();
        final ImmutableList<BakedQuad> quads = faceQuads.get(side);
        return novaeng$exposed || quads.isEmpty() ? quads : new OwnedFluidQuadList(quads);
    }

    @Override
    public boolean novaeng$isGeometryExposed() {
        return novaeng$exposed;
    }
}
