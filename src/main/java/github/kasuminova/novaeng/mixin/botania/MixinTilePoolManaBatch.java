package github.kasuminova.novaeng.mixin.botania;

import java.awt.Color;
import java.util.List;
import github.kasuminova.novaeng.common.entity.EntityManaPoolOutputItem;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import ink.ikx.rt.impl.mods.botania.event.PoolTradeEvent;
import vazkii.botania.api.internal.VanillaPacketDispatcher;
import vazkii.botania.api.item.IManaDissolvable;
import vazkii.botania.api.mana.IKeyLocked;
import vazkii.botania.api.mana.IManaItem;
import vazkii.botania.api.mana.IManaPool;
import vazkii.botania.api.mana.IThrottledPacket;
import vazkii.botania.api.mana.ManaNetworkEvent;
import vazkii.botania.api.mana.spark.ISparkAttachable;
import vazkii.botania.api.recipe.RecipeManaInfusion;
import vazkii.botania.api.state.BotaniaStateProps;
import vazkii.botania.api.state.enums.PoolVariant;
import vazkii.botania.common.Botania;
import vazkii.botania.common.block.tile.TileMod;
import vazkii.botania.common.block.tile.mana.TilePool;
import vazkii.botania.common.core.handler.ConfigHandler;
import vazkii.botania.common.core.handler.ManaNetworkHandler;
import vazkii.botania.common.core.handler.ModSounds;

/**
 * Runs pool item handling as a single server-side batch every 20 ticks.
 *
 * <p>Both tick processing and collision processing are overwritten so entity
 * handling happens only in the scheduled batch.</p>
 */
@Mixin(value = TilePool.class, remap = false, priority = 1100)
public abstract class MixinTilePoolManaBatch extends TileMod implements IManaPool, IKeyLocked, ISparkAttachable, IThrottledPacket, ITickable {

    @Unique
    private static final int BATCH_INTERVAL = 20;

    @Shadow
    private boolean outputting;
    @Shadow
    private int soundTicks;
    @Shadow
    private boolean canAccept;
    @Shadow
    private boolean canSpare;
    @Shadow
    boolean isDoingTransfer;
    @Shadow
    int ticksDoingTransfer;
    @Shadow
    private int ticks;
    @Shadow
    private boolean sendPacket;
    @Shadow
    public int manaCap;

    @Shadow
    public abstract int getCurrentMana();

    @Shadow
    public abstract void recieveMana(int mana);

    /**
     * @author Circulation
     * @reason Batch item and mana handling every 20 server ticks.
     */
    @Overwrite
    public void update() {
        if (this.manaCap == -1) {
            this.manaCap = this.world.getBlockState(this.getPos()).getValue(BotaniaStateProps.POOL_VARIANT)
                == PoolVariant.DILUTED ? 10000 : 1000000;
        }

        if (!ManaNetworkHandler.instance.isPoolIn(this) && !this.isInvalid()) {
            ManaNetworkEvent.addPool(this);
        }

        if (this.world.isRemote) {
            double particleChance = 1.0 - (double) this.getCurrentMana() / this.manaCap * 0.1;
            if (Math.random() > particleChance) {
                Color particleColor = TilePool.PARTICLE_COLOR;
                Botania.proxy.wispFX(
                    this.pos.getX() + 0.3 + Math.random() * 0.5,
                    this.pos.getY() + 0.6 + Math.random() * 0.25,
                    this.pos.getZ() + Math.random(),
                    particleColor.getRed() / 255.0F,
                    particleColor.getGreen() / 255.0F,
                    particleColor.getBlue() / 255.0F,
                    (float) Math.random() / 3.0F,
                    (float) (-Math.random()) / 25.0F,
                    2.0F
                );
            }
            return;
        }

        boolean wasDoingTransfer = this.isDoingTransfer;
        this.isDoingTransfer = false;
        if (this.soundTicks > 0) {
            this.soundTicks--;
        }

        if (this.sendPacket && this.ticks % 10 == 0) {
            VanillaPacketDispatcher.dispatchTEToNearbyPlayers(this);
            this.sendPacket = false;
        }

        if (this.ticks % BATCH_INTERVAL == 0) {
            List<EntityItem> items = this.world.getEntitiesWithinAABB(
                EntityItem.class,
                new AxisAlignedBB(this.getPos(), this.getPos().add(1, 1, 1))
            );

            novaeng$processingBatch = true;
            try {
                for (EntityItem item : items) {
                    this.collideEntityItem(item);
                }
                for (EntityItem item : items) {
                    novaeng$processManaItem(item);
                }
            } finally {
                novaeng$processingBatch = false;
            }
        }

        if (this.isDoingTransfer) {
            this.ticksDoingTransfer++;
        } else {
            this.ticksDoingTransfer = 0;
            if (wasDoingTransfer) {
                VanillaPacketDispatcher.dispatchTEToNearbyPlayers(this);
            }
        }

        this.ticks++;
    }

