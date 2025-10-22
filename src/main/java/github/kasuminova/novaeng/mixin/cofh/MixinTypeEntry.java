package github.kasuminova.novaeng.mixin.cofh;

import cofh.thermalexpansion.item.ItemSatchel;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ItemSatchel.TypeEntry.class, remap = false)
public class MixinTypeEntry {

    @Mutable
    @Shadow
    @Final
    public String name;

    @Inject(method = "<init>", at = @At("TAIL"))
    void TypeEntry(ItemSatchel this$0, String name, int level, CallbackInfo ci) {
        if (name == null) {
            this.name = "null";
        }
    }

}
