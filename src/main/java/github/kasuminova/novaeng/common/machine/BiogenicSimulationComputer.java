package github.kasuminova.novaeng.common.machine;

import crafttweaker.CraftTweakerAPI;
import crafttweaker.api.item.IItemStack;
import crafttweaker.api.minecraft.CraftTweakerMC;
import github.kasuminova.mmce.common.event.client.ControllerGUIRenderEvent;
import github.kasuminova.mmce.common.helper.IMachineController;
import github.kasuminova.novaeng.common.crafttweaker.hypernet.HyperNetHelper;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.MachineModifier;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.RecipeBuilder;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.RecipeModifierBuilder;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.machine.factory.FactoryRecipeThread;
import mustapelto.deepmoblearning.common.DMLRegistry;
import mustapelto.deepmoblearning.common.util.DataModelHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

import static github.kasuminova.novaeng.common.crafttweaker.expansion.RecipePrimerHyperNet.requireComputationPoint;
import static github.kasuminova.novaeng.common.util.RecipePrimerEx.setLore;

//TODO:处理硬编码
public class BiogenicSimulationComputer implements MachineSpecial {
    private static final String MachineID = "biogenic_simulation_computer";
    public static final ResourceLocation REGISTRY_NAME = new ResourceLocation(ModularMachinery.MODID, MachineID);
    public static final BiogenicSimulationComputer INSTANCE = new BiogenicSimulationComputer();
    final IItemStack clay = CraftTweakerMC.getIItemStack(new ItemStack(DMLRegistry.ITEM_POLYMER_CLAY));

    private static final String[] inscriberModels = {
            "数位演算模块-α",
            "数位演算模块-β",
            "数位演算模块-δ",
            "数位演算模块-Ω"
    };

