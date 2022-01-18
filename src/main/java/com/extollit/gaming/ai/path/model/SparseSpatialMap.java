package com.extollit.gaming.ai.path.model;

import com.extollit.collect.FilterIterable;
import com.extollit.collect.FlattenIterable;

import java.util.*;

class SparseSpatialMap<T extends INode> {
    private static final float
        OUTER_LOAD_FACTOR = 0.9f;

    private static final class CoarseKey {
        private int x, y, z;
        private int hashCode;

        private CoarseKey(int x, int y, int z) {
            set(x, y, z);
        }

        CoarseKey() {}

        CoarseKey(CoarseKey original) {
            this.x = original.x;
            this.y = original.y;
            this.z = original.z;
            this.hashCode = original.hashCode;
        }

        CoarseKey dup() {
            return new CoarseKey(this);
        }

        void set(int x, int y, int z) {
            this.x = x >>= 3;
            this.y = y >>= 3;
            this.z = z >>= 3;

            int result = 1;
            result = 31 * result + z;
            result = 31 * result + y;
            result = 31 * result + x;
            this.hashCode = result;
        }

        @Override
        public final boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            final CoarseKey coarseKey = (CoarseKey) o;
            return x == coarseKey.x &&
                y == coarseKey.y &&
                z == coarseKey.z;
        }

        @Override
        public final int hashCode() { return this.hashCode; }
    }

    private final HashMap<CoarseKey, TreeMap<Integer, T>> space;
    private final CoarseKey coarseKey = new CoarseKey();

    private int size;
    private int kx0, ky0, kz0;
    private TreeMap<Integer, T> inner0;

    public SparseSpatialMap() {
        this.space = new HashMap<>(4, OUTER_LOAD_FACTOR);
    }

    private int relativeKey(int x, int y, int z) {
        return (x & 0xFF) | (y & 0xFF) << 8 | (z & 0xFF) << 16;
    }

    public boolean has(int x, int y, int z) {
        final Map<Integer, T> inner = acquireInner(x, y, z);
        if (inner != null) {
            final int relativeKey = relativeKey(x, y, z);
            return inner.containsKey(relativeKey);
        }
        return false;
    }

    public boolean has(T value) {
        if (value == null)
            throw new NullPointerException();

        for (Map<Integer, T> inner : this.space.values())
            if (inner.containsValue(value))
                return true;

        return false;
    }

    public T get(int x, int y, int z) {
        final Map<Integer, T> inner = acquireInner(x, y, z);
        if (inner != null) {
            final int relativeKey = relativeKey(x, y, z);
            return inner.get(relativeKey);
        }
        return null;
    }

    public T put(int x, int y, int z, T value) {
        if (value == null)
            throw new NullPointerException();

        this.kx0 = x;
        this.ky0 = y;
        this.kz0 = z;

        final CoarseKey coarseKey = this.coarseKey;
        coarseKey.set(x, y, z);

        TreeMap<Integer, T> inner = this.inner0 = this.space.get(coarseKey);
        if (inner == null)
            this.space.put(coarseKey.dup(), inner = this.inner0 = new TreeMap<Integer, T>());

        final T value0 = inner.put(relativeKey(x, y, z), value);
        if (value0 == null)
            this.size++;

        return value0;
    }

    public T remove(int x, int y, int z) {
        final CoarseKey coarseKey = this.coarseKey;

        final Map<Integer, T> inner;
        if (this.inner0 != null && x == this.kx0 && y == this.ky0 && z == this.kz0)
            inner = this.inner0;
        else {
            this.kx0 = x;
            this.ky0 = y;
            this.kz0 = z;

            coarseKey.set(x, y, z);
            inner = this.inner0 = this.space.get(coarseKey);
        }

        if (inner != null) try {
            final int relativeKey = relativeKey(x, y, z);
            final T value0 = inner.remove(relativeKey);
            if (value0 != null)
                this.size--;
            return value0;
        } finally {
            if (inner.isEmpty()) {
                this.space.remove(coarseKey);
                this.inner0 = null;
            }
        }
        return null;
    }

    public void clear() {
        this.space.clear();
        this.size = 0;
        this.inner0 = null;
    }

    public Iterable<T> cullOutside(int x0, int y0, int z0, int xN, int yN, int zN) {
        final int size0 = this.size;
        final List<Collection<T>> cullees = new LinkedList<Collection<T>>();

        final Iterator<Map.Entry<CoarseKey, TreeMap<Integer, T>>> i = this.space.entrySet().iterator();
        while (i.hasNext()) {
            final Map.Entry<CoarseKey, TreeMap<Integer, T>> entry = i.next();
            final CoarseKey key = entry.getKey();
            final TreeMap<Integer, T> subMap = entry.getValue();
            if (key.x < x0 || key.y < y0 || key.z < z0 || key.x > xN || key.y > yN || key.z > zN) {
                i.remove();
                cullees.add(subMap.values());
                size -= subMap.size();
            }
        }

        if (size0 != size)
            this.inner0 = null;

        return new FlattenIterable<T>(cullees);
    }

    private abstract class AbstractIterator<V> extends FilterIterable.Iter<V> implements Iterator<V> {
        private Iterator<Map.Entry<CoarseKey, TreeMap<Integer, T>>> oi;
        private Iterator<Map.Entry<Integer, T>> ii;

        public AbstractIterator () {
            this.oi = SparseSpatialMap.this.space.entrySet().iterator();
        }

        @Override
        protected V findNext() {
            final Iterator<Map.Entry<CoarseKey, TreeMap<Integer, T>>> oi = this.oi;
            Iterator<Map.Entry<Integer, T>> ii = this.ii;

            while (ii == null || !ii.hasNext()) {
                if (oi.hasNext()) {
                    final Map.Entry<CoarseKey, TreeMap<Integer, T>> entry = oi.next();
                    ii = this.ii = entry.getValue().entrySet().iterator();
                } else
                    return null;
            }

            final Map.Entry<Integer, T> entry = ii.next();
            return map(entry.getValue());
        }
        protected abstract V map(T value);
    }

    private final class KeySet extends AbstractSet<Coords> {
        private final class Iter extends AbstractIterator<Coords> {
            @Override
            protected final Coords map(T value) {
                return value.coordinates();
            }
        }

        @Override
        public Iterator<Coords> iterator() {
            return new Iter();
        }

        @Override
        public int size() {
            return SparseSpatialMap.this.size;
        }
    }

    public Set<Coords> keySet() {
        return new KeySet();
    }
    private final class ValueCollection extends AbstractCollection<T> {
        private final class Iter extends AbstractIterator<T> {
            @Override
            protected final T map(T value) {
                return value;
            }
        }

        @Override
        public Iterator<T> iterator() {
            return new ValueCollection.Iter();
        }

        @Override
        public int size() {
            return SparseSpatialMap.this.size;
        }
    }

    public Collection<T> values() {
        return new ValueCollection();
    }

    private Map<Integer, T> acquireInner(int x, int y, int z) {
        if (this.inner0 != null && this.kx0 == x && this.ky0 == y && this.kz0 == z)
            return this.inner0;

        this.kx0 = x;
        this.ky0 = y;
        this.kz0 = z;

        final CoarseKey coarseKey = this.coarseKey;
        coarseKey.set(x, y, z);
        return this.inner0 = this.space.get(coarseKey);
    }
}
