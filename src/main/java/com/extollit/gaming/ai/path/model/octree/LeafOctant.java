package com.extollit.gaming.ai.path.model.octree;

import com.extollit.linalg.immutable.Vec3i;

import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.NoSuchElementException;

final class LeafOctant<T> extends AbstractOctant<T> {
    private final T [][][] voxels;
    private byte [] indices = new byte[4];
    private byte count;

    @SuppressWarnings("unchecked")
    public LeafOctant(ContainerOctant<T> parent, FramePointer pointer, final Class<T> elementClass) {
        super(pointer, parent);
        this.voxels = (T[][][]) Array.newInstance(elementClass, VoxelOctTreeMap.LEAF_SIZE, VoxelOctTreeMap.LEAF_SIZE, VoxelOctTreeMap.LEAF_SIZE);
    }

    public LeafOctant(FramePointer pointer, final Class<T> elementClass) {
        this(null, pointer, elementClass);
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
        protected LeafOctant<T> delegate;

        protected int i = -1;
        private int i0;

        private T element;
        private int
            dx0, dy0, dz0,
            dx, dy, dz;

        void init(LeafOctant<T> delegate) {
            this.delegate = delegate;
            this.i = -1;
        }

        final LeafOctant<T> delegate() { return this.delegate; }
        final int index() { return this.i0; }

        @Override
        public boolean hasNext() {
            if (this.i == -1)
                this.element = find();

            return this.element != null;
        }

        private T find() {
            final LeafOctant<T> referrent = delegate();
            final byte[] indices = referrent.indices;

            this.i0 = this.i;
            this.dx0 = this.dx;
            this.dy0 = this.dy;
            this.dz0 = this.dz;

            T element;
            int i;
            while ((i = ++this.i) < referrent.size()) {
                byte c = indices[i];
                final int dx = this.dx = c & VoxelOctTreeMap.LEAF_MASK;
                c >>= VoxelOctTreeMap.LEAF_BITS;
                final int dy = this.dy = c & VoxelOctTreeMap.LEAF_MASK;
                c >>= VoxelOctTreeMap.LEAF_BITS;
                final int dz = this.dz = c & VoxelOctTreeMap.LEAF_MASK;

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

        public void next(Iteratee<T> iteratee) {
            final Vec3i mp = this.delegate.pointer.mp;
            final int halfScale = this.delegate.pointer.scale >> 1;
            final T next = next();
            iteratee.visit(
                next,

                mp.x + this.dx0 - halfScale,
                mp.y + this.dy0 - halfScale,
                mp.z + this.dz0 - halfScale
            );
        }

        @Override
        public void remove() {
            final LeafOctant<T> delegate = this.delegate;
            delegate.set(this.dx0, this.dy0, this.dz0, null);
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
            final Vec3i mp = this.delegate.pointer.mp;
            final int halfScale = VoxelOctTreeMap.HALF_LEAF_SIZE;
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
