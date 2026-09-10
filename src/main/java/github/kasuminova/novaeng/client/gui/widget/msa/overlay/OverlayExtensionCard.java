package github.kasuminova.novaeng.client.gui.widget.msa.overlay;

import github.kasuminova.novaeng.client.gui.widget.msa.slot.SlotExtensionCard;
import github.kasuminova.novaeng.novaeng_core.Tags;
import net.minecraft.util.ResourceLocation;

public class OverlayExtensionCard extends SlotConditionTextureOverlay {
    public static final ResourceLocation TEX_LOCATION = new ResourceLocation(Tags.MOD_ID, "textures/gui/msa_elements.png");

    public static final int TEX_X = 215;
    public static final int TEX_Y = 76;

    public static final int WIDTH = 7;
    public static final int HEIGHT = 14;

    public OverlayExtensionCard(final SlotExtensionCard slotExtensionCard) {
        super(slotExtensionCard);
        this.texLocation = TEX_LOCATION;
        this.textureX = TEX_X;
        this.textureY = TEX_Y;
        this.width = WIDTH;
        this.height = HEIGHT;
    }

}
