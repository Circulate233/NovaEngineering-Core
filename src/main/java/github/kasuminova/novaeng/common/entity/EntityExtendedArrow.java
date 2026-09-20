package github.kasuminova.novaeng.common.entity;

import github.kasuminova.novaeng.common.registry.RegistryExtended;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.SPacketEntityVelocity;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

/**
 * 原 Additions 模组 `novaextended-fallen_star_arrow` 的弹射物。
 *
 * <p>原实现把命中效果交给数据包中的 `additions.explode` 与 `additions.damage` 命令执行，
 * 模组移除后这两条命令不再存在，因此在此直接以等价的原版逻辑实现：
 * 一次不破坏地形、不点燃火焰的爆炸，以及 60 点魔法伤害。
 */
public class EntityExtendedArrow extends EntityArrow {

    /** 与原数据包 `damage: 30.0` 一致，实际伤害为飞行速度乘以此倍率。 */
    private static final float DAMAGE_MULTIPLIER = 30.0F;
    private static final float PUNCH = 0.5F;
    /** 原数据包 `gravity: 0.01`，原版固定重力为 0.05。 */
    private static final float GRAVITY = 0.01F;
    private static final double VANILLA_GRAVITY = 0.05D;
    /** 原版在水中施加 0.6 的阻力，乘以此值可抵消。 */
    private static final double WATER_DRAG_COMPENSATION = 1.65D;

    private static final float EXTRA_MAGIC_DAMAGE = 60.0F;
    private static final float EXPLOSION_POWER = 4.0F;
    private static final int INSTANT_DAMAGE_AMPLIFIER = 7;

    private ItemStack bowStack = ItemStack.EMPTY;

    public EntityExtendedArrow(final World world) {
        super(world);
    }

    public EntityExtendedArrow(final World world, final double x, final double y, final double z) {
        super(world, x, y, z);
    }

    public EntityExtendedArrow(final World world, final EntityLivingBase shooter) {
        super(world, shooter);
    }

    public EntityExtendedArrow(final World world, final EntityLivingBase shooter, final ItemStack bow) {
        super(world, shooter);
        this.bowStack = bow;
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (this.inGround) {
            return;
        }

        // 原版在上一步写入了固定的 -0.05 重力，此处换算为数据包定义的重力。
        this.motionY += VANILLA_GRAVITY * (1.0D - GRAVITY);

        if (this.isInWater()) {
            this.motionX *= WATER_DRAG_COMPENSATION;
            this.motionY *= WATER_DRAG_COMPENSATION;
            this.motionZ *= WATER_DRAG_COMPENSATION;
        }
    }

    @Override
    protected void onHit(@NotNull final RayTraceResult result) {
        if (result.entityHit == null) {
            super.onHit(result);
            return;
        }
        this.onHitEntity(result.entityHit);
    }

    private void onHitEntity(final Entity target) {
        if (!this.world.isRemote) {
            this.applyHitEffects(target);
        }

        final float velocity = (float) Math.sqrt(
            this.motionX * this.motionX + this.motionY * this.motionY + this.motionZ * this.motionZ);
        final float damage = Math.max(0.0F, velocity * DAMAGE_MULTIPLIER);

        final DamageSource source = this.shootingEntity == null
            ? DamageSource.causeArrowDamage(this, this)
            : DamageSource.causeArrowDamage(this, this.shootingEntity);

        if (target.attackEntityFrom(source, damage)) {
            if (target instanceof EntityLivingBase living) {
                if (!this.world.isRemote) {
                    living.setArrowCountInEntity(living.getArrowCountInEntity() + 1);
                }
                this.applyKnockback(living);
            }
            if (this.shootingEntity instanceof EntityPlayerMP playerMP && target instanceof EntityPlayer) {
                playerMP.connection.sendPacket(new SPacketEntityVelocity(target));
            }

            this.playSound(SoundEvents.ENTITY_ARROW_HIT, 1.0F,
                1.2F / (this.rand.nextFloat() * 0.2F + 0.9F));

            // 原数据包 `pierces_entities: true`，命中后继续飞行以穿透后续目标。
            return;
        }

        this.motionX *= -0.1D;
        this.motionY *= -0.1D;
        this.motionZ *= -0.1D;
        this.rotationYaw += 180.0F;
        this.prevRotationYaw += 180.0F;
    }

    private void applyHitEffects(final Entity target) {
        this.world.newExplosion(this.shootingEntity, this.posX, this.posY, this.posZ,
            EXPLOSION_POWER, false, false);

        if (target instanceof EntityLivingBase living) {
            final DamageSource magic = this.shootingEntity instanceof EntityLivingBase shooter
                ? DamageSource.causeIndirectMagicDamage(this, shooter)
                : DamageSource.MAGIC;
            living.attackEntityFrom(magic, EXTRA_MAGIC_DAMAGE);
            living.addPotionEffect(new PotionEffect(
                MobEffects.INSTANT_DAMAGE, 1, INSTANT_DAMAGE_AMPLIFIER, false, false));
        }
    }

    private void applyKnockback(final EntityLivingBase target) {
        final float horizontal = (float) Math.sqrt(
            this.motionX * this.motionX + this.motionZ * this.motionZ);
        if (horizontal <= 0.0F) {
            return;
        }
        target.addVelocity(
            this.motionX * PUNCH * 0.6D / horizontal,
            0.1D,
            this.motionZ * PUNCH * 0.6D / horizontal);
    }

    @Override
    @NotNull
    protected ItemStack getArrowStack() {
        return new ItemStack(RegistryExtended.ARROW_ITEM);
    }

    @Override
    protected void arrowHit(@NotNull final EntityLivingBase living) {
        // 命中效果统一在 onHitEntity 中施加，原版此处施加的箭上药水效果并不适用。
    }

    @Override
    public void writeEntityToNBT(@NotNull final NBTTagCompound compound) {
        super.writeEntityToNBT(compound);
        if (!this.bowStack.isEmpty()) {
            compound.setTag("NovaBow", this.bowStack.writeToNBT(new NBTTagCompound()));
        }
    }

    @Override
    public void readEntityFromNBT(@NotNull final NBTTagCompound compound) {
        super.readEntityFromNBT(compound);
        if (compound.hasKey("NovaBow")) {
            this.bowStack = new ItemStack(compound.getCompoundTag("NovaBow"));
        }
    }

}
