package github.kasuminova.novaeng.common.item;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import github.kasuminova.novaeng.common.core.CreativeTabNovaEng;
import github.kasuminova.novaeng.novaeng_core.Tags;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemTool;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.EnumHelper;
import net.minecraftforge.oredict.OreDictionary;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 原 Additions 模组 `novaextended-expert_tools` 的重新实现。
 *
 * <p>行为与原数据包定义一致：多工具类别（含 `wrench`）、超高耐久与挖掘等级、
 * 以 `ingotPsiAlloy` 修复，并附带三条主手属性修饰符。
 */
public class ItemExtendedMultiTool extends ItemTool {

    private static final Set<String> TOOL_CLASSES;

    private static final Item.ToolMaterial MATERIAL = EnumHelper.addToolMaterial(
        "NOVAENG_EXTENDED_MULTITOOL", 7, 114514, 28.0F, 1.0F, 90);

    private static final UUID ATTACK_SPEED_UUID = UUID.fromString("fa233e1c-4180-4865-b01b-bcce9785aca3");
    private static final UUID ATTACK_DAMAGE_UUID = UUID.fromString("cb3f55d3-645c-4f38-a497-9c13a33db5cf");
    private static final UUID LUCK_UUID = UUID.fromString("5743bf7d-e4a2-4b53-a302-f5a469ac6b96");

    static {
        final Set<String> classes = new HashSet<>();
        Collections.addAll(classes, "sword", "pickaxe", "shovel", "axe", "wrench");
        TOOL_CLASSES = Collections.unmodifiableSet(classes);
    }

    public ItemExtendedMultiTool() {
        super(MATERIAL, Collections.emptySet());
        this.setMaxStackSize(1);
        this.setCreativeTab(CreativeTabNovaEng.INSTANCE);
        this.setRegistryName(new ResourceLocation(Tags.MOD_ID, "expert_tools"));
        this.setTranslationKey(Tags.MOD_ID + ".expert_tools");

        for (final String toolClass : TOOL_CLASSES) {
            this.setHarvestLevel(toolClass, 7);
        }
    }

    @Override
    @NotNull
    public Set<String> getToolClasses(@NotNull final ItemStack stack) {
        return TOOL_CLASSES;
    }

    @Override
    public boolean canHarvestBlock(@NotNull final IBlockState state,
                                   @NotNull final ItemStack stack) {
        return true;
    }

    @Override
    public float getDestroySpeed(@NotNull final ItemStack stack, @NotNull final IBlockState state) {
        return 28.0F;
    }

    @Override
    public boolean getIsRepairable(@NotNull final ItemStack toRepair, @NotNull final ItemStack repair) {
        for (final ItemStack candidate : OreDictionary.getOres("ingotPsiAlloy", false)) {
            if (OreDictionary.itemMatches(candidate, repair, false)) {
                return true;
            }
        }
        return false;
    }

    @Override
    @NotNull
    public Multimap<String, AttributeModifier> getItemAttributeModifiers(@NotNull final EntityEquipmentSlot slot) {
        final Multimap<String, AttributeModifier> modifiers = HashMultimap.create();
        if (slot != EntityEquipmentSlot.MAINHAND) {
            return modifiers;
        }

        modifiers.put(SharedMonsterAttributes.ATTACK_SPEED.getName(),
            new AttributeModifier(ATTACK_SPEED_UUID, "Weapon modifier", 1.6D, 0));
        modifiers.put(SharedMonsterAttributes.ATTACK_DAMAGE.getName(),
            new AttributeModifier(ATTACK_DAMAGE_UUID, "Weapon modifier", 20.0D, 0));
        modifiers.put(SharedMonsterAttributes.LUCK.getName(),
            new AttributeModifier(LUCK_UUID, "Weapon modifier", 1.0D, 0));
        return modifiers;
    }

}
