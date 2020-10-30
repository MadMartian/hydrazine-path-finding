package com.extollit.gaming.ai.path.model.octree;

import static com.extollit.num.FastMath.abs;

class Root<T> {
    public final Frame frame;

    private AbstractOctant<T> node;

    public Root(Class<T> elementClass, int x, int y, int z) {
        this.node = new LeafOctant<>(elementClass);
        this.frame = new Frame(x, y, z);
    }

    public final AbstractOctant<T> node() { return this.node; }

    public AbstractOctant<T> findRoot(int x, int y, int z) {
        AbstractOctant<T> root = this.node;
        final Frame frame = this.frame;
        final int
            higherScale = order(
                abs(frame.x() - x) |
                abs(frame.y() - y) |
                abs(frame.z() - z)
            ) << 1;

        while (higherScale > frame.scale()) {
            final byte parity = frame.from(x, y, z);
            frame.up();

            root = new ContainerOctant<T>(root, parity);
        }

        this.node = root;

        return root;
    }

    public void trim() {
        this.node = this.node.trim(this.frame);
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
