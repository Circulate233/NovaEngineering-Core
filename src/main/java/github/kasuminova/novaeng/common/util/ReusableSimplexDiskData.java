package github.kasuminova.novaeng.common.util;

import rtg.api.util.noise.ISimplexData2D;
import rtg.api.util.noise.SimplexData2D;

public final class ReusableSimplexDiskData implements ISimplexData2D {

    private static final ThreadLocal<ReusableSimplexDiskData> LOCAL =
        ThreadLocal.withInitial(ReusableSimplexDiskData::new);

    private final ISimplexData2D delegate = SimplexData2D.newDisk();
    private final DataRequest request = this.delegate.request();

    public static ISimplexData2D get() {
        return LOCAL.get();
    }

    @Override
    public void addToDeltaX(final double value) {
        this.delegate.addToDeltaX(value);
    }

    @Override
    public void addToDeltaY(final double value) {
        this.delegate.addToDeltaY(value);
    }

    @Override
    public double getDeltaX() {
        return this.delegate.getDeltaX();
    }

    @Override
    public void setDeltaX(final double value) {
        this.delegate.setDeltaX(value);
    }

    @Override
    public double getDeltaY() {
        return this.delegate.getDeltaY();
    }

    @Override
    public void setDeltaY(final double value) {
        this.delegate.setDeltaY(value);
    }

    @Override
    public void clear() {
        this.delegate.clear();
    }

    @Override
    public DataRequest request() {
        return this.request;
    }
}
