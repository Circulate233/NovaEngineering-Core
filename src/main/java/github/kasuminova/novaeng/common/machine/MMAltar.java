package github.kasuminova.novaeng.common.machine;

import WayofTime.bloodmagic.api.impl.BloodMagicAPI;
import WayofTime.bloodmagic.api.impl.recipe.RecipeBloodAltar;
import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.item.IIngredient;
import crafttweaker.api.item.IItemStack;
import crafttweaker.api.minecraft.CraftTweakerMC;
import crafttweaker.api.oredict.IOreDictEntry;
import github.kasuminova.mmce.common.event.machine.MachineStructureUpdateEvent;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.RecipeBuilder;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.modifier.MultiBlockModifierReplacement;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import hellfirepvp.modularmachinery.common.util.BlockArray;
import hellfirepvp.modularmachinery.common.util.IBlockStateDescriptor;
import ink.ikx.mmce.common.utils.StackUtils;
import net.minecraft.block.Block;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.registry.GameRegistry;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

//TODO:处理硬编码
@ZenRegister
@ZenClass("novaeng.MMAltar")
public class MMAltar implements MachineSpecial {
    public static final String MachineID = "mm_altar";
    public static final ResourceLocation REGISTRY_NAME = new ResourceLocation(ModularMachinery.MODID, MachineID);
    public static final MMAltar INSTANCE = new MMAltar();

    public static final List<BlockPos> posSet1 = Arrays.asList(
            new BlockPos(-3, -5, -3), new BlockPos(-3, -5, 3),
            new BlockPos(3, -5, -3), new BlockPos(3, -5, 3),
            new BlockPos(-3, -4, -3), new BlockPos(-3, -4, 3),
            new BlockPos(3, -4, -3), new BlockPos(3, -4, 3)
    );

    public static final List<BlockPos> posSet2 = Arrays.asList(
            new BlockPos(-5, -3, -5), new BlockPos(-5, -3, 5),
            new BlockPos(5, -3, -5), new BlockPos(5, -3, 5)
    );

    public static final List<BlockPos> posSet3 = Arrays.asList(
            new BlockPos(-11, -2, -11), new BlockPos(-11, -2, 11),
            new BlockPos(11, -2, -11), new BlockPos(11, -2, 11)
    );

    public static Block BLOCKSJ1 = getOtherModsBlock("contenttweaker", "crystalmatrixforcefieldcontrolblock");
    public static Block BLOCKSJ2 = getOtherModsBlock("contenttweaker", "fallenstarforcefieldcontrolblock");
    public static Block BLOCKSJ3 = getOtherModsBlock("contenttweaker", "universalforcefieldcontrolblock");

    protected MMAltar() {
    }

    protected static Block getOtherModsBlock(String modId, String blockName) {
        return GameRegistry.findRegistry(Block.class).getValue(new ResourceLocation(modId, blockName));
    }

    protected static BlockArray buildModifierReplacementBlockArray(final Block block, final List<BlockPos> posSet) {
        BlockArray blockArray = new BlockArray();
        IBlockStateDescriptor descriptor = new IBlockStateDescriptor(block);
        posSet.forEach(pos -> blockArray.addBlock(pos, new BlockArray.BlockInformation(Collections.singletonList(descriptor))));
        return blockArray;
    }

