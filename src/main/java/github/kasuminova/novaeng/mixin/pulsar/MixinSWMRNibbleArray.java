package github.kasuminova.novaeng.mixin.pulsar;

import com.sumirelabs.pulsar.light.SWMRNibbleArray;
import com.sumirelabs.pulsar.light.SWMRNibbleArray.SaveState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;

@Mixin(value = SWMRNibbleArray.class, remap = false)
public abstract class MixinSWMRNibbleArray {
    @Unique
    private static final VarHandle NOVAENG$WORDS = MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.nativeOrder());
    @Unique
    private static final SaveState NOVAENG$EMPTY_SAVE = new SaveState(null, SWMRNibbleArray.INIT_STATE_UNINIT);
    @Shadow
    private volatile int stateVisible;
    @Shadow
    private volatile byte[] storageVisible;

    /**
     * @author NovaEngineering
     * @reason Scan saved light eight bytes at a time, as in Pulsar 56896c7.
     */
    @Overwrite
    private static boolean isAllZero(final byte[] data) {
        for (int offset = 0; offset < SWMRNibbleArray.ARRAY_SIZE; offset += Long.BYTES) {
            if ((long) NOVAENG$WORDS.get(data, offset) != 0L) {
                return false;
            }
        }
        return true;
    }

    /**
     * @author NovaEngineering
     * @reason Reuse the immutable empty save state while retaining byte ownership for nonempty snapshots.
     */
    @Overwrite
    public synchronized SaveState getSaveState() {
        final int state = this.stateVisible;
        if (state == SWMRNibbleArray.INIT_STATE_NULL) return null;
        if (state == SWMRNibbleArray.INIT_STATE_UNINIT) return NOVAENG$EMPTY_SAVE;
        final byte[] data = this.storageVisible;
        if (isAllZero(data)) {
            return state == SWMRNibbleArray.INIT_STATE_INIT ? NOVAENG$EMPTY_SAVE : null;
        }
        return new SaveState(data.clone(), state);
    }
}
