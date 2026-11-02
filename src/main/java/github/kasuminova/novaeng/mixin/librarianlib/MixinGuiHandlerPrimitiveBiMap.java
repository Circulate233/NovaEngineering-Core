package github.kasuminova.novaeng.mixin.librarianlib;

import com.teamwizardry.librarianlib.core.LibrarianLib;
import com.teamwizardry.librarianlib.features.container.ContainerBase;
import com.teamwizardry.librarianlib.features.container.GuiEntry;
import com.teamwizardry.librarianlib.features.container.GuiHandler;
import com.teamwizardry.librarianlib.features.container.internal.ContainerImpl;

import github.kasuminova.novaeng.common.util.collection.Object2IntBiMap;
import github.kasuminova.novaeng.common.util.collection.Object2IntHashBiMap;

import kotlin.jvm.functions.Function3;
import kotlin.jvm.internal.Intrinsics;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.Map;

@Mixin(value = GuiHandler.class, remap = false)
public abstract class MixinGuiHandlerPrimitiveBiMap {
    @Unique
    private static final Object2IntBiMap<ResourceLocation> nova$ids = new Object2IntHashBiMap<>();

    @Shadow
    @Final
    private static Map<ResourceLocation, GuiEntry> registry;

    /**
     * @author NovaEngineering
     * @reason Open registered GUIs with the primitive discriminator.
     */
    @Overwrite
    public static void open(
        final ResourceLocation name, final EntityPlayer player, final BlockPos pos) {
        Intrinsics.checkNotNullParameter(name, "name");
        Intrinsics.checkNotNullParameter(player, "player");
        Intrinsics.checkNotNullParameter(pos, "pos");
        if (!nova$ids.containsKey(name))
            throw new IllegalArgumentException("No GUI handler registered for " + name);
        player.openGui(
            LibrarianLib.INSTANCE,
            nova$ids.getIntRequired(name),
            player.world,
            pos.getX(),
            pos.getY(),
            pos.getZ());
    }

    /**
     * @author NovaEngineering
     * @reason Register GUI IDs without creating numeric wrappers.
     */
    @Overwrite
    public static void registerRaw(
        final ResourceLocation name,
        final Function3<
            ? super EntityPlayer,
            ? super World,
            ? super BlockPos,
            ? extends ContainerBase>
            server,
        final Function3<
            ? super EntityPlayer,
            ? super World,
            ? super BlockPos,
            ? extends GuiScreen>
            client) {
        Intrinsics.checkNotNullParameter(name, "name");
        if (!nova$ids.containsKey(name)) nova$ids.putDiscard(name, nova$ids.size(), false);
        if (registry.containsKey(name))
            throw new IllegalArgumentException("GUI handler for " + name + " already exists");
        registry.put(name, new GuiEntry(server, client));
    }

    /**
     * @author NovaEngineering
     * @reason Resolve the GUI entry with an unboxed ID.
     */
    @Overwrite
    public Object getClientGuiElement(
        final int ID,
        final EntityPlayer player,
        final World world,
        final int x,
        final int y,
        final int z) {
        Intrinsics.checkNotNullParameter(player, "player");
        Intrinsics.checkNotNullParameter(world, "world");
        if (!nova$ids.containsValue(ID)) return null;
        final GuiEntry entry = registry.get(nova$ids.inverse().get(ID));
        final var handler = entry == null ? null : entry.getClient();
        if (handler == null) return null;
        return handler.invoke(player, world, new BlockPos(x, y, z));
    }

    /**
     * @author NovaEngineering
     * @reason Resolve the GUI entry with an unboxed ID.
     */
    @Overwrite
    public Object getServerGuiElement(
        final int ID,
        final EntityPlayer player,
        final World world,
        final int x,
        final int y,
        final int z) {
        Intrinsics.checkNotNullParameter(player, "player");
        Intrinsics.checkNotNullParameter(world, "world");
        if (!nova$ids.containsValue(ID)) return null;
        final GuiEntry entry = registry.get(nova$ids.inverse().get(ID));
        final var handler = entry == null ? null : entry.getServer();
        if (handler == null) return null;
        final ContainerBase container = handler.invoke(player, world, new BlockPos(x, y, z));
        return container == null ? null : new ContainerImpl(container);
    }
}
