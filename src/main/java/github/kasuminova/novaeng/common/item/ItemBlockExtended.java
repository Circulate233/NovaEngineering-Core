package github.kasuminova.novaeng.common.item;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.block.Block;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * 承载原 Additions 数据包中 `item_block.attribute_modifiers` 的方块物品。
 *
 * <p>`potato_server` 与 `server_tier_1` 在手持时会施加移速修饰符，
 * 数值与 UUID 均与原定义一致。
 */
public class ItemBlockExtended extends ItemBlock {

    private final Multimap<String, AttributeModifier> modifiers;

    public ItemBlockExtended(final Block block, final UUID uuid, final String name, final double amount) {
        super(block);
        this.modifiers = HashMultimap.create();
        this.modifiers.put(SharedMonsterAttributes.MOVEMENT_SPEED.getName(),
            new AttributeModifier(uuid, name, amount, 1));
    }

    @Override
    @NotNull
    public Multimap<String, AttributeModifier> getAttributeModifiers(@NotNull final EntityEquipmentSlot slot,
                                                                    @NotNull final ItemStack stack) {
        final Multimap<String, AttributeModifier> result =
            HashMultimap.create(super.getAttributeModifiers(slot, stack));
        if (slot == EntityEquipmentSlot.MAINHAND || slot == EntityEquipmentSlot.OFFHAND) {
            result.putAll(this.modifiers);
        }
        return result;
    }

}
