package github.kasuminova.novaeng.mixin.pulsar;

import com.sumirelabs.pulsar.light.LightDataSerializer;
import com.sumirelabs.pulsar.light.SWMRNibbleArray;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.Constants;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = LightDataSerializer.class, remap = false)
public abstract class MixinLightDataSerializer {
    @Unique
    private static final int NOVAENG$LIGHT_VERSION = 15;

    /**
     * @author NovaEngineering
     * @reason Invalidate pre-4dbd94e scalar snapshots on the first load after the slab fix.
     */
    @Overwrite
    static boolean hasCompatibleGameplayLight(final NBTTagCompound root, final boolean projected) {
        return root.getInteger("version") == NOVAENG$LIGHT_VERSION
            && root.hasKey("rgbMaxGameplayLight", Constants.NBT.TAG_BYTE)
            && root.getBoolean("rgbMaxGameplayLight") == projected;
    }

    /**
     * @author NovaEngineering
     * @reason Reject invalid state bytes before restoring saved light, as in Pulsar f5eede8.
     */
    @Overwrite
    private static SWMRNibbleArray restoreNibble(final NBTTagCompound section, final String stateTag, final String dataTag) {
        final int state = section.getByte(stateTag);
        if (state < SWMRNibbleArray.INIT_STATE_NULL || state > SWMRNibbleArray.INIT_STATE_HIDDEN) {
            throw new IllegalArgumentException("Invalid light nibble state: " + state);
        }
        final byte[] data = section.hasKey(dataTag, Constants.NBT.TAG_BYTE_ARRAY) ? section.getByteArray(dataTag) : null;
        if (data != null && data.length != SWMRNibbleArray.ARRAY_SIZE) {
            throw new IllegalStateException("Light nibble of wrong length: " + data.length);
        }
        // The NBT event owns its bytes. fromOwned transfers the separate array to the engine.
        return SWMRNibbleArray.fromOwned(data == null ? null : data.clone(), state);
    }

    @Redirect(method = "saveLight", at = @At(value = "INVOKE", ordinal = 1,
        target = "Lnet/minecraft/nbt/NBTTagCompound;setInteger(Ljava/lang/String;I)V", remap = true),
        remap = false, require = 1)
    private void novaeng$writeLightVersion(final NBTTagCompound root, final String key, final int oldVersion) {
        root.setInteger(key, NOVAENG$LIGHT_VERSION);
    }
}
