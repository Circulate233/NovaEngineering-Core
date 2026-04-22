package github.kasuminova.novaeng.common.hypernet.old;

import crafttweaker.annotations.ZenRegister;
import github.kasuminova.mmce.common.event.recipe.FactoryRecipeTickEvent;
import github.kasuminova.mmce.common.event.recipe.RecipeCheckEvent;
import github.kasuminova.mmce.common.helper.IDynamicPatternInfo;
import github.kasuminova.mmce.common.util.concurrent.Queues;
import github.kasuminova.novaeng.common.hypernet.old.upgrade.ProcessorModuleCPU;
import github.kasuminova.novaeng.common.hypernet.old.upgrade.ProcessorModuleRAM;
import github.kasuminova.novaeng.common.registry.RegistryHyperNet;
import hellfirepvp.modularmachinery.common.lib.RequirementTypesMM;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.factory.FactoryRecipeThread;
import hellfirepvp.modularmachinery.common.modifier.RecipeModifier;
import hellfirepvp.modularmachinery.common.tiles.TileFactoryController;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import net.minecraft.nbt.NBTTagCompound;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenGetter;
import stanhebben.zenscript.annotations.ZenMethod;
import stanhebben.zenscript.annotations.ZenSetter;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@ZenRegister
@ZenClass("novaeng.hypernet.DataProcessor")
public class DataProcessor extends NetNode {
    private final Lock lock = new ReentrantLock();

    private final Queue<Long> recentEnergyUsage = Queues.createConcurrentQueue();
    private final Queue<Double> recentCalculation = Queues.createConcurrentQueue();

    private final DataProcessorType type;
    private final Deque<Double> computationalLoadHistory = new ArrayDeque<>();

    private final List<ProcessorModuleCPU> moduleCPUS = new CopyOnWriteArrayList<>();
    private final List<ProcessorModuleRAM> moduleRAMS = new CopyOnWriteArrayList<>();
    private volatile int dynamicPatternSize = 0;
    private volatile double maxGeneration = 0;
    private volatile long lastProvisionTick = Long.MIN_VALUE;
    private volatile double availableGeneratedThisTick = 0D;
    private volatile double generatedThisTick = 0D;
    private int storedHU = 0;
    private boolean overheat = false;
    private double computationalLoadHistoryCache = 0;
    private double computationalLoad = 0;

    public DataProcessor(final TileMultiblockMachineController owner) {
        super(owner);
        this.type = RegistryHyperNet.getDataProcessorType(
            Objects.requireNonNull(owner.getFoundMachine()).getRegistryName().getPath()
        );
    }

    @ZenMethod
    public void onRecipeCheck(RecipeCheckEvent event) {
        if (this.centerPos == null || this.center == null) {
            event.setFailed("novaeng.hypernet.prrocessor.link.false");
            return;
        }

        if (this.overheat) {
            event.setFailed("novaeng.hypernet.prrocessor.overheat");
            return;
        }

        if (this.moduleCPUS.isEmpty() && this.moduleRAMS.isEmpty()) {
            event.setFailed("novaeng.hypernet.prrocessor.no_module");
            return;
        }

        if (this.moduleCPUS.isEmpty()) {
            event.setFailed("novaeng.hypernet.prrocessor.no_cpu");
            return;
        }

        if (this.moduleRAMS.isEmpty()) {
            event.setFailed("novaeng.hypernet.prrocessor.no_ram");
        }
    }

    @ZenMethod
    public void heatDistributionRecipeCheck(RecipeCheckEvent event, int heatDistribution) {
        if (this.storedHU - heatDistribution < 0) {
            event.setFailed("novaeng.hypernet.craftcheck.heat_distribution.failed");
        }
    }

    @ZenMethod
    public void onWorkingTick(FactoryRecipeTickEvent event) {
        event.getActiveRecipe().setTick(0);
        if (this.workingCheck(event)) {
            return;
        }

        long baseEnergyUsage = this.type.getEnergyUsage();
        long energyUsage = 0;

        Long usage;
        while ((usage = this.recentEnergyUsage.poll()) != null) {
            energyUsage += usage;
        }

        float heatPercent = this.getOverHeatPercent();
        if (heatPercent <= 0.1F) {
            energyUsage += (baseEnergyUsage / 10) * this.dynamicPatternSize > 0 ? this.dynamicPatternSize : 1;
        } else if (heatPercent <= 0.5F) {
            energyUsage += (baseEnergyUsage / 5) * this.dynamicPatternSize > 0 ? this.dynamicPatternSize : 1;
        } else if (heatPercent <= 0.75F) {
            energyUsage += (baseEnergyUsage / 2) * this.dynamicPatternSize > 0 ? this.dynamicPatternSize : 1;
        } else {
            energyUsage += baseEnergyUsage * this.dynamicPatternSize > 0 ? this.dynamicPatternSize : 1;
        }

        float mul = (float) ((double) (energyUsage + baseEnergyUsage) / baseEnergyUsage);
        event.getRecipeThread().addModifier("energy", new RecipeModifier(
            RequirementTypesMM.REQUIREMENT_ENERGY,
            IOType.INPUT, mul, 1, false
        ));
    }

