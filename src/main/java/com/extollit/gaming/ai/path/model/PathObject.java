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
    private static FloatRange DIRECT_LINE_TIME_LIMIT = new FloatRange(1, 2);

    final Vec3i[] points;
    private final float speed;
    private final boolean flying;

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
        this(speed, false, points);
    }
    protected PathObject(float speed, boolean flying, Vec3i... points) {
        this.points = points;
        this.length = points.length;
        this.speed = speed;
        this.flying = flying;
        this.nextDirectLineTimeout = DIRECT_LINE_TIME_LIMIT.next(this.random);
    }

    public void setRandomNumberGenerator(Random random) {
        this.random = random;
    }

    public static PathObject fromHead(float speed, boolean flying, Node head) {
        int i = 1;

        for (Node p = head; p.up() != null; p = p.up())
            ++i;

        final Vec3i[] result = new Vec3i[i];
        final Vec3i key = head.key;
        result[--i] = key;

        for (Node p = head; p.up() != null; result[--i] = p.key)
            p = p.up();

        if (result.length <= 1)
            return null;
        else
            return new PathObject(speed, flying, result);
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

    public static boolean active(PathObject path) { return path != null && !path.done(); }

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
            } else if (minDistanceSquared > 0.5 || targetIndex < this.taxiUntil)
                targetIndex = adjacentIndex;

            mutated = adjacentIndex > this.adjacentIndex;
            this.adjacentIndex = adjacentIndex;
            this.i = Math.max(adjacentIndex, targetIndex);

            if (stagnantFor(subject) > this.nextDirectLineTimeout) {
                if (this.taxiUntil < adjacentIndex)
                    this.taxiUntil = adjacentIndex + 1;
                else
                    this.taxiUntil++;

                this.nextDirectLineTimeout += DIRECT_LINE_TIME_LIMIT.next(this.random);
            }

            final Vec3i point = done() ? last() : current();
            if (point != null)
                subject.moveTo(positionFor(subject, point));
        } finally {
            if (mutated || this.lastMutationTime < 0) {
                this.lastMutationTime = subject.age() * this.speed;
                if (this.nextDirectLineTimeout > DIRECT_LINE_TIME_LIMIT.max)
                    this.nextDirectLineTimeout = DIRECT_LINE_TIME_LIMIT.next(this.random);
            }
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
        int [] xis = new int[4],
               yis = new int[4],
               zis = new int[4];
        int ii = 0,
            i = from,
            i0 = i,
            xi00 = 0,
            yi00 = 0,
            zi00 = 0,
            sq;

        boolean bdx = false, bdy = false, bdz = false;
        final int n = until - 1;

        final Vec3i[] points = this.points;
        Vec3i p0 = points[i];

        while (i++ < n) {
            final Vec3i p = points[i];
            final int
                dx = p.x - p0.x,
                dy = p.y - p0.y,
                dz = p.z - p0.z;

            if (!flying && dy != 0)
                return i - 1;

            int
                xi = xis[ii],
                yi = yis[ii],
                zi = zis[ii];

            xi += dx;
            yi += dy;
            zi += dz;

            if (((xi * zi) | (zi * yi) | (yi * xi)) != 0) {
                final int
                    xi0 = xis[ii],
                    yi0 = yis[ii],
                    zi0 = zis[ii];

                xi00 = xi0;
                yi00 = yi0;
                zi00 = zi0;

                xi -= xi0;
                yi -= yi0;
                zi -= zi0;

                final boolean
                    bdx0 = bdx,
                    bdy0 = bdy,
                    bdz0 = bdz;

                if ((!(bdx ^= (dx != 0)) && bdx0) ||
                    (!(bdy ^= (dy != 0)) && bdy0) ||
                    (!(bdz ^= (dz != 0)) && bdz0))
                    break;
                else
                    ++ii;

                i0 = i - 1;
            }

            xis[ii] = xi;
            yis[ii] = yi;
            zis[ii] = zi;

            sq =  (xi * zi00 + xi * yi00)
                + (zi * xi00 + zi * yi00)
                + (yi * xi00 + yi * zi00);
            sq *= sq;

            if (sq > (zi00 + yi00 + xi00) * (zi00 + yi00 + xi00))
                break;

            p0 = p;
        }
        i = i0;

        xi00 = xis[0];
        yi00 = yis[0];
        zi00 = zis[0];

        final int iiN = ii;
        int xi = 0,
            yi = 0,
            zi = 0,

            axi00 = abs(xi00),
            ayi00 = abs(yi00),
            azi00 = abs(zi00);

        ii = 0;
        p0 = points[i];
        while (i++ < n) {
            final Vec3i p = points[i];
            final int
                    dx = p.x - p0.x,
                    dy = p.y - p0.y,
                    dz = p.z - p0.z,

                    xi0 = xi,
                    yi0 = yi,
                    zi0 = zi;

            xi += dx;
            yi += dy;
            zi += dz;

            if (abs(xi0) > axi00 || abs(yi0) > ayi00 || abs(zi0) > azi00) {
                --i;
                break;
            }

            if (((xi * zi) | (zi * yi) | (yi * xi)) != 0) {
                if (xi0 != xi00 || yi0 != yi00 || zi0 != zi00)
                    break;

                xi -= xi00;
                yi -= yi00;
                zi -= zi00;

                ii = (ii + 1) % iiN;

                xi00 = xis[ii];
                yi00 = yis[ii];
                zi00 = zis[ii];

                axi00 = abs(xi00);
                ayi00 = abs(yi00);
                azi00 = abs(zi00);
            }

            if (dx * xi00 < 0 || dy * yi00 < 0 || dz * zi00 < 0)
                break;

            p0 = p;
        }

        return --i;
    }

    private int unlevelIndex(int from, com.extollit.linalg.immutable.Vec3d position) {
        if (this.flying)
            return this.points.length - 1;

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

    public void adjustPathPosition(PathObject formerPath, final IPathingEntity pathingEntity) {
        final float pointOffset = pointToPositionOffset(pathingEntity.width());
        final int length = this.length;
        final Vec3i
                lastPointVisited = formerPath.current();

        final com.extollit.linalg.immutable.Vec3d coordinates = pathingEntity.coordinates();
        final double
            x = coordinates.x,
            y = coordinates.y,
            z = coordinates.z;

        double minSquareDistFromSource = Double.MAX_VALUE;
        int c = -1;

        while(++c < formerPath.i && c < length && at(c).equals(formerPath.at(c)));
        c--;

        while(++c < length) {
            final Vec3i p = at(c);
            if (p.equals(lastPointVisited)) {
                i = c;
                break;
            }

            final double
                    dx = p.x - x + pointOffset,
                    dy = p.y - y,
                    dz = p.z - z + pointOffset,

                    squareDelta = dx * dx + dy * dy + dz * dz;

            if (squareDelta < minSquareDistFromSource) {
                minSquareDistFromSource = squareDelta;
                i = c;
            }
        }
    }

    public boolean reachableFrom(PathObject otherPath) {
        final Vec3i point = otherPath.current();

        for (Vec3i p : this.points)
            if (p.equals(point))
                return true;

        return false;
    }
}
