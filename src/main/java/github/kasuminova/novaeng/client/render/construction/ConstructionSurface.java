package github.kasuminova.novaeng.client.render.construction;

final class ConstructionSurface {
    static final int WIDTH = 16;
    static final int HEIGHT = 8;
    static final byte SOLID = 1;
    static final byte FLUID = 2;
    private static final int STRIDE = WIDTH + 2;
    private static final int LAYER = STRIDE * STRIDE;
    private static final int SAMPLE_ROWS = STRIDE * (HEIGHT + 2);

    private final byte[] cells = new byte[LAYER * (HEIGHT + 2)];
    private final int[] rows = new int[WIDTH];
    private int step;

    private static boolean exposes(final byte material, final byte neighbor) {
        return neighbor == 0 || (material & 3) == SOLID && (neighbor & 3) == FLUID;
    }

    void reset(final boolean reuseSamples) {
        step = reuseSamples ? SAMPLE_ROWS : 0;
    }

    static byte groupMaterial(int group, byte material) {
        return (byte) (group * 4 + material);
    }

    // Each step samples one row, merges one face plane, or merges one line along an axis.
    boolean step(Cells source, byte material, Vertex output) {
        boolean wire = material == 0;
        int index = step++;
        if (index < SAMPLE_ROWS) {
            int y = index / STRIDE - 1;
            int z = index % STRIDE - 1;
            for (int x = -1; x <= WIDTH; x++) cells[index * STRIDE + x + 1] = source.at(x, y, z);
            return false;
        }
        index -= SAMPLE_ROWS;
        for (var axis : Axis.VALUES) {
            int steps = wire ? (axis.u + 1) * (axis.v + 1) : 2 * axis.length;
            if (index < steps) {
                if (wire) line(axis, index % (axis.u + 1), index / (axis.u + 1), output);
                else plane(axis, index >> 1, (index & 1) == 0 ? -1 : 1, material, output);
                return false;
            }
            index -= steps;
        }
        return true;
    }

    private void plane(Axis axis, int n, int side, byte material, Vertex output) {
        for (int v = 0; v < axis.v; v++) {
            int row = 0;
            for (int u = 0; u < axis.u; u++) {
                if (cell(axis, n, u, v) != material) continue;
                byte neighbor = cell(axis, n + side, u, v);
                if (exposes(material, neighbor)) row |= 1 << u;
            }
            rows[v] = row;
        }
        for (int v = 0; v < axis.v; v++) {
            while (rows[v] != 0) {
                int u = Integer.numberOfTrailingZeros(rows[v]);
                int width = Integer.numberOfTrailingZeros(~(rows[v] >>> u));
                int mask = ((1 << width) - 1) << u;
                int end = v;
                while (end < axis.v && (rows[end] & mask) == mask) rows[end++] &= ~mask;
                int face = n + (side > 0 ? 1 : 0);
                vertex(axis, face, u, v, output);
                if (side < 0) vertex(axis, face, u + width, v, output);
                else vertex(axis, face, u, end, output);
                vertex(axis, face, u + width, end, output);
                vertex(axis, face, u, v, output);
                vertex(axis, face, u + width, end, output);
                if (side < 0) vertex(axis, face, u, end, output);
                else vertex(axis, face, u + width, v, output);
            }
        }
    }

    private void line(Axis axis, int u, int v, Vertex output) {
        int start = -1;
        for (int n = 0; n < axis.length; n++) {
            if (ownsEdge(axis, n, u, v)) {
                if (start < 0) start = n;
            } else if (start >= 0) {
                vertex(axis, start, u, v, output);
                vertex(axis, n, u, v, output);
                start = -1;
            }
        }
        if (start >= 0) {
            vertex(axis, start, u, v, output);
            vertex(axis, axis.length, u, v, output);
        }
    }

    private boolean ownsEdge(Axis axis, int n, int u, int v) {
        int mask = present(axis, n, u - 1, v - 1) | present(axis, n, u - 1, v) << 1
            | present(axis, n, u, v - 1) << 2 | present(axis, n, u, v) << 3;
        int count = Integer.bitCount(mask);
        if (count != 1 && count != 3 && mask != 0b0110 && mask != 0b1001) return false;
        int owner = Integer.numberOfTrailingZeros(mask);
        int ownerU = u - 1 + (owner >> 1);
        int ownerV = v - 1 + (owner & 1);
        return ownerU >= 0 && ownerU < axis.u && ownerV >= 0 && ownerV < axis.v;
    }

    private int present(Axis axis, int n, int u, int v) {
        return cell(axis, n, u, v) != 0 ? 1 : 0;
    }

    private byte cell(Axis axis, int n, int u, int v) {
        return switch (axis) {
            case X -> cells[index(n, v, u)];
            case Y -> cells[index(u, n, v)];
            case Z -> cells[index(v, u, n)];
        };
    }

    private static int index(int x, int y, int z) {
        return (y + 1) * LAYER + (z + 1) * STRIDE + x + 1;
    }

    private static void vertex(Axis axis, int n, int u, int v, Vertex output) {
        switch (axis) {
            case X -> output.add(n, v, u);
            case Y -> output.add(u, n, v);
            case Z -> output.add(v, u, n);
        }
    }

    @FunctionalInterface
    interface Cells {
        byte at(int x, int y, int z);
    }

    @FunctionalInterface
    interface Vertex {
        void add(int x, int y, int z);
    }

    private enum Axis {
        X(WIDTH, WIDTH, HEIGHT), Y(HEIGHT, WIDTH, WIDTH), Z(WIDTH, HEIGHT, WIDTH);

        static final Axis[] VALUES = values();
        final int length;
        final int u;
        final int v;

        Axis(int length, int u, int v) {
            this.length = length;
            this.u = u;
            this.v = v;
        }
    }
}
