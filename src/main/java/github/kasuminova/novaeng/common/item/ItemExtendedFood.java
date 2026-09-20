package github.kasuminova.novaeng.common.item;

import github.kasuminova.novaeng.common.core.CreativeTabNovaEng;
import github.kasuminova.novaeng.novaeng_core.Tags;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * 原 Additions 模组 `pie` 系列食物的重新实现。
 *
 * <p>`extraUseDuration` 对应数据包的 `eat_time`（默认 32），
 * 进食效果则对应 `eaten_effects`，其中带 `chance` 的条目按概率触发。
 */
public class ItemExtendedFood extends ItemFood {

    private final int useDuration;
    private final List<Effect> effects;

    public ItemExtendedFood(final String name, final int hunger, final float saturation,
                            final int useDuration, final List<Effect> effects) {
        super(hunger, saturation, false);
        this.useDuration = useDuration;
        this.effects = effects;
        this.setCreativeTab(CreativeTabNovaEng.INSTANCE);
        this.setRegistryName(new ResourceLocation(Tags.MOD_ID, name));
        this.setTranslationKey(Tags.MOD_ID + '.' + name);
    }

    public static List<Effect> effects() {
        return new ArrayList<>();
    }

    @Override
    public int getMaxItemUseDuration(@NotNull final ItemStack stack) {
        return useDuration;
    }

    @Override
    protected void onFoodEaten(@NotNull final ItemStack stack, @NotNull final World world,
                               @NotNull final EntityPlayer player) {
        if (world.isRemote) {
            return;
        }
        for (final Effect effect : effects) {
            if (effect.chance < 1.0F && world.rand.nextFloat() > effect.chance) {
                continue;
            }
            player.addPotionEffect(new PotionEffect(effect.potion, effect.duration, effect.amplifier, false, false));
        }
    }

    public static final class Effect {
        private final Potion potion;
        private final int duration;
        private final int amplifier;
        private final float chance;

        public Effect(final Potion potion, final int duration, final int amplifier, final float chance) {
            this.potion = potion;
            this.duration = duration;
            this.amplifier = amplifier;
            this.chance = chance;
        }
    }

    public static Effect speed(final int duration, final int amplifier) {
        return new Effect(MobEffects.SPEED, duration, amplifier, 1.0F);
    }

    public static Effect strength(final int duration, final int amplifier) {
        return new Effect(MobEffects.STRENGTH, duration, amplifier, 1.0F);
    }

    public static Effect regeneration(final int duration, final int amplifier) {
        return new Effect(MobEffects.REGENERATION, duration, amplifier, 1.0F);
    }

    public static Effect resistance(final int duration, final int amplifier) {
        return new Effect(MobEffects.RESISTANCE, duration, amplifier, 1.0F);
    }

    public static Effect absorption(final int duration, final int amplifier, final float chance) {
        return new Effect(MobEffects.ABSORPTION, duration, amplifier, chance);
    }

}