    @Override
    public void preInit(final DynamicMachine machine) {
        MachineModifier.setMaxThreads(MachineID, 0);
        for (String i : inscriberModels) {
            MachineModifier.addCoreThread(MachineID, FactoryRecipeThread.createCoreThread(i));
        }
        HyperNetHelper.proxyMachineForHyperNet(MachineID);

        for (int i = 0; i < inscriberModels.length; i++) {
            final var ysqname = "ysqname" + i;
            final var ysqddcs = "ysqddcs" + i;
            final String prepare = "prepare" + i;

            var r = RecipeBuilder.newBuilder("moxll" + i, MachineID, 1, 0)
                    .addItemInput(CraftTweakerAPI.oreDict.get("dataModel")).setTag("dataModel")
                    .setNBTChecker((ctrl, iitem) -> {
                        var item = CraftTweakerMC.getItemStack(iitem);
                        var data = ctrl.getController().getCustomDataTag();

                        data.setTag(prepare, item.writeToNBT(new NBTTagCompound()));
                        return true;
                    })
                    .addPreCheckHandler(event -> {
                        var ctrl = event.getController();
                        var data = ctrl.getCustomDataTag();

                        if (data.hasKey(ysqname)) {
                            event.setFailed("数据模块注入完成,可以开始演算");

                            for (int ii = 0; ii < inscriberModels.length; ii++) {
                                data.removeTag("prepare" + ii);
                            }
                        }
                    })
                    .addFactoryStartHandler(event -> {
                        var ctrl = event.getController();
                        var data = ctrl.getCustomDataTag();

                        if (!data.hasKey(ysqname)) {
                            var itemData = (NBTTagCompound) data.getTag(prepare);
                            var preItem = new ItemStack(itemData);
                            int tier = DataModelHelper.getTier(preItem);
                            int dataCount = DataModelHelper.getCurrentTierDataCount(preItem);
                            int tierend = (tier <= 1) ? 32 * tier + dataCount : dataCount + (tier - 1) * 10000 + 32;

                            data.setTag(ysqname, itemData);
                            data.setLong(ysqddcs, tierend);

                            for (int ii = 0; ii < inscriberModels.length; ii++) {
                                data.removeTag("prepare" + ii);
                            }
                        }
                    })
                    .addOutput(CraftTweakerMC.getIItemStack(new ItemStack(DMLRegistry.ITEM_DATA_MODEL_BLANK)))
                    .setParallelized(false)
                    .addRecipeTooltip("将数据模型写入数位演算模块", "请将数据模型放入控制器正上方的微型物品输入仓中")
                    .setThreadName(inscriberModels[i]);
            if (i != 0) {
                r.setLoadJEI(false);
            }
            r.build();

            var o = RecipeBuilder.newBuilder("moni" + i, MachineID, 60, 0)
                    .addEnergyPerTickInput(1000000)
                    .addItemInput(clay)
                    .addPreCheckHandler(event -> {
                        var ctrl = event.getController();
                        var data = ctrl.getCustomDataTag();
                        var parallelism = Math.max(data.getInteger("parallelism"), 1);

                        if (!data.hasKey(ysqname)) {
                            event.setFailed("没有数据模型！");
                            return;
                        }

                        event.getActiveRecipe().setMaxParallelism(parallelism);
                    })
                    .addFactoryStartHandler(event -> {
                        var ctrl = event.getController();
                        var data = ctrl.getCustomDataTag();
                        var ysqddcss = data.getInteger(ysqddcs);
                        var bl = event.getFactoryRecipeThread();
                        if (ysqddcss < 32) {
                            bl.addModifier("duration", RecipeModifierBuilder.create("modularmachinery:duration", "input", 60, 1, false).build());
                            bl.addModifier("energy", RecipeModifierBuilder.create("modularmachinery:energy", "input", 20, 1, false).build());
                        }
                    })
                    .addItemOutput(CraftTweakerAPI.oreDict.get("pristine")).addItemModifier((ctrl, Item) -> outputPristineMatter(ctrl,ysqname,ysqddcs))
                    .addItemOutput(CraftTweakerAPI.oreDict.get("livingMatter")).addItemModifier((ctrl, Item) -> outputLivingMatter(ctrl, ysqname))
                    .addFactoryFinishHandler(event -> {
                        var ctrl = event.getController();
                        var data = ctrl.getCustomDataTag();
                        var bx = event.getFactoryRecipeThread().getActiveRecipe().getParallelism();

                        data.setLong(ysqddcs, data.getLong(ysqddcs) + bx);
                    })
                    .addRecipeTooltip(
                            "使用数位演算模块进行模拟,并且输出物质",
                            "概率继承自模拟室,并且每个等级额外提高2%",
                            "等级为0的模型需要60倍的时间和20倍能量来进行初步推算"
                    )
                    .setThreadName(inscriberModels[i]);
            if (i != 0) {
                o.setLoadJEI(false);
            }
            requireComputationPoint(o, 100.0F).build();

            var d = RecipeBuilder.newBuilder("mxdc" + i, MachineID, 1)
                    .addItemInput(CraftTweakerMC.getIItemStack(new ItemStack(DMLRegistry.ITEM_DATA_MODEL_BLANK)))
                    .addPreCheckHandler(event -> {
                        var ctrl = event.getController();
                        var data = ctrl.getCustomDataTag();

                        if (!data.hasKey(ysqname)) {
                            event.setFailed("没有可以导出的数据");
                        }
                    })
                    .addOutput(CraftTweakerAPI.oreDict.get("dataModel"));
            setLore(d, "§6提取出写入的模型")
                    .addItemModifier((ctrl, Item) -> outputdata(ctrl, ysqname,ysqddcs))
                    .setParallelized(false)
                    .addRecipeTooltip("将数据模型从数位演算模块导出", "会先从哪个数据里导出？谁知道呢,试试不就知道了")
                    .setThreadName(inscriberModels[i]);
            if (i > 0) {
                d.setLoadJEI(false);
            }
            d.build();
        }

        machine.addMachineEventHandler(ControllerGUIRenderEvent.class,event -> {
            var ctrl = event.getController();
            var data = ctrl.getCustomDataTag();
            List<String> info = new ArrayList<>();

            for (int i = 0; i < inscriberModels.length; i++) {
                var itemData = data.getTag("ysqname" + i);
                String ysqname;
                if (itemData == null){
                    ysqname = "暂无";
                } else {
                    var item = new ItemStack((NBTTagCompound) itemData);
                    ysqname = item.getItem().getItemStackDisplayName(item).replaceAll("[(].*","");
                }
                var ysqddcs = data.getLong("ysqddcs" + i);
                info.add("当前记录模型：" + ysqname);
                info.add("当前迭代次数：" + ysqddcs);
            }

            event.setExtraInfo(info.toArray(new String[0]));
        });
    }

