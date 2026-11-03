package github.kasuminova.novaeng.mixin.crafttweaker;

import crafttweaker.CraftTweakerAPI;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.lang.StackWalker.Option;
import java.lang.StackWalker.StackFrame;
import java.util.EnumSet;
import java.util.Iterator;

/**
 * Captures only the script frame CraftTweaker consumes instead of materializing the whole thread stack.
 */
@Mixin(value = CraftTweakerAPI.class, remap = false)
public abstract class MixinCraftTweakerScriptPosition {

    @Unique
    private static final StackWalker NOVA_SCRIPT_WALKER = StackWalker.getInstance(
        EnumSet.of(Option.SHOW_REFLECT_FRAMES, Option.SHOW_HIDDEN_FRAMES));
    @Unique
    private static final StackTraceElement[] NOVA_NO_SCRIPT_FRAME = new StackTraceElement[0];

    @Redirect(method = "getScriptFileAndLine()Ljava/lang/String;",
        at = @At(value = "INVOKE",
            target = "Ljava/lang/Thread;getStackTrace()[Ljava/lang/StackTraceElement;",
            remap = false), remap = false, require = 1)
    private static StackTraceElement[] nova$firstScriptFrame(final Thread thread) {
        return NOVA_SCRIPT_WALKER.walk(frames -> {
            final Iterator<StackFrame> iterator = frames.iterator();
            while (iterator.hasNext()) {
                final StackFrame frame = iterator.next();
                final String fileName = frame.getFileName();
                if (fileName != null && fileName.endsWith(".zs")) {
                    return new StackTraceElement[]{frame.toStackTraceElement()};
                }
            }
            return NOVA_NO_SCRIPT_FRAME;
        });
    }
}
