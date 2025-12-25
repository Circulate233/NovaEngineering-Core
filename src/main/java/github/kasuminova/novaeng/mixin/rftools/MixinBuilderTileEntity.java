package github.kasuminova.novaeng.mixin.rftools;

import github.kasuminova.novaeng.common.command.CommandBuilder;
import mcjty.lib.tileentity.GenericEnergyReceiverTileEntity;
import mcjty.rftools.blocks.builder.BuilderTileEntity;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.WorldServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BuilderTileEntity.class, remap = false)
public abstract class MixinBuilderTileEntity extends GenericEnergyReceiverTileEntity {

    public MixinBuilderTileEntity(long maxEnergy, long maxReceive) {
        super(maxEnergy, maxReceive);
    }

    @Inject(method = "update", at = @At("HEAD"))
    private void commonQuarryBlockI(CallbackInfo ci) {
        if (CommandBuilder.INSTANCE.isTickWork) {
            if (this.getWorld() instanceof WorldServer w) {
                if (w.getTotalWorldTime() % 200 != 0) return;
                if (w.getMinecraftServer() != null) {
                    var server = w.getMinecraftServer();
                    var playerList = server.getPlayerList();
                    var players = playerList.getPlayers();
                    for (EntityPlayerMP player : players) {
                        player.sendMessage(new TextComponentString("位于world:" + w.provider.getDimension() + n$getPosName(this.pos) + "的属于玩家" + this.getOwnerName() + "[" + this.getOwnerUUID() + "]的建造机正常工作"));
                    }
                }
            }
        }
    }

    @Inject(method = "commonQuarryBlock", at = @At("HEAD"))
    private void commonQuarryBlockI(boolean silk, int rfNeeded, BlockPos srcPos, IBlockState srcState, CallbackInfoReturnable<Boolean> cir) {
        if (CommandBuilder.INSTANCE.isQuarryWork) {
            if (this.getWorld() instanceof WorldServer w) {
                if (w.getMinecraftServer() != null) {
                    var server = w.getMinecraftServer();
                    var playerList = server.getPlayerList();
                    var players = playerList.getPlayers();
                    for (EntityPlayerMP player : players) {
                        player.sendMessage(new TextComponentString("位于world:" + w.provider.getDimension() + n$getPosName(this.pos) + "的属于玩家" + this.getOwnerName() + "[" + this.getOwnerUUID() + "]的建造机挖掘了位于" + n$getPosName(srcPos) + "的方块"));
                    }
                }
            }
        }
    }

    @Unique
    private String n$getPosName(BlockPos pos) {
        return "x:" + pos.getX() + ",y:" + pos.getY() + ",z:" + pos.getZ();
    }
}
