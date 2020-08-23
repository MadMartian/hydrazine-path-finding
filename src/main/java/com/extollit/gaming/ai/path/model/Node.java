package com.extollit.gaming.ai.path.model;

import com.extollit.linalg.immutable.Vec3i;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.Objects;

public class Node {
    private static final byte
        BitWidth_512 = 9,
        BitWidth_128 = 7,
        Mask_Passibility = (byte)(3),
        Index_BitOffs = 2,
        Volatile_BitOffs = (byte)(Index_BitOffs + BitWidth_512),
        Length_BitOffs = (byte)(Volatile_BitOffs + 1),
        Remain_BitOffs = (byte)(Length_BitOffs + BitWidth_128),
        Visited_BitOffs = (byte)(Remain_BitOffs + BitWidth_128);

    public static final short MAX_PATH_DISTANCE = (1 << BitWidth_128) - 1;

    private static final int
        Mask_128 = (int)MAX_PATH_DISTANCE,
        Mask_512 = (1 << BitWidth_512) - 1;

    static final int MAX_INDICES = (1 << BitWidth_512) - 1;

    public final Vec3i key;

    private int word;
    private Node previous;
    private NodeLinkedList children;

    public Node(Vec3i key) {
        this.key = key;
        unassign();
    }

    public Node(Vec3i key, Passibility passibility) {
        this(key, passibility, false);
    }

    public Node(Vec3i key, Passibility passibility, boolean volatility) {
        this.key = key;
        this.word = (Mask_512 << Index_BitOffs) | (passibility.ordinal() & Mask_Passibility) | ((volatility ? 1 : 0) << Volatile_BitOffs);
    }

    private static int wordReset(Node copy) {
        return (copy.word & (Mask_Passibility | (1 << Volatile_BitOffs))) | (Mask_512 << Index_BitOffs);
    }

    public final byte length() {
        return (byte)((this.word >> Length_BitOffs) & Mask_128);
    }
    public final byte remaining() {
        return (byte)((this.word >> Remain_BitOffs) & Mask_128);
    }
    public final byte journey() {
        return (byte)(length() + remaining());
    }
    public final Node up() {
        return this.previous;
    }

    public final Passibility passibility() {
        return Passibility.values()[this.word & Mask_Passibility];
    }
    public final void passibility(Passibility passibility) {
        final Node previous = up();
        if (previous != null)
            passibility = passibility.between(previous.passibility());
        this.word = (this.word & ~Mask_Passibility) | passibility.ordinal();
    }
    public final boolean length(int length) {
        if (length > Mask_128 || length < 0)
            return false;

        this.word = (this.word & ~(Mask_128 << Length_BitOffs)) | (length << Length_BitOffs);
        return true;
    }
    final boolean remaining(int delta) {
        if (delta > Mask_128 || delta < 0)
            return false;

        this.word = (this.word & ~(Mask_128 << Remain_BitOffs)) | (delta << Remain_BitOffs);
        return true;
    }

    final void reset() {
        this.word = wordReset(this);
        isolate();
    }

    final short index() {
        short index = (short) ((this.word >> Index_BitOffs) & Mask_512);
        return index == Mask_512 ? -1 : index;
    }
    final boolean index(int index) {
        if (index >= Mask_512 || index < -1)
            return false;

        this.word = (this.word & ~(Mask_512 << Index_BitOffs)) | ((index & Mask_512) << Index_BitOffs);
        return true;
    }
    public final boolean visited() {
        return ((this.word >> Visited_BitOffs) & 1) == 1;
    }
    public final void visited(boolean flag) {
        this.word = (this.word & ~(1 << Visited_BitOffs)) | ((flag ? 1 << Visited_BitOffs : 0));
    }
    public final boolean volatile_() {
        return ((this.word >> Volatile_BitOffs) & 1) == 1;
    }
    public final void volatile_(boolean flag) {
        this.word = (this.word & ~(1 << Volatile_BitOffs)) | ((flag ? 1 << Volatile_BitOffs : 0));
    }

    public final boolean assigned() {
        return index() != -1;
    }

    public boolean target(Vec3i targetPoint) {
        final int distance = (int)Math.sqrt(squareDelta(this, targetPoint));
        if (distance > Mask_128)
            return false;

        this.word = (this.word & ~(Mask_128 << Remain_BitOffs)) | (distance << Remain_BitOffs);
        return true;
    }

    public boolean contains(Node node) {
        if (this == node)
            return true;
        else {
            final Node previous = up();
            if (previous != null)
                return previous.contains(node);
        }

        return false;
    }
    public boolean orphaned() {
        return up() == null;
    }
    public void orphan() {
        if (this.previous != null)
            this.previous.removeChild(this);

        this.previous = null;
    }
    public void isolate() {
        orphan();
        sterilize();
    }

    void sterilize() {
        if (this.children != null) {
            for (Node child : this.children) {
                assert child.previous == this;
                child.previous = null;
            }
            this.children = null;
        }
    }

    boolean infecund() { return this.children == null; }

    private void removeChild(Node child) {
        if (this.children != null)
            this.children = this.children.remove(child);

        assert !NodeLinkedList.contains(this.children, child);
    }

    final void unassign() {
        index(-1);
    }

    final boolean appendTo(final Node parent, final int delta, final int remaining) {
        assert !cyclic(parent);

        orphan();
        this.previous = parent;
        parent.addChild(this);

        passibility(passibility());
        return length(parent.length() + delta)
                && remaining(remaining);
    }

    private void addChild(Node child) {
        if (this.children == null)
            this.children = new NodeLinkedList(child);
        else
            this.children.add(child);

        assert NodeLinkedList.contains(this.children, child);
    }

    Iterable<Node> children() { return this.children == null ? Collections.<Node>emptyList() : this.children; }

    public static int squareDelta(Node left, Node right) {
        return squareDelta(left, right.key);
    }

    public static int squareDelta(Node left, Vec3i rightCoords) {
        final Vec3i
                leftCoords = left.key;

        final int
                dx = leftCoords.x - rightCoords.x,
                dy = leftCoords.y - rightCoords.y,
                dz = leftCoords.z - rightCoords.z;

        return dx*dx + dy*dy*2 + dz*dz;
    }

    private boolean cyclic(Node parent) {
        Node p = parent;
        while (p != null)
            if (p == this || p.key.equals(this.key))
                return true;
            else
                p = p.up();

        return false;
    }

    public static boolean passible(Node node) {
        return node != null && node.passibility().betterThan(Passibility.impassible);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(this.key.toString());
        final short index = index();
        if (volatile_())
            sb.append('!');
        if (visited())
            sb.insert(0, '|');

        if (index == -1)
            sb.append(" (unassigned)");
        else {
            sb.append(" @ ");
            sb.append(index);
        }

        return sb.toString() + MessageFormat.format(" ({0}) : length={1}, remaining={2}, journey={3}", passibility(), length(), remaining(), journey());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Node pathPoint = (Node) o;
        return Objects.equals(key, pathPoint.key);
    }

    @Override
    public final int hashCode() {
        final Vec3i key = this.key;
        int result = key.x >> 4;
        result = 31 * result + (key.y >> 4);
        result = 31 * result + (key.z >> 4);
        return result;
    }
}
