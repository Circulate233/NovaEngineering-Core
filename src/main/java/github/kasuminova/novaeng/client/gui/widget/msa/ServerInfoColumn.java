package github.kasuminova.novaeng.client.gui.widget.msa;

import github.kasuminova.mmce.client.gui.widget.HorizontalLine;
import github.kasuminova.mmce.client.gui.widget.MultiLineLabel;
import github.kasuminova.mmce.client.gui.widget.base.WidgetGui;
import github.kasuminova.mmce.client.gui.widget.container.ScrollingColumn;
import github.kasuminova.mmce.client.gui.widget.event.GuiEvent;
import github.kasuminova.novaeng.client.gui.widget.msa.event.AssemblerInvUpdateEvent;
import github.kasuminova.novaeng.common.container.slot.AssemblySlotManager;
import github.kasuminova.novaeng.common.container.slot.SlotCPUItemHandler;
import github.kasuminova.novaeng.common.container.slot.SlotCalculateCardItemHandler;
import github.kasuminova.novaeng.common.container.slot.SlotCapacitorItemHandler;
import github.kasuminova.novaeng.common.container.slot.SlotConditionItemHandler;
import github.kasuminova.novaeng.common.container.slot.SlotExtensionCardItemHandler;
import github.kasuminova.novaeng.common.container.slot.SlotPSUItemHandler;
import github.kasuminova.novaeng.common.container.slot.SlotRAMItemHandler;
import github.kasuminova.novaeng.common.crafttweaker.util.NovaEngUtils;
import github.kasuminova.novaeng.common.hypernet.calculation.Calculable;
import github.kasuminova.novaeng.common.hypernet.calculation.CalculateRequest;
import github.kasuminova.novaeng.common.hypernet.calculation.CalculateStage;
import github.kasuminova.novaeng.common.hypernet.calculation.CalculateType;
import github.kasuminova.novaeng.common.hypernet.calculation.CalculateTypes;
import github.kasuminova.novaeng.common.hypernet.calculation.modifier.ModifierManager;
import github.kasuminova.novaeng.common.hypernet.computer.ModularServer;
import github.kasuminova.novaeng.common.util.TileItemHandler;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.resources.I18n;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;

public class ServerInfoColumn extends ScrollingColumn {

    protected ModularServer server;

    public ServerInfoColumn(final ModularServer server) {
        this.server = server;
    }

    protected static int getInstalledSlots(final TileItemHandler inv, final AssemblySlotManager slotManager, final String invName, Predicate<SlotConditionItemHandler> slotFilter) {
        int[] installedSlots = {0};
        inv.getAvailableSlotsStream()
                .mapToObj(slotID -> slotManager.getSlot(invName, slotID))
                .filter(slotFilter)
                .filter(SlotConditionItemHandler::isInstalled)
                .forEach(slot -> installedSlots[0]++);
        return installedSlots[0];
    }

    protected static void addUninstalledDependenciesTip(final TileItemHandler inv, final AssemblySlotManager slotManager, final String invName, List<String> tip) {
        inv.getAvailableSlotsStream()
                .mapToObj(slotID -> slotManager.getSlot(invName, slotID))
                .filter(SlotConditionItemHandler::isInstalled)
                .forEach(slot -> {
                    slot.getDependencies().stream()
                            .filter(dependency -> !dependency.isInstalled())
                            .map(dependency -> dependency.getSlotDescription() + I18n.format("gui.modular_server_assembler.assembly.dependencies.uninstalled"))
                            .forEach(tip::add);
                    slot.getSoftDependencies().stream()
                            .filter(softDependency -> !softDependency.isInstalled())
                            .map(softDependency -> softDependency.getSlotDescription() + I18n.format("gui.modular_server_assembler.assembly.dependencies.uninstalled"))
                            .forEach(tip::add);
                });
    }

    protected static void addUninstalledDependenciesTip(final TileItemHandler cpu, final AssemblySlotManager slotManager, final TileItemHandler calculateCard, final TileItemHandler extension, final TileItemHandler power, final List<String> errorTips) {
        List<String> uninstalledDependenciesTip = new ObjectArrayList<>();
        addUninstalledDependenciesTip(cpu, slotManager, "cpu", uninstalledDependenciesTip);
        addUninstalledDependenciesTip(calculateCard, slotManager, "calculate_card", uninstalledDependenciesTip);
        addUninstalledDependenciesTip(extension, slotManager, "extension", uninstalledDependenciesTip);
        addUninstalledDependenciesTip(power, slotManager, "power", uninstalledDependenciesTip);
        if (!uninstalledDependenciesTip.isEmpty()) {
            errorTips.add(I18n.format("gui.modular_server_assembler.error.dependencies.0"));
            errorTips.add(I18n.format("gui.modular_server_assembler.error.dependencies.1"));
            errorTips.addAll(uninstalledDependenciesTip);
        }
    }

