package github.kasuminova.novaeng.mixin.mmce;

import hellfirepvp.modularmachinery.common.util.BlockArray;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 消除多方块结构加载期 {@code BlockInformation} 池的同步与扩容开销。
 *
 * <p>{@code BlockArray#addBlock} 对结构中的每个方块位置都会调用一次 {@code canonicalize}，
 * 而原实现每次都对池加锁，且池使用默认容量（16），在机器数量下会反复重哈希。
 * 加载期为单线程，锁没有竞争，因此只去掉同步并把容量调整到合理规模。
 *
 * <p>刻意不缓存 {@code hashCode}：结构反序列化过程中 {@code addMatchingStates}
 * 仍会修改 {@code matchingStates}，缓存会使集合内部状态失配。
 */
@Mixin(value = BlockArray.BlockInformation.class, remap = false)
public class MixinBlockInformation {

    @Final
    @Mutable
    @Shadow
    private static ObjectOpenHashSet<BlockArray.BlockInformation> POOL;

    /**
     * 结构加载期进入池中的实例数量级为数千至数万，默认容量 16 会带来多次重哈希。
     */
    @Unique
    private static final int NOVA$CAPACITY = 8192;

    /**
     * @author circulation
     * @reason 默认容量对于机器数量而言过小，加载期会反复重哈希整张表。
     */
    @Inject(method = "<clinit>", at = @At("TAIL"), remap = false, require = 1)
    private static void nova$preallocate(final CallbackInfo ci) {
        POOL = new ObjectOpenHashSet<>(NOVA$CAPACITY);
    }

    /**
     * @author circulation
     * @reason 结构加载为单线程，同步块无收益且阻碍 JIT 内联。
     */
    @Overwrite(remap = false)
    public BlockArray.BlockInformation canonicalize() {
        return POOL.addOrGet((BlockArray.BlockInformation) (Object) this);
    }

}
