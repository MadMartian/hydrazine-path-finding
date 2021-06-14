package com.extollit.gaming.ai.path;

import com.extollit.gaming.ai.path.model.*;
import com.extollit.gaming.ai.path.persistence.*;
import com.extollit.linalg.immutable.AxisAlignedBBox;
import com.extollit.linalg.immutable.Vec3i;
import com.extollit.linalg.mutable.Vec3d;
import com.extollit.num.FloatRange;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static com.extollit.gaming.ai.path.PassibilityHelpers.impedesMovement;
import static java.lang.Math.*;

/**
 * This is the primary path-finding object and root instance for the library.  There is precisely one instance of this
 * class for each and every entity that requires path-finding.  As such it is a good idea to maintain an instance
 * of this class in a member field of the associated pathing entity.
 *
 * This algorithm is iterative, callers call an update method each tick to progressively compute a path as the entity
 * traverses along it ultimately distributing computation across time.
 *
 * To use this class, first initiate path-finding using one of the initiation methods, then call {@link #updatePathFor(IPathingEntity)}
 * each tick to iterate on the path until it is completed.  To abort path-finding call {@link #reset()}
 */
public class HydrazinePathFinder implements IVersionedReadable, IVersionedWriteable {
    private static final AxisAlignedBBox FULL_BOUNDS = new AxisAlignedBBox(0, 0, 0, 1, 1, 1);

    private static double DOT_THRESHOLD = 0.6;
    private static FloatRange
            PROBATIONARY_TIME_LIMIT = new FloatRange(36, 64),
            PASSIBLE_POINT_TIME_LIMIT = new FloatRange(24, 48);

    private static byte FAULT_COUNT_THRESHOLD = 3;
    private static int FAULT_LIMIT = 23;

    final SortedPointQueue queue = new SortedPointQueue();
    final NodeMap nodeMap;

    private final Set<Vec3i> unreachableFromSource = new HashSet<>(3);
    private final IPathingEntity subject;
    private final IInstanceSpace instanceSpace;

    private com.extollit.linalg.mutable.Vec3d sourcePosition, destinationPosition;
    private com.extollit.linalg.immutable.Vec3d targetPosition;
    private IDynamicMovableObject destinationEntity;

    private INodeCalculator pathPointCalculator;
    private IPathProcessor pathProcessor;
    private IPath currentPath;
    private IPathingEntity.Capabilities capabilities;
    private boolean flying, aqua, pathPointCalculatorChanged, trimmedToCurrent, bound;
    private PathOptions.TargetingStrategy targetingStrategy;
    private Node current, source, target, closest;
    private int initComputeIterations, periodicComputeIterations;
    private int faultCount, nextGraphResetFailureCount;
    private float searchRangeSquared, passiblePointPathTimeLimit, nextGraphCacheReset, actualSize;
    private Random random = new Random();

    /**
     * Configures the path-finding library.  All instances of this class derive configuration from here
     *
     * @param configModel source of the configuration to apply to all instances of this class
     */
    public static void configureFrom(IConfigModel configModel) {
        FAULT_COUNT_THRESHOLD = configModel.faultCountThreshold();
        FAULT_LIMIT = configModel.faultLimit();
        PROBATIONARY_TIME_LIMIT = configModel.probationaryTimeLimit();
        PASSIBLE_POINT_TIME_LIMIT = configModel.passiblePointTimeLimit();
        DOT_THRESHOLD = configModel.dotThreshold();

        GroundNodeCalculator.configureFrom(configModel);
    }

    /**
     * Create a new instance of the path-finder for a given entity and world
     *
     * @param entity the entity that uses this object for path-finding operations
     * @param instanceSpace the instance space that the entity is contained within and should path-find in
     */
    public HydrazinePathFinder(IPathingEntity entity, IInstanceSpace instanceSpace) {
        this(entity, instanceSpace, AreaOcclusionProviderFactory.INSTANCE);
    }

    HydrazinePathFinder(IPathingEntity entity, IInstanceSpace instanceSpace, IOcclusionProviderFactory occlusionProviderFactory) {
        this.subject = entity;
        this.instanceSpace = instanceSpace;
        this.nodeMap = new NodeMap(instanceSpace, occlusionProviderFactory);

        applySubject();
        schedulingPriority(SchedulingPriority.medium);

        resetFaultTimings();
    }

