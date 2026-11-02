package github.kasuminova.novaeng.mixin.minecraft.forge;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.concurrent.ConcurrentMap;

@Mixin(value = DimensionManager.class, remap = false)
public interface AccessorDimensionManagerWorlds {

    @Accessor("worlds")
    static Int2ObjectMap<WorldServer> nova$loadedWorlds() {
        throw new AssertionError();
    }

    @Accessor("weakWorldMap")
    static ConcurrentMap<World, World> nova$weakWorlds() {
        throw new AssertionError();
    }
}
