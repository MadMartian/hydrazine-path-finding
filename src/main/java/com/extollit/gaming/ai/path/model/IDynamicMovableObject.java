package com.extollit.gaming.ai.path.model;

import com.extollit.linalg.immutable.Vec3d;

/**
 * Abstraction for entities, this could potentially be a covariant pathing entity or an entity that a pathing
 * entity is following or pathing to.
 *
 * @see IPathingEntity
 */
public interface IDynamicMovableObject {
    /**
     * Absolute (relative to the instance) three-dimensional coordinates of the entity in the instance it is hosted
     * measured in blocks.
     *
     * Since entities take-up three-dimensional space themselves, these coordinates must meet the following conditions
     * for effective path-finding operation:
     * - The x and z components must point to the center of the bounding box of the entity.  In other words, they must
     *   be offset by half the value of {@link #width()} from the minimum x and z extent of the entity's bounding
     *   region.
     * - The y-component must point to the minimum y-extent of the entity's bounding region.
     *
     * @return a three-dimensional double floating-point vector of the entity's position.
     */
    Vec3d coordinates();

    /**
     * The width and depth of the entity along the x and z axes measured in blocks.
     *
     * This value must match the space occupied by the bounding box of the entity on the x/z plane along either axis.
     *
     * @return width or depth of the entity
     */
    float width();

    /**
     * The height of the entity along the y-axis measured in blocks.
     *
     * This value must match the space occupied by the bounding box of the entity along the y-axis.
     *
     * @return height of the entity
     */
    float height();
}
