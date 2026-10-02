package github.kasuminova.novaeng.mixin.mekanism;

import github.kasuminova.novaeng.NovaEngCoreConfig;
import mekanism.client.render.obj.TransmitterModel;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.MinecraftForgeClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Remembers the quads Mekanism's OBJ transmitter model produced for a block state, instead of rebuilding them on every
 * call.
 *
 * <p>{@code TransmitterModel} has its own cache, but it cannot answer for the states JourneyMap passes: it keys on the
 * extended state's unlisted properties ({@code OBJProperty}, {@code PropertyConnection}) and a state read straight out
 * of the chunk carries none of them, so the lookup throws inside its own try block and every call falls through to
 * {@code OBJBakedModelBase.getQuads}, rebuilding every vertex, UV, normal and float array from scratch. JourneyMap asks
 * for the quads of every Mekanism transmitter on every map pass, from its worker threads, which made this the largest
 * single allocation point in the client and a first-order driver of the collector.</p>
 *
 * <p>The answer only depends on the state instance, the render layer the model is asked about, and the model instance
 * itself, so it can be remembered as is: the returned list is handed out unchanged, exactly as {@code getQuads} would
 * have returned it, and the layer is compared so a state seen during a solid pass is not answered with quads built for
 * the translucent one. Calls with a facing take the cheap {@code ImmutableList.of()} branch and are left alone, as are
 * calls with a null state.</p>
 *
 * <p>The remembered values live in one holder object that is only ever replaced, never written field by field, so a
 * reader on another thread sees a consistent set of values or none at all.</p>
 */
@Mixin(value = TransmitterModel.class, remap = false)
public abstract class MixinTransmitterModelQuadsCache {

    @Unique
    private Object[] nova$quadsCache;

    @Inject(
        method = "getQuads(Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/EnumFacing;J)Ljava/util/List;",
        at = @At("HEAD"), cancellable = true, remap = true, require = 1
    )
    private void nova$reuseQuads(final IBlockState state, final EnumFacing facing, final long rand,
                                 final CallbackInfoReturnable<List<BakedQuad>> cir) {
        if (!NovaEngCoreConfig.CLIENT.optimizeMekanismObjQuads || state == null || facing != null) {
            return;
        }
        final Object[] cache = this.nova$quadsCache;
        if (cache != null && cache[0] == state
                && cache[1] == MinecraftForgeClient.getRenderLayer()
                && ((Long) cache[2]).longValue() == rand) {
            cir.setReturnValue((List<BakedQuad>) cache[3]);
        }
    }

    @Inject(
        method = "getQuads(Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/EnumFacing;J)Ljava/util/List;",
        at = @At("RETURN"), remap = true, require = 1
    )
    private void nova$rememberQuads(final IBlockState state, final EnumFacing facing, final long rand,
                                    final CallbackInfoReturnable<List<BakedQuad>> cir) {
        if (!NovaEngCoreConfig.CLIENT.optimizeMekanismObjQuads || state == null || facing != null) {
            return;
        }
        final List<BakedQuad> quads = cir.getReturnValue();
        if (quads == null) {
            return;
        }
        this.nova$quadsCache = new Object[]{state, MinecraftForgeClient.getRenderLayer(), rand, quads};
    }
}
