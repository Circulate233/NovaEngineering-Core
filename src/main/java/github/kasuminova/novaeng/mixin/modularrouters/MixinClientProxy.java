package github.kasuminova.novaeng.mixin.modularrouters;

import me.desht.modularrouters.proxy.ClientProxy;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import vazkii.psi.common.Psi;

/*
  来自Psi的调用方法,解决一些内存泄露问题
 */
@Mixin(value = ClientProxy.class,remap = false)
public class MixinClientProxy {

    /**
     * @author circulation
     * @reason 调用Psi内已经修复过的粒子
     */
    @Overwrite
    public void sparkleFX(World world, double x, double y, double z, float r, float g, float b, float size, int m, boolean fake) {
        Psi.proxy.sparkleFX(world, x, y, z, r, g, b, size, m);
    }
}