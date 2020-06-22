package com.extollit.gaming.ai.path.model;

/**
 * Abstraction for a world instance capable of yielding smaller virtual units of space aligned along the y-axis (i.e. columns).
 * An implementor would typically modify their own class topology to support these contracts rather than introduce new types
 * for the sake of simplicity.
 *
 * Many of these functions use chunk coordinates, which are at intervals of 16 blocks (cx = x >> 4).
 *
 * @see IColumnarSpace
 * @see IBlockObject
 */
public interface IInstanceSpace {
    /**
     * Yields a comprehensive block object at the specified coordinates in the instance.  This is called only
     * as a last resort by the engine if it cannot obtain sufficient information regarding the passibility of a block
     * from the occlusion field provider.
     *
     * @param x x-coordinate
     * @param y y-coordinate
     * @param z z-coordinate
     * @return an object identifying and describing the block at the aforementioned coordinates
     */
    IBlockObject blockObjectAt(int x, int y, int z);

    /**
     * Retrieves the columnar space at the specified chunk coordinates, if it is loaded and ready.
     * If the columnar space is not loaded and not completely initialized at these coordinates then the return value
     * must be null.
     *
     * This is used by the engine when path-finding is requested in this area.  If the columnar space is not
     * available then the engine considers this area completely solid and is excluded from the A* point graph.
     *
     * @param cx x chunk coordinate
     * @param cz z chunk coordinate
     * @return the columnar space corresponding to the specified chunk coordinates or null if it is not available
     */
    IColumnarSpace columnarSpaceAt(int cx, int cz);
}
