package github.kasuminova.novaeng.mixin.minecraft;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.client.renderer.vertex.VertexFormat;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(VertexFormat.class)
public class MixinVertexFormat {

    @Mutable
    @Shadow
    @Final
    private List<Integer> offsets;

    @Mutable
    @Shadow
    @Final
    private List<Integer> uvOffsetsById;

    @Shadow
    private int vertexSize;

    @Inject(method = "<init>()V", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        offsets = new IntArrayList(8);
        uvOffsetsById = new IntArrayList(8);
    }

    @Redirect(method = "addElement", at = @At(value = "INVOKE", target = "Ljava/lang/Integer;valueOf(I)Ljava/lang/Integer;"))
    public Integer r(int i) {
        return null;
    }

    @Redirect(method = "addElement", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", ordinal = 1))
    public boolean offsetAdd(List<?> instance, Object e) {
        return ((IntArrayList) instance).add(vertexSize);
    }

    @Redirect(method = "addElement", at = @At(value = "INVOKE", target = "Ljava/util/List;add(ILjava/lang/Object;)V"))
    public void uvOffsetsByIdAdd(List<?> instance, int i, Object e) {
        ((IntArrayList) instance).add(i, vertexSize);
    }

    /**
     * @author circulation
     * @reason 重写以直接返回int
     */
    @Overwrite
    public int getOffset(int index) {
        return ((IntArrayList) offsets).getInt(index);
    }

    /**
     * @author circulation
     * @reason 重写以直接返回int
     */
    @Overwrite
    public int getUvOffsetById(int id) {
        return ((IntArrayList) this.uvOffsetsById).getInt(id);
    }

}
