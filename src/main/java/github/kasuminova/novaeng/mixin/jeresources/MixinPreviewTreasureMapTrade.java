package github.kasuminova.novaeng.mixin.jeresources;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import jeresources.util.FakeMerchant;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.IMerchant;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.minecraft.entity.passive.EntityVillager$TreasureMapForEmeralds")
public abstract class MixinPreviewTreasureMapTrade {
    @WrapOperation(method = "addMerchantRecipe", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/entity/IMerchant;getWorld()Lnet/minecraft/world/World;"), require = 1)
    private World nova$existingMerchantWorld(final IMerchant merchant, final Operation<World> original) {
        return merchant.getClass() == FakeMerchant.class ? Minecraft.getMinecraft().world : original.call(merchant);
    }

    @WrapOperation(method = "addMerchantRecipe", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/World;findNearestStructure(Ljava/lang/String;Lnet/minecraft/util/math/BlockPos;Z)Lnet/minecraft/util/math/BlockPos;"),
        require = 1)
    private BlockPos nova$findStructureInExistingWorld(final World world, final String destination,
                                                       final BlockPos from, final boolean unexplored,
                                                       final Operation<BlockPos> original,
                                                       @Local(argsOnly = true) final IMerchant merchant) {
        // The fake world inherits the base World method that returns null. The price RNG has already run.
        return world == null && merchant.getClass() == FakeMerchant.class
            ? null : original.call(world, destination, from, unexplored);
    }
}
