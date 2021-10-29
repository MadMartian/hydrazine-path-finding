package com.extollit.gaming.ai.path.model;

import com.extollit.gaming.ai.path.persistence.internal.LinkableReader;
import com.extollit.gaming.ai.path.persistence.internal.LinkableWriter;
import com.extollit.gaming.ai.path.persistence.internal.ReferableObjectInput;
import com.extollit.gaming.ai.path.persistence.internal.ReferableObjectOutput;
import com.extollit.linalg.immutable.Vec3i;

import java.io.IOException;
import java.util.*;

import static com.extollit.gaming.ai.path.model.Node.squareDelta;

public final class SortedPointQueue implements LinkableReader<SortedPointQueue, Node>, LinkableWriter<SortedPointQueue, Node> {
    private static final float CULL_THRESHOLD = 0.1f;

    private final ArrayList<Node> list = new ArrayList<>(8);

    boolean fastAdd(Node point) {
        if (!point.index(this.list.size()))
            return false;

        this.list.add(point);
        sortBack(point.index());
        return true;
    }

    public final void clear() {
        for (Node point : this.list)
            point.unassign();
        this.list.clear();
    }

    public final boolean isEmpty() {
        return this.list.isEmpty();
    }

    public Node trimFrom(Node source) {
        if (source.orphaned())
            return source;

        final Node root0 = source.root();
        final Vec3i dd = source.key.subOf(root0.key);
        final List<Node> list = this.list;

        final byte length0 = source.length();
        final Stack<Node> path = new Stack<>();
        final TreeTransitional treeTransitional = new TreeTransitional(source);
        final ListIterator<Node> i = list.listIterator();

        while (i.hasNext()) {
            final Node head = i.next();
            Node point = head;
            while (!point.orphaned()) {
                point = point.up();
                path.push(point);
            }
            if (point == source) {
                while (!path.isEmpty()) {
                    point = path.pop();
                    final int length = point.length() - length0;
                    point.length(length);
                }
                final int length = head.length() - length0;
                head.length(length);
                head.index(i.previousIndex());
            } else {
                final Node root;
                if (path.isEmpty())
                    root = head;
                else
                    root = path.pop();

                if (head == point || head.key.subOf(point.key).dot(dd) <= 0) {
                    head.dirty(true);
                    while (!path.isEmpty())
                        path.pop().dirty(true);

                    treeTransitional.queue(head, root);
                    head.index(i.previousIndex());
                } else {
                    if (!path.isEmpty()) {
                        final Node branch = path.pop();
                        branch.dirty(true);
                        treeTransitional.queue(branch, root);
                    }

                    i.remove();

                    path.clear();
                    head.unassign();
                    head.visited(false);
                }
            }
        }

        treeTransitional.finish(this);

        return root0;
    }

    void cullBranch(Node ancestor) {
        final List<Node> list = this.list;
        final Stack<Node> stack = new Stack<>();

        ListIterator<Node> i = list.listIterator();
        final List<Node> culled = new LinkedList<Node>();
        while (i.hasNext()) {
            final Node head = i.next();
            Node point = head;
            while (!point.orphaned() && point != ancestor) {
                point = point.up();
                stack.push(point);
            }
            if (point != ancestor)
                head.index(i.previousIndex());
            else {
                i.remove();
                head.unassign();
                culled.add(head);
                culled.addAll(stack);
            }
            stack.clear();
        }

        for (Node node : culled) {
            node.reset();
            node.visited(false);
        }
    }

    public List<Node> view() { return Collections.unmodifiableList(this.list); }

    public Node top() {
        return this.list.get(0);
    }
    public Node dequeue() {
        final ArrayList<Node> list = this.list;
        final Node point;
        if (list.size() == 1)
            point = list.remove(0);
        else {
            point = list.set(0, list.remove(list.size() - 1));
            sortForward(0);
        }

        point.unassign();
        return point;
    }
    public boolean nextContains(Node ancestor) {
        return this.list.get(0).contains(ancestor);
    }

    private void sortBack(int index) {
        final ArrayList<Node> list = this.list;
        final Node originalPoint = list.get(index);
        final int distanceRemaining = originalPoint.journey();
        final Passibility originalPassibility = originalPoint.passibility();
        while (index > 0) {
            final int i = (index - 1) >> 1;
            final Node point = list.get(i);

            final Passibility passibility = point.passibility();
            if ((distanceRemaining >= point.journey() && originalPassibility == passibility) || originalPassibility.worseThan(passibility))
                break;

            list.set(index, point);
            point.index(index);

            index = i;
        }

        list.set(index, originalPoint);
        originalPoint.index(index);
    }

