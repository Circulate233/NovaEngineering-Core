package github.kasuminova.novaeng.mixin.artisanworktables;

import com.codetaylor.mc.artisanworktables.modules.worktables.tile.MutuallyExclusiveStackHandlerWrapper;
import com.google.common.collect.BiMap;

import github.kasuminova.novaeng.common.util.collection.Reference2IntHashBiMap;
import github.kasuminova.novaeng.common.util.collection.PrimitiveBiMaps;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = MutuallyExclusiveStackHandlerWrapper.class, remap = false)
public abstract class MixinMutuallyExclusiveStackHandlerPrimitiveBiMap {
    @Shadow
    private BiMap<Item, Integer> indexMap;
    @Shadow
    @Final
    private ItemStackHandler handler;

    @Redirect(
        method = {"<init>"},
        at =
        @At(
            value = "FIELD",
            target =
                "Lcom/codetaylor/mc/artisanworktables/modules/worktables/tile/MutuallyExclusiveStackHandlerWrapper;indexMap:Lcom/google/common/collect/BiMap;",
            opcode = Opcodes.PUTFIELD),
        require = 1)
    private void nova$primitiveTable(
        final MutuallyExclusiveStackHandlerWrapper owner, final BiMap<Item, Integer> original) {
        final Reference2IntHashBiMap<Item> replacement = new Reference2IntHashBiMap<>(handler.getSlots());
        replacement.putAll(original);
        indexMap = replacement;
    }

    @Shadow
    private boolean matchStacks(final ItemStack stack, final ItemStack existing) {
        throw new AssertionError();
    }

    /**
     * @author NovaEngineering
     * @reason Preserve slot matching order while using primitive indices.
     */
    @Overwrite
    public ItemStack insertItem(int slot, final ItemStack stack, final boolean simulate) {
        slot = PrimitiveBiMaps.getIntValueOrDefault(indexMap, stack.getItem(), slot);
        ItemStack existing = handler.getStackInSlot(slot);
        if (matchStacks(stack, existing)) return handler.insertItem(slot, stack, simulate);
        for (int i = 0; i < handler.getSlots(); i++) {
            existing = handler.getStackInSlot(i);
            if (!matchStacks(stack, existing)) continue;
            PrimitiveBiMaps.forcePutObjectInt(indexMap, stack.getItem(), i);
            return handler.insertItem(i, stack, simulate);
        }
        return handler.insertItem(slot, stack, simulate);
    }
}
