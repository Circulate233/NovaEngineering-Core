package github.kasuminova.novaeng.mixin.minecraft;

import github.kasuminova.novaeng.common.util.LegacyAdditionsRemap;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 让旧存档中属于已移除的 Additions 模组的物品重定向到本模组的重新实现。
 *
 * <p>物品栈在磁盘上以注册名字符串保存，反序列化集中经过 {@code ItemStack(NBTTagCompound)}
 * 构造器读取的 {@code id} 字段。此处只改写该次读取的返回值，其余逻辑保持原样，
 * 因此未命中映射的名称不受任何影响。
 */
@Mixin(ItemStack.class)
public class MixinItemStackLegacyRedirect {

    @Redirect(
        method = "<init>(Lnet/minecraft/nbt/NBTTagCompound;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/nbt/NBTTagCompound;getString(Ljava/lang/String;)Ljava/lang/String;"
        ),
        require = 1
    )
    private String nova$redirectLegacyId(final NBTTagCompound nbt, final String key) {
        return LegacyAdditionsRemap.apply(nbt.getString(key));
    }

}
