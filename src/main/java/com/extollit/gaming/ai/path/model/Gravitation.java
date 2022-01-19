package com.extollit.gaming.ai.path.model;

/**
 * For traversing a path to a particular point, this indicates whether an entity should walk, fly or swim to the point
 */
public enum Gravitation {
    /**
     * The associated point is on the ground
     */
    grounded,

    /**
     * The associated point is in fluid and not on the ground, a change in buoyancy is required to traverse to this point
     */
    buoyant,

    /**
     * The associated point is in the air and the entity must fly to it
     */
    airborne;

    private static final Gravitation[] VALUES = values();

    /**
     * Determines the greatest gravitation restriction between this and the passed parameter.  For example, if
     * this is {@link #buoyant} and the parameter is {@link #grounded} then the result is <em>grounded</em>.  Also,
     * if this is {@link #airborne} and the parameter is {@link #buoyant} then the result is <em>buoyant</em>.
     *
     * @param other the other gravitation rating to compare with this one
     * @return the more restrictive gravitation between this and the parameter
     */
    public Gravitation between(Gravitation other) {
        return VALUES[Math.min(ordinal(), other.ordinal())];
    }
}
