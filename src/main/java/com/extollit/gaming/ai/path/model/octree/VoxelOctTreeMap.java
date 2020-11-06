package com.extollit.gaming.ai.path.model.octree;

import com.extollit.linalg.immutable.IntAxisAlignedBox;
import com.extollit.linalg.immutable.Vec3i;

import java.util.Collections;
import java.util.Iterator;

public class VoxelOctTreeMap< T > implements Iterable<T> {
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

        private void init(Root root, int x, int y, int z, T element) {
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

        private void init(Root root, int x, int y, int z) {
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

    private static final class BoxIterVisitor<T> extends AbstractIterVisitor<T> implements LeafOctant.IFilterFunc<T> {
        private IntAxisAlignedBox range;

        private void init(Root root, IntAxisAlignedBox range) {
            super.baseInit(root);
            this.range = range;
        }

        @Override
        protected Iterator<T> iterator(Iterator<LeafOctant<T>.Reference> leaves) {
            return new VoxelIterator<T>(new LeafOctant.FilteredIterator<T>(this), leaves);
        }

        public boolean test(T element, int x, int y, int z) {
            return this.range.contains(x, y, z);
        }

        @Override
        public void visit(ContainerOctant<T> container) {
            container.traverseBounded(this, this.range);
        }
    }

    private static final class AllIterVisitor<T> extends AbstractIterVisitor<T> {
        private void init(Root root) {
            super.baseInit(root);
        }

        @Override
        protected Iterator<T> iterator(Iterator<LeafOctant<T>.Reference> leaves) {
            return new VoxelIterator<T>(new LeafOctant.FullIterator<T>(), leaves);
        }

        @Override
        public void visit(ContainerOctant<T> container) {
            container.traverseAll(this);
        }
    }

    private final SetVisitor<T> setVisitor;
    private final GetVisitor<T> getVisitor = new GetVisitor<T>();
    private final BoxIterVisitor<T> boxIterVisitor = new BoxIterVisitor<T>();
    private final AllIterVisitor<T> allIterVisitor = new AllIterVisitor<T>();

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
            this.root.findRoot(x, y, z);

        return root;
    }

    public T put(int x, int y, int z, T value) {
        final Root<T> root = acquireRootIncluding(x, y, z);
        final SetVisitor<T> visitor = this.setVisitor;
        visitor.init(root, x, y, z, value);
        root.node().accept(visitor);
        return visitor.existing();
    }

    public T put(com.extollit.linalg.immutable.Vec3i coords, T value) {
        return put(coords.x, coords.y, coords.z, value);
    }

    public T remove(int x, int y, int z) {
        final Root<T> root = this.root;
        if (root == null)
            return null;

        final SetVisitor<T> visitor = this.setVisitor;
        visitor.init(root, x, y, z, null);
        root.node().accept(visitor);
        root.trim();
        return visitor.existing();
    }

    public T remove(com.extollit.linalg.immutable.Vec3i coords) {
        return remove(coords.x, coords.y, coords.z);
    }

    public T get(int x, int y, int z) {
        final Root<T> root = this.root;
        if (root == null || !root.frame.contains(x, y, z))
            return null;

        final GetVisitor<T> visitor = this.getVisitor;
        visitor.init(root, x, y, z);
        root.node().accept(visitor);
        return visitor.element();
    }

    public T get(com.extollit.linalg.immutable.Vec3i coords) {
        return get(coords.x, coords.y, coords.z);
    }

    public void clear() {
        this.root = null;
    }

    public boolean empty() {
        return this.root != null && !this.root.node().empty();
    }

    public void cullOutside(IntAxisAlignedBox range, Iteratee<T> iteratee) {
        final Root<T> root = this.root;
        if (root == null)
            return;

        final CullOutsideVisitor<T> visitor = new CullOutsideVisitor<T>(root, range, iteratee);
        root.node().accept(visitor);
        root.trim();
    }

    @Override
    public Iterator<T> iterator() {
        final Root<T> root = this.root;
        if (root == null)
            return Collections.emptyIterator();

        final AllIterVisitor<T> visitor = this.allIterVisitor;
        visitor.init(root);
        root.node().accept(visitor);
        return visitor.iterator();
    }

    public Iterator<T> iterator(Vec3i min, Vec3i max) {
        final Root<T> root = this.root;
        if (root == null)
            return Collections.emptyIterator();

        final BoxIterVisitor<T> visitor = this.boxIterVisitor;
        visitor.init(root, new IntAxisAlignedBox(min, max));
        root.node().accept(visitor);
        return visitor.iterator();
    }

    public Iterator<T> iterator(IntAxisAlignedBox bounds) {
        return iterator(bounds.min, bounds.max);
    }
}

