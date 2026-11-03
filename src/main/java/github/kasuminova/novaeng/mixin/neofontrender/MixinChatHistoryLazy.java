package github.kasuminova.novaeng.mixin.neofontrender;

import neofontrender.addons.chat.ChatHistoryManager;
import neofontrender.addons.chat.ChatHistoryStore;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ChatHistoryManager.class, remap = false)
public abstract class MixinChatHistoryLazy {
    @Unique
    private volatile boolean nova$pendingOpen;
    @Unique
    private boolean nova$opening;
    @Unique
    private boolean nova$opened;
    @Unique
    private boolean nova$pendingConfig;

    @Shadow
    private static boolean persistenceEnabled() {
        throw new AssertionError();
    }

    @Shadow
    private static String currentScope(final Minecraft mc) {
        throw new AssertionError();
    }

    @Shadow
    public abstract void initialize();

    @Shadow
    public abstract void configChanged();

    @Inject(method = "initialize", at = @At("HEAD"), cancellable = true, require = 1)
    private void nova$deferOpen(final CallbackInfo ci) {
        if (!nova$opening && !nova$opened) {
            nova$pendingOpen = true;
            ci.cancel();
        }
    }

    @Inject(method = "configChanged", at = @At("HEAD"), require = 1)
    private void nova$rememberTrim(final CallbackInfo ci) {
        if (nova$pendingOpen) {
            nova$pendingConfig = true;
        }
    }

    @Inject(method = {"recordReceived", "recordSent", "restore"}, at = @At("HEAD"), require = 3)
    private void nova$beforeMessage(final CallbackInfo ci) {
        if (nova$pendingOpen && persistenceEnabled() && currentScope(Minecraft.getMinecraft()) != null) {
            nova$ensureStore();
        }
    }

    @Inject(method = "store", at = @At("HEAD"), require = 1)
    private void nova$beforeStoreAccess(final CallbackInfoReturnable<ChatHistoryStore> cir) {
        nova$ensureStore();
    }

    @Unique
    private void nova$ensureStore() {
        if (!nova$pendingOpen) {
            return;
        }
        synchronized (this) {
            if (!nova$pendingOpen || nova$opening) {
                return;
            }
            nova$opening = true;
            try {
                // Original initialize handles opening failures and performs legacy migration first.
                initialize();
                nova$opened = true;
                nova$pendingOpen = false;
                if (nova$pendingConfig) {
                    configChanged();
                    nova$pendingConfig = false;
                }
            } finally {
                nova$opening = false;
            }
        }
    }
}
