package com.extollit.gaming.ai.path.model.octree;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

abstract class AbstractIterVisitor<T> extends OctantVisitor<T> implements LeafOctant.IFilterFunc<T> {
    private final List<LeafOctant<T>.Reference> leaves = new LinkedList<>();

    protected AbstractIterVisitor(Root root) {
        super(root);
    }

    public final Iterator<T> iterator() { return new VoxelIterator<T>(this, this.leaves.iterator()); }

    @Override
    public void visit(LeafOctant<T> leaf) {
        this.leaves.add(leaf.referredBy(super.frame));
    }
}
