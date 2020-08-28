package com.extollit.gaming.ai.path;

import com.extollit.gaming.ai.path.model.*;
import com.extollit.linalg.immutable.AxisAlignedBBox;
import com.extollit.linalg.immutable.Vec3i;
import com.extollit.linalg.mutable.Vec3d;
import com.extollit.num.FloatRange;
import com.extollit.tuple.Pair;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static com.extollit.gaming.ai.path.PassibilityHelpers.impedesMovement;
import static com.extollit.num.FastMath.ceil;
import static com.extollit.num.FastMath.floor;

public class HydrazinePathFinder {
    private static final AxisAlignedBBox FULL_BOUNDS = new AxisAlignedBBox(0, 0, 0, 1, 1, 1);

    private static double DOT_THRESHOLD = 0.6;
    private static FloatRange
            PROBATIONARY_TIME_LIMIT = new FloatRange(16, 24),
            PASSIBLE_POINT_TIME_LIMIT = new FloatRange(12, 36);

    private static byte FAILURE_COUNT_THRESHOLD = 3;

    private final SortedPointQueue queue = new SortedPointQueue();
    private final Set<Vec3i>
            unreachableFromSource = new HashSet<>(3),
            closests = new HashSet<>();
    private final IPathingEntity subject;
    private final IInstanceSpace instanceSpace;

    private com.extollit.linalg.mutable.Vec3d sourcePosition, destinationPosition;
    private IDynamicMovableObject destinationEntity;

    private INodeCalculator pathPointCalculator;
    private IPathProcessor pathProcessor;
    private NodeMap nodeMap;
    private PathObject currentPath;
    private IPathingEntity.Capabilities capabilities;
    private boolean flying, aqua, pathPointCalculatorChanged;
    private Node current, source, target, closest;
    private int initComputeIterations, periodicComputeIterations;
    private int failureCount;
    private float searchRangeSquared, passiblePointPathTimeLimit, nextGraphCacheReset, actualSize;
    private Random random = new Random();

    public static void configureFrom(IConfigModel configModel) {
        FAILURE_COUNT_THRESHOLD = configModel.failureCountThreshold();
        PROBATIONARY_TIME_LIMIT = configModel.probationaryTimeLimit();
        PASSIBLE_POINT_TIME_LIMIT = configModel.passiblePointTimeLimit();
        DOT_THRESHOLD = configModel.dotThreshold();

        GroundNodeCalculator.configureFrom(configModel);
    }

    public HydrazinePathFinder(IPathingEntity entity, IInstanceSpace instanceSpace) {
        this(entity, instanceSpace, AreaOcclusionProviderFactory.INSTANCE);
    }

    HydrazinePathFinder(IPathingEntity entity, IInstanceSpace instanceSpace, IOcclusionProviderFactory occlusionProviderFactory) {
        this.subject = entity;
        this.instanceSpace = instanceSpace;
        this.nodeMap = new NodeMap(instanceSpace, occlusionProviderFactory);

        applySubject();
        schedulingPriority(SchedulingPriority.low);

        this.passiblePointPathTimeLimit = PASSIBLE_POINT_TIME_LIMIT.next(this.random);
    }

    public void setRandomNumberGenerator(Random random) {
        this.random = random;
    }

    public void schedulingPriority(SchedulingPriority schedulingPriority) {
        schedulingPriority(schedulingPriority.initComputeIterations, schedulingPriority.periodicComputeIterations);
    }

    void schedulingPriority(final int initComputeIterations, final int periodicComputeIterations) {
        this.initComputeIterations = initComputeIterations;
        this.periodicComputeIterations = periodicComputeIterations;
    }

    public final Vec3i trackingDestination() {
        if (this.destinationEntity != null && this.destinationPosition != null) {
            final Node pointAtDestination = edgeAtDestination();
            if (impassible(pointAtDestination))
                return null;
            else
                return pointAtDestination.key;
        } else
            return null;
    }
    public final Vec3i currentTarget() { return this.target == null ? null : this.target.key; }

