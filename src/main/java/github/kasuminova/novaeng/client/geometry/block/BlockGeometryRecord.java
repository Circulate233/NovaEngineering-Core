package github.kasuminova.novaeng.client.geometry.block;

import java.util.Arrays;
import java.util.Objects;

/**
 * Exact packed geometry; positions and the remaining vertex columns have independent sharing keys.
 */
public final class BlockGeometryRecord {
    final Shape shape;
    final Columns material;
    final int stride;
    final int position;
    private volatile BlockGeometryPages.Shape pageShape;

    private BlockGeometryRecord(final Shape shape, final Columns material, final int stride, final int position) {
        this.shape = shape;
        this.material = material;
        this.stride = stride;
        this.position = position;
    }

    public static BlockGeometryRecord capture(final int[] words, final int stride, final int position) {
        if (stride < 3 || stride > 66 || position < 0 || position + 3 > stride || words.length != stride * 4) {
            return raw(words);
        }
        final Shape shape = rectangle(words, stride, position);
        if (shape == null) return raw(words);
        final Columns material = Columns.capture(words, stride, position);
        // rectangle checks every position bit (including the grid conversion); Columns copies raw ints.
        return new BlockGeometryRecord(shape, material, stride, position);
    }

    private static BlockGeometryRecord raw(final int[] words) {
        return new BlockGeometryRecord(new RawWords(words.clone()), null, 0, 0);
    }

    static int[] decodeParts(final Shape shape, final Columns material, final int stride, final int position) {
        final int[] words = new int[material == null ? ((RawWords) shape).words.length : stride * 4];
        decodePartsInto(shape, material, stride, position, words);
        return words;
    }

    private static void decodePartsInto(final Shape shape, final Columns material, final int stride, final int position, final int[] target) {
        if (material == null) {
            final int[] words = ((RawWords) shape).words;
            System.arraycopy(words, 0, target, 0, words.length);
            return;
        }
        material.decodeInto(target, stride, position);
        for (int vertex = 0; vertex < 4; vertex++) {
            for (int axis = 0; axis < 3; axis++)
                target[vertex * stride + position + axis] = shape.coordinate(vertex, axis);
        }
    }

    private static Shape rectangle(final int[] words, final int stride, final int position) {
        for (int plane = 0; plane < 3; plane++) {
            final int fixed = words[position + plane];
            boolean constant = true;
            for (int vertex = 1; vertex < 4; vertex++) constant &= words[vertex * stride + position + plane] == fixed;
            if (!constant || !Float.isFinite(Float.intBitsToFloat(fixed))) continue;
            final int firstAxis = (plane + 1) % 3;
            final int secondAxis = (plane + 2) % 3;
            final int firstA = words[position + firstAxis];
            final int firstB = words[position + secondAxis];
            int secondA = 0;
            int secondB = 0;
            boolean firstFound = false;
            boolean secondFound = false;
            for (int vertex = 1; vertex < 4; vertex++) {
                final int a = words[vertex * stride + position + firstAxis];
                final int b = words[vertex * stride + position + secondAxis];
                if (a != firstA) {
                    secondA = a;
                    firstFound = true;
                }
                if (b != firstB) {
                    secondB = b;
                    secondFound = true;
                }
            }
            if (!firstFound || !secondFound) continue;
            int corners = 0;
            int seen = 0;
            boolean valid = Float.isFinite(Float.intBitsToFloat(firstA)) && Float.isFinite(Float.intBitsToFloat(secondA))
                && Float.isFinite(Float.intBitsToFloat(firstB)) && Float.isFinite(Float.intBitsToFloat(secondB));
            for (int vertex = 0; vertex < 4 && valid; vertex++) {
                final int a = words[vertex * stride + position + firstAxis];
                final int b = words[vertex * stride + position + secondAxis];
                if ((a != firstA && a != secondA) || (b != firstB && b != secondB)) {
                    valid = false;
                    break;
                }
                final int corner = (a == secondA ? 1 : 0) | (b == secondB ? 2 : 0);
                if ((seen & 1 << corner) != 0) {
                    valid = false;
                    break;
                }
                seen |= 1 << corner;
                corners |= corner << vertex * 2;
            }
            if (!valid || seen != 15) continue;
            long grid = 0;
            int fifth = 0;
            boolean exactGrid = true;
            for (int index = 0; index < 5; index++) {
                final int bits = switch (index) {
                    case 0 -> fixed;
                    case 1 -> firstA;
                    case 2 -> secondA;
                    case 3 -> firstB;
                    default -> secondB;
                };
                final float value = Float.intBitsToFloat(bits);
                final int cell = (int) (value * 16.0F);
                if (cell < Short.MIN_VALUE || cell > Short.MAX_VALUE || Float.floatToRawIntBits(cell / 16.0F) != bits) {
                    exactGrid = false;
                    break;
                }
                if (index < 4) grid |= (cell & 0xFFFFL) << index * 16;
                else fifth = cell;
            }
            return exactGrid ? new GridRectangle(plane, corners, grid, (short) fifth)
                : new BitsRectangle(plane, corners, fixed, firstA, secondA, firstB, secondB);
        }
        return null;
    }

