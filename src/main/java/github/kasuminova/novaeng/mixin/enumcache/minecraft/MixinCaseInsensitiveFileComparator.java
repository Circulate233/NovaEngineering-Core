package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraftforge.fml.relauncher.FileListHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = FileListHelper.CaseInsensitiveFileComparator.class, remap = false)
public abstract class MixinCaseInsensitiveFileComparator {

    @Shadow
    @Final
    private static FileListHelper.CaseInsensitiveFileComparator[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static FileListHelper.CaseInsensitiveFileComparator[] values() {
        return $VALUES;
    }
}