    /**
     * Apply a random number generator to this object, it is used for various fuzzy-logic operations during path-finding
     *
     * @param random a custom random number generator instance to apply to this path-finder
     */
    public void setRandomNumberGenerator(Random random) {
        this.random = random;
    }

    /**
     * Applies a scheduling priority to this path-finder (and associated entity)
     *
     * @param schedulingPriority priority to use for path-finding with this object's bound entity
     */
    public void schedulingPriority(SchedulingPriority schedulingPriority) {
        schedulingPriority(schedulingPriority.initComputeIterations, schedulingPriority.periodicComputeIterations);
    }

    void schedulingPriority(final int initComputeIterations, final int periodicComputeIterations) {
        this.initComputeIterations = initComputeIterations;
        this.periodicComputeIterations = periodicComputeIterations;
    }

    /**
     * If the last operation called on this path-finder was to track path-finding to some other entity then this will
     * provide the current destination the pathing entity is trying to reach, which will be within close proximity of the
     * tracked entity.
     *
     * @return an approximate (rounded) position near the destination entity being tracked, null if this is not tracking an entity destination
     */
    public final Vec3i trackingDestination() {
        if (this.destinationEntity != null && this.destinationPosition != null) {
            final Node pointAtDestination = edgeAtDestination();
            if (pointAtDestination == null)
                return null;
            else
                return pointAtDestination.key;
        } else
            return null;
    }

    /**
     * Current immediate path-finding target of the pathing entity.  This is not necessarily the final destination of
     * the path, it can also be an intermediary step toward the final destination as well.
     *
     * @return the current target destination, can be null if there is no target available.
     */
    public final Vec3i currentTarget() { return this.target == null ? null : this.target.key; }

    /**
     * Begin path-finding to a destination entity and update the path as necessary as the destination entity changes
     * it's location.  This state is retained until a call to one of the other path-finding initiation methods
     *
     * @param target destination / target entity to track and path-find to
     * @return the best path available toward the destination, the complete path to the destination, or null if a path
     *          cannot be computed at all from the current location
     * @see #updatePathFor(IPathingEntity)
     */
    public IPath trackPathTo(IDynamicMovableObject target) {
        this.destinationEntity = target;
        return initiatePathTo(target.coordinates(), PathOptions.BEST_EFFORT);
    }

    /**
     * Completely computes a path to the specified location.  This is the traditional A* search algorithm, which
     * trades-off performance for accuracy.
     *
     * NOTE: This method can be significantly more expensive than the others since it does not return until all search
     * options have been exhausted.
     *
     * @param coordinates the target destination to path-find to
     * @return the complete path to the destination, or null if the destination is unreachable from the current location
     */
    public IPath computePathTo(com.extollit.linalg.immutable.Vec3d coordinates) {
        return computePathTo(coordinates.x, coordinates.y, coordinates.z);
    }

    /**
     * Completely computes a path to the specified location.  This is the traditional A* search algorithm, which
     * trades-off performance for accuracy.
     *
     * NOTE: This method can be significantly more expensive than the others since it does not return until all search
     * options have been exhausted.
     *
     * @param x x-coordinate of the destination
     * @param y y-coordinate of the destination
     * @param z z-coordinate of the destination
     * @return the complete path to the destination, or null if the destination is unreachable from the current location
     */
    public IPath computePathTo(double x, double y, double z) {
        this.destinationEntity = null;
        this.targetingStrategy = PathOptions.TargetingStrategy.none;

        initializeOperation();
        if (tooFarTo(x, y, z))
            return null;

        updateDestination(x, y, z);

        if (!graphTimeout())
            resetTriage();

        return triage(Integer.MAX_VALUE);
    }

    /**
     * Starts path-finding to the specified destination using the best-effort algorithm.
     *
     * After an initial call to this method, the caller should make subsequent calls to
     * {@link #updatePathFor(IPathingEntity)} until path-finding is completed or exhausted.
     *
     * @param coordinates the coordinates to start path-finding toward
     * @return the best path available toward the destination, the complete path to the destination, or null if a path
     *          cannot be computed at all from the current location
     * @see #initiatePathTo(double, double, double, PathOptions)
     */
    public IPath initiatePathTo(com.extollit.linalg.immutable.Vec3d coordinates) {
        return initiatePathTo(coordinates.x, coordinates.y, coordinates.z);
    }

