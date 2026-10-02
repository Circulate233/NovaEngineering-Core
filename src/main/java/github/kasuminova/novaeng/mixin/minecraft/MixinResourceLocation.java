package github.kasuminova.novaeng.mixin.minecraft;

import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ResourceLocation.class)
public abstract class MixinResourceLocation {

    @Shadow
    @Final
    protected String namespace;

    @Shadow
    @Final
    protected String path;

    @Unique
    private volatile String nova$stableValue;

    @Unique
    private volatile int nova$stableHash;

    @Overwrite
    public String toString() {
        return nova$stableValue();
    }

    @Overwrite
    public int hashCode() {
        int hash = this.nova$stableHash;
        if (hash == 0) {
            hash = 31 * this.namespace.hashCode() + this.path.hashCode();
            this.nova$stableHash = hash;
        }
        return hash;
    }

    @Unique
    private String nova$stableValue() {
        String value = this.nova$stableValue;
        if (value == null) {
            value = this.namespace + ':' + this.path;
            this.nova$stableValue = value;
        }
        return value;
    }
}
