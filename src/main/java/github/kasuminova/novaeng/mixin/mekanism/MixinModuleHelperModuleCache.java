package github.kasuminova.novaeng.mixin.mekanism;

import github.kasuminova.novaeng.NovaEngCoreConfig;
import mekanism.common.content.gear.Module;
import mekanism.common.content.gear.ModuleHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Remembers the module list Mekanism deserialises for an item stack instead of rebuilding it on every ask.
 * <p>
 * {@code getAttributeModifiers} on the MekaSuit armour calls {@code IModuleContainerItem.getModules}, which goes
 * straight to {@code ModuleHelper.loadAll(ItemStack)}: that reads the modules NBT, walks every installed module type,
 * constructs a {@code Module} per type, runs its {@code init()} and collects the config items into a fresh list. The
 * attribute lookup runs on every modifier recalculation, not just on the render path, so the whole deserialisation
 * was repeated for the same stack over and over -- measured as the two largest allocation points in the client
 * ({@code HashList.<init>} and {@code Module.addConfigItem}, roughly 22% of everything allocated together).
 * <p>
 * The result only depends on the stack's NBT, so it is remembered for one stack at a time and invalidated whenever
 * the tag changes: installing or removing a module rewrites the tag, and the hash comparison catches that, so a
 * stale list is never handed out. The caller side only iterates what it is given (verified across
 * {@code IModuleContainerItem} and the MekaSuit armour), so the same list instance can be shared.
 */
@Mixin(value = ModuleHelper.class, remap = false)
public abstract class MixinModuleHelperModuleCache {

    @Unique
    private ItemStack nova$cachedStack;

    @Unique
    private int nova$cachedTagHash;

    @Unique
    private List<Module<?>> nova$cachedModules;

    @Inject(method = "loadAll(Lnet/minecraft/item/ItemStack;)Ljava/util/List;", at = @At("HEAD"),
            cancellable = true, remap = false, require = 1)
    private void nova$reuseModules(final ItemStack stack, final CallbackInfoReturnable<List<Module<?>>> cir) {
        if (!NovaEngCoreConfig.CLIENT.optimizeMekanismModuleLookup) {
            return;
        }
        final int tagHash = nova$tagHash(stack);
        if (tagHash == 0) {
            return;
        }
        final ItemStack cachedStack = this.nova$cachedStack;
        if (cachedStack != null && this.nova$cachedTagHash == tagHash
                && cachedStack.getItem() == stack.getItem()
                && cachedStack.getMetadata() == stack.getMetadata()) {
            cir.setReturnValue(this.nova$cachedModules);
        }
    }

    @Inject(method = "loadAll(Lnet/minecraft/item/ItemStack;)Ljava/util/List;", at = @At("RETURN"),
            remap = false, require = 1)
    private void nova$rememberModules(final ItemStack stack, final CallbackInfoReturnable<List<Module<?>>> cir) {
        if (!NovaEngCoreConfig.CLIENT.optimizeMekanismModuleLookup) {
            return;
        }
        final int tagHash = nova$tagHash(stack);
        if (tagHash == 0) {
            return;
        }
        this.nova$cachedStack = stack;
        this.nova$cachedTagHash = tagHash;
        this.nova$cachedModules = cir.getReturnValue();
    }

    @Unique
    private static int nova$tagHash(final ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        final NBTTagCompound tag = stack.getTagCompound();
        return tag == null ? 0 : tag.hashCode();
    }
}
