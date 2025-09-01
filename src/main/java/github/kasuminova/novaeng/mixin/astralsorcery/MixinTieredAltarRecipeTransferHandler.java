package github.kasuminova.novaeng.mixin.astralsorcery;

import com.llamalad7.mixinextras.sugar.Local;
import hellfirepvp.astralsorcery.common.container.ContainerAltarBase;
import hellfirepvp.astralsorcery.common.integrations.mods.jei.util.TieredAltarRecipeTransferHandler;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import mezz.jei.JustEnoughItems;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.network.packets.PacketRecipeTransfer;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Map;

@Mixin(value = TieredAltarRecipeTransferHandler.class,remap = false)
public abstract class MixinTieredAltarRecipeTransferHandler<C extends ContainerAltarBase> implements IRecipeTransferHandler<C> {

    @Inject(method = "transferRecipe(Lhellfirepvp/astralsorcery/common/container/ContainerAltarBase;Lmezz/jei/api/gui/IRecipeLayout;Lnet/minecraft/entity/player/EntityPlayer;ZZ)Lmezz/jei/api/recipe/transfer/IRecipeTransferError;", at = @At(value = "INVOKE", target = "Lhellfirepvp/astralsorcery/common/integrations/mods/jei/util/TieredAltarRecipeTransferHandler;mirrorSlotGrid(Ljava/util/Map;)V", shift = At.Shift.AFTER), cancellable = true)
    public void fixHEI(C container, IRecipeLayout recipeLayout, EntityPlayer player, boolean maxTransfer, boolean doTransfer, CallbackInfoReturnable<IRecipeTransferError> cir, @Local(name = "slotMap") Map<Integer, Integer> slotMap, @Local(name = "craftingSlotIndexes") List<Integer> craftingSlotIndexes, @Local(name = "inventorySlotIndexes") List<Integer> inventorySlotIndexes) {
        if (doTransfer) {
            PacketRecipeTransfer packet = new PacketRecipeTransfer(new Int2IntOpenHashMap(slotMap), new IntArrayList(craftingSlotIndexes), new IntArrayList(inventorySlotIndexes), maxTransfer ? 64 : 1, false, true);
            JustEnoughItems.getProxy().sendPacketToServer(packet);
        }
        cir.setReturnValue(null);
    }
}
