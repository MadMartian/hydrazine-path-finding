package com.extollit.gaming.ai.path.model.octree;

abstract class IterateeVisitor<T> extends OctantVisitor<T> {
    private final LeafOctant.AbstractIterator<T> voxelIterator;

    private Iteratee<T> iteratee;

    protected IterateeVisitor() {
        this.voxelIterator = createIterator();
    }

    protected abstract LeafOctant.AbstractIterator<T> createIterator();

    protected void baseInit(Root root, Iteratee<T> iteratee) {
        super.baseInit(root);
        this.iteratee = iteratee;
    }

    @Override
    public final void visit(LeafOctant<T> leaf) {
        final LeafOctant.AbstractIterator<T> i = this.voxelIterator;
        i.init(leaf);

        final Iteratee<T> iteratee = this.iteratee;
        while (i.hasNext())
            i.next(iteratee);
    }
}