    /**
     * Starts path-finding to the specified destination using either best-effort or not.
     * Best-effort means that the algorithm will try it's best to get as close as possible to the target
     * destination even if it is unreachable.
     *
     * After an initial call to this method, the caller should make subsequent calls to
     * {@link #updatePathFor(IPathingEntity)} until path-finding is completed or exhausted.
     *
     * @param coordinates the coordinates to start path-finding toward
     * @param pathOptions Options for setting-up the path-finding approach (independent of pathing entity capabilities)
     * @return the best path available toward the destination, the complete path to the destination, or null if the
     *          destination was unreachable with the given path options
     */
    public IPath initiatePathTo(com.extollit.linalg.immutable.Vec3d coordinates, PathOptions pathOptions) {
        return initiatePathTo(coordinates.x, coordinates.y, coordinates.z, pathOptions);
    }

    /**
     * Starts path-finding to the specified destination using the best-effort algorithm.
     *
     * After an initial call to this method, the caller should make subsequent calls to
     * {@link #updatePathFor(IPathingEntity)} until path-finding is completed or exhausted.
     *
     * @param x x-coordinate of the destination
     * @param y y-coordinate of the destination
     * @param z z-coordinate of the destination
     * @return the best path available toward the destination, the complete path to the destination, or null if a path
     *          cannot be computed at all from the current location
     * @see #initiatePathTo(double, double, double, PathOptions)
     */
    public IPath initiatePathTo(double x, double y, double z) {
        return initiatePathTo(x, y, z, PathOptions.BEST_EFFORT);
    }

    /**
     * Starts path-finding to the specified destination using either best-effort or not.
     * Best-effort means that the algorithm will try it's best to get as close as possible to the target
     * destination even if it is unreachable.
     *
     * After an initial call to this method, the caller should make subsequent calls to
     * {@link #updatePathFor(IPathingEntity)} until path-finding is completed or exhausted.
     *
     * @param x x-coordinate of the destination
     * @param y y-coordinate of the destination
     * @param z z-coordinate of the destination
     * @param pathOptions Options for setting-up the path-finding approach (independent of pathing entity capabilities)
     * @return the best path available toward the destination, the complete path to the destination, or null if the
     *          destination was unreachable with the given path options
     */
    public IPath initiatePathTo(double x, double y, double z, PathOptions pathOptions) {
        this.targetingStrategy = pathOptions.targetingStrategy();

        initializeOperation();
        if (this.targetingStrategy == PathOptions.TargetingStrategy.none && tooFarTo(x, y, z))
            return null;

        final boolean initiate = updateDestination(x, y, z) && this.queue.isEmpty();

        if (!graphTimeout() && (initiate || reachedTarget() || triageTimeout() || deviationToTargetUnacceptable(this.subject)))
            resetTriage();

        return triage(this.initComputeIterations);
    }

    private boolean tooFarTo(double x, double y, double z) {
        final float rangeSquared = this.searchRangeSquared;
        final com.extollit.linalg.immutable.Vec3d sourcePos = new com.extollit.linalg.immutable.Vec3d(this.sourcePosition);
        return sourcePos.subOf(x, y, z).mg2() > rangeSquared;
    }

    private boolean tooFarTo(Vec3i target) {
        return tooFarTo(target.x, target.y, target.z);
    }

    private void initializeOperation() {
        applySubject();
        updateSourcePosition();
        resetFaultTimings();
    }

