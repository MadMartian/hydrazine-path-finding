package com.extollit.gaming.ai.path;

import com.extollit.collect.SparseSpatialMap;
import com.extollit.gaming.ai.path.model.*;
import com.extollit.linalg.immutable.AxisAlignedBBox;
import com.extollit.linalg.immutable.IntAxisAlignedBox;
import com.extollit.linalg.immutable.Vec3i;
import com.extollit.linalg.mutable.Vec3d;
import com.extollit.num.FastMath;
import com.extollit.num.FloatRange;
import com.extollit.tuple.Pair;

import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static com.extollit.num.FastMath.ceil;
import static com.extollit.num.FastMath.floor;
import static java.lang.Math.round;

public class HydrazinePathFinder {
    private static final AxisAlignedBBox FULL_BOUNDS = new AxisAlignedBBox(0, 0, 0, 1, 1, 1);

    private static double DOT_THRESHOLD = 0.6;
    private static int
            MAX_SAFE_FALL_DISTANCE = 4,
            MAX_SURVIVE_FALL_DISTANCE = 20,
            CESA_LIMIT = 16;
    private static FloatRange
            PROBATIONARY_TIME_LIMIT = new FloatRange(12, 18),
            PASSIBLE_POINT_TIME_LIMIT = new FloatRange(4, 9);

    private static byte FAILURE_COUNT_THRESHOLD = 3;

    private final SortedPointQueue queue = new SortedPointQueue();
    private final SparseSpatialMap<HydrazinePathPoint> nodeMap = new SparseSpatialMap<>(3);
    private final Set<Vec3i> unreachableFromSource = new HashSet<>(3);
    private final IPathingEntity subject;
    private final IInstanceSpace instanceSpace;

    private com.extollit.linalg.mutable.Vec3d sourcePosition, destinationPosition;
    private IDynamicMovableObject destinationEntity;

    private IOcclusionProvider occlusionProvider;
    private PathObject currentPath;
    private IPathingEntity.Capabilities capabilities;
    private HydrazinePathPoint current, source, target, closest;
    private int cx0, cxN, cz0, czN, discreteSize, tall, initComputeIterations, periodicComputeIterations;
    private int failureCount;
    private float searchRangeSquared, passiblePointPathTimeLimit, nextGraphCacheReset, actualSize;
    private Random random = new Random();

    public static void configureFrom(IConfigModel configModel) {
        FAILURE_COUNT_THRESHOLD = configModel.failureCountThreshold();
        PROBATIONARY_TIME_LIMIT = configModel.probationaryTimeLimit();
        PASSIBLE_POINT_TIME_LIMIT = configModel.passiblePointTimeLimit();
        DOT_THRESHOLD = configModel.dotThreshold();
        MAX_SAFE_FALL_DISTANCE = configModel.safeFallDistance();
        MAX_SURVIVE_FALL_DISTANCE = configModel.surviveFallDistance();
        CESA_LIMIT = configModel.cesaLimit();
    }

    public HydrazinePathFinder(IPathingEntity entity, IInstanceSpace instanceSpace) {
        this.subject = entity;
        this.instanceSpace = instanceSpace;

        applySubject();
        schedulingPriority(SchedulingPriority.low);

        this.passiblePointPathTimeLimit = PASSIBLE_POINT_TIME_LIMIT.next(this.random);
    }

    public void setRandomNumberGenerator(Random random) {
        this.random = random;
    }

    public void schedulingPriority(SchedulingPriority schedulingPriority) {
        this.initComputeIterations = schedulingPriority.initComputeIterations;
        this.periodicComputeIterations = schedulingPriority.periodicComputeIterations;
    }

    public final Vec3i trackingDestination() {
        if (this.destinationEntity != null && this.destinationPosition != null) {
            final HydrazinePathPoint pointAtDestination = pointAtDestination();
            if (impassible(pointAtDestination))
                return null;
            else
                return pointAtDestination.key;
        } else
            return null;
    }
    public final Vec3i currentTarget() { return this.target == null ? null : this.target.key; }

    public PathObject trackPathTo(IDynamicMovableObject target) {
        this.destinationEntity = target;
        return initiatePathTo(target.coordinates());
    }

    public PathObject initiatePathTo(com.extollit.linalg.immutable.Vec3d coordinates) {
        return initiatePathTo(coordinates.x, coordinates.y, coordinates.z);
    }