    public IPath trackPathTo(IDynamicMovableObject target) {
        this.destinationEntity = target;
        return initiatePathTo(target.coordinates());
    }

    public IPath initiatePathTo(com.extollit.linalg.immutable.Vec3d coordinates) {
        return initiatePathTo(coordinates.x, coordinates.y, coordinates.z);
    }

    public IPath initiatePathTo(double x, double y, double z) {
        applySubject();
        updateSourcePosition();

        final float rangeSquared = this.searchRangeSquared;
        final com.extollit.linalg.immutable.Vec3d sourcePos = new com.extollit.linalg.immutable.Vec3d(this.sourcePosition);
        if (sourcePos.subOf(x, y, z).mg2() > rangeSquared)
            return null;

        final boolean initiate = updateDestination(x, y, z) && this.queue.isEmpty();

        if (!graphTimeout() && (initiate || reachedTarget() || triageTimeout() || destinationDeviatedFromTarget()))
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

    public HydrazinePathFinder withGraphNodeFilter(IGraphNodeFilter filter) {
        this.nodeMap.filter(filter);
        return this;
    }
    public IGraphNodeFilter graphNodeFilter() {
        return this.nodeMap.filter();
    }

    public HydrazinePathFinder withPathProcessor(IPathProcessor trimmer) {
        this.pathProcessor = trimmer;
        return this;
    }
    public IPathProcessor pathProcessor() {
        return this.pathProcessor;
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

            final INode culprit = currentPath.at(currentPath.i);
            this.nodeMap.cullBranchAt(culprit.coordinates(), this.queue);

            this.passiblePointPathTimeLimit += PASSIBLE_POINT_TIME_LIMIT.next(this.random);
        }

        return status;
    }

