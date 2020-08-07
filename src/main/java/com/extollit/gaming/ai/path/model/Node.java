package com.extollit.gaming.ai.path.model;

import com.extollit.linalg.immutable.Vec3i;

import java.text.MessageFormat;
import java.util.Objects;

public class Node {
    private static final byte
        BitWidth_4K = 12,
        BitWidth_128 = 7,
        Mask_Passibility = (byte)(3),
        Index_BitOffs = 2,
        Length_BitOffs = (byte)(Index_BitOffs + BitWidth_4K),
        Delta_BitOffs = (byte)(Length_BitOffs + BitWidth_128),
        Journey_BitOffs = (byte)(Delta_BitOffs + BitWidth_128),
        Visited_BitOffs = (byte)(Journey_BitOffs + BitWidth_128),
        GarbageCollected_BitOffs = (byte)(Visited_BitOffs + 1);

    public static final short MAX_PATH_DISTANCE = (1 << BitWidth_128);

    private static final long
        Mask_128 = (long)MAX_PATH_DISTANCE - 1,
        Mask_4K = (1 << BitWidth_4K) - 1;

    public final Vec3i key;

    private long word;
    private Node previous;

    public Node(Vec3i key) {
        this.key = key;
        unassign();
    }

    private Node(Vec3i key, Passibility passibility) {
        this.key = key;
        reset(passibility);
    }

    final Node pointCopy() {
        return new Node(this.key, passibility());
    }

    public final byte length() {
        return (byte)((this.word >> Length_BitOffs) & Mask_128);
    }
    public final byte delta() {
        return (byte)((this.word >> Delta_BitOffs) & Mask_128);
    }
    public final byte journey() {
        return (byte)((this.word >> Journey_BitOffs) & Mask_128);
    }
    public final Node up() {
        final Node previous = this.previous;
        if (previous != null && previous.deleted())
            return this.previous = null;

        return previous;
    }

    public final Passibility passibility() {
        return Passibility.values()[(int)(this.word & Mask_Passibility)];
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

        this.word = (this.word & ~(Mask_128 << Length_BitOffs)) | ((long)length << Length_BitOffs);
        return true;
    }
    final boolean delta(int delta) {
        if (delta > Mask_128 || delta < 0)
            return false;

        this.word = (this.word & ~(Mask_128 << Delta_BitOffs)) | ((long)delta << Delta_BitOffs);
        return true;
    }
    final boolean journey(int journey) {
        if (journey > Mask_128 || journey < 0)
            return false;

        this.word = (this.word & ~(Mask_128 << Journey_BitOffs)) | ((long)journey << Journey_BitOffs);
        return true;
    }

    final void reset() {
        reset(passibility());
        orphan();
    }
    private void reset(Passibility passibility) {
        this.word = (Mask_4K << Index_BitOffs) | ((long)passibility.ordinal() & Mask_Passibility);
    }

    final boolean deleted() {
        return ((this.word >> GarbageCollected_BitOffs) & 1L) == 1L;
    }
    final void delete() {
        this.word |= (1L << GarbageCollected_BitOffs);
    }
    final short index() {
        short index = (short) ((this.word >> Index_BitOffs) & Mask_4K);
        return index == Mask_4K ? -1 : index;
    }
    final boolean index(int index) {
        if (index >= Mask_4K || index < -1)
            return false;

        this.word = (this.word & ~(Mask_4K << Index_BitOffs)) | (((long)index & Mask_4K) << Index_BitOffs);
        return true;
    }
    public final boolean visited() {
        return ((this.word >> Visited_BitOffs) & 1L) == 1L;
    }
    public final void visited(boolean flag) {
        this.word = (this.word & ~(1L << Visited_BitOffs)) | ((flag ? 1L : 0L) << Visited_BitOffs);
    }

    public final boolean assigned() {
        return index() != -1;
    }

    public boolean target(Node target) {
        final long distance = (long)Math.sqrt(squareDelta(this, target));
        if (distance > Mask_128)
            return false;

        this.word = (this.word & ~((Mask_128 << Journey_BitOffs) | (Mask_128 << Delta_BitOffs))) | ((distance << Journey_BitOffs) | (distance << Delta_BitOffs));
        return true;
    }

    public boolean contains(Vec3i currentPathPoint) {
        if (this.key.equals(currentPathPoint))
            return true;
        else {
            final Node previous = up();
            if (previous != null)
                return previous.contains(currentPathPoint);
        }

        return false;
    }
    public boolean orphaned() {
        return up() == null;
    }
    public void orphan() {
        this.previous = null;
    }
    final void unassign() {
        index(-1);
    }

    final boolean appendTo(final Node parent, final int delta, final int remaining) {
        assert !cyclic(parent);

        this.previous = parent;
        passibility(passibility());
        return length(parent.length() + delta)
                && delta(remaining);
    }

    public static int squareDelta(Node left, Node right) {
        final Vec3i
                leftCoords = left.key,
                rightCoords = right.key;

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

    public static boolean deleted(Node node) {
        return node == null || node.deleted();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(this.key.toString());
        final short index = index();
        if (deleted())
            sb.append('*');
        if (visited())
            sb.append('V');

        if (index == -1)
            sb.append(" (unassigned)");
        else {
            sb.append(" @ ");
            sb.append(index);
        }

        return sb.toString() + MessageFormat.format(" ({0}) : length={1}, delta={2}, journey={3}", passibility(), length(), delta(), journey());
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