    public PathObject initiatePathTo(double x, double y, double z) {
        updateSourcePosition();
        applySubject();

        final float rangeSquared = this.searchRangeSquared;
        final com.extollit.linalg.immutable.Vec3d sourcePos = new com.extollit.linalg.immutable.Vec3d(this.sourcePosition);
        if (sourcePos.subOf(x, y, z).mg2() > rangeSquared)
            return null;

        updateDestination(x, y, z);

        if (!graphTimeout() && (reachedTarget() || triageTimeout() || destinationDeviatedFromTarget()))
            resetTriage();

        return triage(this.initComputeIterations);
    }

    public PathObject updatePathFor(IPathingEntity pathingEntity) {
        final PathObject path = update();
        if (PathObject.active(path)) {
            path.update(pathingEntity);
            if (path.done())
                return null;

            return path;
        }

        return null;
    }

    protected PathObject update() {
        if (this.destinationEntity != null)
            updateDestination(this.destinationEntity.coordinates());

        if (this.destinationPosition == null)
            return this.currentPath;

        updateSourcePosition();
        graphTimeout();

        if (reachedTarget() || triageTimeout() || destinationDeviatedFromTarget())
            resetTriage();

        return triage(this.periodicComputeIterations);
    }

    private boolean destinationDeviatedFromTarget() {
        final com.extollit.linalg.mutable.Vec3d
                dt = new com.extollit.linalg.mutable.Vec3d(this.target.key),
                dd = new com.extollit.linalg.mutable.Vec3d(destinationPosition);

        dd.x = floor(dd.x);
        dd.y = ceil(dd.y);
        dd.z = floor(dd.z);

        final Vec3i source = this.source.key;
        dt.sub(source);
        dd.sub(source);

        if (dt.mg2() > dd.mg2())
            return true;

        dt.normalize();
        dd.normalize();

        return dt.dot(dd) < DOT_THRESHOLD;
    }

    private boolean triageTimeout() {
        final PathObject currentPath = this.currentPath;
        final boolean status =
                PathObject.active(currentPath) &&
                currentPath.length() > 0 &&
                currentPath.stagnantFor(this.subject) > this.passiblePointPathTimeLimit;

        if (status) {
            if (++this.failureCount == 1)
                this.nextGraphCacheReset = pathTimeAge() + PROBATIONARY_TIME_LIMIT.next(this.random);

            final Vec3i culprit = currentPath.at(currentPath.i);
            this.nodeMap.remove(culprit);
        }

        return status;
    }

    private boolean graphTimeout() {
        if (this.failureCount >= FAILURE_COUNT_THRESHOLD
            && pathTimeAge() > this.nextGraphCacheReset)
        {
            resetGraph();
            return true;
        }

        return false;
    }

    protected final void resetTriage() {
        final Vec3d
                sourcePosition = this.sourcePosition,
                destinationPosition = this.destinationPosition;

        updateFieldWindow(
                floor(sourcePosition.x),
                floor(sourcePosition.z),

                floor(destinationPosition.x),
                floor(destinationPosition.z), true
        );

        applySubject();

        final HydrazinePathPoint source = this.current = this.source = pointAtSource();
        source.length(0);
        source.orphan();

        refinePassibility(source.key);

        setTargetFor(source);

        for (HydrazinePathPoint p : this.nodeMap.values())
            p.first(false);

        this.queue.clear();
        this.queue.add(source);
        this.closest = null;
        this.passiblePointPathTimeLimit = PASSIBLE_POINT_TIME_LIMIT.next(this.random);
    }

