package github.kasuminova.novaeng.common.machine;

import appeng.util.Platform;
import crafttweaker.CraftTweakerAPI;
import crafttweaker.api.item.IItemStack;
import crafttweaker.api.minecraft.CraftTweakerMC;
import github.kasuminova.mmce.common.event.client.ControllerGUIRenderEvent;
import github.kasuminova.mmce.common.helper.IMachineController;
import github.kasuminova.novaeng.common.crafttweaker.expansion.RecipePrimerHyperNet;
import github.kasuminova.novaeng.common.crafttweaker.hypernet.HyperNetHelper;
import github.kasuminova.novaeng.common.util.RecipePrimerEx;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.MachineModifier;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.RecipeBuilder;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.RecipeModifierBuilder;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.machine.factory.FactoryRecipeThread;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import mustapelto.deepmoblearning.common.DMLRegistry;
import mustapelto.deepmoblearning.common.util.DataModelHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

public final class BiogenicSimulationComputer implements MachineSpecial {
    public static final BiogenicSimulationComputer INSTANCE = new BiogenicSimulationComputer();
    private static final IItemStack CLAY = CraftTweakerMC.getIItemStack(new ItemStack(DMLRegistry.ITEM_POLYMER_CLAY));
    private static final IItemStack BLANK = CraftTweakerMC.getIItemStack(new ItemStack(DMLRegistry.ITEM_DATA_MODEL_BLANK));
    private static final String MACHINEID = "biogenic_simulation_computer";
    public static final ResourceLocation REGISTRY_NAME = new ResourceLocation(ModularMachinery.MODID, MACHINEID);
    private static final String[] MODELS = {"数位演算模块-α", "数位演算模块-β", "数位演算模块-δ", "数位演算模块-Ω"};

    private BiogenicSimulationComputer() {
    }

    @Override
    public ResourceLocation getRegistryName() {
        return REGISTRY_NAME;
    }

    @Override
    public void preInit(final DynamicMachine machine) {
        MachineModifier.setMaxThreads(MACHINEID, 0);
        for (final var model : MODELS) {
            MachineModifier.addCoreThread(MACHINEID, FactoryRecipeThread.createCoreThread(model));
        }
        HyperNetHelper.proxyMachineForHyperNet(MACHINEID);
        for (int index = 0; index < MODELS.length; index++) {
            final int i = index;
            final String modelKey = "ysqname" + i, countKey = "ysqddcs" + i, prepare = "prepare" + i;
            RecipeBuilder.newBuilder("moxll" + i, MACHINEID, 1, 0).addItemInput(CraftTweakerAPI.oreDict.get("dataModel")).setTag("dataModel")
                         .setNBTChecker((controller, item) -> {
                             controller.getController().getCustomDataTag().setTag(prepare, CraftTweakerMC.getItemStack(item).writeToNBT(new NBTTagCompound()));
                             return true;
                         }).addPreCheckHandler(event -> {
                             final var data = event.getController().getCustomDataTag();
                             if (data.hasKey(modelKey)) {
                                 event.setFailed("数据模块注入完成,可以开始演算");
                                 for (int j = 0; j < MODELS.length; j++) {
                                     data.removeTag("prepare" + j);
                                 }
                             }
                         }).addFactoryStartHandler(event -> {
                             final var data = event.getController().getCustomDataTag();
                             if (!data.hasKey(modelKey)) {
                                 final var itemData = (NBTTagCompound) data.getTag(prepare);
                                 final var stack = new ItemStack(itemData);
                                 final int tier = DataModelHelper.getTier(stack), count = DataModelHelper.getCurrentTierDataCount(stack);
                                 final int total = tier <= 1 ? 32 * tier + count : count + (tier - 1) * 10000 + 32;
                                 data.setTag(modelKey, itemData);
                                 data.setLong(countKey, total);
                                 for (int j = 0; j < MODELS.length; j++) {
                                     data.removeTag("prepare" + j);
                                 }
                             }
                         }).addOutput(BLANK).setParallelized(false)
                         .addRecipeTooltip("将数据模型写入数位演算模块", "请将数据模型放入控制器正上方的微型物品输入仓中")
                         .setThreadName(MODELS[i]).setLoadJEI(i == 0).build();
            final var simulation = RecipeBuilder.newBuilder("moni" + i, MACHINEID, 60, 0).addEnergyPerTickInput(1000000).addItemInput(CLAY)
                                                .addPreCheckHandler(event -> {
                                                    final var data = event.getController().getCustomDataTag();
                                                    final int parallelism = Math.max(data.getInteger("parallelism"), 1);
                                                    if (!data.hasKey(modelKey)) {
                                                        event.setFailed("没有数据模型！");
                                                        return;
                                                    }
                                                    event.getActiveRecipe().setMaxParallelism(parallelism);
                                                }).addFactoryStartHandler(event -> {
                    if (event.getController().getCustomDataTag().getInteger(countKey) < 32) {
                        final var thread = event.getFactoryRecipeThread();
                        thread.addModifier("duration", RecipeModifierBuilder.create("modularmachinery:duration", "input", 60, 1, false).build());
                        thread.addModifier("energy", RecipeModifierBuilder.create("modularmachinery:energy", "input", 20, 1, false).build());
                    }
                }).addItemOutput(CraftTweakerAPI.oreDict.get("pristine"))
                                                .addItemModifier((controller, item) -> this.outputPristineMatter(controller, modelKey, countKey))
                                                .addItemOutput(CraftTweakerAPI.oreDict.get("livingMatter"))
                                                .addItemModifier((controller, item) -> this.outputLivingMatter(controller, modelKey))
                                                .addFactoryFinishHandler(event -> {
                                                    final var data = event.getController().getCustomDataTag();
                                                    data.setLong(countKey, data.getLong(countKey) + event.getFactoryRecipeThread().getActiveRecipe().getParallelism());
                                                }).addRecipeTooltip("使用数位演算模块进行模拟,并且输出物质", "概率继承自模拟室,并且每个等级额外提高2%", "等级为0的模型需要60倍的时间和20倍能量来进行初步推算")
                                                .setThreadName(MODELS[i]).setLoadJEI(i == 0);
            RecipePrimerHyperNet.requireComputationPoint(simulation, 100).build();
            final var export = RecipeBuilder.newBuilder("mxdc" + i, MACHINEID, 1).addItemInput(BLANK)
                                            .addPreCheckHandler(event -> {
                                                if (!event.getController().getCustomDataTag().hasKey(modelKey)) {
                                                    event.setFailed("没有可以导出的数据");
                                                }
                                            })
                                            .addOutput(CraftTweakerAPI.oreDict.get("dataModel"));
            RecipePrimerEx.setLore(export, "§6提取出写入的模型").addItemModifier((controller, item) -> this.outputData(controller, modelKey, countKey))
                          .setParallelized(false).addRecipeTooltip("将数据模型从数位演算模块导出", "会先从哪个数据里导出？谁知道呢,试试不就知道了")
                          .setThreadName(MODELS[i]).setLoadJEI(i == 0).build();
        }
        machine.addMachineEventHandler(ControllerGUIRenderEvent.class, event -> {
            final var data = event.getController().getCustomDataTag();
            final var info = new ObjectArrayList<String>();
            for (int i = 0; i < MODELS.length; i++) {
                final var itemData = data.getTag("ysqname" + i);
                final String name;
                if (itemData == null) {
                    name = "暂无";
                } else {
                    final var item = new ItemStack((NBTTagCompound) itemData);
                    name = item.getItem().getItemStackDisplayName(item).replaceAll("[(].*", "");
                }
                info.add("当前记录模型：" + name);
                info.add("当前迭代次数：" + data.getLong("ysqddcs" + i));
            }
            event.setExtraInfo(info.toArray(String[]::new));
        });
    }

