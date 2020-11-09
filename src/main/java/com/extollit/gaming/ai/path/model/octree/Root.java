package com.extollit.gaming.ai.path.model.octree;

import com.extollit.linalg.immutable.Vec3i;

import static com.extollit.num.FastMath.abs;

class Root<T> {
    private AbstractOctant<T> node;

    public Root(Class<T> elementClass, int x, int y, int z) {
        this.node = new LeafOctant<T>(new FramePointer(x, y, z), elementClass);
    }

    public FramePointer pointer() {
        return this.node.pointer;
    }

    public boolean contains(int x, int y, int z) {
        return this.node.pointer.contains(x, y, z);
    }

    public void accept(OctantVisitor<T> visitor) {
        this.node.accept(visitor);
    }

    public void accept(AbstractOctant<T> child, OctantVisitor<T> visitor) {
        this.node = child;
        child.accept(visitor);
    }

    public AbstractOctant<T> resetRoot() {
        AbstractOctant<T> root = this.node;

        if (root == null)
            return null;

        while (!root.orphan())
            root = root.parent();

        return this.node = root;
    }

    public AbstractOctant<T> findRoot(int x, int y, int z, boolean allocate) {
        AbstractOctant<T> root = this.node;
        FramePointer pointer = this.node.pointer;
        int higherScale = scaleRequired(pointer, x, y, z);

        while (!root.orphan() && higherScale > pointer.scale) {
            root = root.parent();
            pointer = root.pointer;
            higherScale = scaleRequired(pointer, x, y, z);
        }

        if (allocate)
            while (higherScale > pointer.scale) {
                final byte parity = pointer.parentParityUpToward(x, y, z);
                final FramePointer upPointer = pointer.upTo(parity);

                root = new ContainerOctant<T>(upPointer, root, parity);
                pointer = upPointer;
                higherScale = scaleRequired(pointer, x, y, z);
            }
        else if (higherScale > pointer.scale && !root.pointer.contains(x, y, z)) {
            this.node = root;
            return null;
        }

        assert root.pointer.contains(x, y, z);

        return this.node = root;
    }

    private int scaleRequired(FramePointer pointer, int x, int y, int z) {
        int higherScale;
        final Vec3i mp = pointer.mp;
        higherScale = order(
            abs(mp.x - x) |
            abs(mp.y - y) |
            abs(mp.z - z)
        ) << 1;
        return higherScale;
    }

    public boolean empty() {
        return this.node.empty();
    }

    public void trim() {
        this.node = this.node.trim();
    }

    static int order(int n) {
        n |= (n >>  1);
        n |= (n >>  2);
        n |= (n >>  4);
        n |= (n >>  8);
        n |= (n >> 16);
        return (n ^ (n >> 1)) << 1;
    }
}