    private boolean graphTimeout() {
        if (this.failureCount >= FAILURE_COUNT_THRESHOLD
            && pathTimeAge() > this.nextGraphCacheReset
            || this.pathPointCalculatorChanged)
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

        final Node source = this.current = this.source = pointAtSource();
        source.length(0);
        source.orphan();

        refinePassibility(source.key);

        setTargetFor(source);

        this.closests.clear();
        this.nodeMap.reset(this.queue);
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

    private INodeCalculator createPassibilityCalculator(IPathingEntity.Capabilities capabilities) {
        final INodeCalculator calculator;
        final boolean
                flyer = capabilities.flyer(),
                swimmer = capabilities.swimmer(),
                gilled = capabilities.gilled();

        if (flyer || (swimmer && gilled))
            calculator = new FluidicNodeCalculator(this.instanceSpace);
        else
            calculator = new GroundNodeCalculator(this.instanceSpace);
        return calculator;
    }

    private void applySubject() {
        final IPathingEntity.Capabilities capabilities = this.capabilities = this.subject.capabilities();
        final boolean
            flying = capabilities.flyer(),
            aqua = capabilities.swimmer() && capabilities.gilled();

        final boolean initPathPointCalculator = this.pathPointCalculator == null;
        if (initPathPointCalculator || flying != this.flying || aqua != this.aqua) {
            this.pathPointCalculatorChanged = !initPathPointCalculator;
            this.nodeMap.calculator(this.pathPointCalculator = createPassibilityCalculator(capabilities));
            this.flying = flying;
            this.aqua = aqua;
        }

        this.actualSize = subject.width();
        this.pathPointCalculator.applySubject(this.subject);
        final float pathSearchRange = this.subject.searchRange();
        this.searchRangeSquared = pathSearchRange*pathSearchRange;
    }

    private void setTargetFor(Node source) {
        final Vec3d
                destinationPosition = this.destinationPosition;

        int distance = Node.MAX_PATH_DISTANCE;
        this.target = edgeAtDestination();
        while (distance > 0 && !source.target(this.target.key)) {
            final Vec3d
                v = new Vec3d(destinationPosition),
                init = new Vec3d(source.key);

            distance--;

            v.sub(init);
            v.normalize();
            v.mul(distance);
            v.add(init);
            this.target = edgeAtTarget(v.x, v.y, v.z);
        }

        if (distance == 0)
            source.target((this.target = this.source).key);
    }

    private void resetGraph() {
        this.nodeMap.clear();
        resetTriage();
        this.failureCount = 0;
        this.nextGraphCacheReset = 0;
        this.pathPointCalculatorChanged = false;
    }

    private void updateFieldWindow(PathObject path) {
        INode node = path.last();
        if (node == null)
            return;

        Vec3i pp = node.coordinates();

        final com.extollit.linalg.mutable.Vec3i
                min = new com.extollit.linalg.mutable.Vec3i(pp.x, pp.y, pp.z),
                max = new com.extollit.linalg.mutable.Vec3i(pp.x, pp.y, pp.z);

        if (!path.done())
            for (int c = path.i; c < path.length(); ++c) {
                node = path.at(c);
                pp = node.coordinates();
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

        this.nodeMap.updateFieldWindow(x0, z0, xN, zN, cull);
    }

    private Node edgeAtTarget(final double x, final double y, final double z) {
        final int
                nx = floor(x),
                ny = floor(y),
                nz = floor(z);

        Node node = this.nodeMap.cachedPointAt(nx, ny, nz);
        if (node.passibility() == Passibility.impassible && !this.capabilities.cautious())
            node.passibility(Passibility.dangerous);
        return node;
    }

    private Node edgeAtDestination() {
        final Vec3d destinationPosition = this.destinationPosition;
        if (destinationPosition == null)
            return null;

        return edgeAtTarget(destinationPosition.x, destinationPosition.y, destinationPosition.z);
    }

    private Node pointAtSource() {
        final Vec3d sourcePosition = this.sourcePosition;
        final int
                x = floor(sourcePosition.x),
                y = floor(sourcePosition.y),
                z = floor(sourcePosition.z);

        Node candidate = cachedPassiblePointNear(x, y, z);
        if (impassible(candidate))
            candidate.passibility(this.capabilities.cautious() ? Passibility.passible : Passibility.risky);
        return candidate;
    }

    private boolean updateDestination(double x, double y, double z) {
        if (this.destinationPosition != null) {
            final Vec3d destinationPosition = this.destinationPosition;
            final boolean modified = differs(x, y, z, destinationPosition);
            destinationPosition.x = x;
            destinationPosition.y = y;
            destinationPosition.z = z;
            return modified;
        } else {
            this.destinationPosition = new Vec3d(x, y, z);
            return true;
        }
    }

    private boolean updateDestination(com.extollit.linalg.immutable.Vec3d coordinates) {
        if (this.destinationPosition != null) {
            final boolean modified = differs(coordinates, this.destinationPosition);
            this.destinationPosition.set(coordinates);
            return modified;
        } else {
            this.destinationPosition = new Vec3d(coordinates);
            return true;
        }
    }

    private static boolean differs(com.extollit.linalg.immutable.Vec3d a, Vec3d b) {
        return differs(a.x, a.y, a.z, b);
    }

    private static boolean differs(final double x, final double y, final double z, Vec3d other) {
        return
            floor(other.x) != floor(x) ||
            floor(other.y) != floor(y) ||
            floor(other.z) != floor(z);
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
        this.closests.clear();
        this.queue.clear();
        this.nodeMap.reset();
        this.unreachableFromSource.clear();
        this.target =
        this.source =
        this.closest = null;
        this.current = null;
        this.sourcePosition =
        this.destinationPosition = null;
        this.destinationEntity = null;
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

            if (this.nodeMap.needsOcclusionProvider())
                updateFieldWindow(newPath);

            if (newPath.done())
                newPath = null;

            return this.currentPath = newPath;
        } else {
            if (!PathObject.active(this.currentPath))
                this.currentPath = null;

            return this.currentPath;
        }
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

        while (!queue.isEmpty() && iterations-- > 0) {
            final Node source = this.current;
            if (!queue.nextContains(source)) {
                if (source != null)
                    this.queue.trimFrom(source);
                continue;
            }

            final Node
                current = queue.dequeue(),
                closest = this.closest;

            if ((
                    closest == null
                    || closest.orphaned()
                    || Node.squareDelta(current, this.target) < Node.squareDelta(closest, this.target)
                ) && !this.closests.contains(current.key)) {
                this.closest = current;
                this.closests.add(current.key);
            }

            if (current == target) {
                nextPath = createPath(current);
                if (PathObject.active(nextPath)) {
                    nextPath.setRandomNumberGenerator(this.random);
                    this.queue.clear();
                    break;
                }

                resetTriage();
            } else
                processNode(current);
        }

        if (nextPath == null && this.closest != null && !queue.isEmpty())
            nextPath = createPath(this.closest);

        return updatePath(nextPath);
    }

    private PathObject createPath(Node head) {
        final IPathingEntity.Capabilities capabilities = this.capabilities;
        final PathObject path = PathObject.fromHead(capabilities.speed(), head);
        if (this.pathProcessor != null && path != null)
            this.pathProcessor.processPath(path);
        return path;
    }

    private void processNode(Node current) {
        current.visited(true);

        final Vec3i coords = current.key;
        final Node
                west = cachedPassiblePointNear(coords.x - 1, coords.y, coords.z, coords),
                east = cachedPassiblePointNear(coords.x + 1, coords.y, coords.z, coords),
                north = cachedPassiblePointNear(coords.x, coords.y, coords.z - 1, coords),
                south = cachedPassiblePointNear(coords.x, coords.y, coords.z + 1, coords);
        final Node
                up, down;

        final boolean omnidirectional = this.pathPointCalculator.omnidirectional();
        if (omnidirectional) {
            up = cachedPassiblePointNear(coords.x, coords.y + 1, coords.z, coords);
            down = cachedPassiblePointNear(coords.x, coords.y - 1, coords.z, coords);
        } else
            up = down = null;

        final boolean found = applyPointOptions(current, up, down, west, east, north, south);

        if (!found) {
            final com.extollit.linalg.mutable.AxisAlignedBBox
                    southBounds = blockBounds(coords, 0, 0, +1),
                    northBounds = blockBounds(coords, 0, 0, -1),
                    eastBounds = blockBounds(coords, +1, 0, 0),
                    westBounds = blockBounds(coords, -1, 0, 0);

            final float actualSizeSquared = this.actualSize * this.actualSize;

            final Node[] pointOptions;

            if (omnidirectional) {
                final com.extollit.linalg.mutable.AxisAlignedBBox
                        upBounds = blockBounds(coords, 0, +1, 0),
                        downBounds = blockBounds(coords, 0, -1, 0);

                pointOptions = new Node[] {
                        northBounds == null || upBounds == null || northBounds.mg2(upBounds) >= actualSizeSquared ? cachedPassiblePointNear(coords.x, coords.y + 1, coords.z - 1, coords) : null,
                        eastBounds == null  || upBounds == null || eastBounds.mg2(upBounds) >= actualSizeSquared ? cachedPassiblePointNear(coords.x + 1, coords.y + 1, coords.z, coords) : null,
                        southBounds == null || upBounds == null || southBounds.mg2(upBounds) >= actualSizeSquared ? cachedPassiblePointNear(coords.x, coords.y + 1, coords.z + 1, coords) : null,
                        westBounds == null  || upBounds == null || westBounds.mg2(upBounds) >= actualSizeSquared ? cachedPassiblePointNear(coords.x - 1, coords.y + 1, coords.z, coords) : null,

                        northBounds == null || downBounds == null || northBounds.mg2(downBounds) >= actualSizeSquared ? cachedPassiblePointNear(coords.x, coords.y - 1, coords.z - 1, coords) : null,
                        eastBounds == null  || downBounds == null || eastBounds.mg2(downBounds) >= actualSizeSquared ? cachedPassiblePointNear(coords.x + 1, coords.y - 1, coords.z, coords) : null,
                        southBounds == null || downBounds == null || southBounds.mg2(downBounds) >= actualSizeSquared ? cachedPassiblePointNear(coords.x, coords.y - 1, coords.z + 1, coords) : null,
                        westBounds == null  || downBounds == null || westBounds.mg2(downBounds) >= actualSizeSquared ? cachedPassiblePointNear(coords.x - 1, coords.y - 1, coords.z, coords) : null
                };

                applyPointOptions(current, pointOptions);
            } else
                pointOptions = new Node[4];

            pointOptions[0] = westBounds == null || northBounds == null || westBounds.mg2(northBounds) >= actualSizeSquared ? cachedPassiblePointNear(coords.x - 1, coords.y, coords.z - 1, coords) : null;
            pointOptions[1] = eastBounds == null || southBounds == null || eastBounds.mg2(southBounds) >= actualSizeSquared ? cachedPassiblePointNear(coords.x + 1, coords.y, coords.z + 1, coords) : null;
            pointOptions[2] = eastBounds == null || northBounds == null || eastBounds.mg2(northBounds) >= actualSizeSquared ? cachedPassiblePointNear(coords.x + 1, coords.y, coords.z - 1, coords) : null;
            pointOptions[3] = westBounds == null || southBounds == null || westBounds.mg2(southBounds) >= actualSizeSquared ? cachedPassiblePointNear(coords.x - 1, coords.y, coords.z + 1, coords) : null;

            applyPointOptions(current, pointOptions);
        }
    }

    private com.extollit.linalg.mutable.AxisAlignedBBox blockBounds(Vec3i coords, int dx, int dy, int dz) {
        final int
            x = coords.x + dx,
            y = coords.y + dy,
            z = coords.z + dz;

        final AxisAlignedBBox bounds;
        final byte flags = this.nodeMap.flagsAt(x, y, z);
        if (fuzzyPassibility(flags)) {
            final IBlockObject block = instanceSpace.blockObjectAt(x, y, z);
            if (!block.isImpeding())
                return null;

            bounds = block.bounds();
        } else if (impedesMovement(flags, this.capabilities))
            bounds = FULL_BOUNDS;
        else
            return null;

        final com.extollit.linalg.mutable.AxisAlignedBBox result = new com.extollit.linalg.mutable.AxisAlignedBBox(bounds);
        result.add(dx, dy, dz);
        return result;
    }

    private boolean applyPointOptions(Node current, Node... pointOptions) {
        boolean found = false;
        for (Node alternative : pointOptions) {
            if (impassible(alternative) || alternative.visited() || Node.squareDelta(alternative, this.target) >= this.searchRangeSquared)
                continue;

            found = true;
            this.queue.appendTo(alternative, current, this.target.key);
        }
        return found;
    }

    private boolean impassible(Node alternative) {
        return alternative == null || alternative.passibility().impassible(this.capabilities);
    }

    private Node cachedPassiblePointNear(final int x0, final int y0, final int z0) {
        return cachedPassiblePointNear(x0, y0, z0, null);
    }

    private Node cachedPassiblePointNear(final int x0, final int y0, final int z0, final Vec3i origin) {
        final Vec3i coords0 = new Vec3i(x0, y0, z0);
        final Node result = this.nodeMap.cachedPassiblePointNear(coords0, origin);
        if (Node.passible(result) && origin != null && unreachableFromSource(origin, coords0))
            return null;

        return result;
    }

    private boolean fuzzyPassibility(int x, int y, int z) {
        return fuzzyPassibility(this.nodeMap.flagsAt(x, y, z));
    }

    private boolean fuzzyPassibility(byte flags) {
        return impedesMovement(flags, this.capabilities) && (Logic.fuzzy.in(flags) || Logic.doorway.in(flags));
    }

    protected final boolean unreachableFromSource(Vec3i current, Vec3i target) {
        final Vec3i sourcePoint = this.source.key;
        return sourcePoint != null && current.equals(sourcePoint) && this.unreachableFromSource.contains(target);
    }

    public IPathingEntity subject() {
        return this.subject;
    }

    public boolean sameDestination(IPath delegate, com.extollit.linalg.immutable.Vec3d target) {
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

        final Node point = this.nodeMap.cachedPassiblePointNear(tx, ty, tz);
        return Pair.Sealed.of(point.passibility(), point.key);
    }
}
