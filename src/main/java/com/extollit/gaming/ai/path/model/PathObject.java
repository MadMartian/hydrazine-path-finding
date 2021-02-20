package com.extollit.gaming.ai.path.model;

import com.extollit.gaming.ai.path.IConfigModel;
import com.extollit.gaming.ai.path.persistence.*;
import com.extollit.linalg.immutable.Vec3i;
import com.extollit.linalg.mutable.Vec3d;
import com.extollit.num.FloatRange;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Random;

import static java.lang.Math.*;

public final class PathObject implements IPath {
    private static final double PATHPOINT_SNAP_MARGIN_SQ = 0.25;
    private static FloatRange DIRECT_LINE_TIME_LIMIT = new FloatRange(1, 2);

    final Node[] nodes;
    private final float speed;
    private final Random random;

    public int i;

    private int
        taxiUntil = 0,
        adjacentIndex = 0,
        length;

    private float nextDirectLineTimeout, lastMutationTime = -1;

    public static void configureFrom(IConfigModel configModel) {
        DIRECT_LINE_TIME_LIMIT = configModel.directLineTimeLimit();
    }

    PathObject(float speed, Node... nodes) {
        this(speed, new Random(), nodes);
    }

    protected PathObject(float speed, Random random, Node... nodes) {
        this.nodes = nodes;
        this.length = nodes.length;
        this.speed = speed;
        this.random = random;
        this.nextDirectLineTimeout = DIRECT_LINE_TIME_LIMIT.next(random);
    }

    public static IPath fromHead(float speed, Random random, Node head) {
        int i = 1;

        for (Node p = head; p.up() != null; p = p.up())
            ++i;

        final Node[] result = new Node[i];
        result[--i] = head;

        for (Node p = head; p.up() != null; result[--i] = p)
            p = p.up();

        if (result.length <= 1)
            return new IncompletePath(result[0]);
        else
            return new PathObject(speed, random, result);
    }

    @Override
    public void truncateTo(int length) {
        if (length < 0 || length >= this.nodes.length)
            throw new ArrayIndexOutOfBoundsException(
                MessageFormat.format("Length is out of bounds 0 <= length < {0} but length = {1}", this.nodes.length, length)
            );

        this.length = length;
    }

    @Override
    public void untruncate() {
        this.length = this.nodes.length;
    }

    @Override
    public Iterator<INode> iterator() {
        return Arrays.<INode>stream(this.nodes).limit(this.length).iterator();
    }
    @Override
    public final int length() { return this.length; }

    @Override
    public final int cursor() {
        return this.i;
    }

    @Override
    public final INode at(int i) { return this.nodes[i]; }
    @Override
    public final INode current() {
        return this.nodes[this.i];
    }
    @Override
    public final INode last() {
        final Node[] nodes = this.nodes;
        final int length = this.length;
        if (length > 0)
            return nodes[length - 1];
        else
            return null;
    }

    public static boolean active(IPath path) { return path != null && !path.done(); }

    @Override
    public final boolean done() { return this.i >= this.length; }

