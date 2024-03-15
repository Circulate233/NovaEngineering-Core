package github.kasuminova.novaeng.mixin.ic2;

import ic2.core.energy.grid.Grid;
import ic2.core.energy.grid.Node;
import ic2.core.energy.leg.EnergyCalculatorLeg;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Collection;
import java.util.Collections;

@Mixin(EnergyCalculatorLeg.class)
public class MixinEnergyCalculatorLeg {

    @Redirect(
            method = "runCalculation",
            at = @At(
                    value = "INVOKE",
                    target = "Lic2/core/energy/grid/Grid;getNodes()Ljava/util/Collection;",
                    remap = false),
            remap = false
    )
    private static Collection<Node> injectRunCalculation(final Grid instance) {
        Collection<Node> nodes = instance.getNodes();
        if (nodes.size() <= 1) {
            return Collections.emptyList();
        }
        return nodes;
    }

}
