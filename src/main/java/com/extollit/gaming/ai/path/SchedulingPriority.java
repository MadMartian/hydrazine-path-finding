package com.extollit.gaming.ai.path;

/**
 * Preset scheduling priorities for the co-routine-like behavior of the engine's A* triage process.  This determines
 * how many iterations (per cycle) a path-finding engine instance for an entity dedicates to the A* algorithm.
 *
 * @see IConfigModel.Schedule
 */
public enum SchedulingPriority {
    /**
     * Indicates high-priority scheduling, entities with engines configured for this rating complete path-finding sooner.
     * This is initialized with default values:
     *  - 12 initial compute iterations
     *  - 7 subsequent compute iterations
     */
    high    (12, 7),

    /**
     * Indicates low-priority scheduling, entities with engines configured for this rating complete path-finding later.
     * This is initialized with default values:
     *  - 6 initial compute iterations
     *  - 4 subsequent compute iterations
     */
    low     (6, 4);

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