    protected final boolean refinePassibility(Vec3i sourcePoint) {
        this.unreachableFromSource.clear();

        if (!fuzzyPassibility(sourcePoint.x, sourcePoint.y, sourcePoint.z))
            return false;

        final IBlockObject blockObject = this.instanceSpace.blockObjectAt(sourcePoint.x, sourcePoint.y, sourcePoint.z);
        if (!blockObject.isImpeding())
            return false;

        final AxisAlignedBBox bounds = blockObject.bounds();
        final Vec3d
            c = new Vec3d(subject.coordinates());

        c.sub(sourcePoint);
        final com.extollit.linalg.immutable.Vec3d delta = new com.extollit.linalg.immutable.Vec3d(c);

        c.sub(bounds.center());

        boolean mutated = false;

        if (delta.z >= bounds.min.z && delta.z <= bounds.max.z) {
            final int x = sourcePoint.x + (c.x < 0 ? +1 : -1);

            for (int dz = -1; dz <= +1; ++dz)
                this.unreachableFromSource.add(
                    new Vec3i(x, sourcePoint.y, sourcePoint.z + dz)
                );

            mutated = true;
        }

        if (delta.x >= bounds.min.x && delta.x <= bounds.max.x) {
            final int z = sourcePoint.z + (c.z < 0 ? +1 : -1);

            for (int dx = -1; dx <= +1; ++dx)
                this.unreachableFromSource.add(
                    new Vec3i(sourcePoint.x + dx, sourcePoint.y, z)
                );

            mutated = true;
        }

        return mutated;
    }

    private void applySubject() {
        this.discreteSize = FastMath.floor((this.actualSize = this.subject.width()) + 1);
        this.tall = FastMath.floor(this.subject.height() + 1);
        final float pathSearchRange = this.subject.searchRange();
        this.searchRangeSquared = pathSearchRange*pathSearchRange;
        this.capabilities = this.subject.capabilities();
    }

    private void setTargetFor(HydrazinePathPoint source) {
        final Vec3d
                destinationPosition = this.destinationPosition;

        int distance = 0x40;
        this.target = pointAtDestination();
        while (distance > 0 && !source.target(this.target)) {
            final Vec3d
                v = new Vec3d(destinationPosition),
                init = new Vec3d(source.key);

            distance--;

            v.sub(init);
            v.normalize();
            v.mul(distance);
            v.add(init);
            this.target = cachedPointAt(
                floor(v.x),
                ceil(v.y),
                floor(v.z)
            );
        }

        if (distance == 0)
            source.target(this.target = this.source);
    }

    private void resetGraph() {
        this.nodeMap.clear();
        resetTriage();
        this.failureCount = 0;
        this.nextGraphCacheReset = 0;
    }

    private void updateFieldWindow(PathObject path) {
        Vec3i pp = path.last();
        if (pp == null)
            return;

        final com.extollit.linalg.mutable.Vec3i
                min = new com.extollit.linalg.mutable.Vec3i(pp.x, pp.y, pp.z),
                max = new com.extollit.linalg.mutable.Vec3i(pp.x, pp.y, pp.z);

        if (!path.done())
            for (int c = path.i; c < path.length(); ++c) {
                pp = path.at(c);
                if (pp.x < min.x)
                    min.x = pp.x;
                if (pp.y < min.y)
                    min.y = pp.y;
                if (pp.z < min.z)
                    min.z = pp.z;

                if (pp.x > max.x)
                    max.x = pp.x;
                if (pp.y > max.y)
                    max.y = pp.y;
                if (pp.z > max.z)
                    max.z = pp.z;
            }

        updateFieldWindow(min.x, min.z, max.x, max.z, true);
    }
    private void updateFieldWindow(int sourceX, int sourceZ, int targetX, int targetZ, final boolean cull) {
        int x0, xN, z0, zN;

        if (sourceX > targetX) {
            x0 = targetX;
            xN = sourceX;
        } else {
            x0 = sourceX;
            xN = targetX;
        }

        if (sourceZ > targetZ) {
            z0 = targetZ;
            zN = sourceZ;
        } else {
            z0 = sourceZ;
            zN = targetZ;
        }

        final IDynamicMovableObject destinationEntity = this.destinationEntity;
        final float entityWidth = this.subject.width();
        int entitySize = ceil(
                destinationEntity != null ?
                        Math.max(entityWidth, destinationEntity.width()) :
                        entityWidth
        );

        final float searchAreaRange = this.subject.searchRange();
        x0 -= searchAreaRange + entitySize;
        z0 -= searchAreaRange + entitySize;
        xN += searchAreaRange + entitySize;
        zN += searchAreaRange + entitySize;

        final int
                cx0 = x0 >> 4,
                cz0 = z0 >> 4,
                cxN = xN >> 4,
                czN = zN >> 4;

        final IOcclusionProvider aop = this.occlusionProvider;
        final boolean windowTest;

        if (cull)
            windowTest = cx0 != this.cx0 || cz0 != this.cz0 || cxN != this.cxN || czN != this.czN;
        else
            windowTest = cx0 < this.cx0 || cz0 < this.cz0 || cxN > this.cxN || czN > this.czN;

        if (aop == null || windowTest) {
            this.occlusionProvider = occlusionProviderFor(cx0, cz0, cxN, czN);
            this.cx0 = cx0;
            this.cz0 = cz0;
            this.cxN = cxN;
            this.czN = czN;

            if (cull)
                this.nodeMap.cullOutside(new IntAxisAlignedBox(x0, Integer.MIN_VALUE, z0, xN, Integer.MAX_VALUE, zN));
        }
    }