    protected boolean workingCheck(final FactoryRecipeTickEvent event) {
        if (this.centerPos == null) {
            event.setFailed(true, "novaeng.hypernet.prrocessor.link.false");
            return true;
        }
        if (this.center == null) {
            event.preventProgressing("novaeng.hypernet.prrocessor.link.false");
            return true;
        }
        if (this.overheat) {
            event.setFailed(true, "novaeng.hypernet.prrocessor.overheat");
            return true;
        }
        if (this.moduleCPUS.isEmpty() && this.moduleRAMS.isEmpty()) {
            event.setFailed(true, "novaeng.hypernet.prrocessor.no_module");
            return true;
        }
        if (this.moduleCPUS.isEmpty()) {
            event.setFailed(true, "novaeng.hypernet.prrocessor.no_cpu");
            return true;
        }
        if (this.moduleRAMS.isEmpty()) {
            event.setFailed(true, "novaeng.hypernet.prrocessor.no_ram");
            return true;
        }
        return false;
    }

    @ZenMethod
    public void onMachineTick() {
        super.onMachineTick();

        if (!this.isWorking()) {
            this.availableGeneratedThisTick = 0D;
            this.generatedThisTick = 0D;
            this.lastProvisionTick = Long.MIN_VALUE;
            this.computationalLoad = 0F;
            this.computationalLoadHistoryCache = 0F;
            this.computationalLoadHistory.clear();
        } else {
            double totalCalculation = 0F;
            Double calculation;
            while ((calculation = this.recentCalculation.poll()) != null) {
                totalCalculation += calculation;
            }

            this.computationalLoadHistory.addFirst(totalCalculation);
            this.computationalLoadHistoryCache += totalCalculation;
            if (this.computationalLoadHistory.size() > 100) {
                this.computationalLoadHistoryCache -= this.computationalLoadHistory.pollLast();
            }

            this.computationalLoad = this.computationalLoadHistoryCache / this.computationalLoadHistory.size();
        }

        if (this.owner.getTicksExisted() % 20 == 0) {
            this.maxGeneration = this.getComputationPointProvision(0xFFFFFF);
            IDynamicPatternInfo dynamicPattern = this.owner.getDynamicPattern(this.type.getDynamicPatternName());
            if (dynamicPattern != null) {
                this.dynamicPatternSize = dynamicPattern.getSize();
            } else {
                this.dynamicPatternSize = 0;
            }
            this.writeNBT();
        }

        if (this.storedHU > 0) {
            int heatDist = this.calculateHeatDist();

            this.storedHU -= Math.min(heatDist, this.storedHU);
            if (this.storedHU <= 0) {
                this.overheat = false;
            }
            this.maxGeneration = this.getComputationPointProvision(0xFFFFFF);
            this.writeNBT();
        }
    }

    @ZenMethod
    public void onStructureUpdate() {
        try {
            this.lock.lock();
            this.moduleCPUS.clear();
            this.moduleRAMS.clear();
            this.moduleCPUS.addAll(ProcessorModuleCPU.filter(this.owner.getFoundUpgrades().values()));
            this.moduleRAMS.addAll(ProcessorModuleRAM.filter(this.owner.getFoundUpgrades().values()));
        } finally {
            this.lock.unlock();
        }
    }

    private int calculateHeatDist() {
        float heatPercent = this.getOverHeatPercent();
        float heatDist = this.type.getHeatDistribution();
        if (this.dynamicPatternSize > 1) {
            heatDist *= this.dynamicPatternSize;
        }

        if (heatPercent <= 0.25F) {
            heatDist *= 0.25F;
        } else if (heatPercent <= 0.75F) {
            heatDist *= 0.25F + (heatPercent);
        } else {
            heatDist *= 1.0F;
        }

        return (int) heatDist;
    }

    @Override
    public double requireComputationPoint(final double maxGeneration, final boolean doCalculate) {
        if (!this.isConnected() || this.center == null || !this.isWorking()) {
            return 0F;
        }

        ensureProvisionForCurrentTick();
        double polledCounter = Math.min(this.availableGeneratedThisTick, maxGeneration);
        if (polledCounter <= 0D) {
            return 0F;
        }
        this.availableGeneratedThisTick -= polledCounter;

        double generated = this.calculateComputationPointProvision(polledCounter, doCalculate) * this.getEfficiency();

        if (doCalculate) {
            this.doHeatGeneration(generated);
            this.generatedThisTick += generated;
            this.availableGeneratedThisTick += (polledCounter - generated);
        }

        return generated;
    }

