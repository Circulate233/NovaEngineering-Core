package github.kasuminova.novaeng.mixin.botania;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import vazkii.botania.client.core.proxy.ClientProxy;
import vazkii.psi.common.Psi;

/*
  来自Psi的调用方法,解决一些内存泄露问题
 */
@Mixin(value = ClientProxy.class,remap = false)
public abstract class MixinClientProxy {

    /**
     * @author circulation
     * @reason 调用Psi内已经修复过的粒子
     */
    @Overwrite
    public void wispFX(double x, double y, double z, float r, float g, float b, float size, float motionx, float motiony, float motionz, float maxAgeMul) {
        Psi.proxy.wispFX(Minecraft.getMinecraft().world,x, y, z, r, g, b, size,motionx,motiony,motionz,maxAgeMul);
    }

    /**
     * @author circulation
     * @reason 调用Psi内已经修复过的粒子
     */
    @Overwrite
    public void sparkleFX(double x, double y, double z, float r, float g, float b, float size, int m, boolean fake) {
        Psi.proxy.sparkleFX(Minecraft.getMinecraft().world, x, y, z, r, g, b, size, m);
    }

    /**
     * @author circulation
     * @reason 调用Psi内已经修复过的粒子
     */
    @Overwrite
    public void setWispFXDistanceLimit(boolean limit) {
        Psi.proxy.setWispFXDistanceLimit(limit);
    }

    /**
     * @author circulation
     * @reason 调用Psi内已经修复过的粒子
     */
    @Overwrite
    public void setWispFXDepthTest(boolean test) {
        Psi.proxy.setWispFXDepthTest(test);
    }
}