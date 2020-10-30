package com.extollit.gaming.ai.path.model.octree;

import java.util.Iterator;
import java.util.NoSuchElementException;

final class VoxelIterator<T> implements Iterator<T> {
    private final Iterator<LeafOctant<T>.Reference> leaves;
    private final AbstractIterVisitor<T> host;

    private Iterator<T> li, li0;
    private T element;

    public VoxelIterator(AbstractIterVisitor<T> host, Iterator<LeafOctant<T>.Reference> leaves) {
        this.host = host;
        this.leaves = leaves;
    }

    @Override
    public boolean hasNext() {
        return current() != null;
    }

    private void advanceLeafIterator() {
        this.li = this.leaves.next().iterator(this.host);
    }

    private T current() {
        if (this.li == null) {
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

        this.li0 = this.li;
        do {
            if (this.li.hasNext())
                element = this.li.next();
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
        this.li0.remove();
    }
}
