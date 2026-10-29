package github.kasuminova.novaeng.client.gui;

import appeng.client.gui.implementations.GuiCraftConfirm;
import appeng.helpers.WirelessTerminalGuiObject;
import github.kasuminova.novaeng.NovaEngineeringCore;
import github.kasuminova.novaeng.client.util.RCGuiCraftConfirm;
import github.kasuminova.novaeng.common.network.PktAutoCraftConfirm;
import github.kasuminova.novaeng.mixin.ae2.AccessorGuiCraftConfirm;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.InventoryPlayer;

import java.io.IOException;

public class GuiNEWCraftConfirm extends GuiCraftConfirm implements RCGuiCraftConfirm {
    private final AccessorGuiCraftConfirm utils = (AccessorGuiCraftConfirm) this;
    private GuiButton cancel;
    private GuiButton start;

    public GuiNEWCraftConfirm(final InventoryPlayer inventory, final WirelessTerminalGuiObject terminal) {
        super(inventory, terminal);
    }

    public GuiButton getCancel() {
        return this.cancel;
    }

    public void setCancel(final GuiButton cancel) {
        this.cancel = cancel;
    }

    public GuiButton getStart() {
        return this.start;
    }

    public void setStart(final GuiButton start) {
        this.start = start;
    }

    public AccessorGuiCraftConfirm getUtils() {
        return this.utils;
    }

    @Override
    public void initGui() {
        super.initGui();
        this.cancel = this.utils.getCancel();
        this.start = this.utils.getStart();
    }

    @Override
    protected void actionPerformed(final GuiButton button) throws IOException {
        if (button == this.cancel) {
            if (isShiftKeyDown()) {
                this.rc$addMissBookmark();
            }
            NovaEngineeringCore.NET_CHANNEL.sendToServer(new PktAutoCraftConfirm());
            this.mc.player.closeScreenAndDropStack();
            return;
        }
        super.actionPerformed(button);
        if (button == this.start) {
            this.mc.player.closeScreenAndDropStack();
        }
    }
}
