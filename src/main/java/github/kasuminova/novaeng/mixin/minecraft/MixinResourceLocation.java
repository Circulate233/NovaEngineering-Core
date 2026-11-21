package github.kasuminova.novaeng.mixin.minecraft;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import github.kasuminova.novaeng.common.performance.string.ResourceStringCodec;
import github.kasuminova.novaeng.common.performance.string.StringCanon;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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

    /**
     * @author circulation
     * @reason Look up exact slices before allocating substring storage; preserve this public parser's case.
     */
    @Overwrite
    public static String[] splitObjectName(final String value) {
        return ResourceStringCodec.split(value);
    }

    @WrapOperation(method = "<init>(I[Ljava/lang/String;)V", at = @At(value = "INVOKE",
        target = "Ljava/lang/String;toLowerCase(Ljava/util/Locale;)Ljava/lang/String;", ordinal = 0), require = 0)
    private String nova$namespace(final String value, final Locale locale, final Operation<String> original) {
        return StringCanon.namespaces().lower(value, locale);
    }

    @WrapOperation(method = "<init>(I[Ljava/lang/String;)V", at = @At(value = "INVOKE",
        target = "Ljava/lang/String;toLowerCase(Ljava/util/Locale;)Ljava/lang/String;", ordinal = 1), require = 0)
    private String nova$path(final String value, final Locale locale, final Operation<String> original) {
        return StringCanon.locations().lower(value, locale);
    }

    @Inject(method = "<init>(I[Ljava/lang/String;)V", at = @At("RETURN"))
    private void nova$shareFields(final int unused, final String[] parts, final CallbackInfo ci) {
        // Also covers environments where another optimizer redirects the lowercase calls.
        this.namespace = StringCanon.namespaces().canonicalize(this.namespace);
        this.path = StringCanon.locations().canonicalize(this.path);
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
            value = StringCanon.locations().join(this.namespace, ':', this.path);
            this.nova$stableValue = value;
        }
        return value;
    }
}
