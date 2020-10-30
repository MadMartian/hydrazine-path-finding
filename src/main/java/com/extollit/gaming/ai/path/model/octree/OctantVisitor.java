package com.extollit.gaming.ai.path.model.octree;

abstract class OctantVisitor<T> {
    final Frame frame;

    protected OctantVisitor(Root root) {
        this.frame = new Frame(root.frame);
    }

    public OctantAllocator<T> octantAllocator() { return null; }
    public abstract void visit(ContainerOctant<T> container);
    public abstract void visit(LeafOctant<T> leaf);

    protected final com.extollit.linalg.immutable.Vec3i offset() {
        final int halfLeafSize = VoxelOctTreeMap.HALF_LEAF_SIZE;
        final Frame frame = this.frame;
        return new com.extollit.linalg.immutable.Vec3i(
            frame.x() - halfLeafSize,
            frame.y() - halfLeafSize,
            frame.z() - halfLeafSize
        );
    }

    protected final T set(LeafOctant<T> leaf, int x, int y, int z, T element) {
        final Frame frame = this.frame;
        final int halfLeafSize = VoxelOctTreeMap.HALF_LEAF_SIZE;
        final int
                dx = x - (frame.x() - halfLeafSize),
                dy = y - (frame.y() - halfLeafSize),
                dz = z - (frame.z() - halfLeafSize);

        return leaf.set(dx, dy, dz, element);
    }

    protected final T get(LeafOctant<T> leaf, int x, int y, int z) {
        final Frame frame = this.frame;
        final int halfLeafSize = VoxelOctTreeMap.HALF_LEAF_SIZE;
        final int
                dx = x - (frame.x() - halfLeafSize),
                dy = y - (frame.y() - halfLeafSize),
                dz = z - (frame.z() - halfLeafSize);

        return leaf.get(dx, dy, dz);
    }
}

abstract class AllocatingOctantVisitor<T> extends OctantVisitor<T> implements OctantAllocator<T> {
    final Class<T> elementClass;

    protected AllocatingOctantVisitor(Root root, Class<T> elementClass) {
        super(root);
        this.elementClass = elementClass;
    }

    @Override
    public final OctantAllocator<T> octantAllocator() {
        return this;
    }

    @Override
    public LeafOctant<T> allocateLeaf() {
        return new LeafOctant<>(this.elementClass);
    }

    @Override
    public ContainerOctant<T> allocateContainer() {
        return new ContainerOctant<>();
    }
}