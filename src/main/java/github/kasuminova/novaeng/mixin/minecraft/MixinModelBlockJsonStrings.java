package github.kasuminova.novaeng.mixin.minecraft;

import github.kasuminova.novaeng.common.performance.string.StringCanon;
import net.minecraft.client.renderer.block.model.ModelBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Map;

@Mixin(ModelBlock.Deserializer.class)
public abstract class MixinModelBlockJsonStrings {
    @Redirect(method = "getTextures", at = @At(value = "INVOKE",
        target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object nova$textureTokens(final Map<String, String> map, final Object key, final Object value) {
        return map.put(StringCanon.tokens().canonicalize((String) key), StringCanon.tokens().canonicalize((String) value));
    }
}
