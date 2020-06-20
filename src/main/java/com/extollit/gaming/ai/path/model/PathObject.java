package com.extollit.gaming.ai.path.model;

import com.extollit.collect.ArrayIterable;
import com.extollit.gaming.ai.path.IConfigModel;
import com.extollit.linalg.immutable.Vec3i;
import com.extollit.linalg.mutable.Vec3d;
import com.extollit.num.FloatRange;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Random;

import static com.extollit.num.FastMath.*;

public final class PathObject implements Iterable<Vec3i> {
    private static FloatRange DIRECT_LINE_TIME_LIMIT = new FloatRange(2, 4);

    final Vec3i[] points;
    private final float speed;

    public int i;

    private Random random = new Random();
    private int
        taxiUntil = 0,
        adjacentIndex = 0,
        length;

    private float nextDirectLineTimeout, lastMutationTime = -1;

    public static void configureFrom(IConfigModel configModel) {
        DIRECT_LINE_TIME_LIMIT = configModel.directLineTimeLimit();
    }

    protected PathObject(float speed, Vec3i... points) {
        this.points = points;
        this.length = points.length;
        this.speed = speed;
        this.nextDirectLineTimeout = DIRECT_LINE_TIME_LIMIT.next(this.random);
    }

    public void setRandomNumberGenerator(Random random) {
        this.random = random;
    }

    public static PathObject fromHead(float speed, HydrazinePathPoint head) {
        int i = 1;

        for (HydrazinePathPoint p = head; p.up() != null; p = p.up())
            ++i;

        final Vec3i[] result = new Vec3i[i];
        final Vec3i key = head.key;
        result[--i] = key;

        for (HydrazinePathPoint p = head; p.up() != null; result[--i] = p.key)
            p = p.up();

        if (result.length <= 1)
            return null;
        else
            return new PathObject(speed, result);
    }

    public void truncateTo(int length) {
        if (length < 0 || length >= this.points.length)
            throw new ArrayIndexOutOfBoundsException(
                MessageFormat.format("Length is out of bounds 0 <= length < {0} but length = {1}", this.points.length, length)
            );

        this.length = length;
    }

    public void untruncate() {
        this.length = this.points.length;
    }

    @Override
    public Iterator<Vec3i> iterator() {
        return new ArrayIterable.Iter<Vec3i>(this.points, this.length);
    }
    public final int length() { return this.length; }
    public final Vec3i at(int i) { return this.points[i]; }
    public final Vec3i current() {
        return this.points[this.i];
    }
    public final Vec3i last() {
        final Vec3i[] points = this.points;
        final int length = this.length;
        if (length > 0)
            return points[length - 1];
        else
            return null;
    }

    public final boolean done() { return this.i >= this.length; }

    public void update(final IPathingEntity subject) {
        boolean mutated = false;

        try {
            if (done())
                return;

            final int unlevelIndex = unlevelIndex(this.i, subject.coordinates());

            int adjacentIndex;
            double minDistanceSquared = Double.MAX_VALUE;

            {
                final com.extollit.linalg.immutable.Vec3d currentPosition = subject.coordinates();
                final float width = subject.width();
                final float offset = pointToPositionOffset(width);
                final Vec3d d = new Vec3d(currentPosition);
                final int end = unlevelIndex + 1;

                for (int i = adjacentIndex = this.adjacentIndex; i < this.length && i < end; ++i) {
                    final Vec3i pp = at(i);
                    d.sub(pp);
                    d.sub(offset, 0, offset);
                    d.y = 0;

                    final double distanceSquared = d.mg2();

                    if (distanceSquared < minDistanceSquared) {
                        adjacentIndex = i;
                        minDistanceSquared = distanceSquared;
                    }

                    d.set(currentPosition);
                }
            }

            int targetIndex = this.i;
            if (minDistanceSquared <= 0.25) {
                int advanceTargetIndex;

                targetIndex = adjacentIndex;
                if (targetIndex >= this.taxiUntil && (advanceTargetIndex = directLine(targetIndex, unlevelIndex)) > targetIndex)
                    targetIndex = advanceTargetIndex;
                else
                    targetIndex = adjacentIndex + 1;
            } else if (minDistanceSquared > 0.5)
                targetIndex = adjacentIndex;

            mutated = this.adjacentIndex != adjacentIndex;
            this.adjacentIndex = adjacentIndex;
            this.i = Math.max(adjacentIndex, targetIndex);

            if (stagnantFor(subject) > this.nextDirectLineTimeout) {
                if (this.taxiUntil < adjacentIndex)
                    this.taxiUntil = adjacentIndex + 1;
                else
                    this.taxiUntil++;

                this.nextDirectLineTimeout = DIRECT_LINE_TIME_LIMIT.next(this.random);
            }

            final Vec3i point = done() ? last() : current();
            if (point != null)
                subject.moveTo(positionFor(subject, point));
        } finally {
            if (mutated || this.lastMutationTime < 0)
                this.lastMutationTime = subject.age() * this.speed;
        }
    }

