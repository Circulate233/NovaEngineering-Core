package github.kasuminova.novaeng.common.handler;

import com.google.common.collect.Multimap;
import github.kasuminova.novaeng.common.enchantment.MagicBreaking;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Collection;

public class EnchantmentHandler {

    public static final EnchantmentHandler INSTANCE = new EnchantmentHandler();

    @SubscribeEvent
    public void onMagicBreaking(net.minecraftforge.event.entity.living.LivingHurtEvent event) {
        if (event.getEntity().world.isRemote || event.getSource().getTrueSource() == null) {
            return;
        }
        if (event.getSource().getTrueSource() instanceof EntityPlayer player) {
            EntityLivingBase entity = event.getEntityLiving();
            ItemStack currentItem = player.getHeldItemMainhand();
            if (entity.getHealth() > 10) {
                if (!currentItem.isEmpty() && EnchantmentHelper.getEnchantments(currentItem).containsKey(MagicBreaking.MAGICBREAKING)) {
                    Multimap<String, AttributeModifier> mapAttackModifier = currentItem.getAttributeModifiers(EntityEquipmentSlot.MAINHAND);
                    float attackDamage = 1.0F;
                    if (!mapAttackModifier.isEmpty()) {
                        final Collection<AttributeModifier> attackModifiers = mapAttackModifier.get(SharedMonsterAttributes.ATTACK_DAMAGE.getName());
                        for (AttributeModifier modifier : attackModifiers) {
                            attackDamage += (float) modifier.getAmount();
                        }
                        float damage = Math.max(attackDamage, event.getAmount());
                        float source = Math.max(entity.getHealth() - damage, 1);

                        while (entity.getHealth() > source) {
                            entity.setHealth(source);
                        }

                        event.setAmount(0);
                    }
                }
            }
        }
    }
}
