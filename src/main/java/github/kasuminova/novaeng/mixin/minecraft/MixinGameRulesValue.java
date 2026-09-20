package github.kasuminova.novaeng.mixin.minecraft;

import github.kasuminova.novaeng.common.gamerule.GameRuleBooleanFastPath;
import github.kasuminova.novaeng.common.gamerule.GameRuleBooleanFastPathImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.world.GameRules$Value")
public class MixinGameRulesValue {

    @Shadow
    private String valueString;

    @Shadow
    private boolean valueBoolean;

    @Shadow
    private int valueInteger;

    @Inject(method = "setValue", at = @At("HEAD"), cancellable = true)
    private void nova$setBooleanWithoutParsing(final String value, final CallbackInfo ci) {
        final GameRuleBooleanFastPath.Result result = GameRuleBooleanFastPathImpl.instance()
            .match(value).orElse(null);
        if (result == null) {
            return;
        }
        this.valueString = value;
        this.valueBoolean = result.booleanValue();
        this.valueInteger = result.integerValue();
        ci.cancel();
    }
}
