package github.kasuminova.novaeng.mixin.opticheck;

import net.kettlemc.opticheck.OptiCheckConfig;
import net.kettlemc.opticheck.OptiCheckScreen;
import net.kettlemc.opticheck.Utils;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = OptiCheckScreen.class)
public class MixinOptiCheckScreen extends GuiScreen {

    /**
     * @author circulaiton
     * @reason i18n支持
     */
    @Overwrite
    public void initGui() {
        if (OptiCheckConfig.mode == 0) {
            this.buttonList.add(new GuiButton(0, this.width / 2 - 154, this.height / 2 + 96, 144, 20, Utils.color(I18n.format("text.key.continue"))));
        } else {
            this.buttonList.clear();
            this.buttonList.add(new GuiButton(0, this.width / 2 - 154, this.height / 2 + 96, 144, 20, Utils.color(I18n.format("text.key.quit"))));
        }

        this.buttonList.add(new GuiButton(1, this.width / 2 + 10, this.height / 2 + 96, 144, 20, Utils.color(I18n.format("text.key.link"))));
    }

    /**
     * @author circulaiton
     * @reason i18n支持
     */
    @Overwrite
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        GlStateManager.disableTexture2D();
        GlStateManager.enableTexture2D();
        this.drawDefaultBackground();
        this.drawCenteredString(this.fontRenderer, Utils.color(I18n.format("text.tooltip.title")), this.width / 2, this.height / 2 - 100, 16777215);
        int i = 0;
        for (String text : novaEngineering_Core$getString()) {
            Utils.handleGuiText(Utils.color(text), this.fontRenderer, this, this.width, this.height + i * 20);
            i++;
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Unique
    public String[] novaEngineering_Core$getString() {
        return I18n.format("text.tooltip.message").split("#n");
    }

}