    @Override
    public boolean isWorking() {
        if (!(this.owner instanceof final TileFactoryController factory)) {
            return false;
        }

        FactoryRecipeThread thread = factory.getCoreRecipeThreads().get(DataProcessorType.PROCESSOR_WORKING_THREAD_NAME);

        return this.owner.isWorking() && thread != null && thread.isWorking();
    }

    @ZenGetter("maxGeneration")
    public double getMaxGeneration() {
        return this.maxGeneration;
    }

    public float getEfficiency() {
        float overHeatPercent = this.getOverHeatPercent();
        return overHeatPercent >= 0.85F ? (1.0F - overHeatPercent) / 0.15F : 1F;
    }

    @ZenGetter("overHeatPercent")
    public float getOverHeatPercent() {
        return this.overheat ? 1F : (float) this.storedHU / this.type.getOverheatThreshold();
    }

    public void doHeatGeneration(double computationPointGeneration) {
        this.storedHU += (int) (computationPointGeneration * 2);
        if (this.storedHU >= this.type.getOverheatThreshold()) {
            this.overheat = true;
        }
    }

    public double calculateComputationPointProvision(double maxGeneration, boolean doCalculate) {
        if (this.overheat || !this.isWorking()) {
            return 0;
        }

        if (this.owner.getFoundUpgrades().isEmpty()) {
            return 0;
        }

        if (this.moduleCPUS.isEmpty()) {
            return 0;
        }

        if (this.moduleRAMS.isEmpty()) {
            return 0;
        }

        long totalEnergyConsumption = 0;

        double generationLimit = 0F;
        double totalGenerated = 0F;

        for (ProcessorModuleRAM ram : this.moduleRAMS) {
            double generated = ram.calculate(doCalculate, maxGeneration - generationLimit);
            generationLimit += generated;
            if (doCalculate) {
                totalEnergyConsumption += (long) ((generated / ram.getComputationPointGenerationLimit()) * ram.getEnergyConsumption());
            }
        }
        for (final ProcessorModuleCPU cpu : this.moduleCPUS) {
            double generated = cpu.calculate(doCalculate, generationLimit - totalGenerated);
            totalGenerated += generated;
            if (doCalculate) {
                totalEnergyConsumption += (long) ((generated / cpu.getComputationPointGeneration()) * cpu.getEnergyConsumption());
            }

            if (totalGenerated >= generationLimit) {
                break;
            }
        }

        if (doCalculate) {
            this.recentCalculation.offer(totalGenerated);
            this.recentEnergyUsage.offer(totalEnergyConsumption);
        }

        return totalGenerated;
    }

    private void ensureProvisionForCurrentTick() {
        long tickId = this.owner.getWorld().getTotalWorldTime();
        if (this.lastProvisionTick == tickId) {
            return;
        }
        this.lastProvisionTick = tickId;
        this.availableGeneratedThisTick = this.maxGeneration;
        this.generatedThisTick = 0D;
    }

    @Override
    public void readNBT(final NBTTagCompound customData) {
        super.readNBT(customData);
        this.storedHU = customData.getInteger("storedHU");
        if (customData.hasKey("overheat")) {
            this.overheat = customData.getBoolean("overheat");
        }

        this.computationalLoad = customData.getFloat("computationalLoad");
        this.maxGeneration = customData.getFloat("maxGeneration");
    }

    @Override
    public void writeNBT() {
        super.writeNBT();
        NBTTagCompound tag = this.owner.getCustomDataTag();
        tag.setInteger("storedHU", this.storedHU);
        tag.setBoolean("overheat", this.overheat);
        tag.setDouble("computationalLoad", this.computationalLoad);
        tag.setDouble("maxGeneration", this.maxGeneration);
    }

    @Override
    public double getComputationPointProvision(final double maxGeneration) {
        return this.calculateComputationPointProvision(maxGeneration, false) * this.getEfficiency();
    }

    @ZenGetter("computationalLoad")
    public double getComputationalLoad() {
        return this.computationalLoad;
    }

    @ZenGetter("type")
    public DataProcessorType getType() {
        return this.type;
    }

    @ZenGetter("storedHU")
    public int getStoredHU() {
        return this.storedHU;
    }

    @ZenSetter("storedHU")
    public void setStoredHU(final int storedHU) {
        this.storedHU = storedHU;
        this.writeNBT();
    }

    @ZenGetter("overheat")
    public boolean isOverheat() {
        return this.overheat;
    }
}
