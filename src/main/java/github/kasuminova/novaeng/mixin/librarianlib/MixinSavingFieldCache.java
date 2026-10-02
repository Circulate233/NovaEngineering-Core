package github.kasuminova.novaeng.mixin.librarianlib;

import github.kasuminova.novaeng.NovaEngCoreConfig;
import kotlin.Metadata;
import kotlin.jvm.JvmClassMappingKt;
import kotlin.reflect.KClass;
import kotlin.reflect.full.KClasses;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Collection;
import java.util.Collections;

@Mixin(targets = "com.teamwizardry.librarianlib.features.saving.SavingFieldCache", remap = false)
public abstract class MixinSavingFieldCache {

    /**
     * LibrarianLib builds its save field cache for every registered packet and tile class, and walks
     * the Kotlin property list of each one through kotlin-reflect, which is what the bulk of that
     * walk costs (504ms of the 664ms measured, plus 92ms in the Kotlin getter/setter lookups it
     * leads to).
     * Plain Java classes have no Kotlin properties, and their getters and setters are collected by
     * {@code buildClassGetSetters} through plain reflection, so the property walk is pure overhead
     * for them. Classes compiled by Kotlin carry {@code @kotlin.Metadata}, which is what tells the
     * two apart; everything else keeps the original path.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Redirect(method = "buildClassFields",
              at = @At(value = "INVOKE",
                       target = "Lkotlin/reflect/full/KClasses;getDeclaredMemberProperties(Lkotlin/reflect/KClass;)Ljava/util/Collection;"),
              remap = false)
    private Collection<?> nova$skipKotlinPropertiesForJavaClasses(final KClass<?> kClass) {
        if (NovaEngCoreConfig.CLIENT.optimizeLibrarianLibFieldScan
                && JvmClassMappingKt.getJavaClass(kClass).getAnnotation(Metadata.class) == null) {
            return Collections.emptyList();
        }
        return KClasses.getDeclaredMemberProperties((KClass) kClass);
    }
}