    public void update(final IPathingEntity subject) {
        boolean mutated = false;

        try {
            if (done())
                return;

            final int unlevelIndex;

            final IPathingEntity.Capabilities capabilities = subject.capabilities();
            final boolean grounded = !(capabilities.avian() || capabilities.aquatic() && capabilities.swimmer());
            final float fy;

            if (grounded) {
                unlevelIndex = unlevelIndex(this.i, subject.coordinates());
                fy = 0;
            } else {
                unlevelIndex = this.length;
                fy = 1;
            }

            final int adjacentIndex0 = this.adjacentIndex;
            final double minDistanceSquared = updateNearestAdjacentIndex(subject, unlevelIndex, fy);
            final int adjacentIndex = this.adjacentIndex;

            int targetIndex = this.i;
            if (minDistanceSquared <= PATHPOINT_SNAP_MARGIN_SQ) {
                int advanceTargetIndex;

                targetIndex = adjacentIndex;
                if (targetIndex >= this.taxiUntil && (advanceTargetIndex = directLine(targetIndex, unlevelIndex, grounded)) > targetIndex)
                    targetIndex = advanceTargetIndex;
                else
                    targetIndex = adjacentIndex + 1;
            } else if (minDistanceSquared > 0.5 || targetIndex < this.taxiUntil)
                targetIndex = adjacentIndex;

            mutated = adjacentIndex > adjacentIndex0;
            this.adjacentIndex = adjacentIndex;
            this.i = Math.max(adjacentIndex, targetIndex);

            if (stagnantFor(subject) > this.nextDirectLineTimeout) {
                if (this.taxiUntil < adjacentIndex)
                    this.taxiUntil = adjacentIndex + 1;
                else
                    this.taxiUntil++;

                this.nextDirectLineTimeout += DIRECT_LINE_TIME_LIMIT.next(this.random);
            }

            final INode node = done() ? last() : current();
            if (node != null)
                moveSubjectTo(subject, node);
        } finally {
            if (mutated || this.lastMutationTime < 0) {
                this.lastMutationTime = subject.age() * this.speed;
                if (this.nextDirectLineTimeout > DIRECT_LINE_TIME_LIMIT.max)
                    this.nextDirectLineTimeout = DIRECT_LINE_TIME_LIMIT.next(this.random);
            }
        }
    }

    private double updateNearestAdjacentIndex(IPathingEntity subject, int unlevelIndex, float fy) {
        double minDistanceSquared = Double.MAX_VALUE;
        int nextAdjacentIndex;
        final com.extollit.linalg.immutable.Vec3d currentPosition = subject.coordinates();
        final float width = subject.width();
        final double offset = pointToPositionOffset(width);
        final Vec3d d = new Vec3d(currentPosition);
        final int end = unlevelIndex + 1;

        for (int i = nextAdjacentIndex = this.adjacentIndex; i < this.length && i < end; ++i) {
            final Node node = this.nodes[i];
            final Vec3i pp = node.key;
            d.sub(pp);
            d.sub(offset, 0, offset);
            d.y *= fy;

            final double distanceSquared = d.mg2();

            if (distanceSquared < minDistanceSquared) {
                nextAdjacentIndex = i;
                minDistanceSquared = distanceSquared;
            }

            d.set(currentPosition);
        }
        this.adjacentIndex = nextAdjacentIndex;
        return minDistanceSquared;
    }

    private void moveSubjectTo(IPathingEntity subject, INode pathPoint) {
        final Vec3d d = new Vec3d(subject.coordinates());
        final com.extollit.linalg.immutable.Vec3d position = positionFor(subject, pathPoint.coordinates());
        d.sub(position);
        if (d.mg2() > PATHPOINT_SNAP_MARGIN_SQ)
            subject.moveTo(position, pathPoint.passibility(), pathPoint.gravitation());
    }

    @Override
    public boolean taxiing() {
        return this.taxiUntil >= this.adjacentIndex;
    }

    @Override
    public void taxiUntil(int index) {
        this.taxiUntil = index;
    }

    public static com.extollit.linalg.immutable.Vec3d positionFor(IPathingEntity subject, Vec3i point) {
        final double offset = pointToPositionOffset(subject.width());
        return new com.extollit.linalg.immutable.Vec3d(
                point.x + offset,
                point.y,
                point.z + offset
        );
    }

    private static double pointToPositionOffset(final float subjectWidth) {
        final double dw = ceil(subjectWidth) / 2;
        return dw - floor(dw);
    }

    public float stagnantFor(IPathingEntity pathingEntity) { return this.lastMutationTime < 0 ? 0 : pathingEntity.age() * this.speed - this.lastMutationTime; }

