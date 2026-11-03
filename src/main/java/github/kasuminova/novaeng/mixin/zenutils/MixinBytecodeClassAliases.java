package github.kasuminova.novaeng.mixin.zenutils;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import youyihj.zenutils.impl.member.ClassData;
import youyihj.zenutils.impl.member.ClassDataFetcher;
import youyihj.zenutils.impl.member.bytecode.BytecodeClassDataFetcher;

import java.util.Map;
import java.util.Set;

@Mixin(value = BytecodeClassDataFetcher.class, remap = false)
public abstract class MixinBytecodeClassAliases {
    @Shadow
    @Final
    private Map<String, ClassData> cache;
    @Shadow
    @Final
    private Set<String> absentTries;
    @Shadow
    @Final
    private ClassDataFetcher parent;
    @Shadow
    private boolean closed;

    @Shadow
    private ClassData findClass(final String className) throws ClassNotFoundException {
        throw new AssertionError();
    }

    /**
     * @author circulation
     * @reason Share successful class descriptions between slash/dot aliases while retaining original failure paths.
     */
    @Overwrite
    public ClassData forName(final String className) throws ClassNotFoundException {
        if (closed) {
            return parent.forName(className);
        }
        final String key = className.replace('/', '.');
        final ClassData cached = cache.get(key);
        if (cached != null || cache.containsKey(key)) {
            return cached;
        }
        // Negative probes keep the caller's spelling and the original exception/cause behavior.
        if (absentTries.contains(className)) {
            throw new ClassNotFoundException(className);
        }
        try {
            final ClassData result = findClass(className);
            cache.put(key, result);
            return result;
        } catch (ClassNotFoundException failure) {
            absentTries.add(className);
            throw failure;
        }
    }
}
