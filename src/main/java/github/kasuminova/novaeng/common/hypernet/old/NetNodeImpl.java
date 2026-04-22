package github.kasuminova.novaeng.common.hypernet.old;

import crafttweaker.annotations.ZenRegister;
import github.kasuminova.mmce.common.event.recipe.FactoryRecipeStartEvent;
import github.kasuminova.mmce.common.event.recipe.FactoryRecipeTickEvent;
import github.kasuminova.mmce.common.event.recipe.RecipeCheckEvent;
import github.kasuminova.mmce.common.event.recipe.RecipeStartEvent;
import github.kasuminova.mmce.common.event.recipe.RecipeTickEvent;
import github.kasuminova.novaeng.common.crafttweaker.util.NovaEngUtils;
import github.kasuminova.novaeng.common.hypernet.old.research.ResearchCognitionData;
import hellfirepvp.modularmachinery.common.machine.RecipeThread;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import net.minecraft.nbt.NBTTagCompound;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ZenRegister
@ZenClass("novaeng.hypernet.NetNodeImpl")
public class NetNodeImpl extends NetNode {
    protected final Map<RecipeThread, Double> recipeConsumers = new ConcurrentHashMap<>();
    protected volatile double computationPointConsumption = 0;

    public NetNodeImpl(final TileMultiblockMachineController owner) {
        super(owner);
    }

    @Override
    public void onMachineTick() {
        super.onMachineTick();
        if (!this.isWorking()) {
            clearRecipeConsumers();
        }
    }

    @ZenMethod
    public void checkComputationPoint(final RecipeCheckEvent event,
                                      final double pointRequired,
                                      final ResearchCognitionData... researchRequired) {
        if (this.centerPos == null || this.center == null) {
            event.setFailed("novaeng.hypernet.prrocessor.link.false");
            return;
        }

        double available = this.center.getAvailableComputationPoint();
        if (available < pointRequired) {
            event.setFailed("算力不足！预期："
                + NovaEngUtils.formatFLOPS(pointRequired) + "，当前："
                + NovaEngUtils.formatFLOPS(available));
            return;
        }

        int currentParallelism = event.getActiveRecipe().getMaxParallelism();
        if (currentParallelism > 1) {
            int max = (int) Math.min(currentParallelism, available / pointRequired);
            event.getActiveRecipe().setMaxParallelism(max);
        }
    }

    @ZenMethod
    public void checkResearch(final RecipeCheckEvent event,
                              final ResearchCognitionData... researchRequired) {
        if (this.centerPos == null || this.center == null) {
            event.setFailed("novaeng.hypernet.prrocessor.link.false");
            return;
        }

        Collection<Database> nodes = this.center.getNode(Database.class);
        if (nodes.isEmpty()) {
            event.setFailed("计算网络中未找到数据库！");
            return;
        }

        for (ResearchCognitionData researchCognitionData : researchRequired) {
            if (nodes.stream().noneMatch(database -> database.hasResearchCognition(researchCognitionData))) {
                event.setFailed("缺失研究：" + researchCognitionData.getTranslatedName() + "！");
                break;
            }
        }
    }

    public void onRecipeStart(final RecipeStartEvent event, final double computation) {
        updateRecipeConsumer(event.getRecipeThread(), computation * event.getActiveRecipe().getParallelism());
    }

    public void onRecipeStart(final FactoryRecipeStartEvent event, final double computation) {
        updateRecipeConsumer(event.getRecipeThread(), computation * event.getActiveRecipe().getParallelism());
    }

    public void onRecipePreTick(final RecipeTickEvent event, final double computation, final boolean triggerFailure) {
        if (this.centerPos == null) {
            event.setFailed(true, "novaeng.hypernet.prrocessor.link.false");
            return;
        }
        if (this.center == null) {
            event.preventProgressing("novaeng.hypernet.prrocessor.link.false");
            return;
        }
        double required = computation * event.getActiveRecipe().getParallelism();
        updateRecipeConsumer(event.getRecipeThread(), required);

        if (!this.center.consumeComputationPoint(required)) {
            String failureMessage = "算力不足！预期需求：" +
                NovaEngUtils.formatFLOPS(required);

            if (triggerFailure) {
                event.setFailed(event.getActiveRecipe().getRecipe().doesCancelRecipeOnPerTickFailure(), failureMessage);
            } else {
                event.preventProgressing(failureMessage);
            }
        }
    }

    public void onRecipePreTick(final FactoryRecipeTickEvent event, final double computation, final boolean triggerFailure) {
        if (this.centerPos == null) {
            event.setFailed(true, "novaeng.hypernet.prrocessor.link.false");
            return;
        }
        if (this.center == null) {
            event.preventProgressing("novaeng.hypernet.prrocessor.link.false");
            return;
        }
        double required = computation * event.getActiveRecipe().getParallelism();
        updateRecipeConsumer(event.getRecipeThread(), required);

        if (!this.center.consumeComputationPoint(required)) {
            String failureMessage = "算力不足！预期需求：" +
                NovaEngUtils.formatFLOPS(required);

            if (triggerFailure) {
                event.setFailed(event.getActiveRecipe().getRecipe().doesCancelRecipeOnPerTickFailure(), failureMessage);
            } else {
                event.preventProgressing(failureMessage);
            }
        }
    }

    public void onRecipeFinished(final RecipeThread thread) {
        removeRecipeConsumer(thread);
    }

    @Override
    public void readNBT(final NBTTagCompound customData) {
        super.readNBT(customData);
        this.computationPointConsumption = customData.getDouble("c");
    }

    @Override
    public void writeNBT() {
        super.writeNBT();
        NBTTagCompound tag = this.owner.getCustomDataTag();
        tag.setDouble("c", this.computationPointConsumption);
    }

    @Override
    public double getComputationPointConsumption() {
        return this.computationPointConsumption;
    }

    protected synchronized void updateRecipeConsumer(final RecipeThread thread, final double required) {
        Double previous = this.recipeConsumers.put(thread, required);
        if (previous != null) {
            this.computationPointConsumption -= previous;
        }
        this.computationPointConsumption += required;
    }

    protected synchronized void removeRecipeConsumer(final RecipeThread thread) {
        Double removed = this.recipeConsumers.remove(thread);
        if (removed != null) {
            this.computationPointConsumption -= removed;
        }
    }

    protected synchronized void clearRecipeConsumers() {
        this.recipeConsumers.clear();
        this.computationPointConsumption = 0D;
    }
}
