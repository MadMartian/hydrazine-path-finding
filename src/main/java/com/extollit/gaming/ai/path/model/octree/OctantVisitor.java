package com.extollit.gaming.ai.path.model.octree;

import com.extollit.linalg.immutable.Vec3i;

abstract class OctantVisitor<T> {
    protected Root<T> frame;

    protected void baseInit(Root<T> root) {
        this.frame = root;
    }

    public OctantAllocator<T> octantAllocator() { return null; }
    public abstract void visit(ContainerOctant<T> container);
    public abstract void visit(LeafOctant<T> leaf);

    protected final com.extollit.linalg.immutable.Vec3i offset() {
        final int halfLeafSize = VoxelOctTreeMap.HALF_LEAF_SIZE;
        final FramePointer pointer = this.frame.pointer();
        return pointer.mp.subOf(halfLeafSize);
    }

    protected final T set(LeafOctant<T> leaf, int x, int y, int z, T element) {
        final int halfLeafSize = VoxelOctTreeMap.HALF_LEAF_SIZE;
        final FramePointer pointer = this.frame.pointer();
        final Vec3i mp = pointer.mp;
        final int
                dx = x - (mp.x - halfLeafSize),
                dy = y - (mp.y - halfLeafSize),
                dz = z - (mp.z - halfLeafSize);

        return leaf.set(dx, dy, dz, element);
    }

    protected final T get(LeafOctant<T> leaf, int x, int y, int z) {
        final int halfLeafSize = VoxelOctTreeMap.HALF_LEAF_SIZE;
        final FramePointer pointer = this.frame.pointer();
        final Vec3i mp = pointer.mp;
        final int
                dx = x - (mp.x - halfLeafSize),
                dy = y - (mp.y - halfLeafSize),
                dz = z - (mp.z - halfLeafSize);

        return leaf.get(dx, dy, dz);
    }
}

abstract class AllocatingOctantVisitor<T> extends OctantVisitor<T> implements OctantAllocator<T> {
    private final Class<T> elementClass;

    protected AllocatingOctantVisitor(Class<T> elementClass) {
        this.elementClass = elementClass;
    }

    @Override
    public final OctantAllocator<T> octantAllocator() {
        return this;
    }

    @Override
    public LeafOctant<T> allocateLeaf(ContainerOctant<T> parent, FramePointer pointer) {
        return new LeafOctant<>(parent, pointer, this.elementClass);
    }

    @Override
    public ContainerOctant<T> allocateContainer(ContainerOctant<T> parent, FramePointer pointer) {
        return new ContainerOctant<>(parent, pointer);
    }
}