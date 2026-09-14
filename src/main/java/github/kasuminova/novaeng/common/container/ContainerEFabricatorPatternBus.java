package github.kasuminova.novaeng.common.container;

import ae2.container.AEBaseContainer;
import ae2.container.slot.RestrictedInputSlot;
import ae2.util.inv.AppEngInternalInventory;
import github.kasuminova.novaeng.common.tile.ecotech.efabricator.EFabricatorPatternBus;
import lombok.Getter;
import net.minecraft.entity.player.EntityPlayer;

public class ContainerEFabricatorPatternBus extends AEBaseContainer {

    public static final int ROWS = 6;
    public static final int COLS = 12;

    @Getter
    private final EFabricatorPatternBus owner;

    public ContainerEFabricatorPatternBus(final EFabricatorPatternBus owner, final EntityPlayer player) {
        super(player.inventory, owner);
        this.owner = owner;

        this.addPlayerInventorySlots(27, 150);

        AppEngInternalInventory patterns = owner.getPatterns();
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                this.addSlotToContainer(new RestrictedInputSlot(RestrictedInputSlot.PlacableItemType.ENCODED_PATTERN, patterns,
                    (row * COLS) + col, 8 + (col * 18), 28 + (row * 18)));
            }
        }
    }

}