    private HydrazinePathPoint pointAtDestination() {
        final Vec3d destinationPosition = this.destinationPosition;
        if (destinationPosition == null)
            return null;

        return cachedPointAt(
                floor(destinationPosition.x),
                ceil(destinationPosition.y),
                floor(destinationPosition.z)
        );
    }

    private HydrazinePathPoint pointAtSource() {
        final Vec3d sourcePosition = this.sourcePosition;
        final int
                x = floor(sourcePosition.x),
                y = floor(sourcePosition.y),
                z = floor(sourcePosition.z);

        HydrazinePathPoint candidate = cachedPassiblePointNear(x, y, z, null);
        if (candidate == null) {
            candidate = cachedPointAt(x, y, z);
            candidate.passibility(Passibility.impassible);
        }
        return candidate;
    }

    private void updateDestination(double x, double y, double z) {
        if (this.destinationPosition != null) {
            final Vec3d destinationPosition = this.destinationPosition;
            destinationPosition.x = x;
            destinationPosition.y = y;
            destinationPosition.z = z;
        } else
            this.destinationPosition = new Vec3d(x, y, z);
    }

    private void updateDestination(com.extollit.linalg.immutable.Vec3d coordinates) {
        if (this.destinationPosition != null)
            this.destinationPosition.set(coordinates);
        else
            this.destinationPosition = new Vec3d(coordinates);
    }

    private boolean reachedTarget() {
        final boolean flag = this.target == null || this.source == this.target || this.current == this.target;
        if (flag) {
            this.failureCount = 0;
            this.passiblePointPathTimeLimit = PASSIBLE_POINT_TIME_LIMIT.next(this.random);
        }
        return flag;
    }

    private void updateSourcePosition() {
        final com.extollit.linalg.immutable.Vec3d coordinates = this.subject.coordinates();
        if (this.sourcePosition != null) {
            final Vec3d sourcePosition;
            sourcePosition = this.sourcePosition;
            sourcePosition.x = coordinates.x;
            sourcePosition.y = coordinates.y;
            sourcePosition.z = coordinates.z;
        } else
            this.sourcePosition = new Vec3d(coordinates.x, coordinates.y, coordinates.z);

        final int
            x = floor(coordinates.x),
            z = floor(coordinates.z);

        updateFieldWindow(x, z, x, z, false);

        this.current = pointAtSource();
    }

    public void reset() {
        this.currentPath = null;
        this.queue.clear();
        this.nodeMap.clear();
        this.unreachableFromSource.clear();
        this.target =
        this.source =
        this.closest = null;
        this.current = null;
        this.sourcePosition =
        this.destinationPosition = null;
        this.destinationEntity = null;
        this.occlusionProvider = null;
        this.failureCount = 0;
        this.nextGraphCacheReset = 0;
        this.passiblePointPathTimeLimit = PASSIBLE_POINT_TIME_LIMIT.next(this.random);
    }

    private PathObject updatePath(PathObject newPath) {
        if (PathObject.active(newPath)) {
            if (this.currentPath != null) {
                if (this.currentPath.sameAs(newPath))
                    newPath = this.currentPath;
                else if (!this.currentPath.done())
                    newPath.adjustPathPosition(this.currentPath, this.subject);
            }

            if (this.occlusionProvider == null)
                updateFieldWindow(newPath);

            if (newPath.done())
                newPath = null;

            return this.currentPath = newPath;
        } else
            return this.currentPath = null;
    }

