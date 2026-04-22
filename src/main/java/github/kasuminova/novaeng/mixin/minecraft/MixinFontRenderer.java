package github.kasuminova.novaeng.mixin.minecraft;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.Color;

@Mixin(FontRenderer.class)
public class MixinFontRenderer {

    @Shadow
    @Final
    private int[] colorCode;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(final GameSettings gameSettingsIn, final ResourceLocation location, final TextureManager textureManagerIn, final boolean unicode, final CallbackInfo ci) {
        // 修改字体颜色喵
        this.colorCode[0] = 0x000000;
        this.colorCode[1] = 0x1e90ff;
        this.colorCode[2] = 0x00c853;
        this.colorCode[3] = 0x4db6ac;
        this.colorCode[4] = 0xd32f2f;
        this.colorCode[5] = 0xe040fb;
        this.colorCode[6] = 0xffa726;
        this.colorCode[7] = 0xbdbdbd;
        this.colorCode[8] = 0x546e7a;
        this.colorCode[9] = 0x03a9f4;
        this.colorCode[10] = 0x69f0ae;
        this.colorCode[11] = 0x18ffff;
        this.colorCode[12] = 0xff5e62;
        this.colorCode[13] = 0xff80ab;
        this.colorCode[14] = 0xffeb3b;
        this.colorCode[15] = 0xffffff;

        for (int i = 0; i < 16; i++) {
            Color color = new Color(this.colorCode[i]);
            Color backgroundColor = new Color(color.getRed() / 4, color.getGreen() / 4, color.getBlue() / 4);
            this.colorCode[i + 16] = backgroundColor.getRGB();
        }
    }

}
