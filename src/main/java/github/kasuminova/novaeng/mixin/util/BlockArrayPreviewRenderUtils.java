package github.kasuminova.novaeng.mixin.util;

import hellfirepvp.modularmachinery.client.util.BlockArrayRenderHelper;
import hellfirepvp.modularmachinery.common.util.BlockArray;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;

public interface BlockArrayPreviewRenderUtils {

    BlockPos getAttachedPosition();

    void setAttachedPosition(BlockPos pos);

    int getRenderedLayer();

    void setRenderedLayer(int layer);

    BlockArrayRenderHelper getRenderHelper();

    void setRenderHelper(BlockArrayRenderHelper renderHelper);

    void n$renderTranslucentBlocks();

    BlockArray getMatchArray();

    void setMatchArray(BlockArray matchArray);

    Vec3i getRenderHelperOffset();

    void setRenderHelperOffset(Vec3i renderHelperOffset);

    EnumFacing getFacing();

    void setFacing(EnumFacing facing);
}
