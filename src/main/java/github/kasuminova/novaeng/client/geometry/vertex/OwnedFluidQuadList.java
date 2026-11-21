package github.kasuminova.novaeng.client.geometry.vertex;

import github.kasuminova.novaeng.client.geometry.TrustedGeometryList;
import it.unimi.dsi.fastutil.objects.AbstractObjectList;
import net.minecraft.client.renderer.block.model.BakedQuad;

import java.util.List;
import java.util.RandomAccess;

/**
 * Decodes once per list get, giving Actinium's injected quad views ordinary populated fields.
 */
public final class OwnedFluidQuadList extends AbstractObjectList<BakedQuad> implements RandomAccess, TrustedGeometryList {
    private final List<BakedQuad> records;

    public OwnedFluidQuadList(final List<BakedQuad> records) {
        this.records = records;
    }

    @Override
    public BakedQuad get(final int index) {
        final BakedQuad record = records.get(index);
        return record instanceof OwnedUnpackedQuadView view ? view.read() : record;
    }

    @Override
    public int size() {
        return records.size();
    }
}
