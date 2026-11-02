package github.kasuminova.novaeng.client.gui.widget.efabricator;

import github.kasuminova.mmce.client.gui.widget.MultiLineLabel;
import github.kasuminova.mmce.client.gui.widget.container.Row;
import github.kasuminova.mmce.client.gui.widget.event.GuiEvent;
import github.kasuminova.novaeng.client.gui.widget.efabricator.event.EFGUIDataUpdateEvent;
import github.kasuminova.novaeng.common.crafttweaker.util.NovaEngUtils;
import github.kasuminova.novaeng.common.handler.EFabricatorEventHandler;
import it.unimi.dsi.fastutil.doubles.DoubleArrayFIFOQueue;
import net.minecraft.client.resources.I18n;

import java.util.Collections;

public class TotalCraftedLabel extends Row {

    private final MultiLineLabel label;
    private final DoubleArrayFIFOQueue history = new DoubleArrayFIFOQueue(11);
    private long value = 0;
    private double historyTotal;

    public TotalCraftedLabel() {
        this.label = new MultiLineLabel(Collections.singletonList(
            I18n.format("gui.efabricator.total_crafted", 0)
        ));
        this.label.setScale(.8f);
        this.label.setAutoWrap(false);
        this.addWidget(this.label);
    }

    @Override
    public boolean onGuiEvent(final GuiEvent event) {
        if (event instanceof EFGUIDataUpdateEvent efGUIEvent) {
            onValueUpdate(efGUIEvent.getEFGui().getData().totalCrafted());
        }
        return super.onGuiEvent(event);
    }

    public void onValueUpdate(final long newValue) {
        if (history.size() >= 10) {
            historyTotal -= history.dequeueLastDouble();
        }

        int added = (int) (newValue - value);
        if (value != 0 && added > 0) {
            double craftPerTick = ((added * (20F / EFabricatorEventHandler.UPDATE_INTERVAL)) / 20F);
            history.enqueueFirst(craftPerTick);
            historyTotal += craftPerTick;
            double avgCraftPerTick = averageHistory();
            updateLabel(newValue, avgCraftPerTick);
        } else {
            history.enqueueFirst(0D);
            double avgCraftPerTick = averageHistory();
            if (avgCraftPerTick > 0) {
                updateLabel(newValue, avgCraftPerTick);
            } else {
                this.label.setContents(Collections.singletonList(
                    I18n.format("gui.efabricator.total_crafted", NovaEngUtils.formatDecimal(newValue))
                ));
            }
        }
        value = newValue;
    }

    private void updateLabel(final long newValue, final double avgCraftPerTick) {
        this.label.setContents(Collections.singletonList(
            I18n.format("gui.efabricator.total_crafted.update",
                NovaEngUtils.formatDecimal(newValue),
                avgCraftPerTick <= 0.1 ? "<0.1" : NovaEngUtils.formatDouble(avgCraftPerTick, 1)
            )
        ));
    }

    private double averageHistory() {
        if (history.isEmpty()) return 0D;
        return historyTotal / history.size();
    }

}
