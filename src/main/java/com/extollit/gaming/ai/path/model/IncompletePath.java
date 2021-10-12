package com.extollit.gaming.ai.path.model;

import java.util.Collections;
import java.util.Iterator;

public final class IncompletePath implements IPath {
    final INode node;

    private boolean truncated;

    public IncompletePath(INode node) {
        this(node, false);
    }
    public IncompletePath(INode node, boolean truncated) {
        if (node == null)
            throw new NullPointerException();

        this.node = node;
        this.truncated = truncated;
    }

    @Override
    public void truncateTo(int length) {
        if (length == 0)
            this.truncated = true;
        else if (length > 1)
            throw new ArrayIndexOutOfBoundsException("Cannot truncate incomplete paths");
    }

    @Override
    public void untruncate() {
        this.truncated = false;
    }

    @Override
    public int length() {
        return this.truncated ? 0 : 1;
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
        return this.truncated;
    }

    @Override
    public boolean taxiing() {
        return false;
    }

    @Override
    public void taxiUntil(int index) {}

    @Override
    public boolean sameAs(IPath other) {
        if (other instanceof IncompletePath) {
            final IncompletePath otherIncomplete = (IncompletePath) other;
            return otherIncomplete.truncated && this.truncated || !otherIncomplete.truncated && !this.truncated && otherIncomplete.node.coordinates().equals(this.node.coordinates());
        }

        Iterator<INode> i = other.iterator();
        return !i.hasNext() && this.truncated || (!i.hasNext() || this.node.coordinates().equals(i.next().coordinates())) && !i.hasNext();
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

        IncompletePath other = (IncompletePath) o;

        return truncated == other.truncated && node.equals(other.node);
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
        return truncated ? Collections.<INode>emptyIterator() : Collections.singletonList(this.node).iterator();
    }
}
