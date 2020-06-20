package com.extollit.gaming.ai.path.model;

/**
 * Abstraction for a world instance capable of yielding smaller virtual units of space.  It also yields occlusion
 * fields representing those spaces.
 *
 * Many of these functions use chunk coordinates, which are at intervals of 16 blocks (cx = x >> 4).
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
     *  Looks-up the occlusion field at the specified chunk coordinates if it has been initialized.
     *  Occlusion fields ought to be lazy-loaded and only populated when path-finding is requested through
     *  those areas using an occlusion provider.
     *
     * @param cx x chunk coordinate
     * @param cy y chunk coordinate, no less than 0, no more than 15
     * @param cz z chunk coordinate
     * @return the pre-existing occlusion field at the aforementioned chunk coordinates, null otherwise
     * @see IOcclusionProvider
     */
    OcclusionField optOcclusionFieldAt(int cx, int cy, int cz);

    /**
     * Produces a facade to an array of occlusion fields defined by the specified chunk coordinate range.
     * This is used for computing a path through an instance, as information is requested the provider should
     * initialize / load the respective occlusion fields.
     *
     * In the Notchian implementation, a world (instance) is sub-divided into columnar chunk objects that each contain
     * all the block information in a 16x16 block column along the y-axis at some x/z coordinates in the world.  This is
     * based on that approach, while occlusion fields along the x and z axis are bounded, those along the y-axis are not.
     *
     * @param cx0 Minimum x chunk coordinate of the bounded range
     * @param cz0 Minimum z chunk coordinate of the bounded range
     * @param cxN Maximum x chunk coordinate of the bounded range
     * @param czN Maximum z chunk coordinate of the bounded range
     * @return the populated occlusion field representing the chunk at the aforementioned chunk coordinates
     */
    IOcclusionProvider occlusionProviderFor(int cx0, int cz0, int cxN, int czN);
}