    private void sortForward(int index) {
        final ArrayList<Node> list = this.list;
        Node originalPoint = list.get(index);
        final int distanceRemaining = originalPoint.journey();
        final Passibility originalPassibility = originalPoint.passibility();

        do {
            final int i = 1 + (index << 1);
            final int j = i + 1;

            if (i >= list.size())
                break;

            final Node pointAlpha = list.get(i);
            final int distAlpha = pointAlpha.journey();
            final Passibility passibilityAlpha = pointAlpha.passibility();
            final Node pointBeta;
            final int distBeta;
            final Passibility passibilityBeta;

            if (j >= list.size()) {
                pointBeta = null;
                distBeta = Integer.MIN_VALUE;
                passibilityBeta = Passibility.passible;
            } else {
                pointBeta = list.get(j);
                distBeta = pointBeta.journey();
                passibilityBeta = pointBeta.passibility();
            }

            if ((distAlpha < distBeta && passibilityAlpha == passibilityBeta)
                    || passibilityAlpha.betterThan(passibilityBeta)) {
                if ((distAlpha >= distanceRemaining && passibilityAlpha == originalPassibility)
                        || passibilityAlpha.worseThan(originalPassibility))
                    break;

                list.set(index, pointAlpha);
                pointAlpha.index(index);
                index = i;
            } else {
                if (pointBeta == null || (distBeta >= distanceRemaining && passibilityAlpha == originalPassibility)
                        || passibilityBeta.worseThan(originalPassibility))
                    break;

                list.set(index, pointBeta);
                pointBeta.index(index);
                index = j;
            }
        } while (true);

        list.set(index, originalPoint);
        originalPoint.index(index);
    }

    public boolean appendTo(Node point, Node parent, Vec3i targetPoint) {
        return appendTo(point, parent, (int)Math.sqrt(squareDelta(point, targetPoint)));
    }

    public boolean appendTo(Node point, Node parent, final int remaining) {
        final int squareDelta = squareDelta(parent, point);

        final byte length = point.length();
        if (!point.assigned() || (parent.length() + squareDelta < length*length && !point.passibility().betterThan(parent.passibility()))) {
            final byte distance0 = point.journey();
            if (point.appendTo(parent, (int)Math.sqrt(squareDelta), remaining))
                return resort(point, distance0);
            else
                point.orphan();
        }

        return false;
    }

    public boolean addLength(Node point, int diff) {
        final byte journey0 = point.journey();
        point.addLength(diff);
        return resort(point, journey0);
    }

    private boolean resort(Node point, byte journey0) {
        final int journey = point.journey();
        if (point.assigned()) {
            if (journey < journey0)
                sortBack(point.index());
            else
                sortForward(point.index());

            return true;
        } else
            add(point);
        return false;
    }

    public void add(Node point) {
        if (point.assigned())
            throw new IllegalStateException("Point is already assigned");

        if (fastAdd(point))
            return;

        final ArrayList<Node> list = this.list;
        final int size = size();

        final ListIterator<Node> i = list.listIterator(size);
        for (int amount = (int)Math.ceil((float)size * CULL_THRESHOLD); amount > 0 && i.hasPrevious(); --amount) {
            i.previous().unassign();
            i.remove();
        }

        fastAdd(point);
    }

    public int size() {
        return this.list.size();
    }

    public final Set<Node> roots() {
        final Set<Node> roots = new HashSet<>(1);

        for (Node node : this.list) {
            final Node root = node.root();
            if (!roots.contains(root))
                roots.add(root);
        }

        return roots;
    }

    @Override
    public String toString() {
        return this.list.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SortedPointQueue that = (SortedPointQueue) o;

        return list.equals(that.list);
    }

    @Override
    public int hashCode() {
        return list.hashCode();
    }

    @Override
    public void readLinkages(SortedPointQueue object, ReferableObjectInput<Node> in) throws IOException {
        final ArrayList<Node> list = object.list;
        int count = in.readInt();
        while (count-- > 0)
            list.add(in.readRef());
    }

    @Override
    public void writeLinkages(SortedPointQueue object, ReferableObjectOutput<Node> out) throws IOException {
        final ArrayList<Node> list = object.list;
        out.writeInt(list.size());
        for (Node node : list)
            out.writeRef(node);
    }
}
