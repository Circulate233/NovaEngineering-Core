package github.kasuminova.novaeng.mixin.ic2;

import ic2.api.tile.IEnergyStorage;
import ic2.core.IC2;
import ic2.core.block.BlockTileEntity;
import ic2.core.block.TileEntityBlock;
import ic2.core.item.block.ItemBlockTileEntity;
import ic2.core.profile.Version;
import ic2.core.ref.TeBlock;
import ic2.core.util.StackUtil;
import ic2.core.util.Util;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

@Mixin(value = TeBlock.class, remap = false)
public abstract class MixinIc2LazyDummies {
    @Shadow
    @Final
    public static TeBlock[] values;
    @Shadow
    @Final
    private Class<? extends TileEntityBlock> teClass;
    @Shadow
    private TileEntityBlock dummyTe;
    @Unique
    private volatile boolean nova$pendingDummy;
    @Unique
    private boolean nova$buildingDummy;

    /**
     * @author circulation
     * @reason Keep IC2's activation gate while postponing unused dummy tile construction.
     */
    @Overwrite
    public static void buildDummies() {
        final ModContainer active = Loader.instance().activeModContainer();
        if (active == null || !"ic2".equals(active.getModId())) {
            throw new IllegalAccessError("Don't mess with this please.");
        }
        for (final TeBlock block : values) {
            final MixinIc2LazyDummies state = (MixinIc2LazyDummies) (Object) block;
            synchronized (block) {
                state.dummyTe = null;
                state.nova$pendingDummy = state.teClass != null;
            }
        }
    }

    /**
     * @author circulation
     * @reason Construct and publish each requested dummy once, preserving IC2's error policy.
     */
    @Deprecated
    @Overwrite
    public TileEntityBlock getDummyTe() {
        if (nova$pendingDummy) {
            synchronized (this) {
                if (nova$pendingDummy && !nova$buildingDummy) {
                    nova$buildingDummy = true;
                    try {
                        dummyTe = (TileEntityBlock) MethodHandles.publicLookup()
                                                                 .findConstructor(teClass, MethodType.methodType(void.class))
                                                                 .asType(MethodType.methodType(TileEntityBlock.class)).invokeExact();
                    } catch (Error error) {
                        throw error;
                    } catch (Throwable failure) {
                        if (Util.inDev()) {
                            failure.printStackTrace();
                        }
                    } finally {
                        nova$pendingDummy = false;
                        nova$buildingDummy = false;
                    }
                }
            }
        }
        return dummyTe;
    }

    /**
     * @author circulation
     * @reason Determine energy-storage support from the tile class before requesting a dummy.
     */
    @Overwrite
    public void addSubBlocks(final NonNullList<ItemStack> list, final BlockTileEntity block,
                             final ItemBlockTileEntity item, final CreativeTabs tab) {
        if (tab == IC2.tabIC2 || tab == CreativeTabs.SEARCH) {
            for (final TeBlock type : values) {
                if (type.hasItem() && Version.shouldEnable(type.getTeClass())) {
                    list.add(block.getItemStack(type));
                    if (IEnergyStorage.class.isAssignableFrom(type.getTeClass())
                        && type.getDummyTe() instanceof IEnergyStorage storage) {
                        final ItemStack filled = block.getItemStack(type);
                        StackUtil.getOrCreateNbtData(filled).setDouble("energy", storage.getCapacity());
                        list.add(filled);
                    }
                }
            }
        }
    }
}
