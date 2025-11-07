package github.kasuminova.novaeng.common.hypernet.old.upgrade;

import crafttweaker.annotations.ZenRegister;
import github.kasuminova.mmce.common.upgrade.UpgradeType;
import github.kasuminova.novaeng.common.crafttweaker.util.NovaEngUtils;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import stanhebben.zenscript.annotations.ZenClass;

import java.util.ArrayList;
import java.util.List;

@ZenRegister
@ZenClass("novaeng.hypernet.upgrade.ProcessorModuleGPU")
public class ProcessorModuleGPU extends ProcessorModuleCPU {
    public ProcessorModuleGPU(final UpgradeType type) {
        super(type);
    }

    @Override
    public ProcessorModuleGPU copy(ItemStack parentStack) {
        ProcessorModuleGPU upgrade = new ProcessorModuleGPU(getType());
        upgrade.eventProcessor.putAll(eventProcessor);
        upgrade.parentStack = parentStack;
        return upgrade;
    }

    @Override
    public List<String> getDescriptions() {
        List<String> desc = new ArrayList<>();
        desc.add(I18n.format("upgrade.data_processor.module.gpu.tip.0"));

        desc.add(I18n.format("upgrade.data_processor.module.cpu.generate",
            NovaEngUtils.formatFLOPS(calculate(false, getComputationPointGeneration()))
        ));

        getEnergyDurabilityTip(desc, moduleType);

        return desc;
    }
}
