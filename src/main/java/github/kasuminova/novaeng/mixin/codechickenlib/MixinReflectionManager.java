package github.kasuminova.novaeng.mixin.codechickenlib;

import codechicken.lib.reflect.ReflectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.lang.reflect.Field;

@Mixin(value = ReflectionManager.class,remap = false)
public abstract class MixinReflectionManager {

    @Shadow
    private static Field modifiersField;

    @Shadow
    public static Class<?> findClass(String name) {
        return null;
    }

    /**
     * @author circulation
     * @reason 防止无限递归的出现
     */
    @Overwrite
    public static void removeFinal(Field field) {
        if ((field.getModifiers() & 16) != 0) {
            try {
                if (modifiersField == null) {
                    modifiersField = Field.class.getDeclaredField("modifiers");
                    modifiersField.setAccessible(true);
                }

                modifiersField.set(field, field.getModifiers() & -17);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
