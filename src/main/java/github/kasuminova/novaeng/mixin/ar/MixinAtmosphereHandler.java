package github.kasuminova.novaeng.mixin.ar;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import zmaster587.advancedRocketry.api.ARConfiguration;
import zmaster587.advancedRocketry.api.AreaBlob;
import zmaster587.advancedRocketry.api.IAtmosphere;
import zmaster587.advancedRocketry.api.util.IBlobHandler;
import zmaster587.advancedRocketry.atmosphere.AtmosphereHandler;
import zmaster587.advancedRocketry.atmosphere.AtmosphereType;
import zmaster587.libVulpes.util.HashedBlockPosition;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;

@Mixin(value = AtmosphereHandler.class, remap = false)
public abstract class MixinAtmosphereHandler {

    @Unique
    private static final ThreadLocal<NovaBlockChangeLookup> NOVA_BLOCK_CHANGE_LOOKUP =
        ThreadLocal.withInitial(NovaBlockChangeLookup::new);
    @Unique
    private static final ThreadLocal<HashedBlockPosition> NOVA_ATMOSPHERE_POSITION =
        ThreadLocal.withInitial(() -> new HashedBlockPosition(0, 0, 0));

    @Shadow
    private HashMap<IBlobHandler, AreaBlob> blobs;

    @Shadow
    @Nonnull
    public abstract IAtmosphere getDefaultAtmosphereType();

    @Redirect(method = "onBlockChange(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;)V",
        at = @At(value = "INVOKE",
            target = "Lzmaster587/advancedRocketry/atmosphere/AtmosphereHandler;getOxygenHandler(I)Lzmaster587/advancedRocketry/atmosphere/AtmosphereHandler;",
            remap = false), require = 1)
    private static AtmosphereHandler nova$beginBlockChangeLookup(final int dimNumber) {
        final NovaBlockChangeLookup lookup = NOVA_BLOCK_CHANGE_LOOKUP.get();
        lookup.atmosphereValid = false;
        lookup.blockStateValid = false;
        lookup.handler = null;
        lookup.world = null;
        lookup.atmosphere = null;
        lookup.blockState = null;
        return AtmosphereHandler.getOxygenHandler(dimNumber);
    }

    @Redirect(method = "onBlockChange(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;)V",
        at = @At(value = "INVOKE",
            target = "Lzmaster587/advancedRocketry/atmosphere/AtmosphereHandler;getAtmosphereType(Lnet/minecraft/util/math/BlockPos;)Lzmaster587/advancedRocketry/api/IAtmosphere;"),
        require = 1)
    private static IAtmosphere nova$cachedAtmosphereLookup(final AtmosphereHandler handler, final BlockPos pos2) {
        final NovaBlockChangeLookup lookup = NOVA_BLOCK_CHANGE_LOOKUP.get();
        final int x = pos2.getX();
        final int y = pos2.getY();
        final int z = pos2.getZ();
        if (lookup.atmosphereValid && lookup.handler == handler
            && lookup.atmosphereX == x && lookup.atmosphereY == y && lookup.atmosphereZ == z) {
            return lookup.atmosphere;
        }
        final IAtmosphere atmosphere = handler.getAtmosphereType(pos2);
        lookup.handler = handler;
        lookup.atmosphereX = x;
        lookup.atmosphereY = y;
        lookup.atmosphereZ = z;
        lookup.atmosphere = atmosphere;
        lookup.atmosphereValid = true;
        return atmosphere;
    }

    @Redirect(method = "onBlockChange(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;)V",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/World;getBlockState(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/state/IBlockState;",
            remap = true), require = 1)
    private static IBlockState nova$cachedBlockState(final World world, final BlockPos pos) {
        final NovaBlockChangeLookup lookup = NOVA_BLOCK_CHANGE_LOOKUP.get();
        final int x = pos.getX();
        final int y = pos.getY();
        final int z = pos.getZ();
        if (lookup.blockStateValid && lookup.world == world
            && lookup.blockStateX == x && lookup.blockStateY == y && lookup.blockStateZ == z) {
            return lookup.blockState;
        }
        final IBlockState state = world.getBlockState(pos);
        lookup.world = world;
        lookup.blockStateX = x;
        lookup.blockStateY = y;
        lookup.blockStateZ = z;
        lookup.blockState = state;
        lookup.blockStateValid = true;
        return state;
    }

