package github.kasuminova.novaeng.mixin.minecraft.forge;

import net.minecraft.util.text.translation.LanguageMap;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.relauncher.FMLLaunchHandler;
import net.minecraftforge.fml.server.FMLServerHandler;
import org.apache.commons.io.IOUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@SuppressWarnings("MethodMayBeStatic")
@Mixin(value = FMLServerHandler.class, remap = false)
public class MixinFMLServerHandler {

    @Inject(method = "addModAsResource", at = @At("RETURN"))
    private void novaeng_core$addCNLangFile(final ModContainer container, final CallbackInfo ci) {
        String langFile = "assets/" + container.getModId().toLowerCase() + "/lang/zh_cn.lang";
        String langFile2 = "assets/" + container.getModId().toLowerCase() + "/lang/zh_cn.lang";
        File source = container.getSource();
        InputStream stream = null;
        ZipFile zip = null;
        try {
            if (source.isDirectory() && FMLLaunchHandler.isDeobfuscatedEnvironment()) {
                File f = new File(source.toURI().resolve(langFile).getPath());
                if (!f.exists())
                    f = new File(source.toURI().resolve(langFile2).getPath());
                if (!f.exists())
                    throw new FileNotFoundException(source.toURI().resolve(langFile).getPath());
                stream = new FileInputStream(f);
            } else if (source.exists()) {
                zip = new ZipFile(source);
                ZipEntry entry = zip.getEntry(langFile);
                if (entry == null) {
                    entry = zip.getEntry(langFile2);
                }
                if (entry == null) {
                    throw new FileNotFoundException(langFile);
                }
                stream = zip.getInputStream(entry);
            }
            if (stream != null) {
                LanguageMap.inject(stream);
            }
        } catch (IOException e) {
            // hush
        } catch (Exception e) {
            FMLLog.log.error(e);
        } finally {
            IOUtils.closeQuietly(stream);
            IOUtils.closeQuietly(zip);
        }
    }

    /**
     * @author Kasumi_Nova
     * @reason en_US -> zh_CN
     */
    @Overwrite
    public String getCurrentLanguage() {
        return "zh_CN";
    }

}