    /**
     * 合成配方
     *
     * @param need      最低血液需求
     * @param Maxneed   总血液需求
     * @param AltarTier 祭坛等级需求
     * @param input     输入物品
     * @param output    输出物品
     */
    @ZenMethod
    public static void registerRecipe(int need, int Maxneed, int AltarTier, IIngredient input, IItemStack output) {
        var time = Maxneed / need;
        String name;
        if (input instanceof IItemStack item) {
            name = CraftTweakerMC.getItem(item.getDefinition()).getRegistryName().toString() + item.getMetadata();
        } else if (input instanceof IOreDictEntry od) {
            name = od.getName();
        } else {
            name = CraftTweakerMC.getItem(output.getDefinition()).getRegistryName().toString() + output.getMetadata();
        }
        RecipeBuilder.newBuilder(name, MachineID, time, 1000)
                .addItemInputs(input)
                .addPreCheckHandler(event -> {
                    var ctrl = event.getController();
                    var data = ctrl.getCustomDataTag();
                    var xycc = data.getLong("xycc");
                    var jtdj = data.getInteger("jtdj");
                    var cpdj = data.getInteger("cpdj");
                    var ccjx = data.getInteger("ccjx");
                    var bx = Math.min(Math.pow(4, cpdj), ((double) ccjx / need));

                    if (xycc < need) {
                        event.setFailed("§4缓存的生命源质无法启动配方！");
                        return;
                    }
                    if (jtdj < AltarTier) {
                        event.setFailed("§4祭坛等级不足以运行配方！");
                        return;
                    }
                    event.getActiveRecipe().setMaxParallelism((int) bx);
                })
                .addFactoryStartHandler(event -> {
                    var ctrl = event.getController();
                    var data = ctrl.getCustomDataTag();
                    var bx = event.getFactoryRecipeThread().getActiveRecipe().getParallelism();

                    data.setLong("hcjd", 0);
                    data.setLong("hcjdmax", (long) bx * Maxneed);
                })
                .addFactoryPreTickHandler(event -> {
                    var ctrl = event.getController();
                    var data = ctrl.getCustomDataTag();
                    var xycc = data.getLong("xycc");
                    data.getInteger("jtdj");
                    var hcjd = data.getInteger("hcjd");
                    var hcjdmax = data.getInteger("hcjdmax");
                    var sdfwxg = 0.2 * data.getInteger("sdfw");
                    var thread = event.getFactoryRecipeThread();
                    var bx = thread.getActiveRecipe().getParallelism();
                    var totalTick = thread.getActiveRecipe().getTotalTick();
                    var sjneed = need * bx;

                    if (xycc > sjneed) {
                        if (hcjd < hcjdmax) {
                            thread.getActiveRecipe().setTick((totalTick / 2));
                            event.preventProgressing("§6合成中,还差§a" + (hcjdmax - hcjd) + "生命源质§6完成合成");
                        }
                        if (xycc <= (1 + sdfwxg) * sjneed) {
                            if (hcjd + xycc <= hcjdmax) {
                                data.setLong("hcjd", hcjd + xycc);
                                data.setLong("xycc", 0);
                            } else {
                                data.setLong("hcjd", hcjdmax);
                                data.setLong("xycc", xycc - (hcjdmax - hcjd));
                                thread.getActiveRecipe().setTick(totalTick);
                            }
                        } else {
                            if (hcjd + ((1 + sdfwxg) * sjneed) <= hcjdmax) {
                                data.setLong("hcjd", (long) (hcjd + ((1 + sdfwxg) * sjneed)));
                                data.setLong("xycc", (long) (xycc - ((1 + sdfwxg) * sjneed)));
                            } else {
                                data.setLong("hcjd", hcjdmax);
                                data.setLong("xycc", xycc - (hcjdmax - hcjd));
                                thread.getActiveRecipe().setTick(totalTick);
                            }
                        }
                    } else {
                        event.preventProgressing("§6剩余的生命源质不足最低值§a" + (sjneed));
                    }
                })
                .addOutput(output)
                .setThreadName("血之合成")
                .addRecipeTooltip(
                        "§4所需基础生命源质" + need,
                        "§4所需生命源质总量" + Maxneed,
                        "§4配方所要求最低层级：" + AltarTier,
                        "§6实际速度与消耗将取决于速度符文",
                        "§6并行状态每次需要消耗的血量会乘并行数"
                )
                .build();
    }

    @Override
    public void preInit(final DynamicMachine machine) {
        machine.addMachineEventHandler(MachineStructureUpdateEvent.class, event -> {
            TileMultiblockMachineController controller = event.getController();
            controller.setWorkMode(TileMultiblockMachineController.WorkMode.SEMI_SYNC);
        });
        machine.getMultiBlockModifiers().add(new MultiBlockModifierReplacement("xzjtsj1",
                buildModifierReplacementBlockArray(BLOCKSJ1, posSet1),
                Collections.emptyList(),
                Arrays.asList(
                        "§7柱子可以是任意完整方块",
                        "§6将3级祭坛的柱子方块全部替换为" + BLOCKSJ1.getLocalizedName(),
                        "§6即可激活升级数1"
                ),
                StackUtils.getStackFromBlockState(BLOCKSJ1.getDefaultState()))
        );
        machine.getMultiBlockModifiers().add(new MultiBlockModifierReplacement("xzjtsj2",
                buildModifierReplacementBlockArray(BLOCKSJ2, posSet2),
                Collections.emptyList(),
                Arrays.asList(
                        "§7柱子可以是任意完整方块",
                        "§6将4级祭坛的所有的大血石下方的1个柱子方块全部替换为" + BLOCKSJ2.getLocalizedName(),
                        "§6即可激活升级数1"
                ),
                StackUtils.getStackFromBlockState(BLOCKSJ2.getDefaultState()))
        );
        machine.getMultiBlockModifiers().add(new MultiBlockModifierReplacement("xzjtsj3",
                buildModifierReplacementBlockArray(BLOCKSJ3, posSet3),
                Collections.emptyList(),
                Arrays.asList(
                        "§7柱子可以是任意完整方块",
                        "§6将6级祭坛的所有的晶簇下方的1个柱子方块全部替换为" + BLOCKSJ3.getLocalizedName(),
                        "§6即可激活升级数2",
                        "§6并且额外提升1级祭坛位阶"
                ),
                StackUtils.getStackFromBlockState(BLOCKSJ3.getDefaultState()))
        );
        var altarRecipe = BloodMagicAPI.INSTANCE.getRecipeRegistrar();
        altarRecipe.removeBloodAltar(new ItemStack(Items.BUCKET));
        for (RecipeBloodAltar recipe : altarRecipe.getAltarRecipes()) {
            registerRecipe(
                    recipe.getConsumeRate(),
                    recipe.getSyphon(),
                    recipe.getMinimumTier().toInt(),
                    CraftTweakerMC.getIIngredient(recipe.getInput()),
                    CraftTweakerMC.getIItemStack(recipe.getOutput())
            );
        }
    }

    @Override
    public ResourceLocation getRegistryName() {
        return REGISTRY_NAME;
    }
}