    @Redirect(method = "onBlockChange(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;)V",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;)Z",
            remap = true), require = 1)
    private static boolean nova$invalidateAfterSetBlockState(final World world, final BlockPos pos,
                                                             final IBlockState state) {
        try {
            return world.setBlockState(pos, state);
        } finally {
            nova$invalidateBlockChangeLookups();
        }
    }

    @Redirect(method = "onBlockChange(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;)V",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/World;setBlockToAir(Lnet/minecraft/util/math/BlockPos;)Z",
            remap = true), require = 1)
    private static boolean nova$invalidateAfterSetBlockToAir(final World world, final BlockPos pos) {
        try {
            return world.setBlockToAir(pos);
        } finally {
            nova$invalidateBlockChangeLookups();
        }
    }

    @Unique
    private static void nova$invalidateBlockChangeLookups() {
        final NovaBlockChangeLookup lookup = NOVA_BLOCK_CHANGE_LOOKUP.get();
        lookup.atmosphereValid = false;
        lookup.blockStateValid = false;
    }

    @Redirect(method = "getAtmosphereType(Lnet/minecraft/entity/Entity;)Lzmaster587/advancedRocketry/api/IAtmosphere;",
        at = @At(value = "NEW", target = "zmaster587/libVulpes/util/HashedBlockPosition"), require = 1)
    private static HashedBlockPosition nova$reuseEntityAtmospherePosition(final int x, final int y, final int z) {
        final HashedBlockPosition position = NOVA_ATMOSPHERE_POSITION.get();
        position.x = x;
        position.y = (short) y;
        position.z = z;
        return position;
    }

    @Redirect(method = "getAtmospherePressure(Lnet/minecraft/entity/Entity;)I",
        at = @At(value = "NEW", target = "zmaster587/libVulpes/util/HashedBlockPosition"), require = 1)
    private static HashedBlockPosition nova$reusePressurePosition(final int x, final int y, final int z) {
        return nova$reuseEntityAtmospherePosition(x, y, z);
    }

    @Redirect(method = "canEntityBreathe(Lnet/minecraft/entity/EntityLiving;)Z",
        at = @At(value = "NEW", target = "zmaster587/libVulpes/util/HashedBlockPosition"), require = 1)
    private static HashedBlockPosition nova$reuseBreathingPosition(final int x, final int y, final int z) {
        return nova$reuseEntityAtmospherePosition(x, y, z);
    }

    @Redirect(method = "onBlockChange(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;)V",
        at = @At(value = "NEW", target = "zmaster587/libVulpes/util/HashedBlockPosition"), require = 1)
    private static HashedBlockPosition nova$reuseBlockChangePosition(final BlockPos pos) {
        return nova$reuseEntityAtmospherePosition(pos.getX(), pos.getY(), pos.getZ());
    }

    /**
     * @author circulation
     * @reason Avoid coordinate wrappers and iterators when a dimension has no local atmosphere blobs.
     */
    @Nonnull
    @Overwrite
    public IAtmosphere getAtmosphereType(@Nonnull final BlockPos position) {
        if (!ARConfiguration.getCurrentConfig().enableOxygen) {
            return AtmosphereType.AIR;
        }
        if (!this.blobs.isEmpty()) {
            final HashedBlockPosition hashed = NOVA_ATMOSPHERE_POSITION.get();
            hashed.x = position.getX();
            hashed.y = (short) position.getY();
            hashed.z = position.getZ();
            for (final AreaBlob blob : this.blobs.values()) {
                if (blob.contains(hashed)) {
                    final IAtmosphere atmosphere = (IAtmosphere) blob.getData();
                    if (atmosphere != null) {
                        return atmosphere;
                    }
                }
            }
        }
        return this.getDefaultAtmosphereType();
    }

    private static final class NovaBlockChangeLookup {
        private AtmosphereHandler handler;
        private World world;
        private int atmosphereX;
        private int atmosphereY;
        private int atmosphereZ;
        private int blockStateX;
        private int blockStateY;
        private int blockStateZ;
        private IAtmosphere atmosphere;
        private IBlockState blockState;
        private boolean atmosphereValid;
        private boolean blockStateValid;
    }

    /**
     * @author circulation
     * @reason Keep the mutable, ordered result without allocating a linked-list node for every nearby blob.
     */
    @Nonnull
    @Overwrite
    protected List<AreaBlob> getBlobWithinRadius(@Nonnull final HashedBlockPosition position, final int radius) {
        final ObjectArrayList<AreaBlob> nearby = new ObjectArrayList<>(this.blobs.size());
        if (!this.blobs.isEmpty()) {
            for (final AreaBlob blob : this.blobs.values()) {
                if (blob.getRootPosition().getDistance(position) - radius <= 0.0D) {
                    nearby.add(blob);
                }
            }
        }
        return nearby;
    }
}
