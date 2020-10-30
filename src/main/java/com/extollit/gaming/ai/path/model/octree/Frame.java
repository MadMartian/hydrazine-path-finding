package com.extollit.gaming.ai.path.model.octree;

import com.extollit.linalg.immutable.IntAxisAlignedBox;
import com.extollit.linalg.mutable.Vec3i;

import static com.extollit.gaming.ai.path.model.octree.VoxelOctTreeMap.LEAF_MASK;
import static com.extollit.gaming.ai.path.model.octree.VoxelOctTreeMap.LEAF_SIZE;

public final class Frame {
    private int scale, tx, ty, tz;
    private byte parity;

    private final Vec3i mp;
    private final int [] offs;

    public Frame(Frame frame) {
        this.mp = new Vec3i(frame.mp);
        this.offs = frame.offs;
        this.scale = frame.scale;
        this.tx = frame.tx;
        this.ty = frame.ty;
        this.tz = frame.tz;
        this.parity = frame.parity;
    }
    public Frame(int x, int y, int z) {
        final int mask = ~LEAF_MASK;
        final int halfOrder = (this.scale = LEAF_SIZE) >> 1;
        this.mp = new Vec3i(
                (x & mask) + halfOrder,
                (y & mask) + halfOrder,
                (z & mask) + halfOrder
        );
        this.offs = new int[] { -halfOrder, +halfOrder };
    }

    public int scale() { return this.scale; }
    public int x() { return this.mp.x; }
    public int y() { return this.mp.y; }
    public int z() { return this.mp.z; }
    public Vec3i mp() {
        return new Vec3i(this.mp);
    }

    public byte from(int x, int y, int z) {
        parity((byte)(7 ^ parityTo(x, y, z)));
        return this.parity;
    }

    public byte parity() {
        return this.parity;
    }

    public void parity(byte parity) {
        final int
                i = (parity >> 0) & 1,
                j = (parity >> 1) & 1,
                k = (parity >> 2) & 1;

        final int[] offs = this.offs;

        this.tx = offs[i];
        this.ty = offs[j];
        this.tz = offs[k];

        this.parity = parity;
    }

    public final void down(byte parity) {
        parity(parity);
        down();
    }

    public final void up(byte parity) {
        parity(parity);
        up();
    }

    public void down() {
        final Vec3i mp = this.mp;

        this.scale >>= 1;
        offs[0] >>= 1;
        offs[1] >>= 1;
        tx >>= 1;
        ty >>= 1;
        tz >>= 1;

        mp.x += tx;
        mp.y += ty;
        mp.z += tz;
    }

    public void up() {
        final Vec3i mp = this.mp;

        mp.x -= tx;
        mp.y -= ty;
        mp.z -= tz;

        scale <<= 1;
        offs[0] <<= 1;
        offs[1] <<= 1;
        tx <<= 1;
        ty <<= 1;
        tz <<= 1;
    }

    private static final int
            TOP_BITNUM = (Integer.SIZE - 1),
            TOP_BIT = 1 << TOP_BITNUM;

    byte parityTo(Vec3i value) {
        return parityTo(value.x, value.y, value.z);
    }

    public byte parityTo(final int x, final int y, final int z) {
        return (byte)(parityOf(x, mp.x) | (parityOf(y, mp.y) << 1) | (parityOf(z, mp.z) << 2));
    }

    private static byte parityOf(int value, int mp) {
        return (byte)(((((value - mp) & TOP_BIT) >> TOP_BITNUM) & 1) ^ 1);
    }

    public boolean contains(int x, int y, int z) {
        final int scale = this.scale;
        final int halfScale = scale >> 1;
        final Vec3i mp = this.mp;
        final int
                x0 = mp.x - halfScale,
                y0 = mp.y - halfScale,
                z0 = mp.z - halfScale;

        return
            x >= x0 && x < x0 + scale &&
            y >= y0 && y < y0 + scale &&
            z >= z0 && z < z0 + scale;
    }

    public boolean inside(IntAxisAlignedBox range) {
        final int scale = this.scale;
        final int halfOrder = scale >> 1;
        final Vec3i mp = this.mp;
        final int
            x0 = mp.x - halfOrder,
            y0 = mp.y - halfOrder,
            z0 = mp.z - halfOrder;

        return range.contains(x0, y0, z0) && range.contains(x0 + scale, y0 + scale, z0 + scale);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Frame frame = (Frame) o;

        if (scale != frame.scale) return false;
        return mp.equals(frame.mp);
    }

    @Override
    public int hashCode() {
        return scale;
    }
}
