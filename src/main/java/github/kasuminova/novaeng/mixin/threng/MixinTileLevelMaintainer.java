package github.kasuminova.novaeng.mixin.threng;

import appeng.api.config.Actionable;
import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.networking.crafting.ICraftingRequester;
import appeng.api.networking.storage.IStackWatcherHost;
import appeng.api.storage.data.IAEItemStack;
import appeng.util.item.AEItemStack;
import io.github.phantamanta44.libnine.util.data.serialization.AutoSerialize;
import io.github.phantamanta44.threng.ThrEngConfig;
import io.github.phantamanta44.threng.tile.TileLevelMaintainer;
import io.github.phantamanta44.threng.tile.base.TileNetworkDevice;
import io.github.phantamanta44.threng.util.ThrEngCraftingTracker;
import net.minecraftforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;

@Mixin(value = TileLevelMaintainer.class, remap = false)
public abstract class MixinTileLevelMaintainer extends TileNetworkDevice implements IStackWatcherHost, ICraftingRequester {

    @Shadow
    private int sleepTicks;

    @Final
    @Shadow
    @AutoSerialize
    private IItemHandler results;

    @Shadow
    private int sleepIncrement;

    @Final
    @Shadow
    @AutoSerialize
    private ThrEngCraftingTracker crafter;

    /**
     * @author a
     * @reason v
     */
    @Nullable
    @Overwrite
    public IAEItemStack injectCraftedItems(ICraftingLink link, @Nullable IAEItemStack stack, Actionable mode) {
        if (stack == null) {
            return null;
        } else {
            int slot = this.crafter.getSlotForJob(link);
            if (slot == -1) {
                return stack;
            } else if (mode == Actionable.SIMULATE) {
                return AEItemStack.fromItemStack(this.results.insertItem(slot, stack.createItemStack(), true));
            } else {
                IAEItemStack rem = AEItemStack.fromItemStack(this.results.insertItem(slot, stack.createItemStack(), false));
                if (rem == null || rem.getStackSize() < stack.getStackSize()) {
                    this.sleepIncrement = ThrEngConfig.networkDevices.levelMaintainerSleepMin;
                    this.sleepTicks = 0;
                }

                return rem;
            }
        }
    }

}
