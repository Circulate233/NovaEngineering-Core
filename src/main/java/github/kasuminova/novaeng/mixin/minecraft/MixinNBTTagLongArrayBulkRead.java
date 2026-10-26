package github.kasuminova.novaeng.mixin.minecraft;

import github.kasuminova.novaeng.common.nbt.NovaNbtLongArrayReader;
import net.minecraft.nbt.NBTSizeTracker;
import net.minecraft.nbt.NBTTagLongArray;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;

@Mixin(NBTTagLongArray.class)
public abstract class MixinNBTTagLongArrayBulkRead {
    @Shadow
    public long[] data;

    /**
     * @author circulation
     * @reason Bulk-read large disk NBT arrays while preserving the vanilla path for network and
     * small inputs. DataInputStream is deliberately required because the block reader must not
     * change the behavior of custom DataInput implementations.
     */
    @Overwrite
    void read(final DataInput input, final int depth, final NBTSizeTracker sizeTracker) throws IOException {
        sizeTracker.read(192L);
        final int length = input.readInt();
        sizeTracker.read(64L * length);
        final long[] target = new long[length];

        if (NovaNbtLongArrayReader.worthBulking(length) && input instanceof DataInputStream) {
            NovaNbtLongArrayReader.read((DataInputStream) input, target, length);
        } else {
            for (int index = 0; index < length; index++) {
                target[index] = input.readLong();
            }
        }

        this.data = target;
    }
}
