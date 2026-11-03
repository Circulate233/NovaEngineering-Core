package github.kasuminova.novaeng.common.handler;

import github.kasuminova.novaeng.common.performance.PrimerOreChunk;
import github.kasuminova.novaeng.common.performance.PrimerOreGeneration;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagLongArray;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Arrays;

public final class PrimerOreEvents {
    public static final PrimerOreEvents INSTANCE = new PrimerOreEvents();
    private static final String KEY = "NovaPrimerOreRulesV1";

    private PrimerOreEvents() {
    }

    public static void read(final PrimerOreChunk chunk, final NBTTagCompound data) {
        final NBTBase stored = data.getTag(KEY);
        if (stored instanceof NBTTagLongArray array) {
            final long[] rules = array.data.clone();
            Arrays.sort(rules);
            chunk.nova$setPrimerOreRules(rules);
        }
    }

    public static void write(final PrimerOreChunk chunk, final NBTTagCompound data) {
        final long[] rules = chunk.nova$getPrimerOreRules();
        if (rules != null && rules.length != 0) data.setTag(KEY, new NBTTagLongArray(rules));
    }

    @SubscribeEvent
    public void onLoad(final ChunkDataEvent.Load event) {
        if (event.getChunk() instanceof PrimerOreChunk chunk) read(chunk, event.getData());
    }

    @SubscribeEvent
    public void onSave(final ChunkDataEvent.Save event) {
        if (event.getChunk() instanceof PrimerOreChunk chunk) write(chunk, event.getData());
    }

    @SubscribeEvent
    public void onUnload(final WorldEvent.Unload event) {
        PrimerOreGeneration.unload(event.getWorld());
    }
}
