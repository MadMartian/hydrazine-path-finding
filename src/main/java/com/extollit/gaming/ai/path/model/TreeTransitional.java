package com.extollit.gaming.ai.path.model;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

final class TreeTransitional {
    private static final class RotateNodeOp {
        public final Node root;
        public final int diff;
        public final List<Node> heads = new LinkedList<>();

        public RotateNodeOp(Node root, int diff) {
            this.root = root;
            this.diff = diff;
        }

        @Override
        public String toString() {
            return this.diff + ": " + this.root;
        }
    }

    private final Node nextRoot;
    private final Deque<RotateNodeOp> dq;

    public TreeTransitional(Node nextRoot) {
        final Deque<RotateNodeOp> dq = new LinkedList<>();
        Node curr = (this.nextRoot = nextRoot).up();
        int length = nextRoot.length(),
            newLength0 = 0;
        while (curr != null) {
            final Node up = curr.up();
            final int length0 = length;
            length = curr.length();
            curr.orphan();
            curr.dirty(true);
            final int dl = newLength0 + (length0 - length) - length;
            dq.add(new RotateNodeOp(curr, dl));
            newLength0 = length + dl;
            curr = up;
        }
        this.dq = dq;
        this.nextRoot.orphan();
    }

    public boolean queue(Node head, Node root) {
        for (RotateNodeOp op : this.dq) {
            if (op.root == root) {
                op.heads.add(head);
                return true;
            }
        }

        return false;
    }

    public void finish(SortedPointQueue queue) {
        final Deque<RotateNodeOp> dq = this.dq;
        Node prev = this.nextRoot;
        while (!dq.isEmpty()) {
            final RotateNodeOp op = dq.pop();
            final Node next = op.root;
            next.bindParent(prev);
            for (final Node head : op.heads) {
                if (head.dirty())
                    queue.addLength(head, op.diff);

                Node curr = head.up();
                while (curr != null && curr != next && curr.dirty()) {
                    curr.addLength(op.diff);
                    curr = curr.up();
                }

                if (next.dirty())
                    next.addLength(op.diff);
            }

            prev = next;
        }
    }
}