    /**
     * After a path-finding operation has been initiated by one of the initiation methods, sub-sequent calls to this
     * method are used to refine path-finding and continue path-finding to the destination requested when the initiation
     * method was called.  This method also drives the entity along its path.
     *
     * NOTE: If the pathing entity has already reached the destination and no further path-finding is necessary then the
     * returned path will be a completed path object (not null).
     *
     * @param pathingEntity the pathing entity that will receive movement commands along the path
     * @return the next and updated / refined path or null if the destination is unreachable.
     */
    public IPath updatePathFor(IPathingEntity pathingEntity) {
        final IPath path = update(pathingEntity);
        if (path == null)
            return null;

        if (!path.done()) {
            path.update(pathingEntity);
            if (!path.done())
                return path;
        }

        final INode last = path.last();
        if (last != null) {
            final Vec3d dd = new Vec3d(this.destinationPosition);
            dd.sub(last.coordinates());
            if (dd.mg2() < 1)
                return path;
            else if (last == edgeAtDestination())
                return null;
            else
                return new IncompletePath(last);
        } else
            return null;
    }

    /**
     * Optionally apply a graph node filter to this object which will be applied to all path-points computed by this path-finder.
     * This allows a caller to modify the passibility of points as they are computed.  For example, vampires are
     * vulnerable to sunlight, so a filter used here could mark all sunlit points as {@link Passibility#dangerous}.
     *
     * @param filter a caller-supplied callback for altering node passibility
     * @return this
     */
    public HydrazinePathFinder withGraphNodeFilter(IGraphNodeFilter filter) {
        this.nodeMap.filter(filter);
        return this;
    }

    /**
     * Retrieve the current graph node filter (if one was set)
     *
     * @return current graph node filter, null if not set
     */
    public IGraphNodeFilter graphNodeFilter() {
        return this.nodeMap.filter();
    }

    /**
     * Apply a path processor to this object which will be applied to all computed paths by this path-finder.
     * This allows a caller to modify computed paths before they are returned to the caller, typically this would
     * involve trimming a path shorter if desired.
     *
     * @param trimmer a callback used to modify paths computed by this engine
     * @return this
     */
    public HydrazinePathFinder withPathProcessor(IPathProcessor trimmer) {
        this.pathProcessor = trimmer;
        return this;
    }

    /**
     * Retrieve the current path processor (if one was set)
     *
     * @return current path processor, null if not set
     */
    public IPathProcessor pathProcessor() {
        return this.pathProcessor;
    }

    protected IPath update(IPathingEntity pathingEntity) {
        if (this.destinationEntity != null)
            updateDestination(this.destinationEntity.coordinates());

        if (this.destinationPosition == null)
            return this.currentPath;

        updateSourcePosition();
        graphTimeout();

        if (this.faultCount >= FAULT_LIMIT) {
            resetTriage();
            return null;
        } else if (reachedTarget()) {
            resetTriage();
            return completedPath();
        }

        if (triageTimeout() || deviationToTargetUnacceptable(pathingEntity))
            resetTriage();

        return triage(this.periodicComputeIterations);
    }

    private IncompletePath completedPath() {
        return new IncompletePath(this.current, true);
    }

    private boolean deviationToTargetUnacceptable(IPathingEntity pathingEntity) {
        final Vec3d destinationPosition = this.destinationPosition;
        final com.extollit.linalg.mutable.Vec3d
                dt = new com.extollit.linalg.mutable.Vec3d(this.targetPosition),
                dd = new com.extollit.linalg.mutable.Vec3d(destinationPosition);

        final Vec3i source = this.source.key;
        dt.sub(source);
        dd.sub(source);

        if (dt.mg2() > dd.mg2())
            return true;

        dt.normalize();
        dd.normalize();

        if (dt.dot(dd) < DOT_THRESHOLD)
            return true;

        if (this.bound && PathObject.active(this.currentPath))
        {
            dt.set(this.currentPath.current().coordinates());
            dd.x = destinationPosition.x;
            dd.y = destinationPosition.y;
            dd.z = destinationPosition.z;

            final com.extollit.linalg.immutable.Vec3d pos = pathingEntity.coordinates();
            dt.sub(pos);
            dd.sub(pos);

            return dt.dot(dd) < 0;
        }

        return false;
    }

