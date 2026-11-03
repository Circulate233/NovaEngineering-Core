package github.kasuminova.novaeng.mixin.kirino;

import com.cleanroommc.kirino.ecs.storage.ArrayRange;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Set;

@Mixin(value = ArrayRange.class, remap = false)
public interface InvokerArrayRange {
    @Invoker("<init>")
    static ArrayRange nova$createRange(int start, int end, Set<Integer> deprecatedIndexes) {
        throw new AssertionError();
    }
}
