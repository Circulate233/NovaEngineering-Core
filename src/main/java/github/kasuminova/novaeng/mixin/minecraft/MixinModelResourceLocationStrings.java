package github.kasuminova.novaeng.mixin.minecraft;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import github.kasuminova.novaeng.common.performance.string.ResourceStringCodec;
import github.kasuminova.novaeng.common.performance.string.StringCanon;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Locale;

@Mixin(ModelResourceLocation.class)
public abstract class MixinModelResourceLocationStrings extends ResourceLocation {
    @Shadow
    @Final
    @Mutable
    private String variant;
    @Unique
    private volatile String nova$modelValue;

    protected MixinModelResourceLocationStrings(final String value) {
        super(value);
    }

    /**
     * @author circulation
     * @reason Parse all three slices without an intermediate path substring or second parts array.
     */
    @Overwrite
    protected static String[] parsePathString(final String value) {
        return ResourceStringCodec.model(value);
    }

    @WrapOperation(method = "<init>(I[Ljava/lang/String;)V", at = @At(value = "INVOKE",
        target = "Ljava/lang/String;toLowerCase(Ljava/util/Locale;)Ljava/lang/String;"), require = 0)
    private String nova$variant(final String value, final Locale locale, final Operation<String> original) {
        return StringCanon.locations().lower(value, locale);
    }

    @Inject(method = "<init>(I[Ljava/lang/String;)V", at = @At("RETURN"))
    private void nova$shareVariant(final int unused, final String[] parts, final CallbackInfo ci) {
        this.variant = StringCanon.locations().canonicalize(this.variant);
    }

    /**
     * @author circulation
     * @reason Share the full representation before allocating a concatenation and cache repeated calls.
     */
    @Overwrite
    public String toString() {
        String result = this.nova$modelValue;
        if (result == null) {
            result = StringCanon.locations().join(super.toString(), '#', this.variant);
            this.nova$modelValue = result;
        }
        return result;
    }
}
