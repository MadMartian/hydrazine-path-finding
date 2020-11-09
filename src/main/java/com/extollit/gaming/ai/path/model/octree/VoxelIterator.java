package com.extollit.gaming.ai.path.model.octree;

import java.util.Iterator;
import java.util.NoSuchElementException;

final class VoxelIterator<T> implements Iterator<T> {
    private final Iterator<LeafOctant<T>> leaves;
    private final LeafOctant.AbstractIterator<T> leafIterator;

    private LeafOctant<T> leaf0;
    private int index0;
    private T element;

    public VoxelIterator(LeafOctant.AbstractIterator<T> leafIterator, Iterator<LeafOctant<T>> leaves) {
        this.leaves = leaves;
        this.leafIterator = leafIterator;
    }

    @Override
    public boolean hasNext() {
        return current() != null;
    }

    private void advanceLeafIterator() {
        this.leafIterator.init(this.leaves.next());
    }

    private T current() {
        if (this.element == null) {
            if (this.leaves.hasNext())
                advanceLeafIterator();
            else
                return this.element = null;

            return this.element = find();
        }

        return this.element;
    }

    private T find() {
        T element = null;

        final LeafOctant.AbstractIterator<T> voxelIterator = this.leafIterator;
        this.leaf0 = voxelIterator.delegate();
        this.index0 = voxelIterator.index();
        do {
            if (voxelIterator.hasNext())
                element = voxelIterator.next();
            else if (this.leaves.hasNext())
                advanceLeafIterator();
            else {
                element = null;
                break;
            }
        } while (element == null);

        return element;
    }

    @Override
    public T next() {
        final T current = current();

        if (current == null)
            throw new NoSuchElementException();

        this.element = find();
        return current;
    }

    @Override
    public void remove() {
        this.leaf0.remove(this.index0);
    }
}
