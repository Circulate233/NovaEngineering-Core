package github.kasuminova.novaeng.mixin.lootoverhaul;

import com.tmtravlr.lootoverhaul.utilities.ConfigIdFileGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(value = ConfigIdFileGenerator.class,remap = false)
public class MixinConfigIdFileGenerator {

    /**
     * 我无法理解为什么会崩溃，我选择直接把它炸掉
     */
    @Redirect(method = "generateIDFiles",at = @At(value = "INVOKE", target = "Ljava/util/Collections;sort(Ljava/util/List;)V"))
    private static void sortR(List<Object> list){

    }
}
