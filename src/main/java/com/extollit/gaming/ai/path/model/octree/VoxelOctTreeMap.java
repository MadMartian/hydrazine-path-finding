package com.extollit.gaming.ai.path.model.octree;

import com.extollit.linalg.immutable.IntAxisAlignedBox;
import com.extollit.linalg.immutable.Vec3i;

import java.util.Collections;
import java.util.Iterator;

public class VoxelOctTreeMap< T > implements Iterable<T> {
    static final int
            LEAF_SIZE = 8,
            LEAF_MASK = LEAF_SIZE - 1,
            HALF_LEAF_SIZE = LEAF_SIZE / 2;

    private final class SetVisitor extends AllocatingOctantVisitor<T> {
        public final int x, y, z;
        public final T element;

        private T existing;

        public SetVisitor(T element, int x, int y, int z) {
            super(VoxelOctTreeMap.this.root, VoxelOctTreeMap.this.elementClass);
            this.x = x;
            this.y = y;
            this.z = z;
            this.element = element;
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

    private final class GetVisitor extends OctantVisitor<T> {
        public final int x, y, z;
        
        private T element;

        public GetVisitor(int x, int y, int z) {
            super(VoxelOctTreeMap.this.root);
            this.x = x;
            this.y = y;
            this.z = z;
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

    private final class BoxIterVisitor extends AbstractIterVisitor<T> {
        public final IntAxisAlignedBox range;

        public BoxIterVisitor(IntAxisAlignedBox range) {
            super(VoxelOctTreeMap.this.root);
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

    private final class AllIterVisitor extends AbstractIterVisitor<T> {
        public AllIterVisitor() {
            super(VoxelOctTreeMap.this.root);
        }

        public boolean test(T element, int x, int y, int z) {
            return true;
        }

        @Override
        public void visit(ContainerOctant<T> container) {
            container.traverseAll(this);
        }
    }

    private final Class<T> elementClass;

    private Root<T> root;

    public VoxelOctTreeMap(Class<T> elementClass) {
        this.elementClass = elementClass;
    }

    private AbstractOctant<T> acquireRoot(int x, int y, int z) {
        Root<T> root = this.root;

        if (root == null)
            root = this.root = new Root<T>(elementClass, x, y, z);
        else
            this.root.findRoot(x, y, z);

        return root.node();
    }

    public T put(int x, int y, int z, T value) {
        final AbstractOctant<T> root = acquireRoot(x, y, z);
        final SetVisitor visitor = new SetVisitor(value, x, y, z);
        root.accept(visitor);
        return visitor.existing();
    }

    public T put(com.extollit.linalg.immutable.Vec3i coords, T value) {
        return put(coords.x, coords.y, coords.z, value);
    }

    public T remove(int x, int y, int z) {
        if (this.root == null)
            return null;

        final SetVisitor visitor = new SetVisitor(null, x, y, z);
        this.root.node().accept(visitor);
        this.root.trim();
        return visitor.existing();
    }

    public T remove(com.extollit.linalg.immutable.Vec3i coords) {
        return remove(coords.x, coords.y, coords.z);
    }

    public T get(int x, int y, int z) {
        if (this.root == null || !this.root.frame.contains(x, y, z))
            return null;

        final GetVisitor visitor = new GetVisitor(x, y, z);
        this.root.node().accept(visitor);
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
        if (this.root == null)
            return;

        final CullOutsideVisitor<T> visitor = new CullOutsideVisitor<T>(this.root, range, iteratee);
        this.root.node().accept(visitor);
        this.root.trim();
    }

    @Override
    public Iterator<T> iterator() {
        if (this.root == null)
            return Collections.emptyIterator();

        final AllIterVisitor visitor = new AllIterVisitor();
        this.root.node().accept(visitor);
        return visitor.iterator();
    }

    public Iterator<T> iterator(Vec3i min, Vec3i max) {
        if (this.root == null)
            return Collections.emptyIterator();

        final BoxIterVisitor visitor = new BoxIterVisitor(new IntAxisAlignedBox(min, max));
        this.root.node().accept(visitor);
        return visitor.iterator();
    }

    public Iterator<T> iterator(IntAxisAlignedBox bounds) {
        return iterator(bounds.min, bounds.max);
    }
}

