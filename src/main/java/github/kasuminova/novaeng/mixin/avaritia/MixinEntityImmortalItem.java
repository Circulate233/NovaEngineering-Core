package github.kasuminova.novaeng.mixin.avaritia;

import morph.avaritia.entity.EntityImmortalItem;
import net.minecraft.block.material.Material;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.item.ItemExpireEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(EntityImmortalItem.class)
public abstract class MixinEntityImmortalItem extends EntityItem {

    @Shadow(remap = false)
    private int pickupDelay;

    @Shadow(remap = false)
    private int extraLife;

    @Shadow(remap = false)
    private int age;

    public MixinEntityImmortalItem(World worldIn, double x, double y, double z) {
        super(worldIn, x, y, z);
    }

    /**
     * @author circulation
     * @reason 离奇的服务端方法里跑客户端类
     */
    @Overwrite
    public void onUpdate() {
        ItemStack stack = this.getItem();
        if (stack.isEmpty() || !stack.getItem().onEntityItemUpdate(this)) {
            if (this.getItem().isEmpty()) {
                this.setDead();
            } else {
                super.onUpdate();
                if (this.pickupDelay > 0) {
                    --this.pickupDelay;
                }

                this.prevPosX = this.posX;
                this.prevPosY = this.posY;
                this.prevPosZ = this.posZ;
                this.motionY -= (double)0.04F;
                this.noClip = this.pushOutOfBlocks(this.posX, (this.getEntityBoundingBox().minY + this.getEntityBoundingBox().maxY) / (double)2.0F, this.posZ);
                this.move(MoverType.SELF, this.motionX, this.motionY, this.motionZ);
                boolean flag = (int)this.prevPosX != (int)this.posX || (int)this.prevPosY != (int)this.posY || (int)this.prevPosZ != (int)this.posZ;
                if ((flag || this.ticksExisted % 25 == 0) && this.world.getBlockState(new BlockPos(this.posX, this.posY, this.posZ)).getMaterial() == Material.LAVA) {
                    this.motionY = (double)0.2F;
                    this.motionX = (double)((this.rand.nextFloat() - this.rand.nextFloat()) * 0.2F);
                    this.motionZ = (double)((this.rand.nextFloat() - this.rand.nextFloat()) * 0.2F);
                    this.playSound(SoundEvents.ENTITY_GENERIC_BURN, 0.4F, 2.0F + this.rand.nextFloat() * 0.4F);
                }

                float f = 0.98F;
                if (this.onGround) {
                    f = this.world.getBlockState(new BlockPos(this.posX, this.getEntityBoundingBox().minY - (double)1.0F, this.posZ)).getBlock().slipperiness * 0.98F;
                }

                this.motionX *= (double)f;
                this.motionY *= (double)0.98F;
                this.motionZ *= (double)f;
                if (this.onGround) {
                    this.motionY *= (double)-0.5F;
                }

                ++this.age;
                ItemStack item = this.getItem();
                if (!this.world.isRemote && this.age >= this.lifespan) {
                    if (!item.isEmpty()) {
                        item.getItem();
                        ItemExpireEvent event = new ItemExpireEvent(this, item.getItem().getEntityLifespan(item, this.world));
                        if (MinecraftForge.EVENT_BUS.post(event)) {
                            this.lifespan += this.extraLife;
                        } else {
                            this.setDead();
                        }
                    } else {
                        this.setDead();
                    }
                }

                if (!item.isEmpty() && item.getCount() <= 0) {
                    this.setDead();
                }
            }

        }
    }
}
