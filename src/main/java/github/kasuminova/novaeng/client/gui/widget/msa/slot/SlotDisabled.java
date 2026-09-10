package github.kasuminova.novaeng.client.gui.widget.msa.slot;

import github.kasuminova.novaeng.common.container.slot.SlotConditionItemHandler;
import github.kasuminova.novaeng.novaeng_core.Tags;
import net.minecraft.util.ResourceLocation;

public class SlotDisabled extends SlotDynamic<SlotConditionItemHandler> {
    public static final ResourceLocation TEX_LOCATION = new ResourceLocation(Tags.MOD_ID, "textures/gui/msa_cpu.png");
    public static final int TEX_X = 23;
    public static final int TEX_Y = 7;

    public SlotDisabled() {
        super(-1);
        this.texLocation = TEX_LOCATION;
        this.unavailableTexLocation = TEX_LOCATION;
        this.textureX = TEX_X;
        this.textureY = TEX_Y;
        this.unavailableTextureX = TEX_X;
        this.unavailableTextureY = TEX_Y;
    }

    @Override
    protected SlotConditionItemHandler getSlot() {
        return null;
    }

    @Override
    public boolean isAvailable() {
        return false;
    }
}