    @Unique
    private boolean novaeng$processingBatch;

    /**
     * @author Circulation
     * @reason Handle every matching input in the entity stack in one batch and
     * publish the pool trade event without relying on another mixin.
     */
    @Overwrite
    public boolean collideEntityItem(EntityItem item) {
        if (this.world.isRemote || !novaeng$processingBatch || item == null || item.isDead
            || item instanceof EntityManaPoolOutputItem || item.getItem().isEmpty()) {
            return false;
        }

        ItemStack stack = item.getItem();
        if (stack.getItem() instanceof IManaDissolvable dissolvable) {
            dissolvable.onDissolveTick(this, stack, item);
        }

        if (item.age > 100 && item.age < 130) {
            return false;
        }

        boolean craftedAny = false;
        while (!item.isDead) {
            stack = item.getItem();
            if (stack.isEmpty()) {
                break;
            }
            RecipeManaInfusion recipe = TilePool.getMatchingRecipe(stack, this.world.getBlockState(this.pos.down()));
            if (recipe == null) {
                break;
            }

            int mana = recipe.getManaToConsume();
            PoolTradeEvent event = new PoolTradeEvent(
                mana,
                this.getCurrentMana(),
                recipe.getOutput().copy(),
                item,
                recipe.isAlchemy(),
                recipe.isConjuration(),
                this.world,
                this.pos
            );
            if (event.post()) {
                break;
            }

            if (event.getMana() > this.getCurrentMana() && !event.isAllowExceed()) {
                break;
            }

            this.recieveMana(-event.getMana());
            stack.shrink(1);
            ItemStack output = event.getOutput();
            EntityItem outputItem = new EntityManaPoolOutputItem(
                this.world,
                this.pos.getX() + 0.5,
                this.pos.getY() + 1.5,
                this.pos.getZ() + 0.5,
                output
            );
            outputItem.age = 105;
            this.world.spawnEntity(outputItem);
            novaeng$craftingFanciness();
            craftedAny = true;
        }

        return craftedAny;
    }

    @Unique
    private void novaeng$craftingFanciness() {
        if (this.soundTicks == 0) {
            this.world.playSound(null, this.pos, ModSounds.manaPoolCraft, SoundCategory.BLOCKS, 0.4F, 4.0F);
            this.soundTicks = 6;
        }
        this.world.addBlockEvent(this.getPos(), this.getBlockType(), 0, 0);
    }

    @Unique
    private void novaeng$processManaItem(EntityItem item) {
        if (item == null || item.isDead) {
            return;
        }

        ItemStack stack = item.getItem();
        if (stack.isEmpty() || !(stack.getItem() instanceof IManaItem mana)) {
            return;
        }

        boolean canTransfer = this.outputting
            ? mana.canReceiveManaFromPool(stack, this)
            : mana.canExportManaToPool(stack, this);
        if (!canTransfer) {
            return;
        }

        boolean didSomething = false;
        if (this.outputting) {
            if (this.canSpare) {
                int manaVal = Math.min(this.getCurrentMana(), mana.getMaxMana(stack) - mana.getMana(stack));
                if (manaVal > 0) {
                    mana.addMana(stack, manaVal);
                    this.recieveMana(-manaVal);
                    didSomething = true;
                }
            }
        } else if (this.canAccept) {
            int manaVal = Math.min(this.manaCap - this.getCurrentMana(), mana.getMana(stack));
            if (manaVal > 0) {
                mana.addMana(stack, -manaVal);
                this.recieveMana(manaVal);
                didSomething = true;
            }
        }

        if (didSomething) {
            if (ConfigHandler.chargingAnimationEnabled && this.world.rand.nextInt(20) == 0) {
                this.world.addBlockEvent(this.getPos(), this.getBlockType(), 1, this.outputting ? 1 : 0);
            }
            this.isDoingTransfer = this.outputting;
        }
    }
}
