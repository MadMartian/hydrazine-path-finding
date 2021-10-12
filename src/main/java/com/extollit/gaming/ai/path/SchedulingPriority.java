package com.extollit.gaming.ai.path;

/**
 * Preset scheduling priorities for the co-routine-like behavior of the engine's A* triage process.  This determines
 * how many iterations (per cycle) a path-finding engine instance for an entity dedicates to the A* algorithm.
 *
 * @see IConfigModel.Schedule
 */
public enum SchedulingPriority {
    /**
     * Indicates extreme-priority scheduling, entities with engines configured for this rating complete path-finding the
     * soonest.  While this results in the most fluid and deterministic pathing behavior it is also the most
     * computationally expensive and should only be used for special circumstances.
     * This is initialized with default values:
     *  - 24 initial compute iterations
     *  - 18 subsequent compute iterations
     */
    extreme (24, 18),

    /**
     * Indicates high-priority scheduling, entities with engines configured for this rating complete path-finding sooner.
     * This is initialized with default values:
     *  - 12 initial compute iterations
     *  - 7 subsequent compute iterations
     */
    high    (12, 7),

    /**
     * Indicates medium-priority scheduling, entities with engines configured for this rating will have slightly better
     * accuracy than low priority, particularly during the beginning of a path traversal.
     */
    medium  (7, 3),

    /**
     * Indicates low-priority scheduling, entities with engines configured for this rating complete path-finding later.
     * While the results of pathing for mobs with this scheduling priority can appear erratic or even stupid it is also
     * the least computationally expensive.  This scheduling priority is most suitable for mindless animals.
     * This is initialized with default values:
     *  - 3 initial compute iterations
     *  - 2 subsequent compute iterations
     */
    low     (3, 2);

    protected int initComputeIterations, periodicComputeIterations;

    SchedulingPriority(int initComputeIterations, int periodicComputeIterations) {
        this.initComputeIterations = initComputeIterations;
        this.periodicComputeIterations = periodicComputeIterations;
    }

    /**
     * Used to configure the co-routine-like compute cycles for each of these priority ratings.
     *
     * @param IConfigModel source containing the appropriate configuration parameters
     * @see IConfigModel#scheduleFor(SchedulingPriority)
     */
    public static void configureFrom(IConfigModel IConfigModel) {
        for (SchedulingPriority priority : SchedulingPriority.values()) {
            final IConfigModel.Schedule schedule = IConfigModel.scheduleFor(priority);
            priority.initComputeIterations = schedule.init;
            priority.periodicComputeIterations = schedule.period;
        }
    }
}
