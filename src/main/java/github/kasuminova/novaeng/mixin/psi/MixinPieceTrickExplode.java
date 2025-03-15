package github.kasuminova.novaeng.mixin.psi;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vazkii.psi.api.spell.*;
import vazkii.psi.api.spell.piece.PieceTrick;
import vazkii.psi.common.spell.trick.PieceTrickExplode;

@Mixin(value = PieceTrickExplode.class,remap = false)
public abstract class MixinPieceTrickExplode extends PieceTrick {

    @Shadow
    SpellParam position;
    @Shadow
    SpellParam power;

    public MixinPieceTrickExplode(Spell spell) {
        super(spell);
    }

    @Shadow public abstract void addToMetadata(SpellMetadata meta) throws SpellCompilationException;

    @Unique
    private static final double novaEngineering_Core$MAXPOWER = 5.0D;
    @Unique
    SpellContext novaEngineering_Core$context;

    @Redirect(method = "addToMetadata",at = @At(value = "INVOKE", target = "Lvazkii/psi/common/spell/trick/PieceTrickExplode;getParamEvaluation(Lvazkii/psi/api/spell/SpellParam;)Ljava/lang/Object;"))
    public Object addToMetadataMixin(PieceTrickExplode instance, SpellParam spellParam) throws SpellCompilationException {
        Double powerVal = this.getParamEvaluation(this.power);
        if (powerVal != null && powerVal > novaEngineering_Core$MAXPOWER) {
            return novaEngineering_Core$MAXPOWER;
        }
        return powerVal;
    }

    @Inject(method = "execute",at = @At(value = "HEAD"))
    public void execute(SpellContext context, CallbackInfoReturnable<Object> cir){
        novaEngineering_Core$context = context;
    }

    @Redirect(method = "execute",at = @At(value = "INVOKE", target = "Lvazkii/psi/common/spell/trick/PieceTrickExplode;getParamValue(Lvazkii/psi/api/spell/SpellContext;Lvazkii/psi/api/spell/SpellParam;)Ljava/lang/Object;",ordinal = 1))
    public Object executeRed(PieceTrickExplode instance, SpellContext spellContext, SpellParam spellParam) {
        Double powerVal = this.getParamValue(novaEngineering_Core$context, this.power);
        if (novaEngineering_Core$context != null && powerVal != null && powerVal > novaEngineering_Core$MAXPOWER){
            return novaEngineering_Core$MAXPOWER;
        }
        return powerVal;
    }
}