    private static int coordinateIndex(final int plane, final int corners, final int vertex, final int axis) {
        if (axis == plane) return 0;
        final int corner = corners >>> vertex * 2;
        return axis == (plane + 1) % 3 ? 1 + (corner & 1) : 3 + ((corner >>> 1) & 1);
    }

    public boolean isRectangle() {
        return material != null;
    }

    public int wordCount() {
        return material == null ? ((RawWords) shape).words.length : stride * 4;
    }

    public int[] decode() {
        return decodeParts(shape, material, stride, position);
    }

    public void decodeInto(final int[] target) {
        if (target.length < wordCount()) throw new IllegalArgumentException("Short packed geometry destination");
        decodePartsInto(shape, material, stride, position, target);
    }

    boolean matches(final int[] words, final int stride, final int position) {
        if (material == null) return Arrays.equals(((RawWords) shape).words, words);
        if (this.stride != stride || this.position != position || words.length != stride * 4) return false;
        for (int vertex = 0; vertex < 4; vertex++)
            for (int axis = 0; axis < 3; axis++)
                if (words[vertex * stride + position + axis] != shape.coordinate(vertex, axis)) return false;
        return material.matches(words, stride, position);
    }

    BlockGeometryPages.Shape pageShape() {
        BlockGeometryPages.Shape value = pageShape;
        if (value == null) pageShape = value = new BlockGeometryPages.Shape(shape, stride, position);
        return value;
    }

    @Override
    public boolean equals(final Object other) {
        return other instanceof BlockGeometryRecord that && stride == that.stride && position == that.position
            && shape.equals(that.shape) && Objects.equals(material, that.material);
    }

    @Override
    public int hashCode() {
        return ((shape.hashCode() * 31 + Objects.hashCode(material)) * 31 + stride) * 31 + position;
    }

    interface Shape {
        int coordinate(int vertex, int axis);
    }

    private record GridRectangle(int plane, int corners, long grid, short fifth) implements Shape {
        @Override
        public int coordinate(final int vertex, final int axis) {
            final int index = coordinateIndex(plane, corners, vertex, axis);
            final int cell = index == 4 ? fifth : (short) (grid >>> index * 16);
            return Float.floatToRawIntBits(cell / 16.0F);
        }
    }

    private record BitsRectangle(int plane, int corners, int fixed, int firstA, int secondA,
                                 int firstB, int secondB) implements Shape {

        @Override
        public int coordinate(final int vertex, final int axis) {
            return switch (coordinateIndex(plane, corners, vertex, axis)) {
                case 0 -> fixed;
                case 1 -> firstA;
                case 2 -> secondA;
                case 3 -> firstB;
                default -> secondB;
            };
        }

        @Override
        public int hashCode() {
            int result = 31 + fixed;
            result = result * 31 + firstA;
            result = result * 31 + secondA;
            result = result * 31 + firstB;
            result = result * 31 + secondB;
            return (result * 31 + plane) * 31 + corners;
        }
    }

    private record RawWords(int[] words) implements Shape {

        @Override
        public int coordinate(final int vertex, final int axis) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean equals(final Object other) {
            return other instanceof RawWords(int[] words1) && Arrays.equals(words, words1);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(words);
        }
    }

    static final class Columns {
        private final long varying;
        private final int[] values;

        private Columns(final long varying, final int[] values) {
            this.varying = varying;
            this.values = values;
        }

        private static Columns capture(final int[] words, final int stride, final int position) {
            long varying = 0;
            int column = 0;
            for (int word = 0; word < stride; word++) {
                if (word >= position && word < position + 3) continue;
                final int first = words[word];
                if (words[stride + word] != first || words[stride * 2 + word] != first || words[stride * 3 + word] != first)
                    varying |= 1L << column;
                column++;
            }
            final int[] values = new int[column + Long.bitCount(varying) * 3];
            int length = 0;
            column = 0;
            for (int word = 0; word < stride; word++) {
                if (word >= position && word < position + 3) continue;
                values[length++] = words[word];
                if ((varying & 1L << column++) != 0) {
                    for (int vertex = 1; vertex < 4; vertex++) values[length++] = words[vertex * stride + word];
                }
            }
            return new Columns(varying, values);
        }

        private void decodeInto(final int[] target, final int stride, final int position) {
            int index = 0;
            int column = 0;
            for (int word = 0; word < stride; word++) {
                if (word >= position && word < position + 3) continue;
                final boolean varied = (varying & 1L << column++) != 0;
                for (int vertex = 0; vertex < 4; vertex++)
                    target[vertex * stride + word] = values[index + (varied ? vertex : 0)];
                index += varied ? 4 : 1;
            }
        }

        private boolean matches(final int[] words, final int stride, final int position) {
            int index = 0;
            int column = 0;
            for (int word = 0; word < stride; word++) {
                if (word >= position && word < position + 3) continue;
                final boolean varied = (varying & 1L << column++) != 0;
                for (int vertex = 0; vertex < 4; vertex++)
                    if (words[vertex * stride + word] != values[index + (varied ? vertex : 0)]) return false;
                index += varied ? 4 : 1;
            }
            return true;
        }

        @Override
        public boolean equals(final Object other) {
            return other instanceof Columns that && varying == that.varying && Arrays.equals(values, that.values);
        }

        @Override
        public int hashCode() {
            return Long.hashCode(varying) * 31 + Arrays.hashCode(values);
        }
    }
}