    @Override
    public void initWidget(final WidgetGui gui) {
        super.initWidget(gui);
        updateTips();
    }

    protected void updateTips() {
        widgets.clear();

        if (server == null) {
            addWidget(createLabel(Collections.singletonList(I18n.format("gui.modular_server_assembler.info.missing_server"))));
            return;
        }

        AssemblySlotManager slotManager = server.getSlotManager();
        TileItemHandler cpu = server.getInvByName("cpu");
        TileItemHandler calculateCard = server.getInvByName("calculate_card");
        TileItemHandler extension = server.getInvByName("extension");
        TileItemHandler power = server.getInvByName("power");

        int installedCPUModules = getInstalledSlots(cpu, slotManager, "cpu", SlotCPUItemHandler.class::isInstance);
        int installedRAMModules = getInstalledSlots(cpu, slotManager, "cpu", SlotRAMItemHandler.class::isInstance);
        int installedCalculateCardModules = getInstalledSlots(calculateCard, slotManager, "calculate_card", SlotCalculateCardItemHandler.class::isInstance);
        int installedExtensionCardModules = getInstalledSlots(extension, slotManager, "extension", SlotExtensionCardItemHandler.class::isInstance);
        int installedPSUModules = getInstalledSlots(power, slotManager, "power", SlotPSUItemHandler.class::isInstance);
        int installedCapacitorModules = getInstalledSlots(power, slotManager, "power", SlotCapacitorItemHandler.class::isInstance);
        int totalInstalledModules = server.getModules().size();

        addModuleTips(totalInstalledModules, installedCPUModules, installedRAMModules, installedCalculateCardModules, installedExtensionCardModules, installedPSUModules, installedCapacitorModules);
        addPropertiesTips();
        addHardwareBandwidthTips();

        List<String> errorTips = new LinkedList<>();
        addErrorTips(installedCPUModules, errorTips, installedRAMModules, installedPSUModules, installedCapacitorModules, cpu, slotManager, calculateCard, extension, power);

        addWidget(createLabel(Collections.singletonList(I18n.format("gui.modular_server_assembler.info.can_start",
                I18n.format("gui.modular_server_assembler.info.can_start." + (errorTips.isEmpty() ? "true" : "false"))))));
        if (!errorTips.isEmpty()) {
            errorTips.add(0, I18n.format("gui.modular_server_assembler.error"));
            addWidget(createLabel(errorTips));
            return;
        }

        addWidget(createSeparator());
        addExpectedCalculateTip();
    }

    private void addModuleTips(final int totalInstalledModules, final int installedCPUModules, final int installedRAMModules, final int installedCalculateCardModules, final int installedExtensionCardModules, final int installedPSUModules, final int installedCapacitorModules) {
        addWidget(createLabel(Collections.singletonList(I18n.format("gui.modular_server_assembler.info.total_modules", totalInstalledModules))));
        List<String> moduleTips = new ObjectArrayList<>();
        moduleTips.add(I18n.format("gui.modular_server_assembler.info.total_cpus", installedCPUModules));
        moduleTips.add(I18n.format("gui.modular_server_assembler.info.total_rams", installedRAMModules));
        moduleTips.add(I18n.format("gui.modular_server_assembler.info.total_calculate_cards", installedCalculateCardModules));
        moduleTips.add(I18n.format("gui.modular_server_assembler.info.total_extensions", installedExtensionCardModules));
        moduleTips.add(I18n.format("gui.modular_server_assembler.info.total_psus", installedPSUModules));
        moduleTips.add(I18n.format("gui.modular_server_assembler.info.total_capacitors", installedCapacitorModules));
        addWidget(createLabel(moduleTips));
        addWidget(createSeparator());
    }

