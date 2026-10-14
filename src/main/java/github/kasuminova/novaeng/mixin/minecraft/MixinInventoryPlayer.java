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

    @Shadow
    public NonNullList<ItemStack> mainInventory;
    @Shadow @Final public NonNullList<ItemStack> armorInventory;
    @Shadow @Final public NonNullList<ItemStack> offHandInventory;

    /**
     * @author NovaEngineering contributors
     * @reason Return the fixed vanilla slot count without traversing the inventory lists.
     */
    @Overwrite
    public int getSizeInventory() {
        return 41;
    }

    /**
     * @author NovaEngineering contributors
     * @reason Index directly into the matching inventory list instead of traversing all lists.
     */
    @Overwrite
    public ItemStack getStackInSlot(final int index) {
        if (index < 36) {
            return this.mainInventory.get(index);
        }
        if (index < 40) {
            return this.armorInventory.get(index - 36);
        }
        if (index == 40) {
            return this.offHandInventory.getFirst();
        }
        return ItemStack.EMPTY;
    }
}