    private IItemStack outputLivingMatter(final IMachineController controller, final String key) {
        final var data = controller.getController().getCustomDataTag();
        return DataModelHelper.getDataModelMetadata(new ItemStack((NBTTagCompound) data.getTag(key)))
                              .map(model -> CraftTweakerMC.getIItemStack(model.getLivingMatter())).orElse(null);
    }

    private IItemStack outputPristineMatter(final IMachineController controller, final String key, final String countKey) {
        final var data = controller.getController().getCustomDataTag();
        final var tag = data.getTag(key);
        if (tag == null) {
            return null;
        }
        final long count = data.getLong(countKey);
        final int random = controller.getController().getWorld().rand.nextInt(99) + 1;
        final var model = DataModelHelper.getDataModelMetadata(new ItemStack((NBTTagCompound) tag));
        final boolean success = count >= 32 && (count < 10032 ? random <= 6 : count < 20032 ? random <= 12 : count < 30032 ? random <= 14 : random <= 20);
        return model.isPresent() ? success ? CraftTweakerMC.getIItemStack(model.get().getPristineMatter()) : null : CLAY.amount(1);
    }

    private IItemStack outputData(final IMachineController controller, final String key, final String countKey) {
        final var data = controller.getController().getCustomDataTag();
        final var tag = data.getCompoundTag(key);
        final long count = data.getLong(countKey);
        final int tier, progress;
        if (count < 32) {
            tier = 0;
            progress = (int) count;
        } else if (count < 10032) {
            tier = 1;
            progress = (int) (count - 32);
        } else if (count < 20032) {
            tier = 2;
            progress = (int) (count - 10032);
        } else if (count < 30032) {
            tier = 3;
            progress = (int) (count - 20032);
        } else {
            tier = 4;
            progress = count > 2000000000 ? 2000000000 : (int) count;
        }
        data.removeTag(key);
        data.removeTag(countKey);
        final var stack = new ItemStack(tag);
        final var nbt = Platform.openNbtData(stack);
        nbt.setLong("totalSimulationCount", count);
        nbt.setInteger("tier", tier);
        nbt.setInteger("dataCount", progress);
        return CraftTweakerMC.getIItemStack(stack);
    }
}
