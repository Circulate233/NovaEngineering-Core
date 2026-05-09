package github.kasuminova.novaeng.mixin.minecraft;

import github.kasuminova.novaeng.common.util.SetList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(World.class)
public class MixinWorld {

    @Shadow
    @Final
    @Mutable
    public List<TileEntity> loadedTileEntityList;
    @Shadow
    @Final
    @Mutable
    public List<TileEntity> tickableTileEntities;
    @Shadow
    @Final
    @Mutable
    private List<TileEntity> addedTileEntityList;
    @Shadow
    @Final
    @Mutable
    private List<TileEntity> tileEntitiesToBeRemoved;

    @Redirect(method = "<init>", at = @At(value = "FIELD", target = "Lnet/minecraft/world/World;addedTileEntityList:Ljava/util/List;", opcode = Opcodes.PUTFIELD))
    public void onInit(World instance, List<TileEntity> value) {
        SetList.SetAdd<TileEntity, LongOpenHashSet> add = (s, t) -> s.add(t.getPos().toLong());
        SetList.SetRemove<TileEntity, LongOpenHashSet> remove = (s, t) -> s.remove(t.getPos().toLong());
        this.loadedTileEntityList = new SetList<>(new LongOpenHashSet(), add, remove);
        this.tickableTileEntities = new SetList<>(new LongOpenHashSet(), add, remove);
        this.addedTileEntityList = new SetList<>(new LongOpenHashSet(), add, remove);
        this.tileEntitiesToBeRemoved = new SetList<>(new LongOpenHashSet(), add, remove);
    }
}