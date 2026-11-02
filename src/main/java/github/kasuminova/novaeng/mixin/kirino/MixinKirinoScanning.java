package github.kasuminova.novaeng.mixin.kirino;

import com.cleanroommc.kirino.ecs.component.scan.helper.ClassScanUtils;
import github.kasuminova.novaeng.common.performance.KirinoScanCache;
import io.github.classgraph.ClassInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.List;
import java.util.Map;

@Mixin(value = ClassScanUtils.class, remap = false)
public abstract class MixinKirinoScanning {
    /**
     * @author circulation
     * @reason Scan the class/annotation/field information actually consumed and reuse the paired scan.
     */
    @Overwrite
    static Map<String, ClassInfo> scan(final List<String> packages, final String annotation) {
        return KirinoScanCache.scan(packages, annotation);
    }
}