    private PathObject triage(int iterations) {
        final PathObject currentPath = this.currentPath;
        final SortedPointQueue queue = this.queue;

        if (queue.isEmpty())
            if (currentPath == null)
                return null;
            else if (!currentPath.done())
                return currentPath;
            else
                resetTriage();

        PathObject nextPath = null;

        final Vec3i currentPathPoint = this.current.key;

        while (!queue.isEmpty() && iterations-- > 0) {
            final HydrazinePathPoint current = queue.dequeue();
            boolean resetTriage = false;

            if (!current.contains(currentPathPoint)) {
                resetTriage = true;
                iterations += 2;
            }

            if (!resetTriage) {
                if (this.closest == null || this.closest.orphaned() || HydrazinePathPoint.squareDelta(current, this.target) < HydrazinePathPoint.squareDelta(this.closest, this.target))
                    this.closest = current;

                if (current == this.target) {
                    nextPath = PathObject.fromHead(this.capabilities.speed(), this.target);
                    if (PathObject.active(nextPath)) {
                        nextPath.setRandomNumberGenerator(this.random);
                        this.queue.clear();
                        break;
                    }

                    resetTriage = true;
                }
            }

            if (resetTriage)
            {
                resetTriage();
                continue;
            }

            processNode(current);
        }

        if (nextPath == null && this.closest != null && !queue.isEmpty())
            nextPath = PathObject.fromHead(this.capabilities.speed(), this.closest);

        return updatePath(nextPath);
    }

    private void processNode(HydrazinePathPoint current) {
        current.first(true);

        final Vec3i coords = current.key;
        final HydrazinePathPoint
                west = cachedPassiblePointNear(coords.x - 1, coords.y, coords.z, coords),
                east = cachedPassiblePointNear(coords.x + 1, coords.y, coords.z, coords),
                north = cachedPassiblePointNear(coords.x, coords.y, coords.z - 1, coords),
                south = cachedPassiblePointNear(coords.x, coords.y, coords.z + 1, coords);

        final boolean found = applyPointOptions(current, west, east, north, south);

        if (!found) {
            com.extollit.linalg.mutable.AxisAlignedBBox
                southBounds = blockBounds(coords, 0, 0, +1),
                northBounds = blockBounds(coords, 0, 0, -1),
                eastBounds = blockBounds(coords, +1, 0, 0),
                westBounds = blockBounds(coords, -1, 0, 0);

            final float actualSizeSquared = this.actualSize * this.actualSize;
            final HydrazinePathPoint[] pointOptions = {
                    westBounds == null || northBounds == null || westBounds.mg2(northBounds) >= actualSizeSquared ? cachedPassiblePointNear(coords.x - 1, coords.y, coords.z - 1, coords) : null,
                    eastBounds == null || southBounds == null || eastBounds.mg2(southBounds) >= actualSizeSquared ? cachedPassiblePointNear(coords.x + 1, coords.y, coords.z + 1, coords) : null,
                    eastBounds == null || northBounds == null || eastBounds.mg2(northBounds) >= actualSizeSquared ? cachedPassiblePointNear(coords.x + 1, coords.y, coords.z - 1, coords) : null,
                    westBounds == null || southBounds == null || westBounds.mg2(southBounds) >= actualSizeSquared ? cachedPassiblePointNear(coords.x - 1, coords.y, coords.z + 1, coords) : null
            };

            applyPointOptions(current, pointOptions);
        }
    }

    private com.extollit.linalg.mutable.AxisAlignedBBox blockBounds(Vec3i coords, int dx, int dy, int dz) {
        final int
            x = coords.x + dx,
            y = coords.y + dy,
            z = coords.z + dz;

        final AxisAlignedBBox bounds;
        final byte flags = this.occlusionProvider.elementAt(x, y, z);
        if (fuzzyPassibility(flags)) {
            final IBlockObject block = instanceSpace.blockObjectAt(x, y, z);
            if (!block.isImpeding())
                return null;

            bounds = block.bounds();
        } else if (impassible(flags))
            bounds = FULL_BOUNDS;
        else
            return null;

        final com.extollit.linalg.mutable.AxisAlignedBBox result = new com.extollit.linalg.mutable.AxisAlignedBBox(bounds);
        result.add(dx, dy, dz);
        return result;
    }

    private boolean applyPointOptions(HydrazinePathPoint current, HydrazinePathPoint... pointOptions) {
        boolean found = false;
        for (HydrazinePathPoint alternative : pointOptions) {
            if (impassible(alternative) || alternative.first() || HydrazinePathPoint.squareDelta(alternative, this.target) >= this.searchRangeSquared)
                continue;

            found = true;
            this.queue.appendTo(alternative, current, target);
        }
        return found;
    }

