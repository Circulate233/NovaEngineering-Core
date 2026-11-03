package github.kasuminova.novaeng.mixin.neofontrender;

import github.kasuminova.novaeng.client.gui.NavigationLists;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSlot;
import net.minecraftforge.fml.client.GuiScrollingList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

// Package-private target; its existing signature/revision checks still control rebuilding.
@Mixin(targets = "neofontrender.addons.navigation.vanilla.VanillaGuiTreeSession", remap = false)
public abstract class MixinVanillaGuiTreeSession {
    @Unique
    private final NavigationLists nova$navigationLists = new NavigationLists();

    @Redirect(method = "refresh", at = @At(value = "INVOKE",
        target = "Lneofontrender/addons/navigation/vanilla/VanillaGuiTreeSession;lists(Lnet/minecraft/client/gui/GuiScreen;)Ljava/util/List;"), require = 1)
    private List<GuiSlot> nova$collectNavigationLists(final GuiScreen screen) {
        return nova$navigationLists.collect(screen);
    }

    @Redirect(method = "refresh", at = @At(value = "INVOKE",
        target = "Lneofontrender/addons/navigation/vanilla/VanillaGuiTreeSession;forgeLists(Lnet/minecraft/client/gui/GuiScreen;)Ljava/util/List;"), require = 1)
    private List<GuiScrollingList> nova$reuseForgeLists(final GuiScreen screen) {
        return nova$navigationLists.forgeLists();
    }

    @Inject(method = "close", at = @At("HEAD"), require = 1)
    private void nova$releaseNavigationLists(final CallbackInfo ci) {
        nova$navigationLists.clear();
    }
}
