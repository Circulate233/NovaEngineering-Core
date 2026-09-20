package github.kasuminova.novaeng.mixin.loliasm;

import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraftforge.client.model.pipeline.UnpackedBakedQuad;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 消除 LoliASM 顶点数据池在模型烘焙期的同步开销与扩容开销。
 *
 * <p>原实现每次调用都对池加锁，但模型烘焙在主线程串行执行，锁不存在竞争，
 * 三百余万次加锁反而阻碍了 JIT 内联。另外池容量固定从 8192 起步，
 * 增长到四十余万唯一项需要反复重哈希。此处只消除这两项开销，其余语义保持不变。
 */
@Pseudo
@Mixin(targets = "zone.rong.loliasm.bakedquad.LoliVertexDataPool", remap = false)
public class MixinLoliVertexDataPool {

    @Shadow
    private static ObjectOpenCustomHashSet<int[]> POOL;

    @Shadow
    private static int deduplicatedCount;

    /**
     * 最终唯一顶点数据约四十万项，按此规模一次性分配可避免约七次重哈希。
     */
    @Unique
    private static final int NOVA$CAPACITY = 1 << 19;

    /**
     * @author circulation
     * @reason 按实测的唯一项规模重新分配池，避免加载期反复重哈希。
     */
    @Inject(method = "<clinit>", at = @At("TAIL"), remap = false, require = 1)
    private static void nova$preallocate(final CallbackInfo ci) {
        POOL = new ObjectOpenCustomHashSet<>(NOVA$CAPACITY, IntArrays.HASH_STRATEGY);
    }

    /**
     * @author circulation
     * @reason 模型烘焙为单线程，同步块无收益且阻碍 JIT 内联。
     */
    @Overwrite(remap = false)
    public static int[] canonicalize(final int[] arr) {
        if (POOL == null) {
            return arr;
        }
        deduplicatedCount++;
        return POOL.addOrGet(arr);
    }

    /**
     * @author circulation
     * @reason 同上；已解包的四边形无需进入池中。
     */
    @Overwrite(remap = false)
    public static int[] canonicalize(final int[] arr, final BakedQuad quad) {
        if (POOL == null || quad instanceof UnpackedBakedQuad) {
            return arr;
        }
        deduplicatedCount++;
        return POOL.addOrGet(arr);
    }

}
