package github.kasuminova.novaeng.mixin.jeresources;

import jeresources.util.LootTableHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = LootTableHelper.class, remap = false)
public abstract class MixinLootTableManagerLookup {
    @Redirect(method = "getManager()Lnet/minecraft/world/storage/loot/LootTableManager;",
        at = @At(value = "INVOKE", target = "Ljeresources/compatibility/CompatBase;getWorld()Lnet/minecraft/world/World;"),
        require = 1)
    private static World nova$useExistingWorldForLoot() {
        // A FakeClientWorld has no loot table manager. The next call already handles null.
        return Minecraft.getMinecraft().world;
    }
}
