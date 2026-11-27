package github.kasuminova.novaeng.common.container.slot;

import github.kasuminova.novaeng.common.util.TileItemHandler;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.Getter;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.SlotItemHandler;

import java.util.List;

public abstract class SlotConditionItemHandler extends SlotItemHandler {
    protected final int displayID;
    @Getter
    protected final List<SlotConditionItemHandler> dependencies = new ObjectArrayList<>();
    @Getter
    protected final List<SlotConditionItemHandler> dependents = new ObjectArrayList<>();
    @Getter
    protected final List<SlotConditionItemHandler> softDependents = new ObjectArrayList<>();
    @Getter
    protected final List<SlotConditionItemHandler> softDependencies = new ObjectArrayList<>();
    protected boolean enabled = false;
    @Getter
    protected boolean hovered = false;

    public SlotConditionItemHandler(final TileItemHandler inventoryIn, final int index, final int displayID) {
        super(inventoryIn, index, 0, 0);
        this.displayID = displayID;
    }

    // Overrides

    @Override
    public boolean canTakeStack(final EntityPlayer playerIn) {
        for (int index = 0; index < dependents.size(); index++) {
            final SlotConditionItemHandler dependent = dependents.get(index);
            if (dependent.isInstalled()) {
                return false;
            }
        }
        return super.canTakeStack(playerIn);
    }

    // Content

    public SlotConditionItemHandler dependsOn(SlotConditionItemHandler dependency) {
        if (this == dependency) {
            throw new IllegalArgumentException("Cannot add self as dependency!");
        }
        if (dependents.contains(dependency)) {
            throw new IllegalArgumentException("Circular dependency!");
        }
        if (!dependencies.contains(this)) {
            dependencies.add(dependency);
            dependency.addDependent(this);
        }
        return this;
    }

    public SlotConditionItemHandler softDependsOn(SlotConditionItemHandler softDependency) {
        if (this == softDependency) {
            throw new IllegalArgumentException("Cannot add self as soft dependency!");
        }
        if (softDependents.contains(softDependency)) {
            throw new IllegalArgumentException("Circular soft dependency!");
        }
        if (!softDependencies.contains(softDependency)) {
            softDependencies.add(softDependency);
            softDependency.addSoftDependent(this);
        }
        return this;
    }

    protected void addDependent(SlotConditionItemHandler dependent) {
        dependents.add(dependent);
    }

    protected void addSoftDependent(SlotConditionItemHandler dependent) {
        softDependents.add(dependent);
    }

    public boolean isInstalled() {
        return getHasStack() && isItemValid(getStack());
    }

    public boolean isAvailable() {
        for (int index = 0; index < dependencies.size(); index++) {
            final SlotConditionItemHandler dependency = dependencies.get(index);
            if (!dependency.isInstalled()) {
                return false;
            }
        }
        return true;
    }

    @SideOnly(Side.CLIENT)
    public List<String> getHoverTooltips() {
        String slotDesc = getSlotDescription();
        List<String> tooltips = new ObjectArrayList<>();
        tooltips.add(slotDesc);

        if (isInvalid()) {
            tooltips.add(I18n.format("gui.modular_server_assembler.assembly.invalid"));
            return tooltips;
        }

        if (!dependencies.isEmpty()) {
            tooltips.add(I18n.format("gui.modular_server_assembler.assembly.dependencies"));
            for (int index = 0; index < dependencies.size(); index++) {
                final SlotConditionItemHandler dependency = dependencies.get(index);
                if (dependency.isInstalled()) {
                    tooltips.add(dependency.getSlotDescription() + I18n.format("gui.modular_server_assembler.assembly.dependencies.installed"));
                } else {
                    tooltips.add(dependency.getSlotDescription() + I18n.format("gui.modular_server_assembler.assembly.dependencies.uninstalled"));
                }
            }
        }
        if (!dependents.isEmpty()) {
            tooltips.add(I18n.format("gui.modular_server_assembler.assembly.dependents"));
            for (int index = 0; index < dependents.size(); index++) {
                final SlotConditionItemHandler dependent = dependents.get(index);
                tooltips.add(dependent.getSlotDescription());
            }
        }
        if (!softDependencies.isEmpty()) {
            tooltips.add(I18n.format("gui.modular_server_assembler.assembly.soft_dependencies"));
            for (int index = 0; index < softDependencies.size(); index++) {
                final SlotConditionItemHandler dependency = softDependencies.get(index);
                if (dependency.isInstalled()) {
                    tooltips.add(dependency.getSlotDescription() + I18n.format("gui.modular_server_assembler.assembly.dependencies.installed"));
                } else {
                    tooltips.add(dependency.getSlotDescription() + I18n.format("gui.modular_server_assembler.assembly.dependencies.uninstalled"));
                }
            }
        }
        if (!softDependents.isEmpty()) {
            tooltips.add(I18n.format("gui.modular_server_assembler.assembly.soft_dependents"));
            for (int index = 0; index < softDependents.size(); index++) {
                final SlotConditionItemHandler dependent = softDependents.get(index);
                tooltips.add(dependent.getSlotDescription());
            }
        }

        return tooltips;
    }

    public abstract String getSlotDescription();

    @Override
    public TileItemHandler getItemHandler() {
        return (TileItemHandler) super.getItemHandler();
    }

    public boolean isInvalid() {
        ItemStack stackInSlot = getStack();
        return !stackInSlot.isEmpty() && !isItemValid(stackInSlot);
    }

    public SlotConditionItemHandler setHovered(final boolean hovered) {
        this.hovered = hovered;
        return this;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public SlotConditionItemHandler setEnabled(final boolean enabled) {
        this.enabled = enabled;
        return this;
    }
}
