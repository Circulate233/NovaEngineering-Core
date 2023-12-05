package github.kasuminova.novaeng.client.gui.widget.msa;

import github.kasuminova.mmce.client.gui.widget.base.WidgetController;
import github.kasuminova.mmce.client.gui.widget.container.Column;
import github.kasuminova.mmce.client.gui.widget.container.Row;
import github.kasuminova.novaeng.NovaEngineeringCore;
import github.kasuminova.novaeng.client.gui.GuiModularServerAssembler;
import github.kasuminova.novaeng.client.gui.widget.msa.overlay.OverlayCalculateCardExt;
import github.kasuminova.novaeng.client.gui.widget.msa.slot.SlotCalculateCard;
import github.kasuminova.novaeng.client.gui.widget.msa.slot.SlotCalculateCardExtension;
import github.kasuminova.novaeng.common.container.slot.AssemblySlotManager;
import github.kasuminova.novaeng.common.hypernet.proc.server.assembly.AssemblyInvCalculateCardConst;
import net.minecraft.util.ResourceLocation;

public class AssemblyInvCalculateCard extends AssemblyInv {

    public static final int CLOSED_WIDTH = 27;
    public static final int CLOSED_HEIGHT = 26;

    public static final int OPENED_WIDTH = 104;
    public static final int OPENED_HEIGHT = 86;

    public static final int BUTTON_TEX_X = 37;

    public AssemblyInvCalculateCard(final AssemblyInvManager assemblyInvManager, final WidgetController widgetController) {
        super(assemblyInvManager, widgetController);
        this.width = CLOSED_WIDTH;
        this.height = CLOSED_HEIGHT;

        this.openedBgTexLocation = new ResourceLocation(NovaEngineeringCore.MOD_ID, "textures/gui/msa_calculate_card.png");
        this.openedInvBgTexWidth = OPENED_WIDTH;
        this.openedInvBgTexHeight = OPENED_HEIGHT;
        this.openedInvBgTexOffsetX = 0;
        this.openedInvBgTexOffsetY = 0;

        this.closedBgTexLocation = GuiModularServerAssembler.TEXTURES_ELEMENTS;
        this.closedInvBgTexWidth = CLOSED_WIDTH;
        this.closedInvBgTexHeight = CLOSED_HEIGHT;
        this.closedInvBgTexOffsetX = 29;
        this.closedInvBgTexOffsetY = 183;

        this.open.setTextureLocation(GuiModularServerAssembler.TEXTURES_ELEMENTS);
        this.open.setMargin(5, 0, 4, 0);
        this.open.setWidth(18).setHeight(18);
        this.open.setTextureXY(BUTTON_TEX_X, 237);
        this.open.setHoveredTextureXY(37, 219);

        AssemblySlotManager slotManager = assemblyInvManager.slotManager;

        SlotCalculateCardExtension ext_0_0 = new SlotCalculateCardExtension(AssemblyInvCalculateCardConst.EXTENSION_SLOT_0_ID, slotManager);
        SlotCalculateCard slot_0_0 = new SlotCalculateCard(0, slotManager);
        SlotCalculateCard slot_0_1 = new SlotCalculateCard(1, slotManager);
        SlotCalculateCard slot_0_2 = new SlotCalculateCard(2, slotManager);
        SlotCalculateCard slot_0_3 = new SlotCalculateCard(3, slotManager);

        SlotCalculateCardExtension ext_1_0 = new SlotCalculateCardExtension(AssemblyInvCalculateCardConst.EXTENSION_SLOT_1_ID, slotManager);
        SlotCalculateCard slot_1_0 = new SlotCalculateCard(4, slotManager);
        SlotCalculateCard slot_1_1 = new SlotCalculateCard(5, slotManager);
        SlotCalculateCard slot_1_2 = new SlotCalculateCard(6, slotManager);
        SlotCalculateCard slot_1_3 = new SlotCalculateCard(7, slotManager);

        SlotCalculateCardExtension ext_2_0 = new SlotCalculateCardExtension(AssemblyInvCalculateCardConst.EXTENSION_SLOT_2_ID, slotManager);
        SlotCalculateCard slot_2_0 = new SlotCalculateCard(8, slotManager);
        SlotCalculateCard slot_2_1 = new SlotCalculateCard(9, slotManager);
        SlotCalculateCard slot_2_2 = new SlotCalculateCard(10, slotManager);
        SlotCalculateCard slot_2_3 = new SlotCalculateCard(11, slotManager);

        SlotCalculateCardExtension ext_3_0 = new SlotCalculateCardExtension(AssemblyInvCalculateCardConst.EXTENSION_SLOT_3_ID, slotManager);
        SlotCalculateCard slot_3_0 = new SlotCalculateCard(12, slotManager);
        SlotCalculateCard slot_3_1 = new SlotCalculateCard(13, slotManager);
        SlotCalculateCard slot_3_2 = new SlotCalculateCard(14, slotManager);
        SlotCalculateCard slot_3_3 = new SlotCalculateCard(15, slotManager);

        slotColum.addWidgets(new Row().addWidgets(ext_0_0, slot_0_0, slot_0_1, slot_0_2, slot_0_3).setMarginLeft(7).setMarginUp(7));
        slotColum.addWidgets(new Row().addWidgets(ext_1_0, slot_1_0, slot_1_1, slot_1_2, slot_1_3).setMarginLeft(7));
        slotColum.addWidgets(new Row().addWidgets(ext_2_0, slot_2_0, slot_2_1, slot_2_2, slot_2_3).setMarginLeft(7));
        slotColum.addWidgets(new Row().addWidgets(ext_3_0, slot_3_0, slot_3_1, slot_3_2, slot_3_3).setMarginLeft(7));

        Column slotCalculateCardExtOverlay = new Column();
        slotCalculateCardExtOverlay.setAbsX(147).setAbsY(59).addWidgets(
                new OverlayCalculateCardExt(ext_0_0),
                new OverlayCalculateCardExt(ext_1_0),
                new OverlayCalculateCardExt(ext_2_0),
                new OverlayCalculateCardExt(ext_3_0)
        );
        widgetController.addWidgetContainer(slotCalculateCardExtOverlay);
    }

    @Override
    public void openInv() {
        super.openInv();

        this.width = OPENED_WIDTH;
        this.height = OPENED_HEIGHT;
    }

    @Override
    public void closeInv() {
        super.closeInv();

        this.width = CLOSED_WIDTH;
        this.height = CLOSED_HEIGHT;
    }
}