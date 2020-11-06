package com.extollit.gaming.ai.path.model.octree;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

abstract class AbstractIterVisitor<T> extends OctantVisitor<T> {
    private final List<LeafOctant<T>.Reference> leaves = new LinkedList<>();

    public final Iterator<T> iterator() { return iterator(this.leaves.iterator()); }

    protected abstract Iterator<T> iterator(Iterator<LeafOctant<T>.Reference> leaves);

    @Override
    public void visit(LeafOctant<T> leaf) {
        this.leaves.add(leaf.referredBy(super.frame));
    }
}