    private boolean impassible(HydrazinePathPoint alternative) {
        return alternative == null || impassible(alternative.passibility());
    }

    private HydrazinePathPoint cachedPointAt(int x, int y, int z)
    {
        final Vec3i coords = new Vec3i(x, y, z);
        return cachedPointAt(coords);
    }

    private HydrazinePathPoint cachedPointAt(Vec3i coords) {
        HydrazinePathPoint point = this.nodeMap.get(coords);

        if (point == null)
            this.nodeMap.put(coords, point = new HydrazinePathPoint(coords));

        return point;
    }

    private HydrazinePathPoint cachedPassiblePointNear(final int x0, final int y0, final int z0, final Vec3i origin) {
        final Vec3i coords0 = new Vec3i(x0, y0, z0);
        return cachedPassiblePointNear(coords0, origin);
    }

    private HydrazinePathPoint cachedPassiblePointNear(Vec3i coords, Vec3i origin) {
        HydrazinePathPoint point = this.nodeMap.get(coords);

        if (point == null) {
            point = passiblePointNear(coords, origin);
            if (point != null)
                this.nodeMap.put(coords, point);
        }

        if (point != null && origin != null && unreachableFromSource(origin, coords))
            return null;

        return point;
    }

    protected HydrazinePathPoint passiblePointNear(Vec3i coords0, Vec3i origin) {
        final HydrazinePathPoint point;
        final IOcclusionProvider op = this.occlusionProvider;
        final int
            x0 = coords0.x,
            y0 = coords0.y,
            z0 = coords0.z;

        final Vec3i d;

        if (origin != null)
            d = coords0.subOf(origin);
        else
            d = Vec3i.ZERO;

        final boolean hasOrigin = d != Vec3i.ZERO && !d.equals(Vec3i.ZERO);

        final boolean
            climbsLadders = this.capabilities.climber();

        Passibility passibility = Passibility.passible;

        int minY = Integer.MIN_VALUE;
        float minPartY = 0;

        for (int r = this.discreteSize / 2,
             x = x0 - r,
             xN = x0 + this.discreteSize - r;

             x < xN;

             ++x
            )
            for (int z = z0 - r,
                 zN = z0 + this.discreteSize - r;

                 z < zN;

                 ++z
            ) {
                int y = y0;

                float partY = topOffsetAt(
                    x - d.x,
                    y - d.y - 1,
                    z - d.z
                );

                byte flags = op.elementAt(x, y, z);
                if (impassible(flags)) {
                    final float partialDisparity = partY - topOffsetAt(flags, x, y++, z);
                    flags = op.elementAt(x, y, z);

                    if (partialDisparity < 0 || impassible(flags)) {
                        if (!hasOrigin)
                            return null;

                        if (d.x * d.x + d.z * d.z <= 1) {
                            y -= d.y + 1;

                            do
                                flags = op.elementAt(x - d.x, y++, z - d.z);
                            while (climbsLadders && Logic.climbable(flags));
                        }

                        if (impassible(flags = op.elementAt(x, --y, z)) && (impassible(flags = op.elementAt(x, ++y, z)) || partY < 0))
                            return null;
                    }
                }
                partY = topOffsetAt(x, y - 1, z);
                final int ys;
                passibility = verticalClearanceAt(this.tall, flags, passibility, d, x, ys = y, z, partY);

                boolean swimable = false;
                for (int j = 0; unstable(flags) && !(swimable = swimable(flags)) && j <= MAX_SURVIVE_FALL_DISTANCE; j++)
                    flags = op.elementAt(x, --y, z);

                if (swimable) {
                    final int cesaLimit = y + CESA_LIMIT;
                    final byte flags00 = flags;
                    byte flags0;
                    do {
                        flags0 = flags;
                        flags = op.elementAt(x, ++y, z);
                    } while (swimable(flags) && unstable(flags) && y < cesaLimit);
                    if (y >= cesaLimit) {
                        y -= CESA_LIMIT + 1;
                        flags = flags00;
                    } else {
                        y--;
                        flags = flags0;
                    }
                }

                partY = topOffsetAt(flags, x, y++, z);
                passibility = verticalClearanceAt(ys - y, op.elementAt(x, y, z), passibility, d, x, y, z, partY);

                if (y > minY) {
                    minY = y;
                    minPartY = partY;
                } else if (y == minY && partY > minPartY)
                    minPartY = partY;

                passibility = passibility.between(passibility(op.elementAt(x, y, z)));
                if (impassible(passibility))
                    return null;
            }

        if (hasOrigin && !impassible(passibility))
            passibility = originHeadClearance(passibility, origin, minY, minPartY);

        passibility = fallingSafety(passibility, y0, minY);

        if (impassible(passibility))
            return null;

        point = new HydrazinePathPoint(new Vec3i(x0, minY + round(minPartY), z0));
        point.passibility(passibility);

        return point;
    }

