package com.extollit.gaming.ai.path.model.octree;

import com.extollit.linalg.immutable.Vec3i;

import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.NoSuchElementException;

final class LeafOctant<T> extends AbstractOctant<T> {
    public final class Reference {
        public final Vec3i mp;

        public Reference(Vec3i mp) {
            this.mp = mp;
        }

        public final LeafOctant<T> referrent() { return LeafOctant.this; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Reference reference = (Reference) o;

            if (reference.referrent() != LeafOctant.this) return false;
            return mp.equals(reference.mp);
        }

        @Override
        public int hashCode() {
            return this.mp.hashCode();
        }
    }

    private final T [][][] voxels;
    private byte [] indices = new byte[4];
    private byte count;

    @SuppressWarnings("unchecked")
    public LeafOctant(final Class<T> elementClass) {
        this.voxels = (T[][][]) Array.newInstance(elementClass, VoxelOctTreeMap.LEAF_SIZE, VoxelOctTreeMap.LEAF_SIZE, VoxelOctTreeMap.LEAF_SIZE);
    }

    public final Reference referredBy(Frame frame) {
        return new Reference(new Vec3i(frame.mp()));
    }

    public final byte size() { return this.count; }

    @Override
    public final void accept(OctantVisitor<T> visitor) {
        visitor.visit(this);
    }

    public final T get(int x, int y, int z) {
        return this.voxels[z][y][x];
    }

    final T remove(int index) {
        byte c = this.indices[index];
        final int dx = c & VoxelOctTreeMap.LEAF_MASK;
        c >>= VoxelOctTreeMap.LEAF_BITS;
        final int dy = c & VoxelOctTreeMap.LEAF_MASK;
        c >>= VoxelOctTreeMap.LEAF_BITS;
        final int dz = c & VoxelOctTreeMap.LEAF_MASK;

        return set(dx, dy, dz, null);
    }

    public final T set(int x, int y, int z, T element) {
        T existing = this.voxels[z][y][x];
        this.voxels[z][y][x] = element;
        if (existing == null && element != null) {
            final int leafSize = VoxelOctTreeMap.LEAF_SIZE;
            final int i = this.count++;
            byte[] indices = this.indices;
            if (i >= indices.length) {
                byte [] newIndices = new byte[Math.min(leafSize * leafSize * leafSize, indices.length  + indices.length / 3)];
                System.arraycopy(indices, 0, newIndices, 0, indices.length);
                this.indices = indices = newIndices;
            }

            indices[i] = serial(x, y, z);
        } else if (existing != null && element == null) {
            final int count0 = this.count;
            final byte[] indices = this.indices;
            final byte serial = serial(x, y, z);
            for (int i = 0; i < count0; ++i) {
                if (indices[i] == serial) {
                    indices[i] = indices[--this.count];
                    break;
                }
            }
        }

        return existing;
    }

    private static byte serial(int x, int y, int z) {
        return (byte)(
            (z << (VoxelOctTreeMap.LEAF_BITS << 1)) |
            (y << VoxelOctTreeMap.LEAF_BITS) |
            x
        );
    }

    public final void clear() {
        final T[][][] voxels = this.voxels;
        final int leafSize = VoxelOctTreeMap.LEAF_SIZE;
        for (int z = 0; z < leafSize; ++z)
            for (int y = 0; y < leafSize; ++y)
                for (int x = 0; x < leafSize; ++x)
                    voxels[z][y][x] = null;

        this.count = 0;
    }

    @Override
    public final boolean empty() { return this.count <= 0; }

    static abstract class AbstractIterator<T> implements Iterator<T> {
        protected LeafOctant<T>.Reference reference;

        protected int i = -1;
        private int i0;

        private T element;

        void init(LeafOctant<T>.Reference reference) {
            this.reference = reference;
            this.i = -1;
        }

        final LeafOctant<T> referrent() { return this.reference.referrent(); }
        final int index() { return this.i0; }

        @Override
        public boolean hasNext() {
            if (this.i == -1)
                this.element = find();

            return this.element != null;
        }

        private T find() {
            final LeafOctant<T> referrent = referrent();
            final byte[] indices = referrent.indices;

            this.i0 = this.i;

            T element;
            int i;
            while ((i = ++this.i) < referrent.size()) {
                byte c = indices[i];
                final int dx = c & VoxelOctTreeMap.LEAF_MASK;
                c >>= VoxelOctTreeMap.LEAF_BITS;
                final int dy = c & VoxelOctTreeMap.LEAF_MASK;
                c >>= VoxelOctTreeMap.LEAF_BITS;
                final int dz = c & VoxelOctTreeMap.LEAF_MASK;

                element = referrent.get(dx, dy, dz);
                if (filterTestRelative(element, dx, dy, dz))
                    return element;
            }

            return null;
        }

        protected abstract boolean filterTestRelative(T element, int dx, int dy, int dz);

        @Override
        public T next() {
            if (this.i == -1)
                this.element = find();
            else if (this.element == null)
                throw new NoSuchElementException();

            final T current = this.element;
            this.element = find();
            return current;
        }

        @Override
        public void remove() {
            final LeafOctant<T> referrent = this.reference.referrent();
            byte c = referrent.indices[this.i0];
            final int dx = c & VoxelOctTreeMap.LEAF_MASK;
            c >>= VoxelOctTreeMap.LEAF_BITS;
            final int dy = c & VoxelOctTreeMap.LEAF_MASK;
            c >>= VoxelOctTreeMap.LEAF_BITS;
            final int dz = c & VoxelOctTreeMap.LEAF_MASK;

            referrent.set(dx, dy, dz, null);
        }
    }


    static final class FullIterator<T> extends AbstractIterator<T> {
        @Override
        protected boolean filterTestRelative(T element, int dx, int dy, int dz) {
            return true;
        }
    }

    static final class FilteredIterator<T> extends AbstractIterator<T> {
        private final IFilterFunc<T> filter;

        public FilteredIterator(IFilterFunc<T> filter) {
            this.filter = filter;
        }

        @Override
        protected boolean filterTestRelative(T element, int dx, int dy, int dz) {
            final LeafOctant<T>.Reference reference = this.reference;
            final int halfScale = VoxelOctTreeMap.HALF_LEAF_SIZE;
            final Vec3i mp = reference.mp;
            final int
                    x = dx + mp.x - halfScale,
                    y = dy + mp.y - halfScale,
                    z = dz + mp.z - halfScale;

            return this.filter.test(element, x, y, z);
        }
    }

    interface IFilterFunc<T> {
        boolean test(T element, int x, int y, int z);
    }
}
