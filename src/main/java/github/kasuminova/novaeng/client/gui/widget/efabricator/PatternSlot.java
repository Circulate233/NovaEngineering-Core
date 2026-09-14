package github.kasuminova.novaeng.client.gui.widget.efabricator;

import ae2.api.crafting.IPatternDetails;
import ae2.api.crafting.PatternDetailsHelper;
import ae2.api.stacks.AEItemKey;
import ae2.api.stacks.GenericStack;
import github.kasuminova.mmce.client.gui.util.MousePos;
import github.kasuminova.mmce.client.gui.util.RenderPos;
import github.kasuminova.mmce.client.gui.util.RenderSize;
import github.kasuminova.mmce.client.gui.util.TextureProperties;
import github.kasuminova.mmce.client.gui.widget.base.WidgetGui;
import github.kasuminova.mmce.client.gui.widget.slot.SlotItemVirtual;
import github.kasuminova.novaeng.NovaEngineeringCore;
import github.kasuminova.novaeng.client.gui.GuiEFabricatorController;
import github.kasuminova.novaeng.common.network.PktEFabricatorPatternSearchGUIAction;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.WeakHashMap;

public class PatternSlot extends SlotItemVirtual {

    private static final WeakHashMap<AEItemKey, WeakReference<IPatternDetails>> DETAILS_CACHE = new WeakHashMap<>();

    private final BlockPos slotOwnerPos;
    private final int slotIndex;

    private IPatternDetails details = null;

    public PatternSlot(final BlockPos slotOwnerPos, final int slotIndex) {
        this.slotOwnerPos = slotOwnerPos;
        this.slotIndex = slotIndex;
        setSlotTex(TextureProperties.of(GuiEFabricatorController.TEXTURES_INVENTORY, 1, 120, 18, 18));
    }

    @Override
    public void render(final WidgetGui widgetGui, final RenderSize renderSize, final RenderPos renderPos, final MousePos mousePos) {
        final ItemStack prevStack = stackInSlot;
        if (details != null) {
            GenericStack primaryOutput = details.getPrimaryOutput();
            if (primaryOutput.what() instanceof AEItemKey itemKey) {
                final int amount = (int) Math.min(Integer.MAX_VALUE, primaryOutput.amount());
                stackInSlot = itemKey.toStack(amount);
            } else {
                stackInSlot = GenericStack.wrapInItemStack(primaryOutput);
            }
        }
        super.render(widgetGui, renderSize, renderPos, mousePos);
        stackInSlot = prevStack;
    }

    @Override
    public boolean onMouseClick(final MousePos mousePos, final RenderPos renderPos, final int mouseButton) {
        if (stackInSlot.isEmpty()) {
            return false;
        }
        NovaEngineeringCore.NET_CHANNEL.sendToServer(new PktEFabricatorPatternSearchGUIAction(
            PktEFabricatorPatternSearchGUIAction.Action.PICKUP_PATTERN, slotOwnerPos, slotIndex
        ));
        return true;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof final PatternSlot other)) {
            return false;
        }
        return slotOwnerPos.equals(other.slotOwnerPos) && slotIndex == other.slotIndex;
    }

    @Override
    public SlotItemVirtual setStackInSlot(final ItemStack stackInSlot) {
        AEItemKey key = AEItemKey.of(stackInSlot);
        if (key != null) {
            WeakReference<IPatternDetails> ref = DETAILS_CACHE.get(key);
            IPatternDetails cachedDetails = ref == null ? null : ref.get();
            if (cachedDetails != null) {
                this.details = cachedDetails;
            } else {
                this.details = PatternDetailsHelper.decodePattern(stackInSlot, Minecraft.getMinecraft().world);
                DETAILS_CACHE.put(key, new WeakReference<>(this.details));
            }
        } else {
            this.details = null;
        }

        return super.setStackInSlot(stackInSlot);
    }

    @Nullable
    public IPatternDetails getDetails() {
        return details;
    }

    @Override
    public int hashCode() {
        return Objects.hash(slotOwnerPos, slotIndex);
    }

}