    public boolean taxiing() {
        return this.taxiUntil >= this.adjacentIndex;
    }

    public void taxiUntil(int index) {
        this.taxiUntil = index;
    }

    public static com.extollit.linalg.immutable.Vec3d positionFor(IPathingEntity subject, Vec3i point) {
        final float offset = pointToPositionOffset(subject.width());
        return new com.extollit.linalg.immutable.Vec3d(
                point.x + offset,
                point.y,
                point.z + offset
        );
    }

    private static float pointToPositionOffset(final float subjectWidth) {
        final float dw = (float)(ceil(subjectWidth)) / 2;
        return dw - floor(dw);
    }

    public float stagnantFor(IPathingEntity pathingEntity) { return this.lastMutationTime < 0 ? 0 : pathingEntity.age() * this.speed - this.lastMutationTime; }

    protected int directLine(final int from, final int until) {
        final int levelDistance = until - from + 1;
        if (levelDistance < 2)
            return from;

        int [] xis = new int[3],
               zis = new int[3];
        int ii = 0,
            i = from,
            i0 = i,
            xi00 = 0,
            zi00 = 0,
            sq;

        final int n = until - 1;

        final Vec3i[] points = this.points;
        Vec3i p0 = points[i];

        while (i++ < n) {
            final Vec3i p = points[i];
            final int
                dx = p.x - p0.x,
                dz = p.z - p0.z;

            int
                xi = xis[ii],
                zi = zis[ii];

            xi += dx;
            zi += dz;

            if (xi * zi != 0) {
                final int
                    xi0 = xis[ii],
                    zi0 = zis[ii];

                xi00 = xi0;
                zi00 = zi0;

                xi -= xi0;
                zi -= zi0;

                if (++ii >= 3) {
                    --ii;
                    break;
                }

                i0 = i - 1;
            }

            xis[ii] = xi;
            zis[ii] = zi;

            sq = xi * zi00 + zi * xi00;
            sq *= sq;

            if (sq > (zi00 + xi00) * (zi00 + xi00))
                break;

            p0 = p;
        }
        i = i0;

        xi00 = xis[0];
        zi00 = zis[0];

        final int iiN = ii;
        int xi = 0,
            zi = 0,

            axi00 = abs(xi00),
            azi00 = abs(zi00);

        ii = 0;
        p0 = points[i];
        while (i++ < n) {
            final Vec3i p = points[i];
            final int
                    dx = p.x - p0.x,
                    dz = p.z - p0.z,

                    xi0 = xi,
                    zi0 = zi;

            xi += dx;
            zi += dz;

            if (abs(xi0) > axi00 || (abs(zi0) > azi00)) {
                --i;
                break;
            }

            if (xi * zi != 0) {
                if (xi0 != xi00 || zi0 != zi00)
                    break;

                xi -= xi00;
                zi -= zi00;

                ii = (ii + 1) % iiN;

                xi00 = xis[ii];
                zi00 = zis[ii];

                axi00 = abs(xi00);
                azi00 = abs(zi00);
            }

            if (dx * xi00 < 0 || dz * zi00 < 0)
                break;

            p0 = p;
        }

        return --i;
    }

    private int unlevelIndex(int from, com.extollit.linalg.immutable.Vec3d position) {
        final int y0 = floor(position.y);
        int levelIndex = length();

        for (int i = from; i < length(); ++i)
        {
            final Vec3i pp = at(i);
            if (pp.y != y0)
            {
                levelIndex = i;
                break;
            }
        }
        return levelIndex;
    }

    public boolean sameAs(PathObject other) {
        return Arrays.equals(points, other.points);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PathObject that = (PathObject) o;
        return i == that.i && sameAs(that);
    }

    @Override
    public int hashCode() {
        final Vec3i last = last();
        return last == null ? 0 : last.hashCode();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        int index = 0;
        sb.append("Last Mutation: ");
        sb.append(this.lastMutationTime);
        sb.append(System.lineSeparator());
        for (Vec3i pp : this.points) {
            if (index++ == i)
                sb.append('*');

            sb.append(pp);
            sb.append(System.lineSeparator());
        }
        return sb.toString();
    }
}
