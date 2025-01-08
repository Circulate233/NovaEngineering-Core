package github.kasuminova.novaeng.common.util;

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
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.JEIPlugin;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@JEIPlugin
@SideOnly(Side.CLIENT)
public class ExJEI implements IModPlugin {

    public static IModRegistry registration;
    private static final List<String> blockList = Arrays.asList(
        "mekanismgenerators","artisanworktables"
    );
    
    public static ItemStack getOtherModsItemStack(String modId, String itemName) {
        Item item = GameRegistry.findRegistry(Item.class).getValue(new ResourceLocation(modId, itemName));
        if (item != null) {
            return new ItemStack(item,1);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void register(@Nonnull IModRegistry registration){
        ExJEI.registration = registration;
        registration.addRecipeCatalyst(getOtherModsItemStack("packagedexcrafting","combination_crafter"),
            "extendedcrafting:combination_crafting");
        registration.addRecipeCatalyst(getOtherModsItemStack("packagedexcrafting","marked_pedestal"),
            "extendedcrafting:combination_crafting");
        registration.addRecipeCatalyst(getOtherModsItemStack("packagedexcrafting","advanced_crafter"),
            "extendedcrafting:table_crafting_5x5");
        registration.addRecipeCatalyst(getOtherModsItemStack("packagedexcrafting","elite_crafter"),
            "extendedcrafting:table_crafting_7x7");
        registration.addRecipeCatalyst(getOtherModsItemStack("packagedexcrafting","ultimate_crafter"),
            "extendedcrafting:table_crafting_9x9");
        registration.addRecipeCatalyst(getOtherModsItemStack("packagedavaritia","extreme_crafter"),
            "Avatitia.Extreme");
        registration.addRecipeCatalyst(getOtherModsItemStack("packagedastral","trait_crafter"),
            "astralsorcery.altar.trait");
        registration.addRecipeCatalyst(getOtherModsItemStack("packagedastral","constellation_crafter"),
            "astralsorcery.altar.constellation");
        registration.addRecipeCatalyst(getOtherModsItemStack("packagedastral","attunement_crafter"),
            "astralsorcery.altar.attunement");
        registration.addRecipeCatalyst(getOtherModsItemStack("packagedastral","discovery_crafter"),
            "astralsorcery.altar.discovery");
        registration.addRecipeCatalyst(getOtherModsItemStack("packageddraconic","fusion_crafter"),
            "DraconicEvolution.Fusion");
    }

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