package com.extollit.gaming.ai.path.model;

import com.extollit.collect.CollectionsExt;

import java.util.Iterator;

public final class NodeLinkedList implements Iterable<Node> {
    public final Node self;

    private NodeLinkedList next;

    public NodeLinkedList(Node self) {
        this(self, null);
    }
    private NodeLinkedList(Node self, NodeLinkedList next) {
        this.next = next;
        this.self = self;
    }

    private static class Iter implements Iterator<Node> {
        private NodeLinkedList head;

        public Iter(NodeLinkedList head) {
            this.head = head;
        }

        @Override
        public boolean hasNext() {
            return this.head != null;
        }

        @Override
        public Node next() {
            final NodeLinkedList head = this.head;
            this.head = head.next;
            return head.self;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public Iterator<Node> iterator() {
        return new Iter(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NodeLinkedList nodes = (NodeLinkedList) o;

        if (next != null ? !next.equals(nodes.next) : nodes.next != null) return false;
        return self.equals(nodes.self);
    }

    public NodeLinkedList remove(Node child) {
        NodeLinkedList
            e = this,
            last = null;

        do
        {
            if (e.self == child)
            {
                final NodeLinkedList tail = e.next;
                NodeLinkedList head = this;
                if (last == null)
                    head = tail;
                else
                    last.next = tail;

                return head;
            }
            last = e;
            e = e.next;
        } while (e != null);

        return this;
    }

    public boolean add(Node child) {
        NodeLinkedList
            e = this,
            last;

        do {
            if (e.self == child)
                return false;

            last = e;
        } while ((e = e.next) != null);

        last.next = new NodeLinkedList(child);

        return true;
    }

    @Override
    public int hashCode() {
        return self.hashCode();
    }

    @Override
    public String toString() {
        return CollectionsExt.toList(this).toString();
    }
}
