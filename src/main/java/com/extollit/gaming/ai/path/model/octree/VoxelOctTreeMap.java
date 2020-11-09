package com.extollit.gaming.ai.path.model.octree;

import com.extollit.linalg.immutable.IntAxisAlignedBox;
import com.extollit.linalg.immutable.Vec3i;

public class VoxelOctTreeMap< T > {
    static final int
            LEAF_BITS = 2,
            LEAF_SIZE = 1 << LEAF_BITS,
            LEAF_MASK = LEAF_SIZE - 1,
            HALF_LEAF_SIZE = LEAF_SIZE / 2;

    private static final class SetVisitor<T> extends AllocatingOctantVisitor<T> {
        private int x, y, z;
        private T element, existing;

        protected SetVisitor(final Class<T> elementClass) {
            super(elementClass);
        }

        private void init(Root<T> root, int x, int y, int z, T element) {
            super.baseInit(root);
            this.x = x;
            this.y = y;
            this.z = z;
            this.element = element;
            this.existing = null;
        }

        @Override
        public void visit(ContainerOctant<T> container) {
            container.traverseTo(this, this.x, this.y, this.z);
        }

        @Override
        public void visit(LeafOctant<T> leaf) {
            this.existing = set(leaf, this.x, this.y, this.z, this.element);
        }

        public final T existing() { return this.existing; }
    }

    private static final class GetVisitor<T> extends OctantVisitor<T> {
        private int x, y, z;
        
        private T element;

        private void init(Root<T> root, int x, int y, int z) {
            super.baseInit(root);
            this.x = x;
            this.y = y;
            this.z = z;
            this.element = null;
        }

        @Override
        public void visit(ContainerOctant<T> container) {
            container.traverseTo(this, this.x, this.y, this.z);
        }

        @Override
        public void visit(LeafOctant<T> leaf) {
            this.element = get(leaf, this.x, this.y, this.z);
        }

        public final T element() { return this.element; }
    }

    private static final class BoxEachVisitor<T> extends IterateeVisitor<T> implements LeafOctant.IFilterFunc<T> {
        private IntAxisAlignedBox range;

        private BoxEachVisitor() {}

        @Override
        protected LeafOctant.AbstractIterator<T> createIterator() {
            return new LeafOctant.FilteredIterator<T>(this);
        }

        private void init(Root root, IntAxisAlignedBox range, Iteratee<T> iteratee) {
            super.baseInit(root, iteratee);
            this.range = range;
        }

        public boolean test(T element, int x, int y, int z) {
            return this.range.contains(x, y, z);
        }

        @Override
        public void visit(ContainerOctant<T> container) {
            container.traverseBounded(this, this.range);
        }

    }

    private static final class ForEachVisitor<T> extends IterateeVisitor<T> {
        private ForEachVisitor() {}

        @Override
        protected LeafOctant.AbstractIterator<T> createIterator() {
            return new LeafOctant.FullIterator<T>();
        }

        private void init(Root root, Iteratee<T> iteratee) {
            super.baseInit(root, iteratee);
        }

        @Override
        public void visit(ContainerOctant<T> container) {
            container.traverseAll(this);
        }
    }

    private final SetVisitor<T> setVisitor;
    private final GetVisitor<T> getVisitor = new GetVisitor<T>();
    private final BoxEachVisitor<T> boxEachVisitor = new BoxEachVisitor<T>();
    private final ForEachVisitor<T> forEachVisitor = new ForEachVisitor<T>();

    private final Class<T> elementClass;

    private Root<T> root;

    public VoxelOctTreeMap(Class<T> elementClass) {
        this.setVisitor = new SetVisitor<T>(this.elementClass = elementClass);
    }

    private Root<T> acquireRootIncluding(int x, int y, int z) {
        Root<T> root = this.root;

        if (root == null)
            root = this.root = new Root<T>(elementClass, x, y, z);
        else
            this.root.findRoot(x, y, z, true);

        return root;
    }

    private AbstractOctant<T> attainRootIncluding(int x, int y, int z) {
        Root<T> root = this.root;

        if (root == null)
            return null;
        else
            return this.root.findRoot(x, y, z, false);
    }

    public T put(int x, int y, int z, T value) {
        final Root<T> root = acquireRootIncluding(x, y, z);
        final SetVisitor<T> visitor = this.setVisitor;
        visitor.init(root, x, y, z, value);
        root.accept(visitor);
        return visitor.existing();
    }

    public T put(com.extollit.linalg.immutable.Vec3i coords, T value) {
        return put(coords.x, coords.y, coords.z, value);
    }

    public T remove(int x, int y, int z) {
        final AbstractOctant<T> octant = attainRootIncluding(x, y, z);
        if (octant == null)
            return null;

        final SetVisitor<T> visitor = this.setVisitor;
        visitor.init(this.root, x, y, z, null);
        octant.accept(visitor);
        this.root.trim();
        return visitor.existing();
    }

    public T remove(com.extollit.linalg.immutable.Vec3i coords) {
        return remove(coords.x, coords.y, coords.z);
    }

    public T get(int x, int y, int z) {
        final AbstractOctant<T> octant = attainRootIncluding(x, y, z);
        if (octant == null)
            return null;

        final GetVisitor<T> visitor = this.getVisitor;
        visitor.init(this.root, x, y, z);
        octant.accept(visitor);
        return visitor.element();
    }

    public T get(com.extollit.linalg.immutable.Vec3i coords) {
        return get(coords.x, coords.y, coords.z);
    }

    public void clear() {
        this.root = null;
    }

    public boolean empty() {
        return this.root != null && !this.root.empty();
    }

    public void cullOutside(IntAxisAlignedBox range, Iteratee<T> iteratee) {
        if (this.root == null)
            return;

        final AbstractOctant<T> octant = this.root.resetRoot();
        if (octant == null)
            return;

        final CullOutsideVisitor<T> visitor = new CullOutsideVisitor<T>(this.root, range, iteratee);
        this.root.accept(visitor);
        this.root.trim();
    }

    public void forEach(Iteratee<T> iteratee) {
        if (this.root == null)
            return;

        final AbstractOctant<T> octant = this.root.resetRoot();
        if (octant == null)
            return;

        final ForEachVisitor<T> visitor = this.forEachVisitor;
        visitor.init(this.root, iteratee);
        this.root.accept(visitor);
    }

    public void forEachIn(Vec3i min, Vec3i max, Iteratee<T> iteratee) {
        if (this.root == null)
            return;

        final AbstractOctant<T> octant = this.root.resetRoot();
        if (octant == null)
            return;

        final BoxEachVisitor<T> visitor = this.boxEachVisitor;
        visitor.init(this.root, new IntAxisAlignedBox(min, max), iteratee);
        this.root.accept(visitor);
    }

    public void forEachIn(IntAxisAlignedBox bounds, Iteratee<T> iteratee) {
        forEachIn(bounds.min, bounds.max, iteratee);
    }
}

