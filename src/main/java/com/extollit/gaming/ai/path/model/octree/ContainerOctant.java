package com.extollit.gaming.ai.path.model.octree;

import com.extollit.linalg.immutable.IntAxisAlignedBox;
import com.extollit.linalg.mutable.Vec3i;

import java.lang.reflect.Array;

final class ContainerOctant<T> extends AbstractOctant<T> {
    private final AbstractOctant<T> [] children;
    private byte occupied;

    public ContainerOctant() {
        this.children = createChildren();
    }

    public ContainerOctant(AbstractOctant<T> child, final byte parity) {
        (this.children = createChildren())[parity] = child;
        this.occupied = 1;
    }

    @Override
    public AbstractOctant<T> trim(Frame frame) {
        final AbstractOctant<T>[] children = this.children;
        AbstractOctant<T> next = this;
        byte parity = -1;
        for (byte c = 0; c < children.length; ++c) {
            final AbstractOctant<T> o = children[c];

            if (o != null)
                if (next == this) {
                    parity = c;
                    next = o;
                } else
                    return this;
        }

        if (next == this)
            return this;

        frame.down(parity);

        return next.trim(frame);
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
        final Frame frame = visitor.frame;
        final Vec3i p = new Vec3i(key.min);
        final boolean [] flagged = new boolean[8];

        flagged[frame.parityTo(p)] = true;
        p.x = key.max.x;
        flagged[frame.parityTo(p)] = true;
        p.x = key.min.x;
        p.y = key.max.y;
        flagged[frame.parityTo(p)] = true;
        p.x = key.max.x;
        flagged[frame.parityTo(p)] = true;
        p.x = key.min.x;
        p.y = key.min.y;
        p.z = key.max.z;
        flagged[frame.parityTo(p)] = true;
        p.x = key.max.x;
        flagged[frame.parityTo(p)] = true;
        p.x = key.min.x;
        p.y = key.max.y;
        flagged[frame.parityTo(p)] = true;
        p.x = key.max.x;
        flagged[frame.parityTo(p)] = true;

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
        assert visitor.frame.contains(x, y, z);
        traverse(visitor, visitor.frame.parityTo(x, y, z));
    }

    private void traverse(OctantVisitor<T> visitor, byte parity) {
        final Frame
            frame = visitor.frame;

        frame.down(parity);

        AbstractOctant<T> child = this.children[parity];
        if (child == null) {
            final OctantAllocator<T> allocator = visitor.octantAllocator();
            if (allocator != null)
                child = allocateChild(allocator, parity, frame.scale());
        }

        if (child != null) {
            child.accept(visitor);

            if (child.empty()) {
                this.children[parity] = null;
                this.occupied--;
            }
        }

        frame.up(parity);
    }

    private AbstractOctant<T> allocateChild(final OctantAllocator<T> allocator, byte parity, int scale) {
        AbstractOctant<T> child;
        if (scale <= VoxelOctTreeMap.LEAF_SIZE)
            child = allocator.allocateLeaf();
        else
            child = allocator.allocateContainer();

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
