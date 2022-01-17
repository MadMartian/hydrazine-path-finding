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

    private static final Passibility[] VALUES = values();

    /**
     * Retrieves the passibility having the given ordinal value
     *
     * @param index The ordinal value of the passibility to return
     * @return the passibility corresponding to the given ordinal
     */
    public static Passibility of(int index) {
        return VALUES[index];
    }

    /**
     * Renders the least passibility between this and the given passibility rating.  For example, if this is
     * {@link #passible} and the parameter is {@link #risky} then the result is <em>risky</em>.  Also, if this
     * is {@link #dangerous} and the parameter is {@link #risky} then the result is <em>dangerous</em>.
     *
     * @param other other passibility to compare to
     * @return the lesser of the two passibility ratings
     */
    public Passibility between(Passibility other) {
        return VALUES[Math.max(ordinal(), other.ordinal())];
    }

    /**
     * Determines if the given passibility rating is better than this one.  For example, if this is {@link #dangerous}
     * and the parameter is {@link #risky} then the result is <em>risky</em>.
     *
     * @param other other rating to compare with as potentially better than this one
     * @return true if the given passibility rating is better than this one, false otherwise
     */
    public boolean betterThan(Passibility other) {
        return ordinal() < other.ordinal();
    }

    /**
     * Determines if the given passibility rating is worse than this one.  For example, if this is {@link #dangerous}
     * and the parameter is {@link #risky} then the result is <em>dangerous</em>.
     *
     * @param other other rating to compare with as potentially worse than this one
     * @return true if the given passibility rating is worse than this one, false otherwise
     */
    public boolean worseThan(Passibility other) {
        return ordinal() > other.ordinal();
    }

    /**
     * Determines if the entity should path to a node rated this way.  This may return false even if the location rated
     * this way does not physically impede or even harm the entity, it depends on the capabilities of the entity.
     *
     * @param capabilities capabilities of the entity, used to determine if the entity is a cautious path-finder or not
     * @return true if the entity can path to this rating, false if it should not
     */
    public boolean impassible(IPathingEntity.Capabilities capabilities) {
        return this == Passibility.impassible
                || (capabilities.cautious() && worseThan(Passibility.passible));
    }
}
