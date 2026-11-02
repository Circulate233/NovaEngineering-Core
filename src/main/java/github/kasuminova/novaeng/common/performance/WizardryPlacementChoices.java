package github.kasuminova.novaeng.common.performance;

import electroblob.wizardry.worldgen.WorldGenWizardryStructure;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public final class WizardryPlacementChoices {
    private static final Rotation[] ROTATIONS = Rotation.values();
    private static final Mirror[] MIRRORS = {Mirror.NONE, Mirror.LEFT_RIGHT};
    private static final ClassValue<Integer> DEFAULTS = new ClassValue<>() {
        @Override
        protected Integer computeValue(final Class<?> type) {
            try {
                final MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(type, MethodHandles.lookup());
                final boolean rotations = lookup.revealDirect(lookup.findVirtual(type, "getValidRotations", MethodType.methodType(Rotation[].class)))
                                                .getDeclaringClass() == WorldGenWizardryStructure.class;
                final boolean mirrors = lookup.revealDirect(lookup.findVirtual(type, "getValidMirrors", MethodType.methodType(Mirror[].class)))
                                              .getDeclaringClass() == WorldGenWizardryStructure.class;
                return (rotations ? 1 : 0) | (mirrors ? 2 : 0);
            } catch (final ReflectiveOperationException unavailable) {
                return 0;
            }
        }
    };

    private WizardryPlacementChoices() {
    }

    public static Rotation[] rotations(final WorldGenWizardryStructure generator) {
        return (DEFAULTS.get(generator.getClass()) & 1) != 0 ? ROTATIONS : generator.getValidRotations();
    }

    public static Mirror[] mirrors(final WorldGenWizardryStructure generator) {
        return (DEFAULTS.get(generator.getClass()) & 2) != 0 ? MIRRORS : generator.getValidMirrors();
    }
}
