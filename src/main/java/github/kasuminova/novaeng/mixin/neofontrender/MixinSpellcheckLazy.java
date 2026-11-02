package github.kasuminova.novaeng.mixin.neofontrender;

import com.swabunga.spell.engine.SpellDictionary;
import com.swabunga.spell.event.SpellChecker;
import mnm.mods.tabbychat.extra.spell.LangDict;
import mnm.mods.tabbychat.extra.spell.Spellcheck;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Spellcheck.class, remap = false)
public abstract class MixinSpellcheckLazy {
    @Shadow
    private SpellChecker spellCheck;
    @Shadow
    private SpellDictionary userDict;
    @Shadow
    private LangDict language;
    @Unique
    private volatile boolean nova$pending;
    @Unique
    private boolean nova$used;
    @Unique
    private boolean nova$loading;

    @Shadow
    private void loadCurrentLanguage() {
        throw new AssertionError();
    }

    @Redirect(method = "<init>", at = @At(value = "INVOKE",
        target = "Lmnm/mods/tabbychat/extra/spell/Spellcheck;loadCurrentLanguage()V"), require = 1)
    private void nova$deferDictionary(final Spellcheck owner) {
        nova$pending = true;
    }

    @Redirect(method = "onResourceManagerReload", remap = true, at = @At(value = "INVOKE", remap = false,
        target = "Lmnm/mods/tabbychat/extra/spell/Spellcheck;loadCurrentLanguage()V"), require = 1)
    private void nova$reloadUsedDictionary(final Spellcheck owner) {
        synchronized (this) {
            nova$pending = true;
            if (nova$used) {
                nova$ensureDictionary();
            }
        }
    }

    @Inject(method = {"checkSpelling", "addToDictionary", "loadUserDictionary", "loadDictionary"},
        at = @At("HEAD"), require = 4)
    private void nova$beforeDictionaryUse(final CallbackInfo ci) {
        nova$ensureDictionary();
    }

    @Inject(method = "getLanguage", at = @At("HEAD"), require = 1)
    private void nova$beforeLanguageUse(final CallbackInfoReturnable<LangDict> cir) {
        nova$ensureDictionary();
    }

    @Unique
    private void nova$ensureDictionary() {
        if (!nova$pending) {
            return;
        }
        synchronized (this) {
            if (!nova$pending || nova$loading) {
                return;
            }
            nova$used = true;
            nova$loading = true;
            try {
                // Keep the original fallback, logger and user-dictionary installation order.
                loadCurrentLanguage();
                nova$pending = spellCheck == null || userDict == null || language == null;
            } finally {
                nova$loading = false;
            }
        }
    }
}
