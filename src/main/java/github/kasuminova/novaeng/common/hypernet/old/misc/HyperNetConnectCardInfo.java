package github.kasuminova.novaeng.common.hypernet.old.misc;

import com.github.bsideup.jabel.Desugar;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

@Desugar
public record HyperNetConnectCardInfo(BlockPos pos, UUID networkOwner) {
}
