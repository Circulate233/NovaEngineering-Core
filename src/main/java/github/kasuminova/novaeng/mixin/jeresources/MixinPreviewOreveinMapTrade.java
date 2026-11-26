package github.kasuminova.novaeng.mixin.jeresources;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import jeresources.util.FakeClientWorld;
import jeresources.util.FakeMerchant;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.IMerchant;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "blusunrize.immersiveengineering.common.util.IEVillagerHandler$OreveinMapForEmeralds", remap = false)
public abstract class MixinPreviewOreveinMapTrade {
    @WrapOperation(method = {"addMerchantRecipe", "func_190888_a"}, at = @At(value = "INVOKE", ordinal = 0, remap = true,
        target = "Lnet/minecraft/entity/IMerchant;getWorld()Lnet/minecraft/world/World;"), require = 1)
    private World nova$recordPreviewWorld(final IMerchant merchant, final Operation<World> original,
                                          @Share("previewWorld") final LocalRef<World> previewWorld) {
        final World world = original.call(merchant);
        previewWorld.set(world);
        return world;
    }

    @WrapOperation(method = {"addMerchantRecipe", "func_190888_a"}, at = @At(value = "INVOKE", ordinal = 1, remap = true,
        target = "Lnet/minecraft/entity/IMerchant;getWorld()Lnet/minecraft/world/World;"), require = 1)
    private World nova$reusePreviewDimension(final IMerchant merchant, final Operation<World> original,
                                             @Share("previewWorld") final LocalRef<World> previewWorld) {
        final World world = previewWorld.get();
        // This call only reads the dimension from the WorldProvider shared by every FakeClientWorld.
        return merchant.getClass() == FakeMerchant.class && world instanceof FakeClientWorld
            && Minecraft.getMinecraft().world == null ? world : original.call(merchant);
    }
}