    private boolean triageTimeout() {
        final IPath currentPath = this.currentPath;
        final boolean status =
                PathObject.active(currentPath) &&
                currentPath.length() > 0 &&
                currentPath.stagnantFor(this.subject) > this.passiblePointPathTimeLimit;

        if (status) {
            if (++this.faultCount == 1)
                this.nextGraphCacheReset = pathTimeAge() + PROBATIONARY_TIME_LIMIT.next(this.random);

            final INode culprit = currentPath.current();
            this.nodeMap.cullBranchAt(culprit.coordinates(), this.queue);

            this.passiblePointPathTimeLimit += PASSIBLE_POINT_TIME_LIMIT.next(this.random);
        }

        return status;
    }

    private boolean graphTimeout() {
        final int failureCount = this.faultCount;
        if (failureCount >= this.nextGraphResetFailureCount
            && pathTimeAge() > this.nextGraphCacheReset) {
            this.nextGraphResetFailureCount = failureCount + FAULT_COUNT_THRESHOLD;
            resetGraph();
            return true;
        }

        if (this.pathPointCalculatorChanged)
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
                (int)floor(sourcePosition.x),
                (int)floor(sourcePosition.z),

                (int)floor(destinationPosition.x),
                (int)floor(destinationPosition.z),

                true
        );

        applySubject();

        final Node source = this.current = this.source = pointAtSource();
        source.length(0);
        source.isolate();
        this.trimmedToCurrent = true;

        refinePassibility(source.key);

        setTargetFor(source);

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
                flyer = capabilities.avian(),
                swimmer = capabilities.swimmer(),
                gilled = capabilities.aquatic();

