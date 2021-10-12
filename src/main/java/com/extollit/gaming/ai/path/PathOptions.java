package com.extollit.gaming.ai.path;

import com.extollit.linalg.immutable.Vec3d;

/**
 * This class is used to control path-finding behaviour on a per-operation basis.  It is passed to the path-finding object
 * upon initiating path-finding for a pathing entity.  This approach is meant to be independent from pathing-entity-specific
 * configuration and capabilities.
 *
 * This class follows the builder pattern, which conveniently employs method chaining:
 * <pre>
 *     final PathOptions myPathOptions =
 *          new PathOptions()
 *              .targetingStrategy(PathOptions.TargetingStrategy.bestEffort)
 * </pre>
 *
 * NOTE: For performance reasons it is recommended to maintain static objects of this class rather than building a new
 * configured instance upon each and every call.
 *
 * @see HydrazinePathFinder#initiatePathTo(Vec3d, PathOptions)
 * @see HydrazinePathFinder#initiatePathTo(double, double, double, PathOptions)
 */
public class PathOptions {
    /**
     * This is a way to dictate how a pathing entity should resolve its final destination if it is not strictly reachable.
     * Since often a desired target destination is arbitrary and random (because choosing a perfect target destination
     * depends upon path-finding in and of itself) a target destination is often unreachable or impassible (e.g. a target
     * that is up in the middle of the air is unreachable for a walking non-flying pathing entity).  A targeting strategy
     * can be employed to determine what to do in such cases.
     */
    public enum TargetingStrategy {
        /**
         * If the destination position is unreachable the pathing-entity will still attempt to get as close as possible
         * to it.  This strategy is less computationally intensive than {@link #gravitySnap} and is most appropriate for
         * path-finding operations where accuracy is not important.
         */
        bestEffort,

        /**
         * If the destination position is unreachable the destination position is recomputed to the nearest passible
         * point below instead.  Use this strategy to mitigate erratic path-finding behaviour for unreachable destinations.
         */
        gravitySnap,

        /**
         * If the destination position is unreachable then path-finding will not commence and the path-finder will
         * not return a path.  This is the least computationally intensive strategy and is most appropriate where
         * path-finding must strictly adhere to the destination requested.
         */
        none
    }

    static final PathOptions
        BEST_EFFORT = new PathOptions().targetingStrategy(TargetingStrategy.bestEffort),
        NONE = new PathOptions().targetingStrategy(TargetingStrategy.none);

    private TargetingStrategy targetingStrategy = TargetingStrategy.none;

    /**
     * Configure with the specified targeting strategy
     *
     * @param targetingStrategy targeting strategy to use for this configuration
     * @return this (builder pattern)
     */
    public PathOptions targetingStrategy(TargetingStrategy targetingStrategy) {
        this.targetingStrategy = targetingStrategy;
        return this;
    }

    /**
     * Retrieve the configured targeting strategy
     * @return the configured targeting strategy
     */
    public TargetingStrategy targetingStrategy() { return this.targetingStrategy; }
}