    protected final boolean unreachableFromSource(Vec3i current, Vec3i target) {
        final HydrazinePathPoint source = this.source;
        return source != null && current.equals(source.key) && this.unreachableFromSource.contains(target);
    }

    private Passibility fallingSafety(Passibility passibility, int y0, int minY) {
        final int dy = y0 - minY;
        if (dy > 1)
            passibility = passibility.between(
                dy > MAX_SAFE_FALL_DISTANCE ?
                    Passibility.dangerous :
                    Passibility.risky
            );
        return passibility;
    }

    private Passibility verticalClearanceAt(int max, byte flags, Passibility passibility, Vec3i d, int x, int y, int z, float partY) {
        final IOcclusionProvider op = this.occlusionProvider;
        byte clearanceFlags = flags;
        final int
            yMax = y + max,
            yN = Math.max(y, y - d.y) + this.tall;
        int yt = y;

        for (int yNa = yN + floor(partY);

             yt < yNa && yt < yMax;

             clearanceFlags = op.elementAt(x, ++yt, z)
        )
            passibility = passibility.between(clearance(clearanceFlags));

        if (yt < yN && yt < yMax && (insufficientHeadClearance(clearanceFlags, partY, x, yt, z)))
            passibility = passibility.between(clearance(clearanceFlags));

        return passibility;
    }

    private Passibility originHeadClearance(Passibility passibility, Vec3i origin, int minY, float minPartY) {
        final IOcclusionProvider op = this.occlusionProvider;
        final int
            yN = minY + this.tall,
            yNa = yN + floor(minPartY);

        for (int x = origin.x, xN = origin.x + this.discreteSize; x < xN; ++x)
            for (int z = origin.z, zN = origin.z + this.discreteSize; z < zN; ++z)
                for (int y = origin.y + this.tall; y < yNa; ++y)
                    passibility = passibility.between(clearance(op.elementAt(x, y, z)));

        if (yNa < yN)
            for (int x = origin.x, xN = origin.x + this.discreteSize; x < xN; ++x)
                for (int z = origin.z, zN = origin.z + this.discreteSize; z < zN; ++z) {
                    final byte flags = op.elementAt(x, yNa, z);
                    if (insufficientHeadClearance(flags, minPartY, x, yNa, z))
                        passibility = passibility.between(clearance(flags));
                }

        return passibility;
    }

    private boolean insufficientHeadClearance(byte flags, float partialY0, int x, int yN, int z) {
        return bottomOffsetAt(flags, x, yN, z) + partialY0 > 0;
    }

    private float topOffsetAt(int x, int y, int z) {
        return topOffsetAt(occlusionProvider.elementAt(x, y, z), x, y, z);
    }

    private float topOffsetAt(byte flags, int x, int y, int z) {
        if (Element.air.in(flags)
            || Logic.climbable(flags)
            || Element.earth.in(flags) && Logic.nothing.in(flags)
        )
            return 0;

        if (swimmingRequiredFor(flags))
            return -0.5f;

        final IBlockObject block = this.instanceSpace.blockObjectAt(x, y, z);
        if (!block.isImpeding())
            return 0;

        return (float)block.bounds().max.y - 1;
    }

    private float bottomOffsetAt(byte flags, int x, int y, int z) {
        if (Element.air.in(flags)
            || Logic.climbable(flags)
            || Element.earth.in(flags) && Logic.nothing.in(flags)
            || swimmingRequiredFor(flags)
        )
            return 0;

        final IBlockObject block = this.instanceSpace.blockObjectAt(x, y, z);
        if (!block.isImpeding())
            return 0;

        return (float) block.bounds().min.y;
    }

