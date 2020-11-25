package com.extollit.gaming.ai.path.model;

import com.extollit.collect.Option;

import java.util.Iterator;

public final class IncompletePath implements IPath {
    final INode node;

    public IncompletePath(INode node) {
        this.node = node;
    }

    @Override
    public void truncateTo(int length) {
        throw new ArrayIndexOutOfBoundsException("Cannot truncate incomplete paths");
    }

    @Override
    public void untruncate() {}

    @Override
    public int length() {
        return 1;
    }

    @Override
    public int cursor() {
        return 0;
    }

    @Override
    public INode at(int i) {
        return this.node;
    }

    @Override
    public INode current() {
        return this.node;
    }

    @Override
    public INode last() {
        return this.node;
    }

    @Override
    public boolean done() {
        return false;
    }

    @Override
    public boolean taxiing() {
        return false;
    }

    @Override
    public void taxiUntil(int index) {}

    @Override
    public boolean sameAs(IPath other) {
        if (other instanceof IncompletePath)
            return ((IncompletePath)other).node.coordinates().equals(this.node.coordinates());

        Iterator<INode> i = other.iterator();
        return (!i.hasNext() || this.node.coordinates().equals(i.next().coordinates())) && !i.hasNext();
    }

    @Override
    public float stagnantFor(IPathingEntity subject) {
        return 0;
    }

    @Override
    public void update(IPathingEntity pathingEntity) {}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IncompletePath iNodes = (IncompletePath) o;

        return node.equals(iNodes.node);
    }

    @Override
    public int hashCode() {
        return node.hashCode();
    }

    @Override
    public String toString() {
        return "*" + this.node.coordinates() + "...?";
    }

    @Override
    public Iterator<INode> iterator() {
        return Option.<INode>of(this.node).iterator();
    }
}
