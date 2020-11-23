package com.extollit.gaming.ai.path;

import com.extollit.num.FloatRange;

/**
 * Data abstraction used for globally configuring (once per class-loader) the Hydrazine path-finding engine.  It contains
 * configurations for influencing the co-routine-like scheduling of the A* triage process, timeouts for cache invalidation
 * to aid entities toward their destinations, and some gravity-related limits for all pathing entities.
 *
 * Some of the properties here use a term called "path time" which is a time value relative to the pathing entity's
 * dynamic movement speed.  A single unit of path time is the time it takes (in server ticks) for the pathing entity to
 * move one block with its current movement speed.  Properties measured in path time are typically used to aid the engine
 * in determining whether the pathing entity is stuck and countermeasures necessary to get it unstuck.
 */
public interface IConfigModel {
    /**
     * This is the maximum number of blocks an entity may fall safely without incurring any damage from falling
     * (independent of any other hazards)
     *
     * @return the maximum fall distance measured in blocks
     */
    short safeFallDistance();

    /**
     * This is the maximum number of blocks an entity may fall without dying from incurred falling damage
     * (independent of any other hazards).  Entities that fall further than the safe falling distance but less than
     * or equal to the survival falling distance will incur at least some damage from falling.
     *
     * @return the maximum survive fall distance measured in blocks.
     * @see #surviveFallDistance()
     */
    short surviveFallDistance();

    /**
     * Inspired by a term from the Professional Association of Diving Instructors (PADI), the CESA limit
     * (Controlled Emergency Swimming Ascent) here is the maximum number of blocks an entity submerged in fluid
     * (typically water) would search upward for a passible point at the surface before the path-finding engine
     * writes-off the entity as drowning and chooses a path-point along the bottom instead.
     *
     * @return the preconfigured CESA limit for saving drowning entities out of air.
     */
    short cesaLimit();

    /**
     * This is used by an entity's path-finding engine instance to determine cache invalidation and recalculation of a
     * target point.  In general, it expresses an angular variance between the pathing entity's starting point,
     * it's current target, and it's destination.
     *
     * During path-finding, the engine often chooses a target suitably close to the destination.  The destination may be
     * an entity that is constantly on the move.  When the angle between this destination entity and the pathing entity
     * is too great, then the triage is reset, a new target is determined, and a new path is computed.  Refer to the
     * ASCII art diagram for an example of this angular variance:
     *
     * s------e----t
     *         \
     *          \
     *           \
     *            d
     *
     * In the above example, the formula would be: (s -&gt; t) dot (s -&gt; d) < threshold
     *
     * @return the threshold value the dot product between the destination position and the current target with the
     *         initial source position must be less than for an A* triage reset to occur.
     */
    float dotThreshold();

    /**
     * The minimum and maximum amount of path time that an entity may remain 'stuck' at a point along its path
     * until the engine increments the failure counter.  A random value in this range is picked each time a path-finding
     * session is reset.
     *
     * @return minium and maximum path time period before stuck before the entity's engine increases its failure count.
     * @see #faultCountThreshold()
     */
    FloatRange passiblePointTimeLimit();

    /**
     * The number of faults permitted before a pathing entity's engine proceeds to perform additional steps to aid the
     * entity along its path (typically cache invalidation).  The engine's probationary time-limit must have also been
     * exceeded, so it is possible that the actual failure count may be higher than this threshold depending on the
     * situation.
     *
     * @return the pre-configured fault count
     * @see #passiblePointTimeLimit()
     */
    byte faultCountThreshold();

    /**
     * The maximum number of faults permitted before a path-finding aborts for the current entity.  This will result in
     * the path-engine for the respective entity to return null when requested for an updated path.  Callers must call
     * to initiate a new path and/or reset the path-engine object to continue path-finding again.
     *
     * @return the pre-configured maximum fault count
     * @see #passiblePointTimeLimit()
     * @see HydrazinePathFinder#update()
     * @see HydrazinePathFinder#initiatePathTo(double, double, double)
     * @see HydrazinePathFinder#reset()
     */
    int faultLimit();

    /**
     * The minimum and maximum required path time that an entity may remain in failure state
     * (at least one failure count) before a pathing entity's engine proceeds to perform additional states to aid the
     * entity along its path (typically cache invalidation).  The engine's failure count must also have exceeded
     * its threshold, so it is possible that the actual time may be higher than this maximum depending on the situation.
     *
     * A random value in this range is picked each time a pathing entity enters failure state (transitions from having
     * a zero failure count to a failure count of one).
     *
     * @return the pre-configured minimum and maximum possible time-limit in path time units
     * @see #faultCountThreshold()
     */
    FloatRange probationaryTimeLimit();

    /**
     * Hydrazine tries to optimize pathing by telling entities to move to the furthest path point in a path from its
     * current position that is a straight and continuous direct line.  Sometimes entities will still get stuck because
     * Hydrazine only checks for taxi-cab clearance between path points, it's reasonable to assume (for most cases)
     * that if the taxi-cab path is clear then the associated direct line covering them is also clear.  Furthermore,
     * this assumption is made in real-life scenarios as well and suffers the same erroneous (and acceptable) judgement,
     * which would be even more pronounced for dumb creatures such as monsters or animals.
     *
     * This property is the minimum and maximum path time that an entity may remain stuck at some path-point until
     * the engine removes the direct path shortcut and tells the entity to move to the path point immediately adjacent
     * to its current position instead.
     *
     * The time an entity has been stuck is relative, it is taken from the age of the entity and scaled by its path
     * movement speed.
     *
     * @return the pre-configured minimum and maximum time-limit in path time units to cope with direct-line
     *          optimization errors
     */
    FloatRange directLineTimeLimit();

    /**
     * Determines the dynamically configured co-routine-like schedule for the given priority rating.
     *
     * @param priority the requested priority
     * @return an A* triage schedule for the given priority
     */
    Schedule scheduleFor(SchedulingPriority priority);

    /**
     * A data class used to configure the co-routine-like behavior of the A* triage process.
     */
    final class Schedule {

        /**
         * Number of cycles to dedicate to path-finding triage when a path-finding operation is first initiated for an
         * entity.
         */
        public final int init;

        /**
         * Number of cycles to dedicate to path-finding triage for each sub-sequent cycle after a path-finding operation
         * has been initiated for an entity.
         */
        public final int period;

        public Schedule(int init, int period) {
            this.init = init;
            this.period = period;
        }
    }
}
