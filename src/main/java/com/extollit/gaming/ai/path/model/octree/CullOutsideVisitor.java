package com.extollit.gaming.ai.path.model.octree;

import com.extollit.linalg.immutable.IntAxisAlignedBox;

final class CullOutsideVisitor<T> extends OctantVisitor<T> {
    public final IntAxisAlignedBox range;

    private final Iteratee<T> iteratee;

    public CullOutsideVisitor(Root root, IntAxisAlignedBox range, Iteratee<T> iteratee) {
        this.range = range;
        this.iteratee = iteratee;
        super.baseInit(root);
    }

    @Override
    public void visit(ContainerOctant<T> container) {
        if (!this.frame.inside(this.range))
            container.traverseAll(this);
    }

    @Override
    public void visit(LeafOctant<T> leaf) {
        final com.extollit.linalg.immutable.Vec3i offset = offset();
        final int leafSize = VoxelOctTreeMap.LEAF_SIZE;
        final IntAxisAlignedBox range = this.range;
        final boolean full =
            !range.intersects(
                offset.x, offset.y, offset.z,

                offset.x + leafSize - 1,
                offset.y + leafSize - 1,
                offset.z + leafSize - 1
            );

        final Iteratee<T> iteratee = this.iteratee;

        if (full) {
            for (int z = 0; z < leafSize; ++z)
                for (int y = 0; y < leafSize; ++y)
                    for (int x = 0; x < leafSize; ++x) {
                        final T element = leaf.get(x, y, z);
                        if (element != null)
                            iteratee.visit(
                                    element,
                                    x + offset.x,
                                    y + offset.y,
                                    z + offset.z
                            );
                    }

            leaf.clear();
        } else
            for (int z = 0; z < leafSize; ++z)
                for (int y = 0; y < leafSize; ++y)
                    for (int x = 0; x < leafSize; ++x) {
                        final int
                            xx = x + offset.x,
                            yy = y + offset.y,
                            zz = z + offset.z;

                        if (!range.intersects(xx, yy, zz,
                            xx + leafSize - 1, yy + leafSize - 1, zz + leafSize - 1)
                        ) {
                            final T existing = leaf.set(x, y, z, null);
                            if (existing != null)
                                iteratee.visit(
                                    existing,
                                        xx,
                                        yy,
                                        zz
                                );
                        }
                    }
    }
}