        if (flyer || (swimmer && gilled))
            calculator = new FluidicNodeCalculator(this.instanceSpace);
        else
            calculator = new GroundNodeCalculator(this.instanceSpace);
        return calculator;
    }

    private void applySubject() {
        final IPathingEntity subject = this.subject;
        final IPathingEntity.Capabilities capabilities = this.capabilities = subject.capabilities();
        final boolean
            flying = capabilities.avian(),
            aqua = capabilities.swimmer() && capabilities.aquatic();

        final boolean initPathPointCalculator = this.pathPointCalculator == null;
        if (initPathPointCalculator || flying != this.flying || aqua != this.aqua) {
            this.pathPointCalculatorChanged = !initPathPointCalculator;
            this.nodeMap.calculator(this.pathPointCalculator = createPassibilityCalculator(capabilities));
            this.flying = flying;
            this.aqua = aqua;
        }

        this.actualSize = this.subject.width();
        this.pathPointCalculator.applySubject(subject);
        final float pathSearchRange = subject.searchRange();
        this.searchRangeSquared = pathSearchRange*pathSearchRange;
        this.bound = subject.bound();
    }

    private boolean setTargetFor(Node source) {
        final Vec3d
                destinationPosition = this.destinationPosition;

        this.targetPosition = destinationPosition != null ? new com.extollit.linalg.immutable.Vec3d(destinationPosition) : null;

        if (null == (this.target = edgeAtDestination()))
            return false;
        else if (this.targetingStrategy == PathOptions.TargetingStrategy.bestEffort) {
            int distance = Node.MAX_PATH_DISTANCE;
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
                return source.target((this.target = this.source).key);
            else
                return true;
        } else if (source.target(this.target.key))
            return true;

        this.target = null;
        return false;
    }

    private void resetGraph() {
        this.nodeMap.clear();
        resetTriage();
        this.nextGraphCacheReset = 0;
        this.pathPointCalculatorChanged = false;
    }

    private void updateFieldWindow(IPath path) {
        INode node = path.last();
        if (node == null)
            return;

        Vec3i pp = node.coordinates();

        final com.extollit.linalg.mutable.Vec3i
                min = new com.extollit.linalg.mutable.Vec3i(pp.x, pp.y, pp.z),
                max = new com.extollit.linalg.mutable.Vec3i(pp.x, pp.y, pp.z);

        if (!path.done())
            for (int c = path.cursor(); c < path.length(); ++c) {
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
        int entitySize = (int)ceil(
                destinationEntity != null ?
                        max(entityWidth, destinationEntity.width()) :
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
                nx = (int)floor(x),
                ny = (int)floor(y),
                nz = (int)floor(z);

        final Node node;

        switch (this.targetingStrategy) {
            case none:
                node = this.nodeMap.cachedPointAt(nx, ny, nz);
                if (impassible(node))
                    return null;

                final Vec3d dl = new Vec3d(node.coordinates());
                dl.sub(destinationPosition);
                if (dl.mg2() > 1)
                    return null;

                break;

            case bestEffort:
                node = this.nodeMap.cachedPointAt(nx, ny, nz);
                if (node.passibility() == Passibility.impassible)
                    node.passibility(Passibility.dangerous);

                break;

            case gravitySnap:
                node = this.nodeMap.cachedPassiblePointNear(nx, ny, nz);
                if (impassible(node) || tooFarTo(node.key))
                    return null;

                break;

            default:
                throw new UnsupportedOperationException("Unrecognized targeting strategy: " + this.targetingStrategy);
        }

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
                x = (int)floor(sourcePosition.x),
                y = (int)floor(sourcePosition.y),
                z = (int)floor(sourcePosition.z);

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
            (int)floor(other.x) != (int)floor(x) ||
            (int)floor(other.y) != (int)floor(y) ||
            (int)floor(other.z) != (int)floor(z);
    }

    private boolean reachedTarget() {
        final boolean flag = this.target == null || this.source == this.target || this.current == this.target;
        if (flag)
            resetFaultTimings();
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
            x = (int)floor(coordinates.x),
            z = (int)floor(coordinates.z);

        updateFieldWindow(x, z, x, z, false);

        final Node last = this.current;
        this.current = pointAtSource();
        if (last != null && last != this.current)
            this.trimmedToCurrent = false;
    }

    /**
     * Reset this path-finder to a state suitable for initiating path-finding to some other destination.  This will also
     * purge all previously cached data and reset fault timings.  Call this to recover used memory and abort path-finding,
     * this would be most suitable if the entity is going to stop path-finding and rest for awhile.
     */
    public void reset() {
        this.currentPath = null;
        this.queue.clear();
        this.nodeMap.reset();
        this.unreachableFromSource.clear();
        this.target =
        this.source =
        this.closest = null;
        this.current = null;
        this.trimmedToCurrent = false;
        this.sourcePosition =
        this.destinationPosition = null;
        this.destinationEntity = null;
        this.targetPosition = null;

        resetFaultTimings();
    }

    private void resetFaultTimings() {
        final Random random = this.random;

        this.faultCount = 0;
        this.nextGraphResetFailureCount = FAULT_COUNT_THRESHOLD;
        this.passiblePointPathTimeLimit = PASSIBLE_POINT_TIME_LIMIT.next(random);
        this.nextGraphCacheReset = 0;
    }

    private IPath updatePath(IPath newPath) {
        if (newPath == null)
            return this.currentPath = null;

        final IPath currentPath = this.currentPath;
        if (currentPath != null) {
            if (currentPath.sameAs(newPath))
                newPath = currentPath;
            else if (!currentPath.done() && newPath instanceof PathObject)
                ((PathObject) newPath).adjustPathPosition(currentPath, this.subject);
        }

        if (this.nodeMap.needsOcclusionProvider())
            updateFieldWindow(newPath);

        if (newPath.done()) {
            INode last = newPath.last();
            if (last == null)
                last = pointAtSource();

            return this.currentPath = new IncompletePath(last);
        }

        return this.currentPath = newPath;
    }

    private IPath triage(int iterations) {
        final IPath currentPath = this.currentPath;
        final SortedPointQueue queue = this.queue;

        if (queue.isEmpty())
            if (currentPath == null)
                return null;
            else if (!currentPath.done())
                return currentPath;
            else
                resetTriage();

        if (this.target == null)
            return null;

        IPath nextPath = null;
        boolean trimmedToSource = this.trimmedToCurrent;

        while (!queue.isEmpty() && iterations-- > 0) {
            final Node source = this.current;
            if (!trimmedToSource && !queue.nextContains(source)) {
                if (source != null) {
                    this.queue.trimFrom(source);
                    this.trimmedToCurrent = trimmedToSource = true;
                    iterations++;
                }
                continue;
            }

            final Node
                current = queue.dequeue(),
                closest = this.closest;

            if ((
                    closest == null
                    || closest.orphaned()
                    || Node.squareDelta(current, this.target) < Node.squareDelta(closest, this.target)
                ))
                this.closest = current;

            if (current == target) {
                nextPath = createPath(current);
                if (PathObject.active(nextPath)) {
                    this.queue.clear();
                    break;
                }

                resetTriage();
                if (this.target == null) {
                    nextPath = null;
                    break;
                }
            } else
                processNode(current);
        }

        final Node closest = this.closest;
        if (nextPath == null && closest != null && !queue.isEmpty())
            nextPath = createPath(closest);

        return updatePath(nextPath);
    }

    private IPath createPath(Node head) {
        final IPathingEntity.Capabilities capabilities = this.capabilities;
        final IPath path = PathObject.fromHead(capabilities.speed(), this.random, head);
        if (this.pathProcessor != null)
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

    boolean applyPointOptions(Node current, Node... pointOptions) {
        boolean found = false;
        for (Node alternative : pointOptions) {
            if (impassible(alternative) || alternative.visited() || Node.squareDelta(alternative, this.target) >= this.searchRangeSquared)
                continue;

            found = true;
            alternative.sterilize();
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

    /**
     * The pathing entity this object is associated with
     *
     * @return pathing entity
     */
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

    public PassibilityResult passibilityNear(int tx, int ty, int tz) {
        updateSourcePosition();

        final int
                x = (int)floor(sourcePosition.x),
                z = (int)floor(sourcePosition.z);

        applySubject();
        updateFieldWindow(x, z, tx, tz, false);

        final Node point = this.nodeMap.cachedPassiblePointNear(tx, ty, tz);
        return new PassibilityResult(point.passibility(), point.key);
    }
    
    private static final class NodeBindingsReaderWriter implements LinkableReader<HydrazinePathFinder, Node>, LinkableWriter<HydrazinePathFinder, Node> {
        private final int version;

        public NodeBindingsReaderWriter(int version) {
            this.version = version;
        }

        @Override
        public void readLinkages(HydrazinePathFinder object, ReferableObjectInput<Node> in) throws IOException {
            object.current = in.readRef();
            object.source = in.readRef();
            object.target = in.readRef();
            if (this.version <= 2 || in.readBoolean())
                object.closest = in.readRef();
            else
                object.closest = null;
        }

        @Override
        public void writeLinkages(HydrazinePathFinder object, ReferableObjectOutput<Node> out) throws IOException {
            out.writeRef(object.current);
            out.writeRef(object.source);
            out.writeRef(object.target);
            if (this.version <= 2)
                out.writeRef(object.closest);
            else {
                final boolean present = object.closest != null;
                out.writeBoolean(present);
                if (present)
                    out.writeRef(object.closest);
            }
        }
    }

    private static final class PathReaderWriter implements LinkableReader<HydrazinePathFinder, Node>, LinkableWriter<HydrazinePathFinder, Node> {
        private final PathObject.Reader pathObjectReader;

        public PathReaderWriter(byte version) {
            this.pathObjectReader = PathObject.Reader.forVersion(version);
        }

        @Override
        public void readLinkages(HydrazinePathFinder pathFinder, ReferableObjectInput<Node> in) throws IOException {
            switch (PathType.values()[in.readByte()]) {
                case complete: {
                    final PathObject pathObject = this.pathObjectReader.readPartialObject(in);
                    this.pathObjectReader.readLinkages(pathObject, in);
                    pathFinder.currentPath = pathObject;
                    break;
                }

                case incomplete: {
                    pathFinder.currentPath = new IncompletePath(in.readRef());
                    break;
                }

                case none:
                    pathFinder.currentPath = null;
                    break;
            }
        }

        @Override
        public void writeLinkages(HydrazinePathFinder object, ReferableObjectOutput<Node> out) throws IOException {
            if (object.currentPath instanceof PathObject) {
                out.writeByte(PathType.complete.ordinal());
                final PathObject pathObject = (PathObject) object.currentPath;
                PathObject.Writer.INSTANCE.writePartialObject(pathObject, out);
                PathObject.Writer.INSTANCE.writeLinkages(pathObject, out);
            } else if (object.currentPath instanceof IncompletePath) {
                out.writeByte(PathType.incomplete.ordinal());
                out.writeRef((Node) object.currentPath.current());
            } else if (object.currentPath == null)
                out.writeByte(PathType.none.ordinal());
            else
                throw new IOException("Unhandled type: " + object.currentPath.getClass());
        }
    }
    
    @Override
    public void writeVersioned(byte version, ObjectOutput out) throws IOException {
        final IdentityMapper<Node, Node.ReaderWriter> identities = new IdentityMapper<Node, Node.ReaderWriter>(Node.ReaderWriter.INSTANCE);

        out.writeByte(this.unreachableFromSource.size());
        for (Vec3i coords : this.unreachableFromSource)
            Vec3iReaderWriter.INSTANCE.writePartialObject(coords, out);
        
        MutableVec3dReaderWriter.INSTANCE.writePartialObject(this.sourcePosition, out);
        Vec3dReaderWriter.INSTANCE.writePartialObject(this.targetPosition, out);
        MutableVec3dReaderWriter.INSTANCE.writePartialObject(this.destinationPosition, out);
        DummyDynamicMovableObject.ReaderWriter.INSTANCE.writePartialObject(this.destinationEntity, out);

        out.writeBoolean(this.flying);
        out.writeBoolean(this.aqua);
        out.writeBoolean(this.pathPointCalculatorChanged);
        out.writeBoolean(this.trimmedToCurrent);
        out.writeByte(this.targetingStrategy.ordinal());
        
        out.writeInt(this.initComputeIterations);
        out.writeInt(this.periodicComputeIterations);
        out.writeInt(this.faultCount);
        out.writeInt(this.nextGraphResetFailureCount);
        
        out.writeFloat(this.searchRangeSquared);
        out.writeFloat(this.passiblePointPathTimeLimit);
        out.writeFloat(this.nextGraphCacheReset);
        out.writeFloat(this.actualSize);

        nodeMap.writeTo(out, identities);
        identities.writeLinks(queue, queue, out);
        identities.writeLinks(new NodeBindingsReaderWriter(version), this, out);
        identities.writeLinks(new PathReaderWriter(version), this, out);
    }

    @Override
    public void readVersioned(byte version, ObjectInput in) throws IOException {
        final IdentityMapper<Node, Node.ReaderWriter> identities = new IdentityMapper<Node, Node.ReaderWriter>(Node.ReaderWriter.INSTANCE);

        byte count = in.readByte();
        while (count-- > 0)
            this.unreachableFromSource.add(Vec3iReaderWriter.INSTANCE.readPartialObject(in));

        this.sourcePosition = MutableVec3dReaderWriter.INSTANCE.readPartialObject(in);
        if (version > 1)
            this.targetPosition = Vec3dReaderWriter.INSTANCE.readPartialObject(in);

        com.extollit.linalg.mutable.Vec3d destinationPosition = this.destinationPosition = MutableVec3dReaderWriter.INSTANCE.readPartialObject(in);
        if (version <= 1)
            this.targetPosition = destinationPosition == null ? null : new com.extollit.linalg.immutable.Vec3d(destinationPosition);

        this.destinationEntity = DummyDynamicMovableObject.ReaderWriter.INSTANCE.readPartialObject(in);

        this.flying = in.readBoolean();
        this.aqua = in.readBoolean();
        this.pathPointCalculatorChanged = in.readBoolean();
        this.trimmedToCurrent = in.readBoolean();
        if (version > 1)
            this.targetingStrategy = PathOptions.TargetingStrategy.values()[in.readByte()];
        else
            this.targetingStrategy = in.readBoolean() ? PathOptions.TargetingStrategy.bestEffort : PathOptions.TargetingStrategy.none;

        this.initComputeIterations = in.readInt();
        this.periodicComputeIterations = in.readInt();
        this.faultCount = in.readInt();
        this.nextGraphResetFailureCount = in.readInt();

        this.searchRangeSquared = in.readFloat();
        this.passiblePointPathTimeLimit = in.readFloat();
        this.nextGraphCacheReset = in.readFloat();
        this.actualSize = in.readFloat();

        nodeMap.readFrom(in, identities);
        identities.readLinks(queue, queue, in);
        identities.readLinks(new NodeBindingsReaderWriter(version), this, in);
        identities.readLinks(new PathReaderWriter(version), this, in);
    }
}
