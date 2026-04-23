package github.kasuminova.novaeng.mixin.psi;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import github.kasuminova.novaeng.NovaEngCoreConfig;
import github.kasuminova.novaeng.NovaEngineeringCore;
import net.minecraft.util.text.TextComponentString;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import vazkii.psi.api.spell.Spell;
import vazkii.psi.api.spell.SpellContext;
import vazkii.psi.api.spell.piece.PieceTrick;
import vazkii.psi.common.spell.trick.PieceTrickExplode;

@Mixin(value = PieceTrickExplode.class, remap = false)
public abstract class MixinPieceTrickExplode extends PieceTrick {

    @Unique
    private static final Double novaEngineering_Core$MAXPOWER = 5.0D;

    public MixinPieceTrickExplode(Spell spell) {
        super(spell);
    }

    @Definition(id = "getParamEvaluation", method = "Lvazkii/psi/common/spell/trick/PieceTrickExplode;getParamEvaluation(Lvazkii/psi/api/spell/SpellParam;)Ljava/lang/Object;", remap = false)
    @Definition(id = "power", field = "Lvazkii/psi/common/spell/trick/PieceTrickExplode;power:Lvazkii/psi/api/spell/SpellParam;", remap = false)
    @Expression("this.getParamEvaluation(this.power)")
    @ModifyExpressionValue(method = "addToMetadata", at = @At("MIXINEXTRAS:EXPRESSION"), remap = false)
    private Object novaEngineering_Core$clampMetadataPower(final Object original) {
        Double powerVal = (Double) original;
        if (powerVal != null && powerVal > novaEngineering_Core$MAXPOWER) {
            return novaEngineering_Core$MAXPOWER;
        }
        return powerVal;
    }

    @Definition(id = "getParamValue", method = "Lvazkii/psi/common/spell/trick/PieceTrickExplode;getParamValue(Lvazkii/psi/api/spell/SpellContext;Lvazkii/psi/api/spell/SpellParam;)Ljava/lang/Object;", remap = false)
    @Definition(id = "power", field = "Lvazkii/psi/common/spell/trick/PieceTrickExplode;power:Lvazkii/psi/api/spell/SpellParam;", remap = false)
    @Expression("this.getParamValue(?, this.power)")
    @ModifyExpressionValue(method = "execute", at = @At("MIXINEXTRAS:EXPRESSION"), remap = false)
    private Object novaEngineering_Core$clampExecutePower(final Object original, final SpellContext context) {
        Double powerVal = (Double) original;
        if (NovaEngCoreConfig.CLIENT.piece) {
            if (context != null && powerVal != null && powerVal > novaEngineering_Core$MAXPOWER) {
                NovaEngineeringCore.log.info(context.caster.getName() + "试图释放超过5的爆炸效果，已经修正");
                context.caster.world.playerEntities.forEach(player -> player.sendMessage(new TextComponentString(context.caster.getName() + "[" + context.caster.getUniqueID() + "]试图释放超过5的爆炸效果，已经修正")));
                return novaEngineering_Core$MAXPOWER;
            }
        }
        return powerVal;
    }
}
