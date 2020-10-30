package com.extollit.gaming.ai.path.model.octree;

import com.extollit.linalg.immutable.Vec3i;

import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.NoSuchElementException;

final class LeafOctant<T> extends AbstractOctant<T> implements Iterable<T> {
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

        public Iterator<T> iterator(IFilterFunc<T> filter) {
            return new Iter(this, filter);
        }
    }

    private final T [][][] voxels;
    private int occupied;

    @SuppressWarnings("unchecked")
    public LeafOctant(final Class<T> elementClass) {
        this.voxels = (T[][][]) Array.newInstance(elementClass, VoxelOctTreeMap.LEAF_SIZE, VoxelOctTreeMap.LEAF_SIZE, VoxelOctTreeMap.LEAF_SIZE);
    }

    public Reference referredBy(Frame frame) {
        return new Reference(new Vec3i(frame.mp()));
    }

    @Override
    public final void accept(OctantVisitor<T> visitor) {
        visitor.visit(this);
    }

    public final T set(int x, int y, int z, T element) {
        T existing = this.voxels[z][y][x];
        this.voxels[z][y][x] = element;
        if (existing == null && element != null)
            this.occupied++;
        else if (existing != null && element == null)
            this.occupied--;

        return existing;
    }
    public T get(int x, int y, int z) {
        return this.voxels[z][y][x];
    }
    public void clear() {
        final T[][][] voxels = this.voxels;
        final int leafSize = VoxelOctTreeMap.LEAF_SIZE;
        for (int z = 0; z < leafSize; ++z)
            for (int y = 0; y < leafSize; ++y)
                for (int x = 0; x < leafSize; ++x)
                    voxels[z][y][x] = null;

        this.occupied = 0;
    }

    @Override
    public boolean empty() { return this.occupied <= 0; }

    private class Iter implements Iterator<T> {
        private final Reference reference;
        private final IFilterFunc<T> filter;

        private int
                x = -1, y, z,
                x0, y0, z0;

        private T element;

        public Iter() {
            this(null, null);
        }

        public Iter(Reference reference, IFilterFunc<T> filter) {
            this.reference = reference;
            this.filter = filter;
        }

        @Override
        public boolean hasNext() {
            if (this.x == -1)
                this.element = find();

            return this.element != null;
        }

        private T find() {
            final int last = VoxelOctTreeMap.LEAF_SIZE - 1;

            T element;

            final IFilterFunc<T> filter = this.filter;
            this.x0 = this.x;
            this.y0 = this.y;
            this.z0 = this.z;
            do {
                if (this.x < last)
                    this.x++;
                else if (this.y < last) {
                    this.y++;
                    this.x = 0;
                } else if (this.z < last) {
                    this.z++;
                    this.y = this.x = 0;
                } else {
                    element = null;
                    break;
                }

                element = LeafOctant.this.get(this.x, this.y, this.z);
            } while (element == null || (filter != null && !filterTestRelative(element, filter)));

            return element;
        }

        private boolean filterTestRelative(T element, IFilterFunc<T> filter) {
            final Reference reference = this.reference;
            final int halfScale = VoxelOctTreeMap.HALF_LEAF_SIZE;
            final Vec3i mp = reference.mp;
            final int
                x = this.x + mp.x - halfScale,
                y = this.y + mp.y - halfScale,
                z = this.z + mp.z - halfScale;

            return filter.test(element, x, y, z);
        }

        @Override
        public T next() {
            if (this.x == -1)
                this.element = find();
            else if (this.element == null)
                throw new NoSuchElementException();

            final T current = this.element;
            this.element = find();
            return current;
        }

        @Override
        public void remove() {
            LeafOctant.this.set(this.x0, this.y0, this.z0, null);
        }
    }

    interface IFilterFunc<T> {
        boolean test(T element, int x, int y, int z);
    }

    @Override
    public Iterator<T> iterator() {
        return new Iter();
    }
}
