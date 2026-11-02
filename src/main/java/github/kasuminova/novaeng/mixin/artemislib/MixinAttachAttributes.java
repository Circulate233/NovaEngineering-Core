package github.kasuminova.novaeng.mixin.artemislib;

import com.artemis.artemislib.compatibilities.sizeCap.ISizeCap;
import com.artemis.artemislib.compatibilities.sizeCap.SizeCapPro;
import com.artemis.artemislib.util.AttachAttributes;
import com.artemis.artemislib.util.attributes.ArtemisLibAttributes;
import github.kasuminova.novaeng.common.performance.AttributeModifierPresence;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.attributes.AbstractAttributeMap;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = AttachAttributes.class, remap = false)
public abstract class MixinAttachAttributes {
    /**
     * @author NovaEngineering
     * @reason Check modifier presence without creating a snapshot, preserving subsequent live attribute reads.
     */
    @Overwrite
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        EntityPlayer player = event.player;
        if (player.hasCapability(SizeCapPro.sizeCapability, null)) {
            ISizeCap cap = player.getCapability(SizeCapPro.sizeCapability, null);
            AbstractAttributeMap map = player.getAttributeMap();
            IAttributeInstance heightInstance = map.getAttributeInstance(ArtemisLibAttributes.ENTITY_HEIGHT);
            boolean hasHeightModifier = AttributeModifierPresence.isEmpty(heightInstance);
            boolean hasWidthModifier = AttributeModifierPresence.isEmpty(player.getAttributeMap().getAttributeInstance(ArtemisLibAttributes.ENTITY_WIDTH));
            double heightAttribute = player.getAttributeMap().getAttributeInstance(ArtemisLibAttributes.ENTITY_HEIGHT).getAttributeValue();
            double widthAttribute = player.getAttributeMap().getAttributeInstance(ArtemisLibAttributes.ENTITY_WIDTH).getAttributeValue();
            float height = (float) ((double) cap.getDefaultHeight() * heightAttribute);
            float width = (float) ((double) cap.getDefaultWidth() * widthAttribute);
            if (!hasHeightModifier || !hasWidthModifier) {
                if (!cap.getTrans()) {
                    cap.setDefaultHeight(1.8f);
                    cap.setDefaultWidth(0.6f);
                    cap.setTrans(true);
                }
                if (cap.getTrans()) {
                    float eyeHeight = (float) ((double) player.getDefaultEyeHeight() * heightAttribute);
                    if (player.isSneaking()) {
                        height *= 0.9166667f;
                        eyeHeight *= 0.9382716f;
                    }
                    if (player.isElytraFlying()) {
                        height *= 0.33f;
                    }
                    if (player.isPlayerSleeping()) {
                        width = 0.2f;
                        height = 0.2f;
                    }
                    if (player.isRiding()) {
                        // empty if block
                    }
                    width = MathHelper.clamp(width, 0.15f, width);
                    player.eyeHeight = (height = MathHelper.clamp(height, 0.25f, height)) >= 1.6f ? eyeHeight : eyeHeight * 0.9876542f;
                    player.height = height;
                    player.width = width;
                    double d0 = (double) width / 2.0;
                    AxisAlignedBB aabb = player.getEntityBoundingBox();
                    player.setEntityBoundingBox(new AxisAlignedBB(player.posX - d0, aabb.minY, player.posZ - d0, player.posX + d0, aabb.minY + (double) player.height, player.posZ + d0));
                }
            } else if (cap.getTrans()) {
                player.height = height;
                player.width = width;
                double d0 = (double) width / 2.0;
                AxisAlignedBB aabb = player.getEntityBoundingBox();
                player.setEntityBoundingBox(new AxisAlignedBB(player.posX - d0, aabb.minY, player.posZ - d0, player.posX + d0, aabb.minY + (double) height, player.posZ + d0));
                player.eyeHeight = player.getDefaultEyeHeight();
                cap.setTrans(false);
            }
        }
    }

    /**
     * @author NovaEngineering
     * @reason Check modifier presence without creating a snapshot, preserving subsequent live attribute reads.
     */
    @Overwrite
    public void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        EntityLivingBase entity = event.getEntityLiving();
        if (!(entity instanceof EntityPlayer) && entity.hasCapability(SizeCapPro.sizeCapability, null)) {
            ISizeCap cap = entity.getCapability(SizeCapPro.sizeCapability, null);
            AbstractAttributeMap map = entity.getAttributeMap();
            IAttributeInstance heightInstance = map.getAttributeInstance(ArtemisLibAttributes.ENTITY_HEIGHT);
            boolean hasHeightModifier = AttributeModifierPresence.isEmpty(heightInstance);
            boolean hasWidthModifier = AttributeModifierPresence.isEmpty(entity.getAttributeMap().getAttributeInstance(ArtemisLibAttributes.ENTITY_WIDTH));
            double heightAttribute = entity.getAttributeMap().getAttributeInstance(ArtemisLibAttributes.ENTITY_HEIGHT).getAttributeValue();
            double widthAttribute = entity.getAttributeMap().getAttributeInstance(ArtemisLibAttributes.ENTITY_WIDTH).getAttributeValue();
            float height = (float) ((double) cap.getDefaultHeight() * heightAttribute);
            float width = (float) ((double) cap.getDefaultWidth() * widthAttribute);
            if (!hasHeightModifier || !hasWidthModifier) {
                if (!cap.getTrans()) {
                    cap.setDefaultHeight(entity.height);
                    cap.setDefaultWidth(entity.width);
                    cap.setTrans(true);
                }
                if (cap.getTrans()) {
                    width = MathHelper.clamp(width, 0.04f, width);
                    entity.height = height = MathHelper.clamp(height, 0.08f, height);
                    entity.width = width;
                    double d0 = (double) width / 2.0;
                    AxisAlignedBB aabb = entity.getEntityBoundingBox();
                    entity.setEntityBoundingBox(new AxisAlignedBB(entity.posX - d0, aabb.minY, entity.posZ - d0, entity.posX + d0, aabb.minY + (double) entity.height, entity.posZ + d0));
                }
            } else if (cap.getTrans()) {
                entity.height = height;
                entity.width = width;
                double d0 = (double) width / 2.0;
                AxisAlignedBB aabb = entity.getEntityBoundingBox();
                entity.setEntityBoundingBox(new AxisAlignedBB(entity.posX - d0, aabb.minY, entity.posZ - d0, entity.posX + d0, aabb.minY + (double) height, entity.posZ + d0));
                cap.setTrans(false);
            }
        }
    }
}
