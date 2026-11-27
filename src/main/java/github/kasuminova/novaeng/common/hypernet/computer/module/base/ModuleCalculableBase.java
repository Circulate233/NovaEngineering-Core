package github.kasuminova.novaeng.common.hypernet.computer.module.base;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectHeapPriorityQueue;
import crafttweaker.annotations.ZenRegister;
import github.kasuminova.novaeng.common.crafttweaker.util.NovaEngUtils;
import github.kasuminova.novaeng.common.hypernet.calculation.Calculable;
import github.kasuminova.novaeng.common.hypernet.calculation.CalculateType;
import github.kasuminova.novaeng.common.hypernet.calculation.CalculateTypes;
import github.kasuminova.novaeng.common.hypernet.computer.module.ServerModule;
import net.minecraft.client.resources.I18n;
import stanhebben.zenscript.annotations.ZenClass;

import java.util.List;

@ZenRegister
@ZenClass("novaeng.hypernet.server.module.base.ModuleCalculableBase")
public abstract class ModuleCalculableBase<T extends ServerModule & Calculable> extends ServerModuleBase<T> {

    protected final double baseGeneration;
    protected final double energyConsumeRatio;
    protected final int hardwareBandwidth;

    public ModuleCalculableBase(final String registryName, double baseGeneration, double energyConsumeRatio, int hardwareBandwidth) {
        super(registryName);
        this.baseGeneration = baseGeneration;
        this.energyConsumeRatio = energyConsumeRatio;
        this.hardwareBandwidth = hardwareBandwidth;
    }

    @Override
    public List<String> getTooltip(final T moduleInstance) {
        List<String> tooltip = new ObjectArrayList<>(super.getTooltip(moduleInstance));

        tooltip.add(I18n.format("novaeng.hypernet.hardware_bandwidth.consume", this.hardwareBandwidth));
        tooltip.add(I18n.format("novaeng.hypernet.calculable.tip.base_ratio", this.baseGeneration));
        tooltip.add(I18n.format("novaeng.hypernet.calculable.tip.energy_consume_ratio", NovaEngUtils.formatNumber(Math.round(this.energyConsumeRatio))));
        tooltip.add(I18n.format("novaeng.hypernet.calculable.supported"));

        final ObjectHeapPriorityQueue<EfficiencyTip> efficiencyTips = new ObjectHeapPriorityQueue<>();

        for (final CalculateType type : CalculateTypes.getAvailableTypes().values()) {
            double typeEfficiency = moduleInstance.getCalculateTypeEfficiency(type);
            efficiencyTips.enqueue(new EfficiencyTip(typeEfficiency, I18n.format("novaeng.hypernet.calculate.tip.efficiency",
                type.getFormattedTypeName(),
                type.format(baseGeneration * typeEfficiency),
                Calculable.formatEfficiency(typeEfficiency)
            )));
        }

        while (!efficiencyTips.isEmpty()) tooltip.add("  " + efficiencyTips.dequeue().tip());
        return tooltip;
    }

    record EfficiencyTip(double efficiency, String tip) implements Comparable<EfficiencyTip> {
        @Override
        public int compareTo(final EfficiencyTip efficiencyTip) {
            return Double.compare(efficiencyTip.efficiency, efficiency);
        }
    }
}
