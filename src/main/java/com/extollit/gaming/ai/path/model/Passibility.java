package com.extollit.gaming.ai.path.model;

/**
 * Expresses ratings for traversal into a particular path-point according to increasing risk.
 *
 * This is used to rate path points visited during A* triage according to the type of block.
 */
public enum Passibility {
    /**
     * Pristine, fully-passible, no risk to the entity
     */
    passible,

    /**
     * Mild risk pathing into this point, it could be close to lava or through water.
     */
    risky,

    /**
     * High risk pathing into this point, these points are usually over cliffs or on-fire
     */
    dangerous,

    /**
     * Impossible (or completely impractical) pathing into this point, usually impeded by collision bounds of
     * the block.  This also applies to lava since the chances of survival pathing through even one block of
     * lava (when not fire-resistant) is effectively zero.
     */
    impassible;

    public Passibility between(Passibility other) {
        return values()[Math.max(ordinal(), other.ordinal())];
    }
    public boolean betterThan(Passibility other) {
        return ordinal() < other.ordinal();
    }
    public boolean worseThan(Passibility other) {
        return ordinal() > other.ordinal();
    }

    public boolean impassible(IPathingEntity.Capabilities capabilities) {
        return this == Passibility.impassible
                || (capabilities.cautious() && worseThan(Passibility.passible));
    }
}
