package github.kasuminova.novaeng.client.gui.widget.msa.overlay;

import github.kasuminova.novaeng.client.gui.widget.msa.slot.SlotCPU;
import github.kasuminova.novaeng.novaeng_core.Tags;
import net.minecraft.util.ResourceLocation;

public class OverlayCPU extends SlotConditionTextureOverlay {
    public static final ResourceLocation TEX_LOCATION = new ResourceLocation(Tags.MOD_ID, "textures/gui/msa_elements.png");

    public static final int TEX_X = 234;
    public static final int TEX_Y = 0;

    public static final int WIDTH = 22;
    public static final int HEIGHT = 14;

    public OverlayCPU(final SlotCPU slotCPU) {
        super(slotCPU);
        this.texLocation = TEX_LOCATION;
        this.textureX = TEX_X;
        this.textureY = TEX_Y;
        this.width = WIDTH;
        this.height = HEIGHT;
    }

}
