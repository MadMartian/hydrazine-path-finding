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

    public Gravitation between(Gravitation other) {
        return values()[Math.min(ordinal(), other.ordinal())];
    }

    public static Gravitation from(byte flags) {
        if (Element.earth.in(flags))
            return grounded;
        if (Element.water.in(flags))
            return buoyant;
        if (Element.air.in(flags))
            return airborne;

        return grounded;
    }
}
