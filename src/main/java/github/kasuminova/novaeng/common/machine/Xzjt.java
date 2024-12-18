package github.kasuminova.novaeng.common.machine;

import github.kasuminova.mmce.common.event.machine.MachineStructureUpdateEvent;
import github.kasuminova.mmce.common.event.recipe.FactoryRecipeFinishEvent;
import github.kasuminova.mmce.common.event.recipe.FactoryRecipeTickEvent;
import github.kasuminova.mmce.common.event.recipe.RecipeCheckEvent;
import github.kasuminova.novaeng.common.crafttweaker.util.NovaEngUtils;
import github.kasuminova.novaeng.common.util.RandomUtils;
import hellfirepvp.astralsorcery.AstralSorcery;
import hellfirepvp.astralsorcery.common.item.crystal.CrystalProperties;
import hellfirepvp.astralsorcery.common.lib.BlocksAS;
import hellfirepvp.astralsorcery.common.tile.network.TileCollectorCrystal;
import hellfirepvp.astralsorcery.common.util.data.Vector3;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.crafting.ActiveMachineRecipe;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.machine.factory.FactoryRecipeThread;
import hellfirepvp.modularmachinery.common.modifier.MultiBlockModifierReplacement;
import hellfirepvp.modularmachinery.common.modifier.RecipeModifier;
import hellfirepvp.modularmachinery.common.tiles.TileFactoryController;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import hellfirepvp.modularmachinery.common.util.BlockArray;
import hellfirepvp.modularmachinery.common.util.IBlockStateDescriptor;
import hellfirepvp.modularmachinery.common.util.MiscUtils;
import ink.ikx.mmce.common.utils.StackUtils;
import mcjty.theoneprobe.api.*;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.common.registry.GameRegistry;
import vazkii.botania.common.Botania;
import vazkii.botania.common.block.ModBlocks;

import java.awt.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Xzjt implements MachineSpecial {
    public static final ResourceLocation REGISTRY_NAME = new ResourceLocation(ModularMachinery.MODID, "xzjt");
    public static final Xzjt XZJT = new Xzjt();

    public static final List<BlockPos> posSet1 = Arrays.asList(
        new BlockPos(-3,-5,-3), new BlockPos(-3,-5,3),
        new BlockPos(3,-5,-3), new BlockPos(3,-5,3),
        new BlockPos(-3,-4,-3), new BlockPos(-3,-4,3),
        new BlockPos(3,-4,-3), new BlockPos(3,-4,3)
    );

    public static final List<BlockPos> posSet2 = Arrays.asList(
        new BlockPos(-5,-3,-5), new BlockPos(-5,-3,5),
        new BlockPos(5,-3,-5), new BlockPos(5,-3,5)
    );

    public static final List<BlockPos> posSet3 = Arrays.asList(
        new BlockPos(-11,-2,-11), new BlockPos(-11,-2,11),
        new BlockPos(11,-2,-11), new BlockPos(11,-2,11)
    );
    
    public static Block BLOCKSJ1 = getOtherModsBlock("contenttweaker","crystalmatrixforcefieldcontrolblock");
    public static Block BLOCKSJ2 = getOtherModsBlock("contenttweaker","fallenstarforcefieldcontrolblock");
    public static Block BLOCKSJ3 = getOtherModsBlock("contenttweaker","universalforcefieldcontrolblock");

    protected Xzjt() {
    }

    public static Block getOtherModsBlock(String modId, String blockName) {
        return GameRegistry.findRegistry(Block.class).getValue(new ResourceLocation(modId, blockName));
    }

    @Override
    public void init(final DynamicMachine machine) {
        machine.getMultiBlockModifiers().add(new MultiBlockModifierReplacement("xzjtsj1",
                buildModifierReplacementBlockArray(BLOCKSJ1, posSet1),
                Collections.emptyList(),
                Arrays.asList(
                    "§7柱子可以是任意完整方块",
                    "§6将3级祭坛的柱子方块全部替换为" + BLOCKSJ1.getLocalizedName(), 
                    "§6即可激活升级数1"
                ),
                StackUtils.getStackFromBlockState(BLOCKSJ1.getDefaultState())));
        machine.getMultiBlockModifiers().add(new MultiBlockModifierReplacement("xzjtsj2",
                buildModifierReplacementBlockArray(BLOCKSJ2, posSet2),
                Collections.emptyList(),
                Arrays.asList(
                    "§7柱子可以是任意完整方块",
                    "§6将4级祭坛的所有的大血石下方的1个柱子方块全部替换为" + BLOCKSJ2.getLocalizedName(), 
                    "§6即可激活升级数1"
                ),
                StackUtils.getStackFromBlockState(BLOCKSJ2.getDefaultState())));
        machine.getMultiBlockModifiers().add(new MultiBlockModifierReplacement("xzjtsj3",
                buildModifierReplacementBlockArray(BLOCKSJ3, posSet3),
                Collections.emptyList(),
                Arrays.asList(
                    "§7柱子可以是任意完整方块",
                    "§6将6级祭坛的所有的晶簇下方的1个柱子方块全部替换为" + BLOCKSJ3.getLocalizedName(), 
                    "§6即可激活升级数2", 
                    "§6并且额外提升1级祭坛位阶"
                ),
                StackUtils.getStackFromBlockState(BLOCKSJ3.getDefaultState())));
    }


    @Override
    public ResourceLocation getRegistryName() {
        return REGISTRY_NAME;
    }
    
    protected static BlockArray buildModifierReplacementBlockArray(final Block block, final List<BlockPos> posSet) {
        BlockArray blockArray = new BlockArray();
        IBlockStateDescriptor descriptor = new IBlockStateDescriptor(block);
        posSet.forEach(pos -> blockArray.addBlock(pos, new BlockArray.BlockInformation(Collections.singletonList(descriptor))));
        return blockArray;
    }
}
