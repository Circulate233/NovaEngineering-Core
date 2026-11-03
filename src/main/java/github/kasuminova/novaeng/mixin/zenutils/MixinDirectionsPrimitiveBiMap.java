package github.kasuminova.novaeng.mixin.zenutils;

import com.google.common.collect.BiMap;

import github.kasuminova.novaeng.common.util.collection.Object2IntHashBiMap;
import github.kasuminova.novaeng.common.util.collection.PrimitiveBiMaps;

import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import youyihj.zenutils.api.cotx.block.DirectionalBlockContent;
import youyihj.zenutils.api.cotx.block.DirectionalBlockRepresentation.Directions;
import youyihj.zenutils.api.cotx.block.DirectionalBlockRepresentation.PlaneRotation;

@Mixin(value = Directions.class, remap = false)
public abstract class MixinDirectionsPrimitiveBiMap {
    @Shadow
    @Final
    @Mutable
    private BiMap<EnumFacing, Integer> metaFacingMapping;
    @Shadow
    @Final
    private IProperty<EnumFacing> blockProperty;
    @Shadow
    @Final
    private EnumFacing[] validFacings;

    @Redirect(
        method = "<init>",
        at =
        @At(
            value = "FIELD",
            target =
                "Lyouyihj/zenutils/api/cotx/block/DirectionalBlockRepresentation$Directions;metaFacingMapping:Lcom/google/common/collect/BiMap;",
            opcode = Opcodes.PUTFIELD),
        require = 1)
    private void nova$primitiveMetadata(
        final Directions owner, final BiMap<EnumFacing, Integer> original) {
        final var replacement = Object2IntHashBiMap.forEnum(EnumFacing.class);
        replacement.putAll(original);
        metaFacingMapping = replacement;
    }

    /**
     * @author NovaEngineering
     * @reason Populate primitive metadata values in the original facing order.
     */
    @Overwrite
    private void fillMetaMapping() {
        for (int i = 0; i < validFacings.length; i++)
            PrimitiveBiMaps.putObjectInt(metaFacingMapping, validFacings[i], i);
    }

    /**
     * @author NovaEngineering
     * @reason Encode state metadata through the primitive index.
     */
    @Overwrite
    public int toMeta(final IBlockState state, final boolean planeRotatable) {
        final int facing =
            PrimitiveBiMaps.getIntValue(metaFacingMapping, state.getValue(blockProperty));
        return planeRotatable
            ? facing * 4
            + state.getValue(DirectionalBlockContent.PLANE_ROTATION_PROPERTY).ordinal()
            : facing;
    }

    /**
     * @author NovaEngineering
     * @reason Decode metadata without boxing.
     */
    @Overwrite
    public IBlockState toState(
        final int meta, final IBlockState state, final boolean planeRotatable) {
        final EnumFacing facing =
            (EnumFacing)
                PrimitiveBiMaps.getIntKey(
                    metaFacingMapping.inverse(), planeRotatable ? meta / 4 : meta);
        final IBlockState result = state.withProperty(blockProperty, facing);
        return planeRotatable
            ? result.withProperty(
            DirectionalBlockContent.PLANE_ROTATION_PROPERTY,
            PlaneRotation.indexOf(meta % 4))
            : result;
    }
}
