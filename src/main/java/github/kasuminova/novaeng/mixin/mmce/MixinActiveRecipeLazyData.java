package github.kasuminova.novaeng.mixin.mmce;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import hellfirepvp.modularmachinery.common.crafting.ActiveMachineRecipe;
import net.minecraft.nbt.NBTTagCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ActiveMachineRecipe.class, remap = false)
public abstract class MixinActiveRecipeLazyData {
    @Shadow
    private NBTTagCompound data;

    // Constructor redirects reject null; a wrapped construction may legally be omitted.
    @WrapOperation(method = {"<init>*", "reset"}, at = @At(value = "NEW", target = "net/minecraft/nbt/NBTTagCompound"), require = 3)
    private static NBTTagCompound nova$deferUnusedData(final Operation<NBTTagCompound> original) {
        return null;
    }

    @Unique
    private synchronized void nova$ensureData() {
        if (data == null) data = new NBTTagCompound();
    }

    @Inject(method = {"getData", "getDataCompound"}, at = @At("HEAD"), require = 2)
    private void nova$exposeWritableData(final CallbackInfoReturnable<?> cir) {
        nova$ensureData();
    }

    @WrapOperation(method = "serialize", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/nbt/NBTTagCompound;isEmpty()Z", remap = true), require = 1)
    private boolean nova$unallocatedDataIsEmpty(final NBTTagCompound tag, final Operation<Boolean> original) {
        return tag == null || original.call(tag);
    }
}