    @Override
    public ResourceLocation getRegistryName() {
        return REGISTRY_NAME;
    }

    private IItemStack outputLivingMatter(IMachineController ctrl, String ysqnamess) {
        var data = ctrl.getController().getCustomDataTag();
        var name = data.getTag(ysqnamess);
        var item = DataModelHelper.getDataModelMetadata(new ItemStack((NBTTagCompound) name));

        return item.map(dataModel -> CraftTweakerMC.getIItemStack(dataModel.getLivingMatter())).orElse(null);
    }

    private IItemStack outputPristineMatter(IMachineController ctrl, String ysqnamess,String ysqddcss) {
        var data = ctrl.getController().getCustomDataTag();
        var name = data.getTag(ysqnamess);
        var ysqddcs = data.getLong(ysqddcss);
        var world = ctrl.getController().getWorld();
        var Random = world.rand.nextInt(99) + 1;
        var item = DataModelHelper.getDataModelMetadata(new ItemStack((NBTTagCompound) name));

        boolean itemsl;
        if (ysqddcs >= 32){
            if (ysqddcs < 10032){
                itemsl = 6 >= Random;
            } else if (ysqddcs < 20032){
                itemsl = 12 >= Random;
            } else if (ysqddcs < 30032){
                itemsl = 14 >= Random;
            } else {
                itemsl = 20 >= Random;
            }
        } else {
            itemsl = false;
        }

        if (item.isPresent()){
            if (itemsl) {
                return CraftTweakerMC.getIItemStack(item.get().getPristineMatter());
            } else {
                return null;
            }
        } else {
            return clay.amount(1);
        }
    }

    private IItemStack outputdata(IMachineController ctrl, String ysqnamess, String ysqddcss) {
        var data = ctrl.getController().getCustomDataTag();
        var name = data.getTag(ysqnamess);
        var ysqddcs = data.getLong(ysqddcss);

        var tiers = 0;
        var dataCounts = 0;

        if (ysqddcs < 32) {
            tiers = 0;
            dataCounts = (int) ysqddcs;
        } else if (ysqddcs < 10032){
            tiers = 1;
            dataCounts = (int) (ysqddcs - 32);
        } else if (ysqddcs < 20032){
            tiers = 2;
            dataCounts = (int) (ysqddcs - 10032);
        } else if (ysqddcs < 30032){
            tiers = 3;
            dataCounts = (int) (ysqddcs - 20032);
        } else {
            tiers = 4;
            if (ysqddcs > 2000000000){
                dataCounts = 2000000000;
            } else {
                dataCounts = (int)ysqddcs;
            }
        }

        data.removeTag(ysqnamess);
        data.removeTag(ysqddcss);

        var item = new ItemStack((NBTTagCompound) name);
        if (!item.hasTagCompound()){
            item.setTagCompound(new NBTTagCompound());
        }
        NBTTagCompound nbt = item.getTagCompound();
        nbt.setLong("totalSimulationCount",ysqddcs);
        nbt.setInteger("tier",tiers);
        nbt.setInteger("dataCount",dataCounts);

        return CraftTweakerMC.getIItemStack(item);
    }
}