    private boolean impassible(Passibility passibility) {
        return passibility == Passibility.impassible
            || (this.capabilities.cautious() && passibility.worseThan(Passibility.passible));
    }

    private boolean swimable(byte flags) {
        return this.capabilities.swimmer() && swimmingRequiredFor(flags) && (Element.water.in(flags) || this.capabilities.fireResistant());
    }

    private boolean swimmingRequiredFor(byte flags) {
        return Element.water.in(flags) || (Element.fire.in(flags) && !Logic.fuzzy.in(flags));
    }

    private boolean unstable(byte flags) {
        return (!Element.earth.in(flags) || Logic.ladder.in(flags));
    }

    private boolean impassible(byte flags) {
        return (Element.earth.in(flags) && !(Logic.doorway.in(flags) && this.capabilities.opensDoors()) && !Logic.ladder.in(flags))
                || (Element.air.in(flags) && Logic.doorway.in(flags) && this.capabilities.avoidsDoorways());
    }

    private boolean fuzzyPassibility(int x, int y, int z) {
        return fuzzyPassibility(this.occlusionProvider.elementAt(x, y, z));
    }

    private boolean fuzzyPassibility(byte flags) {
        return impassible(flags) && (Logic.fuzzy.in(flags) || Logic.doorway.in(flags));
    }

    private Passibility clearance(byte flags) {
        if (Element.earth.in(flags))
            if (Logic.ladder.in(flags))
                return Passibility.passible;
            else if (Logic.fuzzy.in(flags))
                return Passibility.risky;
            else
                return Passibility.impassible;
        else if (Element.water.in(flags))
            return Passibility.risky;
        else if (Element.fire.in(flags))
            return Passibility.dangerous;
        else
            return Passibility.passible;
    }

    private Passibility passibility(byte flags) {
        final Element kind = Element.of(flags);
        switch (kind) {
            case earth:
                if (Logic.ladder.in(flags) || (Logic.doorway.in(flags) && this.capabilities.opensDoors()))
                    return Passibility.passible;
                else
                    return Passibility.impassible;

            case air:
                if (Logic.doorway.in(flags) && capabilities.avoidsDoorways())
                    return Passibility.impassible;
                else
                    return Passibility.passible;

            case water:
                if (this.capabilities.aquaphobic() || !this.capabilities.swimmer())
                    return Passibility.dangerous;
                else
                    return Passibility.risky;

            case fire:
                if (!this.capabilities.fireResistant())
                    return Passibility.dangerous;
                else
                    return Passibility.risky;
        }

        throw new IllegalArgumentException(MessageFormat.format("Unhandled element type ''{0}''", kind));
    }

    public IPathingEntity subject() {
        return this.subject;
    }

    void occlusionProvider(IOcclusionProvider occlusionProvider) {
        this.occlusionProvider = occlusionProvider;
    }

    protected IOcclusionProvider occlusionProviderFor(int cx0, int cz0, int cxN, int czN) {
        return AreaOcclusionProvider.fromInstanceSpace(this.instanceSpace, cx0, cz0, cxN, czN);
    }

    public boolean sameDestination(PathObject delegate, com.extollit.linalg.immutable.Vec3d target) {
        if (this.currentPath == null)
            return false;

        if (this.currentPath != delegate && !this.currentPath.sameAs(delegate))
            return false;

        final Vec3d dest = this.destinationPosition;
        if (dest == null)
            return false;

        return
            floor(target.x) == floor(dest.x) &&
            floor(target.y) == floor(dest.y) &&
            floor(target.z) == floor(dest.z);
    }

    private float pathTimeAge() {
        return this.subject.age() * this.capabilities.speed();
    }

    public Pair.Sealed<Passibility, Vec3i> passibilityNear(int tx, int ty, int tz) {
        updateSourcePosition();

        final int
                x = floor(sourcePosition.x),
                y = floor(sourcePosition.y),
                z = floor(sourcePosition.z);

        applySubject();
        updateFieldWindow(x, z, tx, tz, false);

        final HydrazinePathPoint point = cachedPassiblePointNear(tx, ty, tz, null);
        if (point == null)
            return Pair.Sealed.of(Passibility.impassible, null);

        return Pair.Sealed.of(point.passibility(), point.key);
    }
}
