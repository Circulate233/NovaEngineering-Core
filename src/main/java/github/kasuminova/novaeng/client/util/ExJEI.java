package github.kasuminova.novaeng.client.util;

import crafttweaker.api.item.IItemStack;
import crafttweaker.api.minecraft.CraftTweakerMC;
import github.kasuminova.novaeng.NovaEngineeringCore;
import ic2.core.ref.BlockName;
import ic2.core.ref.ItemName;
import ic2.core.ref.TeBlock;
import ic2.core.util.Util;
import ic2.core.uu.UuGraph;
import ink.ikx.rt.api.mods.jei.IJeiUtils;
import ink.ikx.rt.impl.mods.jei.impl.core.MCJeiPanel;
import ink.ikx.rt.impl.mods.jei.impl.core.MCJeiRecipe;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@SideOnly(Side.CLIENT)
public class ExJEI{

    private static final List<String> blockList = Arrays.asList(
        "mekanismgenerators","artisanworktables"
    );

    public static void jeiCreate() {
        IItemStack pattern_storage = CraftTweakerMC.getIItemStack(BlockName.te.getItemStack(TeBlock.pattern_storage));
        IItemStack replicator = CraftTweakerMC.getIItemStack(BlockName.te.getItemStack(TeBlock.replicator));
        MCJeiPanel JeiP = new MCJeiPanel("replicator_jei", I18n.format("gui." + NovaEngineeringCore.MOD_ID + ".replicator"));
        JeiP.setModid("ic2");
        JeiP.recipeCatalysts.addAll(
            Arrays.asList(
                pattern_storage,
                replicator,
                CraftTweakerMC.getIItemStack(ItemName.crystal_memory.getItemStack())
            )
        );
        JeiP.background = IJeiUtils.createBackground(80, 32);
        JeiP.slots.addAll(
            Arrays.asList(
                IJeiUtils.createItemSlot(30,0,true,false),
                IJeiUtils.createItemSlot(30,0,false,false)
            )
        );
        JeiP.icon = replicator;
        JeiP.register();
    }

    public static void jeiRecipeRegister() {
        UuGraph.iterator().forEachRemaining(item -> {
            ItemStack stack = item.getKey().copy();
            if (item.getValue() != Double.POSITIVE_INFINITY && !blockList.contains(Objects.requireNonNull(stack.getItem().getRegistryName()).getNamespace())) {
                double bValue = item.getValue() / 100000;
                new MCJeiRecipe("replicator_jei").addInput(CraftTweakerMC.getIItemStack(stack)).addOutput(CraftTweakerMC.getIItemStack(stack)).addElement(IJeiUtils.createFontInfoElement(I18n.format("gui." + NovaEngineeringCore.MOD_ID + ".replicator.tooltips1",Util.toSiString(bValue, 2)), 0, 20, 0x000000, 0, 0)).build();
            }
        });
    }
}
