package com.extollit.gaming.ai.path.model.octree;

import com.extollit.linalg.immutable.IntAxisAlignedBox;

import java.lang.reflect.Array;

final class ContainerOctant<T> extends AbstractOctant<T> {
    private final AbstractOctant<T> [] children;
    private byte occupied;

    public ContainerOctant(ContainerOctant<T> parent, FramePointer pointer)
    {
        super(pointer, parent);
        this.children = createChildren();
    }

    public ContainerOctant(ContainerOctant<T> parent, FramePointer pointer, AbstractOctant<T> child, final byte parity) {
        super(pointer, parent);
        (this.children = createChildren())[parity] = child;
        child.bindTo(this);
        this.occupied = 1;
    }

    public ContainerOctant(FramePointer pointer) {
        this(null, pointer);
    }

    public ContainerOctant(FramePointer pointer, AbstractOctant<T> child, final byte parity) {
        this(null, pointer, child, parity);
    }

    @Override
    public AbstractOctant<T> trim() {
        final AbstractOctant<T>[] children = this.children;
        AbstractOctant<T> next = this;
        for (final AbstractOctant<T> o : children) {
            if (o != null)
                if (next == this)
                    next = o;
                else
                    return this;
        }

        if (next == this)
            return this;

        return next.trim();
    }

    @SuppressWarnings("unchecked")
    private static <T> AbstractOctant<T>[] createChildren() {
        return (AbstractOctant<T>[]) Array.newInstance(AbstractOctant.class, 2 * 2 * 2);
    }

    public final void traverseAll(OctantVisitor<T> visitor) {
        traverse(visitor, (byte)0);
        traverse(visitor, (byte)1);
        traverse(visitor, (byte)2);
        traverse(visitor, (byte)3);
        traverse(visitor, (byte)4);
        traverse(visitor, (byte)5);
        traverse(visitor, (byte)6);
        traverse(visitor, (byte)7);
    }

    public void traverseBounded(OctantVisitor<T> visitor, IntAxisAlignedBox key) {
        final FramePointer pointer = this.pointer;
        final com.extollit.linalg.mutable.Vec3i p = new com.extollit.linalg.mutable.Vec3i(key.min);
        final boolean [] flagged = new boolean[8];

        flagged[pointer.parityTo(p)] = true;
        p.x = key.max.x;
        flagged[pointer.parityTo(p)] = true;
        p.x = key.min.x;
        p.y = key.max.y;
        flagged[pointer.parityTo(p)] = true;
        p.x = key.max.x;
        flagged[pointer.parityTo(p)] = true;
        p.x = key.min.x;
        p.y = key.min.y;
        p.z = key.max.z;
        flagged[pointer.parityTo(p)] = true;
        p.x = key.max.x;
        flagged[pointer.parityTo(p)] = true;
        p.x = key.min.x;
        p.y = key.max.y;
        flagged[pointer.parityTo(p)] = true;
        p.x = key.max.x;
        flagged[pointer.parityTo(p)] = true;

        if (flagged[0])
            traverse(visitor, (byte)0);
        if (flagged[1])
            traverse(visitor, (byte)1);
        if (flagged[2])
            traverse(visitor, (byte)2);
        if (flagged[3])
            traverse(visitor, (byte)3);
        if (flagged[4])
            traverse(visitor, (byte)4);
        if (flagged[5])
            traverse(visitor, (byte)5);
        if (flagged[6])
            traverse(visitor, (byte)6);
        if (flagged[7])
            traverse(visitor, (byte)7);
    }

    public final void traverseTo(OctantVisitor<T> visitor, int x, int y, int z) {
        assert this.pointer.contains(x, y, z);
        traverse(visitor, this.pointer.parityTo(x, y, z));
    }

    private void traverse(OctantVisitor<T> visitor, byte parity) {
        final Root<T>
            frame = visitor.frame;

        AbstractOctant<T> child = this.children[parity];
        if (child == null) {
            final OctantAllocator<T> allocator = visitor.octantAllocator();
            if (allocator != null)
                child = allocateChild(allocator, parity, this.pointer.downTo(parity));
        }

        if (child != null) {
            frame.accept(child, visitor);

            if (child.empty()) {
                this.children[parity] = child = null;
                this.occupied--;
            }
        }
    }

    private AbstractOctant<T> allocateChild(final OctantAllocator<T> allocator, byte parity, FramePointer pointer) {
        AbstractOctant<T> child;
        if (pointer.scale <= VoxelOctTreeMap.LEAF_SIZE)
            child = allocator.allocateLeaf(this, pointer);
        else
            child = allocator.allocateContainer(this, pointer);

        this.children[parity] = child;
        this.occupied++;
        return child;
    }

    @Override
    public boolean empty() {
        return this.occupied <= 0;
    }

    @Override
    public final void accept(OctantVisitor<T> visitor) {
        visitor.visit(this);
    }
}
