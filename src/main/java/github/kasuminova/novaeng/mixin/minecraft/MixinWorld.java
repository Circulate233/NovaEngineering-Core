package github.kasuminova.novaeng.mixin.minecraft;

import github.kasuminova.novaeng.common.util.SetList;
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

    @Redirect(method = "<init>",at = @At(value = "FIELD", target = "Lnet/minecraft/world/World;addedTileEntityList:Ljava/util/List;", opcode = Opcodes.PUTFIELD))
    public void onInit(World instance, List<TileEntity> value) {
        this.loadedTileEntityList = new SetList<>();
        this.tickableTileEntities = new SetList<>();
        this.addedTileEntityList = new SetList<>();
        this.tileEntitiesToBeRemoved = new SetList<>();
    }
}
