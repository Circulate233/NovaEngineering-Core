package github.kasuminova.novaeng.mixin.enumcache.ingameinfoxml;

import com.github.lunatrius.ingameinfo.parser.text.Token.TokenType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = TokenType.class, remap = false)
public abstract class MixinTokenType {

    @Shadow
    @Final
    private static TokenType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static TokenType[] values() {
        return $VALUES;
    }
}