    protected int directLine(final int from, final int until, boolean grounded) {
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

        final Node[] nodes = this.nodes;
        final Node node0 = nodes[i];
        Vec3i p0 = node0.key;

        while (i++ < n) {
            final Node node = nodes[i];
            final Vec3i p = node.key;
            final int
                dx = p.x - p0.x,
                dy = p.y - p0.y,
                dz = p.z - p0.z;

            if (grounded && dy != 0)
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
        p0 = nodes[i].key;
        while (i++ < n) {
            final Node node = nodes[i];
            final Vec3i p = node.key;
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
        final int y0 = (int)floor(position.y);
        final Node [] nodes = this.nodes;
        int levelIndex = length();

        for (int i = from; i < length(); ++i)
        {
            final Node node = nodes[i];
            if (node.key.y - y0 != 0)
            {
                levelIndex = i;
                break;
            }
        }
        return levelIndex;
    }

    @Override
    public boolean sameAs(IPath other) {
        final Node[] thisNodes = this.nodes;
        final int length = this.length;

        int c = 0;
        Iterator<INode> i = other.iterator();
        while (c < length && i.hasNext()) {
            if (!thisNodes[c].key.equals(i.next().coordinates()))
                return false;

            c++;
        }

        return c >= length && !i.hasNext();
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
        final INode last = last();
        return last == null ? 0 : last.hashCode();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        int index = 0;
        sb.append("Last Mutation: ");
        sb.append(this.lastMutationTime);
        sb.append(System.lineSeparator());
        for (Node pp : this.nodes) {
            if (index++ == i)
                sb.append('*');

            sb.append(pp.key);
            sb.append(System.lineSeparator());
        }
        return sb.toString();
    }

    public void adjustPathPosition(IPath formerPath, final IPathingEntity pathingEntity) {
        final double pointOffset = pointToPositionOffset(pathingEntity.width());
        final int length = this.length;
        final Node []
            nodes = this.nodes;

        final INode
                lastPointVisited = formerPath.current();

        final com.extollit.linalg.immutable.Vec3d coordinates = pathingEntity.coordinates();
        final double
            x = coordinates.x,
            y = coordinates.y,
            z = coordinates.z;

        double minSquareDistFromSource = Double.MAX_VALUE;
        int c = -1;

        while(++c < formerPath.cursor() && c < length && nodes[c].key.equals(formerPath.at(c).coordinates()));
        c--;

        while(++c < length) {
            final INode node = nodes[c];
            final Vec3i p = node.coordinates();
            if (p.equals(lastPointVisited.coordinates())) {
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
        final INode pivot = otherPath.current();

        for (Node node : this.nodes)
            if (node.key.equals(pivot.coordinates()))
                return true;

        return false;
    }
    
    public static final class ReaderWriter implements PartialObjectWriter<PathObject>, PartialObjectReader<PathObject>, LinkableWriter<PathObject, Node>, LinkableReader<PathObject, Node> {
        public static final ReaderWriter INSTANCE = new ReaderWriter();

        private ReaderWriter() {}

        @Override
        public void readLinkages(PathObject path, ReferableObjectInput<Node> in) throws IOException {
            final Node[] nodes = path.nodes;
            for (int c = 0; c < nodes.length; ++c)
                nodes[c] = in.readRef();
        }

        @Override
        public void writeLinkages(PathObject path, ReferableObjectOutput<Node> out) throws IOException {
            out.writeShort(path.nodes.length);
            for (Node node : path.nodes)
                out.writeRef(node);
        }

        @Override
        public PathObject readPartialObject(ObjectInput in) throws IOException {
            final short count = in.readShort();
            final PathObject path = new PathObject(in.readFloat(), new Node[count]);
            path.i = in.readInt();
            path.taxiUntil = in.readInt();
            path.adjacentIndex = in.readInt();
            path.length = in.readInt();
            path.nextDirectLineTimeout = in.readFloat();
            path.lastMutationTime = in.readFloat();
            return path;
        }

        @Override
        public void writePartialObject(PathObject path, ObjectOutput out) throws IOException {
            out.writeShort(path.nodes.length);
            out.writeFloat(path.speed);
            out.writeInt(path.i);
            out.writeInt(path.taxiUntil);
            out.writeInt(path.adjacentIndex);
            out.writeInt(path.length);
            out.writeFloat(path.nextDirectLineTimeout);
            out.writeFloat(path.lastMutationTime);
        }
    }
}
