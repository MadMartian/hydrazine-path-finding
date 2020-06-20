package com.extollit.gaming.ai.path.model;

import com.extollit.linalg.immutable.Vec3i;

import java.text.MessageFormat;
import java.util.Objects;

public class HydrazinePathPoint {

    private static final byte
        BitWidth_1K = 10,
        BitWidth_64 = 6,
        Mask_Passibility = (byte)(3),
        Index_BitOffs = 2,
        Length_BitOffs = (byte)(Index_BitOffs + BitWidth_1K),
        Delta_BitOffs = (byte)(Length_BitOffs + BitWidth_64),
        Journey_BitOffs = (byte)(Delta_BitOffs + BitWidth_64),
        First_BitOffs = (byte)(Journey_BitOffs + BitWidth_64);

    private static final int
        Mask_64 = (1 << BitWidth_64) - 1,
        Mask_1K = (1 << BitWidth_1K) - 1;

    public final Vec3i key;

    private int word;
    private HydrazinePathPoint previous;

    public HydrazinePathPoint(Vec3i key) {
        this.key = key;
        unassign();
    }

    public final byte length() {
        return (byte)((this.word >> Length_BitOffs) & Mask_64);
    }
    public final byte delta() {
        return (byte)((this.word >> Delta_BitOffs) & Mask_64);
    }
    public final byte journey() {
        return (byte)((this.word >> Journey_BitOffs) & Mask_64);
    }
    public final HydrazinePathPoint up() {
        return this.previous;
    }

    public final Passibility passibility() {
        return Passibility.values()[(int)(this.word & Mask_Passibility)];
    }
    public final void passibility(Passibility passibility) {
        if (this.previous != null)
            passibility = passibility.between(this.previous.passibility());
        this.word = (this.word & ~Mask_Passibility) | passibility.ordinal();
    }
    public final boolean length(int length) {
        if (length > Mask_64 || length < 0)
            return false;

        this.word = (this.word & ~(Mask_64 << Length_BitOffs)) | (length << Length_BitOffs);
        return true;
    }
    final boolean delta(int delta) {
        if (delta > Mask_64 || delta < 0)
            return false;

        this.word = (this.word & ~(Mask_64 << Delta_BitOffs)) | (delta << Delta_BitOffs);
        return true;
    }
    final boolean journey(int journey) {
        if (journey > Mask_64 || journey < 0)
            return false;

        this.word = (this.word & ~(Mask_64 << Journey_BitOffs)) | (journey << Journey_BitOffs);
        return true;
    }
    final short index() {
        short index = (short) ((this.word >> Index_BitOffs) & Mask_1K);
        return index == Mask_1K ? -1 : index;
    }
    final boolean index(int index) {
        if (index >= Mask_1K || index < -1)
            return false;

        this.word = (this.word & ~(Mask_1K << Index_BitOffs)) | ((index & Mask_1K) << Index_BitOffs);
        return true;
    }
    public final boolean first() {
        return ((this.word >> First_BitOffs) & 1) == 1;
    }
    public final void first(boolean flag) {
        this.word = (this.word & ~(1 << First_BitOffs)) | ((flag ? 1 : 0) << First_BitOffs);
    }

    public final boolean assigned() {
        return index() != -1;
    }

    public boolean target(HydrazinePathPoint target) {
        final int distance = (int) Math.sqrt(squareDelta(this, target));
        if (distance > Mask_64)
            return false;

        this.word = (this.word & ~((Mask_64 << Journey_BitOffs) | (Mask_64 << Delta_BitOffs))) | ((distance << Journey_BitOffs) | (distance << Delta_BitOffs));
        return true;
    }

    public void orphan() {
        this.previous = null;
    }
    final void unassign() {
        index(-1);
        first(false);
    }
    final boolean appendTo(final HydrazinePathPoint parent, final int delta, final HydrazinePathPoint target) {
        this.previous = parent;
        passibility(passibility());
        return length(parent.length() + delta)
                && delta((int)Math.sqrt(squareDelta(this, target)));
    }

    public static int squareDelta(HydrazinePathPoint left, HydrazinePathPoint right) {
        final Vec3i
                leftCoords = left.key,
                rightCoords = right.key;

        final int
                dx = leftCoords.x - rightCoords.x,
                dy = leftCoords.y - rightCoords.y,
                dz = leftCoords.z - rightCoords.z;

        return dx*dx + dy*dy*2 + dz*dz;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(this.key.toString());
        final short index = index();
        if (first())
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
        HydrazinePathPoint pathPoint = (HydrazinePathPoint) o;
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
