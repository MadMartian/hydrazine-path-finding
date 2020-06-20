package com.extollit.gaming.ai.path.model;

import com.extollit.linalg.immutable.AxisAlignedBBox;

/**
 * Description of a block at a specific location in an instance, this is a more comprehensive type than the
 * super interface IBlockDescription.  The engine uses this as a last resort to determine passibility with
 * certain complex blocks.
 *
 * A block object at a specific location in an instance may have dynamic collision bounds that depend on its
 * state.  That is why this type is necessary.
 */
public interface IBlockObject extends IBlockDescription {
    /**
     * The maximum collision bounds of the block, pathing entities should remain outside of these bounds.  This method
     * only applies to blocks that are impeding, it is not called for blocks that do not impede.
     *
     * This represents the superset of the actual collision bounds, which may be more complex and/or include more than
     * one bounded region and not necessarily axis-aligned.  This is used by the engine to determine which direction an
     * entity can path from if the entity is in this block based on its coordinates.
     * It is primarily inspired by the phenomenon of animals and creatures getting stuck at fences, which have partial
     * collision bounds.
     *
     * @return The maximum axis-aligned bounds for the block, must be non-null
     * @see IBlockDescription#isImpeding()
     */
    AxisAlignedBBox bounds();
}
