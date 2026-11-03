package github.kasuminova.novaeng.mixin.minecraft;

import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Locale;

@Mixin(ResourceLocation.class)
public abstract class MixinResourceLocation {

    @Shadow
    protected String namespace;

    @Shadow
    protected String path;

    @Unique
    private volatile String nova$stableValue;

    @Unique
    private volatile int nova$stableHash;

    @Redirect(method = "<init>(I[Ljava/lang/String;)V", at = @At(value = "INVOKE",
        target = "Ljava/lang/String;toLowerCase(Ljava/util/Locale;)Ljava/lang/String;", ordinal = 0), require = 1)
    @SuppressWarnings("StringEquality")
    private String nova$defaultNamespace(final String namespace, final Locale locale) {
        // splitObjectName supplies this exact literal for names without a namespace.
        // Preserve both content and reference identity, and leave all other folding
        // (including Unicode and non-root locales) to the original implementation.
        return namespace == "minecraft" && locale == Locale.ROOT ? namespace : namespace.toLowerCase(locale);
    }

    /**
     * @author circulation
     * @reason Cache the string representation of this immutable resource location.
     */
    @Overwrite
    public String toString() {
        return nova$stableValue();
    }

    /**
     * @author circulation
     * @reason Reuse the hash of the immutable namespace and path on repeated lookups.
     */
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
