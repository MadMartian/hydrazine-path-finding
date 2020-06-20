package com.extollit.gaming.ai.path.model;

import java.util.Iterator;

/**
 * Represents a logical columnar division of space 16x16 blocks in size on the x/z plane with full extent along the y-axis
 */
public interface IColumnarSpace {
    /**
     * Returns a basic description of the block located at the specified coordinates relative to the columnar space.
     *
     * This is only the basic block description, the result is never downcasted to a {@link IBlockObject} object by
     * the engine.
     *
     * @param x relative x-coordinate
     * @param y relative y-coordinate
     * @param z relative z-coordinate
     * @return invariant concrete type of {@link IBlockDescription} at the aforementioned relative coordinates
     */
    IBlockDescription blockAt(int x, int y, int z);

    /**
     * Returns the Notchian meta-data of the block located at the specified coordinates relative to the columnar space.
     *
     * This is currently not used.
     *
     * @param x relative x-coordinate
     * @param y relative y-coordinate
     * @param z relative z-coordinate
     * @return Notchian block meta-data nibble for the block at the aforementioned relative coordinates
     */
    int metaDataAt(int x, int y, int z);

    /**
     * Retrieves the occlusion field located at the specified absolute chunk coordinates (relative to the instance,
     * not the columnar space).  Populates the field from block data if not yet loaded.
     *
     * @param cx absolute chunk x-coordinate
     * @param cy absolute chunk y coordinate
     * @param cz absolute chunk z-coordinate
     * @return the occlusion field at the specified absolute chunk coordinates in the parent instance.
     */
    OcclusionField occlusionFieldAt(int cx, int cy, int cz);

    /**
     * Retrieves the occlusion field located at the specified y chunk coordinate in the columnar space.  If the
     * field is not yet loaded at that chunk coordinate then no work is done.
     *
     * @param cy absolute y chunk coordinate of the field to retrieve
     * @return a pre-existing occlusion field in the columnar space at the aforementioned y chunk coordinate or null if
     *         there is not one yet loaded there
     */
    OcclusionField optOcclusionFieldAt(int cy);

    /**
     * Provides an iterator of all the loaded / populated occlusion fields in a columnar space.
     *
     * @return iterator of pre-existing occlusion fields in the columnar space
     */
    Iterator<OcclusionField> iterateOcclusionFields();

    /**
     * Each columnar space belongs to an instance, this is its parent.
     *
     * @return parent instance that contains this columnar space
     */
    IInstanceSpace instance();
}
