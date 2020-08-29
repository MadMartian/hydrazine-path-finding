package com.extollit.gaming.ai.path.model;

import com.extollit.gaming.ai.path.IConfigModel;
import com.extollit.linalg.immutable.Vec3d;

/**
 * Abstraction for an entity that requires path-finding support.
 */
public interface IPathingEntity extends IDynamicMovableObject {
    /**
     * Ticks since the entity was spawned / created.  This is typically one server cycle, in the Notchian implementation
     * there are twenty ticks per second.
     *
     * This is used to determine how long a pathing entity has been stuck at the same spot to signal to the engine
     * whether some cache invalidation is necessary to aid the entity toward its target.  If this value exceeds a
     * pre-configured threshold then the engine proceeds to perform some cache invalidation.
     *
     * @return age of the entity in ticks
     * @see IConfigModel#passiblePointTimeLimit()
     */
    int age();

    /**
     * The maximum path-finding range for the entity.  Although the current engine is limited to computing paths no more
     * than 32 blocks in length, if this value is greater than that then the engine will attempt to compute a path toward
     * the destination and refine periodically.
     *
     * @return the maximum search distance for the entity to path-find measured in blocks
     */
    float searchRange();

    /**
     * The pathing capabilities of this entity.  This method is only called once per path-finding entity per path-finding
     * session.  If an entity's path is cancelled and a new one requested then this property is retrieved again.
     *
     * @return the pathing capabilities of this entity
     */
    Capabilities capabilities();

    /**
     * Given a valid path, this is called by the path-finding engine to tell this entity to move in a continuous line from
     * its current position to the specified position.  This may be an adjacent block (no more than one block away) or a
     * distant block if the engine determines that the area between the entity's current position and the target position
     * is clear and safe to traverse.
     *
     * @param position absolute (relative to the instance) target position to move toward
     * @param passibility the passibility of getting to this location (other than {@link Passibility#impassible})
     * @param gravitation whether the entity must walk, fly or swim to this location
     */
    void moveTo(Vec3d position, Passibility passibility, Gravitation gravitation);

    /**
     * Expresses the movement capabilities of an entity, the engine loads these flags once per path-finding session and
     * uses them to rate path-point candidates.  This structure is yielded by pathing entities.
     *
     * @see IPathingEntity
     */
    interface Capabilities {
        /**
         * The absolute movement speed of the entity as it paths from block to block measured in blocks per tick.  For
         * example, if it takes an entity approximately one second to pass from one block to another then this value will
         * be ~0.05.  This value is used by the path-finding engine to help it determine if an entity is stuck and not
         * progressing along their path.
         *
         * @return the blocks per tick land movement speed of the pathing entity
         */
        float speed();

        /**
         * Can this entity navigate through fire or lava or other burning / high-heat blocks without sustaining any damage?
         *
         * @return true if this mob is fire resistant
         */
        boolean fireResistant();

        /**
         * Is this entity daring or cautious? does it take chances in order to reach its target?  This flag typically
         * applies to animals and not to hostile mobs.
         *
         * An entity that is cautious will only use pristine high-quality "passible" paths.  Paths that contain points
         * rated as either "risky", "dangerous" or "impassible" shall all be treated as "impassible" by this entity.
         *
         * @return true if this entity cannot take any risks during path-finding.
         * @see Passibility
         */
        boolean cautious();

        /**
         * Can this entity climb ladders or vines?
         *
         * @return true if this entity should attempt to path up ladders or vines
         */
        boolean climber();

        /**
         * Is this entity capable of swimming?  This flag determines where the engine looks for path-points through fluid
         * for the entity.  It also influences the rating of path points discovered in fluid.  Fluid path-points for swimmers
         * are not considered dangerous (unless it's lava and the entity is not fire resistant, then it's impassible).
         *
         * Type of entities that are not swimmers are typically the following:
         * - Puppies
         * - Kittens
         * - Villager golems
         *
         * @return true if the entity can swim and typically survive in fluid, false if the entity always drowns in fluid.
         */
        boolean swimmer();

        /**
         * Is this entity capable of breathing under water?  This flag determines whether path-finding should keep the entity
         * above the fluid surface or whether it can path beneath and within fluid.  If the entity is also a swimmer, then
         * the engine assumes the entity has full control of their buoyancy and can path-find like a fish.
         *
         * It is theoretically possible to have entities that are aquatic but are not swimmers, these entities would path
         * along the floor of the body of water / magma rather than swim like fish or dog-paddle like dogs.         *
         *
         * @return true if the entity can breathe and survive indefinitely within fluid, false if the entity cannot last
         * for long within fluid
         */
        boolean aquatic();

        /**
         * Is this entity capable of flying?  This flag alters engine behavior to select points anywhere in the air and
         * not necessarily immediately above ground.
         *
         * Type of entities that are flyers are birds and bats whereas fish and sharks are NOT considered flying entities
         *
         * @return true if the entity can fly, false if the entity cannot move freely through the air defying gravity
         */
        boolean avian();

        /**
         * Does this entity avoid water and other fluids?  This also influences the rating of path points discovered in fluid.
         * For aquaphobic entities, fluid path points are considered dangerous.  Entities that are aquaphobic (avoids water)
         * will typically look for a way around the fluid (unless there is no other alternative).
         *
         * It is important to note that this flag does not imply that the entity cannot swim, rather that the entity should
         * void having to.  The type of entity that is a swimmer and not aquaphobic would be a fish or a whale.
         *
         * @return true if the entity should avoid pathing through fluid.
         */
        boolean aquaphobic();

        /**
         * This flag is used only by villagers in the Notchian implementation, it tells the path-finding engine whether to
         * consider open doorways as passible.  When a villager is under threat it should stay indoors and not attempt to path
         * outside where the threat exists.  However, they should still be able to wander around inside the room where they are
         * situated.
         *
         * @return true if this entity should treat open doorways as impassible
         */
        boolean avoidsDoorways();

        /**
         * Does this entity either open doors carefully or smash them down forcefully?  In the Notchian implementation,
         * villagers open doors carefully and zombies knock doors down.  This flag typically only applies to wooden doors
         * since iron doors require a button, lever or other redstone mechanism and that would involve AI outside the scope
         * of Hydrazine path-finding engine.
         *
         * @return true if this entity should treat closed (typically wooden) doors as passible
         */
        boolean opensDoors();
    }
}
