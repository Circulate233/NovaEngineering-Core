package github.kasuminova.novaeng.client.util;

import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.item.IItemStack;
import crafttweaker.api.minecraft.CraftTweakerMC;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import ic2.core.util.Util;
import ic2.core.util.StackUtil;
import ic2.core.ref.TeBlock;
import ic2.core.uu.UuGraph;
import ic2.core.ref.BlockName;
import java.util.Map;
import javax.annotation.Nonnull;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import ink.ikx.rt.api.mods.jei.core.IJeiPanel;
import ink.ikx.rt.api.mods.jei.core.IJeiRecipe;
import ink.ikx.rt.impl.mods.jei.impl.core.MCJeiPanel;
import ink.ikx.rt.impl.mods.jei.impl.core.MCJeiRecipe;
import ink.ikx.rt.api.mods.jei.IJeiUtils;

@JEIPlugin
@SideOnly(Side.CLIENT)
public class ExJEI implements IModPlugin {

    public static IModRegistry registration;
    
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
        ItemStack pattern_storage = BlockName.te.getItemStack(TeBlock.pattern_storage);
        ItemStack replicator = BlockName.te.getItemStack(TeBlock.replicator);

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

        new MCJeiRecipe("replicator_jei").addInput(CraftTweakerMC.getIItemStack(pattern_storage)).addOutput(CraftTweakerMC.getIItemStack(replicator)).addElement(IJeiUtils.createFontInfoElement("需要紧贴" + replicator.getDisplayName() + "才可用",0,20,0x000000,0,0)).build();
        
        UuGraph.iterator().forEachRemaining(item -> {
            ItemStack stack = item.getKey().copy();
            if (stack != null && stack.getItem() != null) {
                if (item.getValue() != Double.POSITIVE_INFINITY) {
                    Double bValue = item.getValue() / 100000;
                    String UUM = "需要" + Util.toSiString(bValue, 2) + "B UU物质复制";
                    new MCJeiRecipe("replicator_jei").addInput(CraftTweakerMC.getIItemStack(stack)).addOutput(CraftTweakerMC.getIItemStack(stack)).addElement(IJeiUtils.createFontInfoElement(UUM,0,20,0x000000,0,0)).build();
                }
            }
        });

    }
}
