package github.kasuminova.novaeng.mixin.minecraft;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(InventoryPlayer.class)
public abstract class MixinInventoryPlayer {

    @Shadow @Final public NonNullList<ItemStack> mainInventory;
    @Shadow @Final public NonNullList<ItemStack> armorInventory;
    @Shadow @Final public NonNullList<ItemStack> offHandInventory;

    @Overwrite
    public int getSizeInventory() {
        return 41;
    }

    @Overwrite
    public ItemStack getStackInSlot(final int index) {
        if (index < 36) {
            return this.mainInventory.get(index);
        }
        if (index < 40) {
            return this.armorInventory.get(index - 36);
        }
        if (index == 40) {
            return this.offHandInventory.get(0);
        }
        return ItemStack.EMPTY;
    }
}
