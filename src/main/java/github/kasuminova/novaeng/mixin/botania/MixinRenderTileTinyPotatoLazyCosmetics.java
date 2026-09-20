package github.kasuminova.novaeng.mixin.botania;

import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vazkii.botania.client.render.tile.RenderTileTinyPotato;
import vazkii.botania.common.item.ModItems;
import vazkii.botania.common.item.equipment.bauble.ItemBaubleCosmetic;

import java.util.function.IntFunction;
import java.util.stream.Stream;

/** Defers Tiny Potato's unused cosmetic stack construction until its first render. */
@Mixin(value = RenderTileTinyPotato.class, remap = false)
public abstract class MixinRenderTileTinyPotatoLazyCosmetics {

    @Shadow
    @Final
    private ItemStack[] cosmetics;

    @Unique
    private boolean nova$cosmeticsBuilt;

    /**
     * Keep the target field's original array allocation and identity, but leave its elements empty
     * until the renderer is actually used. The mapped stream has no other observable consumer.
     */
    @Redirect(
        method = "<init>",
        at = @At(value = "INVOKE",
            target = "Ljava/util/stream/Stream;toArray(Ljava/util/function/IntFunction;)[Ljava/lang/Object;",
            remap = false),
        require = 1)
    private Object[] nova$deferCosmetics(final Stream<?> source, final IntFunction<Object[]> arrayFactory) {
        return new ItemStack[ItemBaubleCosmetic.Variants.values().length];
    }

    /** Populate the same array immediately before the original render body can observe it. */
    @Inject(
        method = "render(Lvazkii/botania/common/block/tile/TileTinyPotato;DDDFIF)V",
        at = @At("HEAD"),
        require = 1)
    private void nova$buildCosmetics(final CallbackInfo ci) {
        if (this.nova$cosmeticsBuilt) {
            return;
        }

        final ItemBaubleCosmetic.Variants[] variants = ItemBaubleCosmetic.Variants.values();
        for (int i = 0; i < variants.length; i++) {
            this.cosmetics[i] = new ItemStack(ModItems.cosmetic, 1, variants[i].ordinal());
        }
        this.nova$cosmeticsBuilt = true;
    }
}
