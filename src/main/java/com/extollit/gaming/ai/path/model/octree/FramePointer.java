package com.extollit.gaming.ai.path.model.octree;

import com.extollit.linalg.immutable.IntAxisAlignedBox;
import com.extollit.linalg.immutable.Vec3i;

import static com.extollit.gaming.ai.path.model.octree.VoxelOctTreeMap.LEAF_MASK;
import static com.extollit.gaming.ai.path.model.octree.VoxelOctTreeMap.LEAF_SIZE;

public final class FramePointer {
    public final Vec3i mp;
    public final int scale;

    public FramePointer(Vec3i mp, int scale) {
        this.mp = mp;
        this.scale = scale;
    }

    public FramePointer(int x, int y, int z) {
        final int mask = ~LEAF_MASK;
        final int halfOrder = (this.scale = LEAF_SIZE) >> 1;
        this.mp = new Vec3i(
                (x & mask) + halfOrder,
                (y & mask) + halfOrder,
                (z & mask) + halfOrder
        );
    }

    public final FramePointer downTo(byte parity) {
        final int
            scale = this.scale >> 1,
            halfScale = scale >> 1;

        final int
                tx = ((((parity >> 0) & 1) << 1) - 1) * halfScale,
                ty = ((((parity >> 1) & 1) << 1) - 1) * halfScale,
                tz = ((((parity >> 2) & 1) << 1) - 1) * halfScale;

        final Vec3i mp = this.mp;
        final Vec3i mp2 = new Vec3i(
                mp.x + tx,
                mp.y + ty,
                mp.z + tz
        );

        return new FramePointer(mp2, scale);
    }

    public final FramePointer upTo(byte parity) {
        final int
                scale = this.scale,
                halfScale = scale >> 1;

        final int
                tx = ((((parity >> 0) & 1) << 1) - 1) * halfScale,
                ty = ((((parity >> 1) & 1) << 1) - 1) * halfScale,
                tz = ((((parity >> 2) & 1) << 1) - 1) * halfScale;

        final Vec3i mp = this.mp;
        final Vec3i mp2 = new Vec3i(
                mp.x - tx,
                mp.y - ty,
                mp.z - tz
        );

        return new FramePointer(mp2, scale << 1);
    }

    public byte parentParityUpToward(int x, int y, int z) {
        return (byte)(7 ^ parityTo(x, y, z));
    }

    private static final int
            TOP_BITNUM = (Integer.SIZE - 1),
            TOP_BIT = 1 << TOP_BITNUM;

    byte parityTo(com.extollit.linalg.mutable.Vec3i value) {
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

        final FramePointer pointer = (FramePointer) o;

        if (scale != pointer.scale) return false;
        return mp.equals(pointer.mp);
    }

    @Override
    public int hashCode() {
        return scale;
    }
}
