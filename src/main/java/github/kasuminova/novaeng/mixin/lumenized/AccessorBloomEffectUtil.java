package github.kasuminova.novaeng.mixin.lumenized;

import gregtech.client.utils.BloomEffectUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Map;

/** Exposes the custom bloom ticket map and private no-FBO cleanup operations. */
@Mixin(value = BloomEffectUtil.class, remap = false)
public interface AccessorBloomEffectUtil {

    /** Returns every custom bloom render group after the original preDraw scheduling step. */
    @Accessor("BLOOM_RENDERS")
    static Map<?, ?> nova$getBloomRenders() {
        throw new AssertionError("Accessor was not transformed");
    }

    /** Executes Lumenized's private cleanup for the preDraw state. */
    @Invoker("postDraw")
    static void nova$postDraw() {
        throw new AssertionError("Invoker was not transformed");
    }
}
