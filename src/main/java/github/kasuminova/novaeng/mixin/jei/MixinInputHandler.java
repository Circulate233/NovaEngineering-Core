package github.kasuminova.novaeng.mixin.jei;

import com.circulation.random_complement.client.handler.ItemTooltipHandler;
import github.kasuminova.novaeng.NovaEngineeringCore;
import github.kasuminova.novaeng.client.ClientProxy;
import github.kasuminova.novaeng.common.network.PktItemDisplay;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import lombok.val;
import mezz.jei.bookmarks.BookmarkItem;
import mezz.jei.bookmarks.BookmarkList;
import mezz.jei.gui.GuiScreenHelper;
import mezz.jei.gui.ghost.GhostIngredientDragManager;
import mezz.jei.gui.overlay.IngredientListOverlay;
import mezz.jei.gui.overlay.bookmarks.LeftAreaDispatcher;
import mezz.jei.ingredients.IngredientRegistry;
import mezz.jei.input.IClickedIngredient;
import mezz.jei.input.InputHandler;
import mezz.jei.input.MouseHelper;
import mezz.jei.runtime.JeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(value = InputHandler.class, remap = false)
public abstract class MixinInputHandler {

    @Shadow
    @Nullable
    protected abstract IClickedIngredient<?> getIngredientUnderMouseForKey(int mouseX, int mouseY);

    @Inject(method = "<init>", at = @At("TAIL"))
    public void onInit(JeiRuntime runtime, IngredientRegistry ingredientRegistry, IngredientListOverlay ingredientListOverlay, GuiScreenHelper guiScreenHelper, LeftAreaDispatcher leftAreaDispatcher, BookmarkList bookmarkList, GhostIngredientDragManager ghostIngredientDragManager, CallbackInfo ci) {
        ItemTooltipHandler.regItemTooltip(GuiScreen.class, () -> {
            val ing = this.getIngredientUnderMouseForKey(MouseHelper.getX(), MouseHelper.getY());
            if (ing == null) {
                if (!(Minecraft.getMinecraft().currentScreen instanceof GuiContainer gui))
                    return ObjectLists.emptyList();
                var slot = gui.getSlotUnderMouse();
                if (slot == null) return ObjectLists.emptyList();
                if (slot.getStack().isEmpty()) return ObjectLists.emptyList();
            }
            return ClientProxy.getItemDisplayTooltip();
        });
    }

    @Unique
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void novaEngineering_Core$onInputEvent(GuiScreenEvent.KeyboardInputEvent.Pre event) {
        int eventKey = Keyboard.getEventKey();
        if (GuiScreen.isCtrlKeyDown() && Keyboard.KEY_L == eventKey && Keyboard.isKeyDown(eventKey)) {
            val ing = this.getIngredientUnderMouseForKey(MouseHelper.getX(), MouseHelper.getY());
            if (ing == null) return;
            var v = ing.getValue();
            ItemStack stack = ItemStack.EMPTY;
            if (v instanceof ItemStack i) {
                stack = i;
            } else if (v instanceof BookmarkItem<?> b && b.ingredient instanceof ItemStack i) {
                stack = i;
            }
            if (stack.isEmpty()) {
                var mc = Minecraft.getMinecraft();
                if (mc.currentScreen instanceof GuiContainer gui) {
                    var slot = gui.getSlotUnderMouse();
                    if (slot != null) {
                        stack = slot.getStack();
                    }
                }
            }
            if (!stack.isEmpty())
                NovaEngineeringCore.NET_CHANNEL.sendToServer(new PktItemDisplay(stack, Minecraft.getMinecraft().player));
        }
    }

}