    protected void addHardwareBandwidthTips() {
        List<String> hardwareBandwidthTips = new ObjectArrayList<>();
        int totalHardwareBandwidth = server.getTotalHardwareBandwidth();
        int usedHardwareBandwidth = server.getUsedHardwareBandwidth();
        hardwareBandwidthTips.add(I18n.format("gui.modular_server_assembler.info.total_hardware_bandwidth", totalHardwareBandwidth));
        if (totalHardwareBandwidth <= 0) {
            hardwareBandwidthTips.add(I18n.format("gui.modular_server_assembler.info.used_hardware_bandwidth", usedHardwareBandwidth, usedHardwareBandwidth > 0 ? "100%" : "0%"));
        } else {
            hardwareBandwidthTips.add(I18n.format("gui.modular_server_assembler.info.used_hardware_bandwidth", usedHardwareBandwidth, NovaEngUtils.formatPercent(usedHardwareBandwidth, totalHardwareBandwidth)));
        }
        addWidget(createLabel(hardwareBandwidthTips));
        addWidget(createSeparator());
    }

    protected void addPropertiesTips() {
        List<String> propertiesTips = new ArrayList<>();
        propertiesTips.add(I18n.format("gui.modular_server_assembler.info.max_energy_cap", NovaEngUtils.formatNumber(server.getMaxEnergyCap())));
        propertiesTips.add(I18n.format("gui.modular_server_assembler.info.max_energy_consumption", NovaEngUtils.formatNumber(server.getMaxEnergyConsumption())));
        propertiesTips.add(I18n.format("gui.modular_server_assembler.info.max_energy_provision", NovaEngUtils.formatNumber(server.getMaxEnergyProvision())));
        addWidget(createLabel(propertiesTips));
        addWidget(createSeparator());
    }

    protected void addErrorTips(final int installedCPUModules, final List<String> errorTips, final int installedRAMModules, final int installedPSUModules, final int installedCapacitorModules, final TileItemHandler cpu, final AssemblySlotManager slotManager, final TileItemHandler calculateCard, final TileItemHandler extension, final TileItemHandler power) {
        if (installedCPUModules <= 0) {
            errorTips.add(I18n.format("gui.modular_server_assembler.error.require_cpu"));
        }
        if (installedRAMModules <= 0) {
            errorTips.add(I18n.format("gui.modular_server_assembler.error.require_ram"));
        }
        if (installedPSUModules <= 0) {
            errorTips.add(I18n.format("gui.modular_server_assembler.error.require_psu"));
        }
        if (installedCapacitorModules <= 0) {
            errorTips.add(I18n.format("gui.modular_server_assembler.error.require_capacitor"));
        }
        int totalHardwareBandwidth = server.getTotalHardwareBandwidth();
        int usedHardwareBandwidth = server.getUsedHardwareBandwidth();
        if (totalHardwareBandwidth * 1.5 < usedHardwareBandwidth) {
            errorTips.add(I18n.format("gui.modular_server_assembler.error.hardware_bandwidth"));
        }
        addUninstalledDependenciesTip(cpu, slotManager, calculateCard, extension, power, errorTips);
    }

    protected void addExpectedCalculateTip() {
        List<String> tip = new ObjectArrayList<>();
        tip.add(I18n.format("gui.modular_server_assembler.calculate.expected"));

        for (final CalculateType type : CalculateTypes.getAvailableTypes().values()) {
            tip.add(I18n.format("gui.modular_server_assembler.calculate.name",
                    type.getFormattedTypeName()
            ));
            tip.add(I18n.format("gui.modular_server_assembler.calculate.value",
                    type.format(server.calculate(
                            new CalculateRequest(Double.MAX_VALUE, true, type, CalculateStage.START, server.getOwner(), new ModifierManager(), new Object2ObjectOpenHashMap<>()))
                            .generated()
                    ),
                    Calculable.formatEfficiency(server.getCalculateAvgEfficiency(type))
            ));
        }

        addWidget(createLabel(tip));
        addWidget(createSeparator());
    }

    protected HorizontalLine createSeparator() {
        return (HorizontalLine) new HorizontalLine().setWidth(width - 8 - 4).setHeight(1).setMarginLeft(2);
    }

    protected MultiLineLabel createLabel(final List<String> contents) {
        return new MultiLineLabel(contents).setWidth(width - 8 - 4).setScale(0.8F);
    }

    @Override
    public boolean onGuiEvent(final GuiEvent event) {
        if (event instanceof AssemblerInvUpdateEvent serverUpdateEvent) {
            this.server = serverUpdateEvent.getServer();
            updateTips();
        }
        return super.onGuiEvent(event);
    }

}
