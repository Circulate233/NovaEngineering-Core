package github.kasuminova.novaeng.mixin.techguns;

import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHandSide;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;
import techguns.api.guns.GunManager;
import techguns.client.render.ItemRenderHack;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Mixin(value = ItemRenderHack.class,remap = false)
public class MixinItemRenderHack {

    @Unique
    private static final Set<ItemCameraTransforms.TransformType> novaEngineering_Core$set = new HashSet<>(
            Arrays.asList(
                    ItemCameraTransforms.TransformType.FIRST_PERSON_LEFT_HAND,
                    ItemCameraTransforms.TransformType.THIRD_PERSON_LEFT_HAND,
                    ItemCameraTransforms.TransformType.FIRST_PERSON_RIGHT_HAND,
                    ItemCameraTransforms.TransformType.THIRD_PERSON_RIGHT_HAND
            )
    );

    /**
     * @author circulation
     * @reason 重写方法防止植物魔法的活木化身崩溃
     */
    @Overwrite
    protected static boolean shouldRenderItem(ItemStack stack, EntityLivingBase elb, ItemCameraTransforms.TransformType transform, boolean leftHanded) {
        if (novaEngineering_Core$set.contains(transform)) {
            boolean mainhand = transform == ItemCameraTransforms.TransformType.FIRST_PERSON_RIGHT_HAND || transform == ItemCameraTransforms.TransformType.THIRD_PERSON_RIGHT_HAND;
            try {
                if (elb.getPrimaryHand() == EnumHandSide.LEFT) {
                    mainhand = !mainhand;
                }
                return mainhand || GunManager.canUseOffhand(elb.getHeldItemMainhand(), stack, elb);
            } catch (NullPointerException e) {
                return true;
            }

        } else {
            return true;
        }
    }
}
